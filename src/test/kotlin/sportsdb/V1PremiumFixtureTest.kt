package sportsdb

import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** v1 with a premium key: same parsing, much bigger results. */
class V1PremiumFixtureTest {
    private val t = FakeTransport()
    private val v1 = client(t, "9999999999").v1

    @Test fun fullResults() = runTest {
        t.respond(fixture("v1-premium/events_day.json"))
        val day = v1.schedule.day(LocalDate.of(2026, 10, 4))
        assertTrue(day.size > 500, "premium eventsday returned ${day.size}")
        assertTrue(day.all { it.id != null })

        t.respond(fixture("v1-premium/events_season.json"))
        assertEquals(380, v1.schedule.season(4328, "2026-2027").size)

        t.respond(fixture("v1-premium/lookup_table.json"))
        val table = v1.lookup.table(4328)
        assertEquals(20, table.size)
        assertEquals((1..20).toList(), table.map { it.rank })

        t.respond(fixture("v1-premium/all_leagues.json"))
        assertTrue(v1.list.leagues().size > 1000)

        t.respond(fixture("v1-premium/events_tv_country_no_sport.json")) // still an empty body
        assertTrue(v1.tv.channel("x").isEmpty())
    }
}
