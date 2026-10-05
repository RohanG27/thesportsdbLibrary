package sportsdb

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import sportsdb.cache.InMemoryResponseCache
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Retries, rate limits, errors, redaction and caching. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RequesterTest {
    private val paidKey = "5550001234"
    private val ok = """{"sports":[{"idSport":"102","strSport":"Soccer"}]}"""

    @Test fun retriesServerErrorsThenSucceeds() = runTest {
        val t = FakeTransport().respond("oops", 503).respond("oops", 502).respond(ok)
        assertEquals(1, client(t).v1.list.sports().size)
        assertEquals(3, t.requests.size)
    }

    @Test fun givesUpAfterMaxRetries() = runTest {
        val t = FakeTransport { sportsdb.http.HttpResponse(500, "down") }
        val e = assertFailsWith<HttpStatusException> { client(t).v1.list.sports() }
        assertEquals(500, e.status)
        assertEquals(3, t.requests.size) // 1 + maxRetries(2)
    }

    @Test fun networkErrorsAreRetriedAndWrapped() = runTest {
        val t = FakeTransport().fail(IOException("reset")).respond(ok)
        assertEquals(1, client(t).v1.list.sports().size)

        val down = FakeTransport { throw IOException("no route") }
        assertFailsWith<NetworkException> { client(down).v1.list.sports() }
    }

    @Test fun rateLimitWaitsForRetryAfterThenRetriesOnce() = runTest {
        val t = FakeTransport().respond("", 429, mapOf("Retry-After" to "7")).respond(ok)
        client(t).v1.list.sports()
        assertEquals(7_000, currentTime)
        assertEquals(2, t.requests.size)

        val always = FakeTransport { sportsdb.http.HttpResponse(429, "") }
        val e = assertFailsWith<RateLimitException> { client(always).v1.list.sports() }
        assertEquals(2, always.requests.size)
        assertEquals(null, e.retryAfter)
    }

    @Test fun rateLimitRetryCanBeDisabled() = runTest {
        val t = FakeTransport().respond("", 429, mapOf("Retry-After" to "30"))
        val e = assertFailsWith<RateLimitException> { client(t) { retryOnRateLimit = false }.v1.list.sports() }
        assertEquals(30.seconds, e.retryAfter)
        assertEquals(1, t.requests.size)
    }

    @Test fun v1KeyNeverAppearsInErrors() = runTest {
        val t = FakeTransport { sportsdb.http.HttpResponse(404, "nope") }
        val e = assertFailsWith<HttpStatusException> { client(t, paidKey).v1.lookup.team(1) }
        assertFalse(paidKey in e.message!!, e.message)
        assertTrue("/api/v1/json/***/lookupteam.php?id=1" in e.message!!, e.message)

        val bad = FakeTransport().respond(fixture("v1-free/invalid_key.json"), 400)
        val e2 = assertFailsWith<InvalidApiKeyException> { client(bad, paidKey).v1.lookup.team(1) }
        assertFalse(paidKey in e2.message!!)

        val net = FakeTransport { throw IOException("boom") }
        val e3 = assertFailsWith<NetworkException> { client(net, paidKey).v1.lookup.team(1) }
        assertFalse(paidKey in e3.message!!)
    }

    @Test fun unknownApiMessage() = runTest {
        val t = FakeTransport().respond("""{"Message":"Something new"}""")
        val e = assertFailsWith<ApiMessageException> { client(t).v1.list.sports() }
        assertEquals("Something new", e.apiMessage)
    }

    @Test fun garbageBody() = runTest {
        val t = FakeTransport().respond("<html>Cloudflare</html>")
        assertFailsWith<ResponseParseException> { client(t).v1.list.sports() }
    }

    @Test fun recordKeyFallback() = runTest {
        // If TheSportsDB renames a record key, any array-valued key is still found.
        val t = FakeTransport().respond("""{"renamed":[{"idSport":"1","strSport":"X"}]}""")
        assertEquals("X", client(t).v1.list.sports().single().name)
    }

    @Test fun cacheServesRepeatsAndKeepsKeysOut() = runTest {
        val cache = InMemoryResponseCache()
        val t = FakeTransport().respond(ok)
        val c = client(t, paidKey) { this.cache = cache }
        c.v1.list.sports()
        c.v1.list.sports()
        assertEquals(1, t.requests.size)
        assertEquals(1, cache.size)
    }

    @Test fun liveScoresAreNotCachedByDefault() = runTest {
        val cache = InMemoryResponseCache()
        val body = """{"livescore":[{"idLiveScore":"1"}]}"""
        val t = FakeTransport().respond(body).respond(body)
        val c = client(t) { this.cache = cache }
        c.v1.live.sport("Soccer")
        c.v1.live.sport("Soccer")
        assertEquals(2, t.requests.size)
    }

    @Test fun emptyBodiesAreNotCached() = runTest {
        val cache = InMemoryResponseCache()
        val t = FakeTransport().respond("").respond(ok)
        val c = client(t) { this.cache = cache }
        assertTrue(c.v1.list.sports().isEmpty())
        assertEquals(1, c.v1.list.sports().size)
    }

    @Test fun userAgentIsSent() = runTest {
        val t = FakeTransport().respond(ok)
        client(t).v1.list.sports()
        assertEquals("sportsdb-kotlin", t.requests.single().second["User-Agent"])
    }

    @Test fun aSilentServerTimesOut() {
        // Accepts the connection but never answers: the call must give up, not hang.
        java.net.ServerSocket(0).use { server ->
            val c = SportsDbClient {
                baseUrl = "http://127.0.0.1:${server.localPort}"
                timeout = 300.milliseconds
                maxRetries = 0
                requestsPerMinute = 0
            }
            val started = System.nanoTime()
            assertFailsWith<NetworkException> { kotlinx.coroutines.runBlocking { c.v1.list.sports() } }
            assertTrue((System.nanoTime() - started) / 1_000_000 < 5_000)
        }
    }
}
