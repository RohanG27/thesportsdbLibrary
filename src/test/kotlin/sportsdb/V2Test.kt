package sportsdb

import kotlinx.coroutines.test.runTest
import sportsdb.model.EventStatus
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/** Every v2 method against a real recorded premium response, plus v2's key handling. */
class V2Test {
    private val premiumKey = "9999999999" // a stand-in; fixtures never contain the real key
    private val t = FakeTransport()
    private val v2 = client(t, premiumKey).v2

    private suspend fun <T> check(fixtureName: String, expectedPath: String, call: suspend () -> List<T>): List<T> {
        t.respond(fixture("v2/$fixtureName.json"))
        val result = call()
        assertEquals("/api/v2/json/$expectedPath", t.lastUrl.encodedPath)
        if (result.isEmpty()) fail("$fixtureName parsed to an empty list")
        return result
    }

    private suspend fun <T : Any> checkOne(fixtureName: String, expectedPath: String, call: suspend () -> T?): T =
        check<T>(fixtureName, expectedPath) { listOfNotNull(call()) }.single()

    /** For endpoints without a recorded fixture: checks the URL only. */
    private suspend fun expectPath(expectedPath: String, call: suspend () -> Unit) {
        t.respond("{}")
        call()
        assertEquals("/api/v2/json/$expectedPath", t.lastUrl.encodedPath)
    }

    @Test fun search() = runTest {
        // v2 search sends ids as JSON numbers, not strings.
        val teams = check("search_team", "search/team/Arsenal") { v2.search.teams("Arsenal") }
        with(teams.first()) {
            assertEquals(133604L, id)
            assertEquals(4328L, leagueId)
            assertEquals(listOf(4328L), leagues.map { it.id })
        }
        val leagues = check("search_league", "search/league/English%20Premier%20League") { v2.search.leagues("English Premier League") }
        assertNotNull(leagues.first().id)
        val players = check("search_player", "search/player/Danny%20Welbeck") { v2.search.players("Danny Welbeck") }
        assertNotNull(players.single().teamId)
        val venues = check("search_venue", "search/venue/Wembley") { v2.search.venues("Wembley") }
        assertNotNull(venues.first().id)

        t.respond(fixture("v2/search_event_none.json")) // {"Message":"No data found"}
        assertTrue(v2.search.events("Arsenal vs Chelsea").isEmpty())
        assertEquals("/api/v2/json/search/event/Arsenal%20vs%20Chelsea", t.lastUrl.encodedPath)
    }

    @Test fun lookups() = runTest {
        val league = checkOne("lookup_league", "lookup/league/4328") { v2.lookup.league(4328) }
        assertEquals("2026-2027", league.currentSeason)
        val team = checkOne("lookup_team", "lookup/team/133604") { v2.lookup.team(133604) }
        assertTrue(team.alternateNames.isNotEmpty())
        val player = checkOne("lookup_player", "lookup/player/34145937") { v2.lookup.player(34145937) }
        assertNotNull(player.born)
        val event = checkOne("lookup_event", "lookup/event/441613") { v2.lookup.event(441613) }
        assertNotNull(event.homeScore)
        val venue = checkOne("lookup_venue", "lookup/venue/16163") { v2.lookup.venue(16163) }
        assertNotNull(venue.capacity)

        check("lookup_team_equipment", "lookup/team_equipment/133597") { v2.lookup.teamEquipment(133597) }
        check("lookup_player_contracts", "lookup/player_contracts/34147178") { v2.lookup.playerContracts(34147178) }
        check("lookup_player_results", "lookup/player_results/34160573") { v2.lookup.playerResults(34160573) }
        check("lookup_player_honours", "lookup/player_honours/34147178") { v2.lookup.playerHonours(34147178) }
        check("lookup_player_milestones", "lookup/player_milestones/34161397") { v2.lookup.playerMilestones(34161397) }
        val former = check("lookup_player_teams", "lookup/player_teams/34147178") { v2.lookup.playerTeams(34147178) }
        assertNotNull(former.first().teamId)
        val stats = check("lookup_player_stats", "lookup/player_stats/34146304") { v2.lookup.playerStats(34146304) }
        assertEquals(315, stats.size)
        val lineup = check("lookup_event_lineup", "lookup/event_lineup/1032723") { v2.lookup.eventLineup(1032723) }
        val first = lineup.first()
        assertEquals<String?>("Aston Villa vs Liverpool", first.event)
        assertEquals(true, first.isHome)
        assertEquals(26, first.squadNumber)
        check("lookup_event_results", "lookup/event_results/652890") { v2.lookup.eventResults(652890) }
        val eventStats = check("lookup_event_stats", "lookup/event_stats/1032723") { v2.lookup.eventStats(1032723) }
        assertNotNull(eventStats.first().home)
        check("lookup_event_timeline", "lookup/event_timeline/1032718") { v2.lookup.eventTimeline(1032718) }
        val tv = check("lookup_event_tv", "lookup/event_tv/2494052") { v2.lookup.eventTv(2494052) }
        assertTrue(tv.all { it.timestamp != null && it.channel != null })
        check("lookup_event_highlights", "lookup/event_highlights/441613") { v2.lookup.eventHighlights(441613) }
    }

    @Test fun listsAndCatalogues() = runTest {
        val teams = check("list_teams", "list/teams/4328") { v2.list.teams(4328) }
        assertEquals(20, teams.size)
        assertTrue(teams.all { it.badge != null && it.alternateNames.isEmpty() })
        val seasons = check("list_seasons", "list/seasons/4328") { v2.list.seasons(4328) }
        assertNotNull(seasons.last().description)
        check("list_players", "list/players/133604") { v2.list.players(133604) }

        val countries = check("all_countries", "all/countries") { v2.all.countries() }
        with(countries.first { it.name == "Andorra" }) {
            assertEquals("AD", code)
            assertEquals("Andorre", nameFr)
            assertNotNull(flag64)
            assertNull(apiFootballId)
        }
        check("all_sports", "all/sports") { v2.all.sports() }
    }

    @Test fun schedules() = runTest {
        val season = check("schedule_league_season", "schedule/league/4328/2026-2027") { v2.schedule.leagueSeason(4328, "2026-2027") }
        assertEquals(380, season.size)
        assertTrue(season.all { it.timestamp != null })
        val full = check("schedule_full_team", "schedule/full/team/133604") { v2.schedule.teamFull(133604) }
        with(full.first()) {
            assertEquals(Instant.parse("2027-05-30T15:00:00Z"), timestamp)
            assertEquals(EventStatus.NOT_STARTED, status)
        }
        check("schedule_next_league", "schedule/next/league/4328") { v2.schedule.leagueNext(4328) }

        expectPath("schedule/previous/league/4328") { v2.schedule.leaguePrevious(4328) }
        expectPath("schedule/next/team/133604") { v2.schedule.teamNext(133604) }
        expectPath("schedule/previous/team/133604") { v2.schedule.teamPrevious(133604) }
        expectPath("schedule/next/venue/16163") { v2.schedule.venueNext(16163) }
        expectPath("schedule/previous/venue/16163") { v2.schedule.venuePrevious(16163) }
    }

    @Test fun tv() = runTest {
        val day = check("filter_tv_day", "filter/tv/day/2026-10-05") { v2.tv.day(LocalDate.of(2026, 10, 5)) }
        assertTrue(day.size > 100)
        check("filter_tv_country", "filter/tv/country/Canada") { v2.tv.country("Canada") }
        val channel = check("filter_tv_channel", "filter/tv/channel/TSN%201") { v2.tv.channel("TSN 1") }
        assertEquals(Instant.parse("2026-10-05T00:20:00Z"), channel.first().timestamp)

        expectPath("filter/tv/channelid/8631") { v2.tv.channel(8631L) }
        expectPath("filter/tv/sport/Ice%20Hockey") { v2.tv.sport("Ice Hockey") }
    }

    @Test fun liveScores() = runTest {
        val live = check("livescore_all", "livescore/all") { v2.live.all() }
        with(live.first()) {
            assertEquals("P3", statusCode)
            assertEquals(EventStatus.IN_PLAY, status)
            assertEquals(2, homeScore)
            assertNotNull(updated)
        }
        expectPath("livescore/Soccer") { v2.live.sport("Soccer") }
        expectPath("livescore/4328") { v2.live.league(4328) }
    }

    @Test fun keyGoesInHeaderNotUrl() = runTest {
        t.respond(fixture("v2/lookup_team.json"))
        v2.lookup.team(133604)
        assertEquals(premiumKey, t.requests.last().second["X-API-KEY"])
        assertTrue(premiumKey !in t.lastUrl.toString())
    }

    @Test fun invalidKeyIsReported() = runTest {
        t.respond(fixture("v2/invalid_key.json"), status = 400)
        assertFailsWith<InvalidApiKeyException> { v2.lookup.league(4328) }
    }

    @Test fun freeKeyNeverCallsV2() = runTest {
        val free = FakeTransport()
        assertFailsWith<PremiumRequiredException> { client(free).v2.all.sports() }
        assertTrue(free.requests.isEmpty())
    }

    @Test fun isPremiumKey() = runTest {
        t.respond(fixture("v2/lookup_league.json"))
        assertTrue(client(t, premiumKey).isPremiumKey())
        t.respond(fixture("v2/invalid_key.json"), status = 400)
        assertEquals(false, client(t, "1234567890").isPremiumKey())
        assertEquals(false, client(FakeTransport()).isPremiumKey())
    }
}
