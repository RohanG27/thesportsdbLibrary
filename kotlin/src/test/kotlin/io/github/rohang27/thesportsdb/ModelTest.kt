package io.github.rohang27.thesportsdb

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ModelTest {
    private val t = FakeTransport()
    private val v1 = client(t).v1

    @Test fun recordsAreEqualWhenTheApiSentTheSameData() = runTest {
        t.respond(fixture("v1-free/lookup_team.json")).respond(fixture("v1-free/lookup_team.json"))
        val a = v1.lookup.team(133604)
        val b = v1.lookup.team(133604)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(setOf(a), setOf(b))

        t.respond(fixture("v1-free/search_all_teams_league.json"))
        val others = v1.list.teamsInLeague("English Premier League")
        assertEquals(others.size, others.toSet().size)
        assertNotEquals<Any?>(a, others.first { it.id != 133604L })
    }

    @Test fun differentTypesFromTheSameFieldsAreNotEqual() = runTest {
        // eventshighlights records are read as Event; a TvListing from identical JSON must not equal it.
        val body = """{"tvhighlights":[{"idEvent":"1"}],"tvevents":[{"idEvent":"1"}]}"""
        t.respond(body).respond(body)
        val event = v1.video.highlights(java.time.LocalDate.of(2026, 1, 1)).single()
        val listing = v1.tv.channel("x").single()
        assertEquals(event.raw, listing.raw)
        assertNotEquals<Any>(event, listing)
    }

    @Test fun toStringIsShort() = runTest {
        t.respond(fixture("v1-free/lookup_team.json"))
        assertEquals("Team(id=133604, name=Arsenal)", v1.lookup.team(133604).toString())
        t.respond(fixture("v1-free/lookup_table.json"))
        // The leader changes during a season, so check the shape rather than the club.
        val row = v1.lookup.table(4328).first().toString()
        assertTrue(Regex("""Standing\(rank=1, team=[^,]+, points=\d+\)""").matches(row), row)
    }
}
