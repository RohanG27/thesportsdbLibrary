package sportsdb

import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import sportsdb.http.RateLimiter
import sportsdb.internal.bool
import sportsdb.internal.date
import sportsdb.internal.instant
import sportsdb.internal.int
import sportsdb.internal.json
import sportsdb.internal.list
import sportsdb.internal.locked
import sportsdb.internal.normalizeRecord
import sportsdb.internal.parseRecords
import sportsdb.internal.str
import sportsdb.internal.time
import sportsdb.internal.year
import sportsdb.model.EventStatus
import sportsdb.model.ImageSize
import sportsdb.model.sized
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ParsingTest {
    private fun rec(vararg pairs: Pair<String, String?>): JsonObject = normalizeRecord(
        json.parseToJsonElement(
            pairs.joinToString(",", "{", "}") { (k, v) -> "\"$k\":" + (v?.let { "\"$it\"" } ?: "null") },
        ).jsonObject,
    )

    @Test fun blanksAndNullsAreMissing() {
        val r = rec("a" to "", "b" to null, "c" to "  ", "d" to "x")
        assertNull(r.str("a")); assertNull(r.str("b")); assertNull(r.str("c")); assertNull(r.str("missing"))
        assertEquals("x", r.str("d"))
    }

    @Test fun numbers() {
        val r = rec("i" to "42", "f" to "3.0", "bad" to "n/a", "zero" to "0")
        assertEquals(42, r.int("i")); assertEquals(3, r.int("f")); assertNull(r.int("bad"))
        assertNull(r.year("zero"))
    }

    @Test fun booleansInAnyCase() {
        val r = rec("a" to "Yes", "b" to "NO", "c" to "no", "d" to "maybe", "strLocked" to "unlocked")
        assertEquals(true, r.bool("a")); assertEquals(false, r.bool("b")); assertEquals(false, r.bool("c"))
        assertNull(r.bool("d"))
        assertEquals(false, r.locked())
    }

    @Test fun timestampsAreUtc() {
        val r = rec(
            "events" to "2026-10-10T11:30:00", "tv" to "2026-10-10 11:30:00",
            "zoned" to "2026-10-10T11:30:00+02:00", "bad" to "soon",
        )
        assertEquals(Instant.parse("2026-10-10T11:30:00Z"), r.instant("events"))
        assertEquals(Instant.parse("2026-10-10T11:30:00Z"), r.instant("tv"))
        assertEquals(Instant.parse("2026-10-10T09:30:00Z"), r.instant("zoned"))
        assertNull(r.instant("bad"))
    }

    @Test fun datesAndTimes() {
        val r = rec("d" to "2026-10-04", "zero" to "0000-00-00", "t1" to "16:00", "t2" to "16:30:15", "t3" to "16:00:00+00:00", "tb" to "TBC")
        assertEquals(LocalDate.of(2026, 10, 4), r.date("d"))
        assertNull(r.date("zero"))
        assertEquals(LocalTime.of(16, 0), r.time("t1"))
        assertEquals(LocalTime.of(16, 30, 15), r.time("t2"))
        assertEquals(LocalTime.of(16, 0), r.time("t3"))
        assertNull(r.time("tb"))
    }

    @Test fun commaLists() {
        assertEquals(listOf("Arsenal Football Club", "AFC", "Arsenal FC"), rec("x" to "Arsenal Football Club, AFC, Arsenal FC").list("x"))
        assertEquals(emptyList(), rec("x" to null).list("x"))
    }

    @Test fun envelopes() {
        assertEquals(emptyList(), parseRecords("", "events", "u"))
        assertEquals(emptyList(), parseRecords("""{"events":null}""", "events", "u"))
        assertEquals(emptyList(), parseRecords("""{"Message":"No data found"}""", "lookup", "u"))
        assertEquals(1, parseRecords("""{"events":[{"idEvent":"1"}]}""", "events", "u").size)
    }

    @Test fun eventStatus() {
        assertEquals(EventStatus.NOT_STARTED, EventStatus.of("NS"))
        assertEquals(EventStatus.IN_PLAY, EventStatus.of("2H"))
        assertEquals(EventStatus.IN_PLAY, EventStatus.of("Q3"))
        assertEquals(EventStatus.IN_PLAY, EventStatus.of("P2"))
        assertEquals(EventStatus.IN_PLAY, EventStatus.of("IN7"))
        assertEquals(EventStatus.FINISHED, EventStatus.of("FT"))
        assertEquals(EventStatus.FINISHED, EventStatus.of("aet"))
        assertEquals(EventStatus.POSTPONED, EventStatus.of("PST"))
        assertEquals(EventStatus.UNKNOWN, EventStatus.of(null))
        assertEquals(EventStatus.UNKNOWN, EventStatus.of("???"))
    }

    @Test fun imageSizes() {
        val badge = "https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png"
        assertEquals("$badge/tiny", badge.sized(ImageSize.TINY))
        assertEquals("$badge/small", "$badge/tiny".sized(ImageSize.SMALL))
        assertEquals("https://example.com/x.png", "https://example.com/x.png".sized(ImageSize.TINY))
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RateLimiterTest {
    @Test fun waitsWhenTheWindowIsFull() = runTest {
        val limiter = RateLimiter(permits = 3, windowMillis = 60_000, now = { currentTime })
        repeat(3) { limiter.acquire() }
        assertEquals(0, currentTime)
        limiter.acquire()
        assertEquals(60_000, currentTime)
    }

    @Test fun slotsFreeUpAsTheWindowSlides() = runTest {
        val limiter = RateLimiter(permits = 2, windowMillis = 1_000, now = { currentTime })
        limiter.acquire()
        kotlinx.coroutines.delay(600)
        limiter.acquire()
        limiter.acquire() // waits for the first call to fall out of the window: t = 1000
        assertEquals(1_000, currentTime)
    }
}
