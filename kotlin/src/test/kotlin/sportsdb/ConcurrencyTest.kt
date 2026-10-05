package sportsdb

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import sportsdb.cache.InMemoryResponseCache
import sportsdb.http.HttpResponse
import sportsdb.http.HttpTransport
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** De-duplication of identical calls in flight, and the request listener. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConcurrencyTest {
    private val sports = """{"sports":[{"idSport":"102","strSport":"Soccer"}]}"""

    /** Holds every request until [release] is called. */
    private class GatedTransport(private val response: () -> HttpResponse) : HttpTransport {
        val gate = CompletableDeferred<Unit>()
        val urls = CopyOnWriteArrayList<HttpUrl>()
        fun release() = gate.complete(Unit)
        override suspend fun get(url: HttpUrl, headers: Map<String, String>): HttpResponse {
            urls += url
            gate.await()
            return response()
        }
    }

    private fun clientWith(transport: HttpTransport, configure: SportsDbConfig.() -> Unit = {}) = SportsDbClient {
        this.transport = transport
        requestsPerMinute = 0
        retryBackoff = kotlin.time.Duration.ZERO
        configure()
    }

    @Test fun identicalCallsShareOneRequest() = runTest {
        val t = GatedTransport { HttpResponse(200, sports) }
        val events = CopyOnWriteArrayList<RequestEvent>()
        val c = clientWith(t) { requestListener = RequestListener { events += it } }

        val calls = List(10) { async { c.v1.list.sports() } }
        runCurrent()
        assertEquals(1, t.urls.size)
        t.release()
        val results = calls.awaitAll()

        assertTrue(results.all { it == results.first() })
        assertEquals(10, events.size)
        assertEquals(1, events.count { !it.shared && it.attempts == 1 })
        assertEquals(9, events.count { it.shared && it.attempts == 0 && it.status == 200 })
    }

    @Test fun differentCallsAreNotMerged() = runTest {
        val t = GatedTransport { HttpResponse(200, sports) }
        val c = clientWith(t)
        val a = async { c.v1.list.sports() }
        val b = async { c.v1.lookup.team(1) }
        runCurrent()
        assertEquals(2, t.urls.size)
        t.release()
        a.await(); b.await()
    }

    @Test fun deduplicationCanBeTurnedOff() = runTest {
        val t = GatedTransport { HttpResponse(200, sports) }
        val c = clientWith(t) { deduplicateRequests = false }
        val calls = List(3) { async { c.v1.list.sports() } }
        runCurrent()
        assertEquals(3, t.urls.size)
        t.release()
        calls.awaitAll()
    }

    @Test fun errorsReachEveryWaiter() = runTest {
        val t = GatedTransport { HttpResponse(404, "nope") }
        val c = clientWith(t)
        val calls = List(3) { async { runCatching { c.v1.list.sports() } } }
        runCurrent()
        t.release()
        val results = calls.awaitAll()
        assertEquals(1, t.urls.size)
        assertTrue(results.all { it.exceptionOrNull() is HttpStatusException })
    }

    @Test fun aCancelledFirstCallerHandsOverToAWaiter() = runTest {
        val t = GatedTransport { HttpResponse(200, sports) }
        val c = clientWith(t)
        val first = async { c.v1.list.sports() }
        runCurrent()
        val second = async { c.v1.list.sports() }
        runCurrent()
        first.cancel()
        runCurrent()
        // The waiter was not cancelled with it: it made its own request.
        assertEquals(2, t.urls.size)
        t.release()
        assertEquals("Soccer", second.await().single().name)
    }

    @Test fun listenerSeesCacheHitsErrorsAndRetries() = runTest {
        val events = mutableListOf<RequestEvent>()
        val t = FakeTransport().respond("busy", 503).respond(sports).respond("gone", 404)
        val c = client(t) {
            cache = InMemoryResponseCache()
            requestListener = RequestListener { events += it }
        }
        c.v1.list.sports() // 503, then 200
        c.v1.list.sports() // cache
        assertFailsWith<HttpStatusException> { c.v1.lookup.team(1) }

        val (retried, cached, failed) = events
        assertEquals(2, retried.attempts); assertEquals(200, retried.status); assertEquals(null, retried.error)
        assertTrue(cached.fromCache); assertEquals(0, cached.attempts)
        assertEquals(404, failed.status); assertTrue(failed.error is HttpStatusException)
        assertTrue(failed.url.endsWith("/api/v1/json/***/lookupteam.php?id=1"))
    }

    @Test fun aThrowingListenerDoesNotBreakCalls() = runTest {
        val t = FakeTransport().respond(sports)
        val c = client(t) { requestListener = RequestListener { error("listener bug") } }
        assertEquals(1, c.v1.list.sports().size)
    }
}
