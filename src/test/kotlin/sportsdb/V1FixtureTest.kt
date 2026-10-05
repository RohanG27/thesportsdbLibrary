package sportsdb

import kotlinx.coroutines.test.runTest
import sportsdb.model.EventStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every v1 method against a real recorded response: checks the URL it builds, that it
 * reads the right record key, and that the main fields parse.
 */
class V1FixtureTest {
    private val t = FakeTransport()
    private val v1 = client(t).v1

    private suspend fun <T> check(fixtureName: String, expectedCall: String, call: suspend () -> List<T>): List<T> {
        t.respond(fixture("v1-free/$fixtureName.json"))
        val result = call()
        assertEquals(expectedCall, t.lastUrl.v1Call)
        assertTrue(t.lastUrl.encodedPath.startsWith("/api/v1/json/123/"))
        if (result.isEmpty()) fail("$fixtureName parsed to an empty list")
        return result
    }

    @Test fun search() = runTest {
        val teams = check("search_teams", "searchteams.php?t=Arsenal") { v1.search.teams("Arsenal") }
        with(teams.first()) {
            assertEquals(133604L, id)
            assertEquals("Arsenal", name)
            assertEquals("ARS", shortName)
            assertTrue("Arsenal FC" in alternateNames)
            assertEquals(4328L, leagues.first().id)
            assertTrue(leagues.size > 1)
            assertTrue(descriptions.containsKey("EN") && descriptions.containsKey("DE"))
            assertEquals("#EF0107", colours.first())
            assertEquals(false, isLocked)
        }
        check("search_events", "searchevents.php?e=Arsenal%20vs%20Chelsea") { v1.search.events("Arsenal vs Chelsea") }
        check("search_events_season", "searchevents.php?e=Arsenal_vs_Chelsea&s=2016-2017") {
            v1.search.events("Arsenal_vs_Chelsea", season = "2016-2017")
        }
        check("search_filename", "searchfilename.php?e=English%20Premier%20League%202015-04-26%20Arsenal%20vs%20Chelsea") {
            v1.search.eventsByFilename("English Premier League 2015-04-26 Arsenal vs Chelsea")
        }
        val players = check("search_players", "searchplayers.php?p=Danny%20Welbeck") { v1.search.players("Danny Welbeck") }
        assertNotNull(players.first().relevance)
        check("search_venues", "searchvenues.php?v=Wembley") { v1.search.venues("Wembley") }
    }

    @Test fun lookup() = runTest {
        val league = v1.lookup.let { l -> t.respond(fixture("v1-free/lookup_league.json")); l.league(4328) }
        assertNotNull(league)
        assertEquals("lookupleague.php?id=4328", t.lastUrl.v1Call)
        assertEquals("English Premier League", league.name)
        assertNotNull(league.currentSeason)
        assertEquals(LocalDate.of(1992, 8, 15), league.firstEventDate)

        val table = check("lookup_table", "lookuptable.php?l=4328") { v1.lookup.table(4328) }
        assertEquals(1, table.first().rank)
        assertNotNull(table.first().points)
        assertNotNull(table.first().updated)

        check("lookup_equipment", "lookupequipment.php?id=133597") { v1.lookup.equipment(133597) }
        check("lookup_honours", "lookuphonours.php?id=34147178") { v1.lookup.honours(34147178) }
        check("lookup_former_teams", "lookupformerteams.php?id=34147178") { v1.lookup.formerTeams(34147178) }
        check("lookup_milestones", "lookupmilestones.php?id=34161397") { v1.lookup.milestones(34161397) }
        val contracts = check("lookup_contracts", "lookupcontracts.php?id=34147178") { v1.lookup.contracts(34147178) }
        assertNotNull(contracts.first().yearStart)
        check("player_results", "playerresults.php?id=34160573") { v1.lookup.playerResults(34160573) }
        check("lookup_player_stats", "lookupplayerstats.php?id=34146304") { v1.lookup.playerStats(34146304) }
        check("event_results", "eventresults.php?id=652890") { v1.lookup.eventResults(652890) }
        val lineup = check("lookup_lineup", "lookuplineup.php?id=1032723") { v1.lookup.lineup(1032723) }
        assertNotNull(lineup.first().isHome)
        assertEquals(false, lineup.first().isSubstitute)
        val timeline = check("lookup_timeline", "lookuptimeline.php?id=1032718") { v1.lookup.timeline(1032718) }
        assertNotNull(timeline.first().minute)
        val stats = check("lookup_event_stats", "lookupeventstats.php?id=1032723") { v1.lookup.eventStats(1032723) }
        assertNotNull(stats.first().home)
        val tv = check("lookup_tv", "lookuptv.php?id=2494052") { v1.lookup.eventTv(2494052) }
        assertNotNull(tv.first().timestamp, "TV listings use strTimeStamp with a space")
        assertNotNull(tv.first().channel)

        t.respond(fixture("v1-free/lookup_player.json"))
        val player = assertNotNull(v1.lookup.player(34145937))
        assertNotNull(player.born)
        assertNotNull(player.externalIds.wikidata)

        t.respond(fixture("v1-free/lookup_event.json"))
        val event = assertNotNull(v1.lookup.event(441613))
        // Older events have a score but no strStatus at all.
        assertNull(event.statusCode)
        assertEquals(EventStatus.UNKNOWN, event.status)
        assertEquals(4, event.homeScore)

        t.respond(fixture("v1-free/lookup_venue.json"))
        val venue = assertNotNull(v1.lookup.venue(16163))
        assertEquals(90000, venue.capacity)
        assertNotNull(venue.coordinates)

        t.respond(fixture("v1-free/lookup_team.json"))
        assertEquals("Arsenal", v1.lookup.team(133604)?.name)
    }

    @Test fun lists() = runTest {
        check("all_sports", "all_sports.php") { v1.list.sports() }
        val countries = check("all_countries", "all_countries.php") { v1.list.countries() }
        assertNotNull(countries.first().flag32)
        check("all_leagues", "all_leagues.php") { v1.list.leagues() }
        check("search_all_leagues", "search_all_leagues.php?c=England&s=Soccer") { v1.list.leagues("England", "Soccer") }
        val seasons = check("search_all_seasons", "search_all_seasons.php?id=4328") { v1.list.seasons(4328) }
        assertTrue(seasons.all { it.name.isNotEmpty() })
        val posters = check("search_all_seasons_poster", "search_all_seasons.php?id=4328&poster=1") {
            v1.list.seasons(4328, posters = true)
        }
        // The poster flag adds the field; early seasons simply have no poster.
        assertTrue(posters.first().raw.fields.containsKey("strPoster"))
        check("search_all_teams_league", "search_all_teams.php?l=English%20Premier%20League") {
            v1.list.teamsInLeague("English Premier League")
        }
        check("search_all_teams_country", "search_all_teams.php?s=Soccer&c=Spain") { v1.list.teamsInCountry("Soccer", "Spain") }
        check("lookup_all_players", "lookup_all_players.php?id=133604") { v1.list.players(133604) }
    }

    @Test fun schedules() = runTest {
        check("events_next", "eventsnext.php?id=133602") { v1.schedule.teamNext(133602) }
        val last = check("events_last", "eventslast.php?id=133602") { v1.schedule.teamLast(133602) }
        assertNotNull(last.first().id, "eventslast.php uses the 'results' key for events")
        check("events_next_league", "eventsnextleague.php?id=4328") { v1.schedule.leagueNext(4328) }
        check("events_past_league", "eventspastleague.php?id=4328") { v1.schedule.leaguePast(4328) }
        val day = check("events_day", "eventsday.php?d=2026-10-04") { v1.schedule.day(LocalDate.of(2026, 10, 4)) }
        assertTrue(day.all { it.timestamp != null })
        val season = check("events_season", "eventsseason.php?id=4328&s=2026-2027") { v1.schedule.season(4328, "2026-2027") }
        with(season.first()) {
            assertEquals(LocalDate.of(2026, 8, 21), date)
            assertEquals(LocalTime.of(19, 0), time)
            assertEquals(Instant.parse("2026-08-21T19:00:00Z"), timestamp)
        }
    }

    @Test fun documentedVariants() = runTest {
        check("search_events_date", "searchevents.php?e=Arsenal%20vs%20Chelsea&d=2015-04-26") {
            v1.search.events("Arsenal vs Chelsea", date = LocalDate.of(2015, 4, 26))
        }
        check("search_filename_season", "searchfilename.php?e=English%20Premier%20League%202015-04-26%20Arsenal%20vs%20Chelsea&s=2014-2015") {
            v1.search.eventsByFilename("English Premier League 2015-04-26 Arsenal vs Chelsea", season = "2014-2015")
        }
        val past = check("lookup_table_season", "lookuptable.php?l=4328&s=2024-2025") { v1.lookup.table(4328, "2024-2025") }
        assertTrue(past.all { it.season == "2024-2025" })
        val badges = check("search_all_seasons_badge", "search_all_seasons.php?id=4328&badge=1") { v1.list.seasons(4328, badges = true) }
        assertTrue(badges.any { it.badge != null })
        val described = check("search_all_seasons_description", "search_all_seasons.php?id=4328&description=1") {
            v1.list.seasons(4328, descriptions = true)
        }
        assertTrue(described.any { it.description != null })
        val hockey = check("events_day_sport", "eventsday.php?d=2026-10-04&s=Ice%20Hockey") {
            v1.schedule.day(LocalDate.of(2026, 10, 4), sport = "Ice Hockey")
        }
        assertTrue(hockey.all { it.sport == "Ice Hockey" })
        val canada = check("events_tv_country", "eventstv.php?d=2026-10-05&a=Canada&s=Ice%20Hockey") {
            v1.tv.day(LocalDate.of(2026, 10, 5), sport = "Ice Hockey", country = "Canada")
        }
        assertTrue(canada.all { it.country == "Canada" })
        check("events_highlights_sport", "eventshighlights.php?d=2026-10-04&s=Soccer") {
            v1.video.highlights(LocalDate.of(2026, 10, 4), sport = "Soccer")
        }
        t.respond(fixture("v1-free/events_highlights_league.json")) // {"tvhighlights":null}
        assertTrue(v1.video.highlights(LocalDate.of(2026, 10, 4), leagueId = 4328).isEmpty())
    }

    @Test fun roundAndLegacyQuirks() = runTest {
        val round = check("events_round", "eventsround.php?id=4328&r=1&s=2026-2027") { v1.schedule.round(4328, 1, "2026-2027") }
        assertEquals(10, round.size, "the free key gets a whole round")
        assertTrue(round.all { it.round == 1 })

        // A rejected parameter comes back as text where the records should be.
        t.respond(fixture("v1-free/search_all_seasons_bad_param.json"))
        val e = assertFailsWith<ApiMessageException> { v1.list.seasons(4328) }
        assertEquals("Invalid League ID passed", e.apiMessage)
        t.respond(fixture("v1-free/events_round_bad_param.json"))
        assertFailsWith<ApiMessageException> { v1.schedule.round(4328, 1, "2026-2027") }
    }

    @Test fun emptyResults() = runTest {
        t.respond(fixture("v1-free/events_day_none.json")) // {"events":null}
        assertTrue(v1.schedule.day(LocalDate.of(2026, 10, 4), leagueId = 4328).isEmpty())
        assertEquals("eventsday.php?d=2026-10-04&l=4328", t.lastUrl.v1Call)

        t.respond(fixture("v1-free/events_tv_country_no_sport.json")) // empty body
        assertTrue(v1.tv.channel("Nothing").isEmpty())
    }

    @Test fun tv() = runTest {
        val day = check("events_tv_day", "eventstv.php?d=2026-10-05") { v1.tv.day(LocalDate.of(2026, 10, 5)) }
        assertNotNull(day.first().timestamp)
        check("events_tv_channel", "eventstv.php?c=TSN%201") { v1.tv.channel("TSN 1") }
        val byId = check("events_tv_channel_id", "eventstv.php?id=8631") { v1.tv.channel(8631L) }
        assertEquals(8631L, byId.first().channelId, "eventstv.php?id= takes a channel id")
        check("events_tv_day", "eventstv.php?d=2026-10-05&a=Canada&s=Ice%20Hockey") {
            v1.tv.day(LocalDate.of(2026, 10, 5), sport = "Ice Hockey", country = "Canada")
        }
    }

    @Test fun tvCountryNeedsSport() = runTest {
        try {
            v1.tv.day(LocalDate.of(2026, 10, 5), country = "Canada")
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
        assertTrue(t.requests.isEmpty())
    }

    @Test fun videoAndLive() = runTest {
        val hl = check("events_highlights", "eventshighlights.php?d=2026-10-04") { v1.video.highlights(LocalDate.of(2026, 10, 4)) }
        assertNotNull(hl.first().video)
        val live = check("livescore_soccer", "livescore.php?s=Soccer") { v1.live.sport("Soccer") }
        with(live.first()) {
            assertNotNull(homeTeam)
            assertNotNull(updated)
            assertFalse(status == EventStatus.UNKNOWN && statusCode != null, "unknown live status code: $statusCode")
        }
    }

    @Test fun noNullIdsInBigLists() = runTest {
        t.respond(fixture("v1-free/search_all_teams_league.json"))
        val teams = v1.list.teamsInLeague("English Premier League")
        assertTrue(teams.all { it.id != null && it.name != null })
        assertNull(teams.firstOrNull { it.formedYear == 0 })
    }
}
