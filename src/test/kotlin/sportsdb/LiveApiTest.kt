package sportsdb

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Calls the real API. Excluded from `./gradlew test`; run with `./gradlew liveTest`.
 * Uses the free key unless THESPORTSDB_API_KEY is set (then v2 is tested too).
 */
@Tag("live")
class LiveApiTest {
    private val key = System.getenv("THESPORTSDB_API_KEY") ?: SportsDbConfig.FREE_API_KEY
    private val client = SportsDbClient { apiKey = key }

    @Test fun lookupTeam() = runBlocking {
        val team = assertNotNull(client.v1.lookup.team(133604))
        assertEquals("Arsenal", team.name)
        assertTrue(team.leagues.isNotEmpty())
    }

    @Test fun namesWithSpacesAreEncoded() = runBlocking {
        val teams = client.v1.search.teams("Toronto Maple Leafs")
        assertEquals("Toronto Maple Leafs", teams.firstOrNull()?.name)
    }

    @Test fun emptyDayIsEmpty() = runBlocking {
        // No Premier League games that day: {"events":null}
        assertTrue(client.v1.schedule.day(LocalDate.of(2026, 10, 4), leagueId = 4328).isEmpty())
    }

    @Test fun tvByChannelId() = runBlocking {
        val listings = client.v1.tv.channel(8631L)
        assertTrue(listings.all { it.channelId == 8631L })
    }

    @Test fun premiumDetection() = runBlocking {
        assertEquals(key != SportsDbConfig.FREE_API_KEY, client.isPremiumKey())
    }

    @Test fun v2WithPremiumKey() = runBlocking {
        assumeTrue(key != SportsDbConfig.FREE_API_KEY, "set THESPORTSDB_API_KEY to test v2")
        val league = assertNotNull(client.v2.lookup.league(4328))
        val season = assertNotNull(league.currentSeason)
        assertTrue(client.v2.schedule.leagueSeason(4328, season).size > 100)
        assertTrue(client.v2.search.teams("Arsenal").isNotEmpty())
    }
}
