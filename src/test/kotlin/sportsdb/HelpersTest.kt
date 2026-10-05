package sportsdb

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import sportsdb.http.HttpResponse
import sportsdb.http.HttpTransport
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HelpersTest {
    /** Answers by URL: the first route whose key is contained in "path?query" wins; anything else is `{}`. */
    private class Routes(vararg routes: Pair<String, String>) : HttpTransport {
        private val routes = routes.toList()
        val calls = CopyOnWriteArrayList<String>()
        override suspend fun get(url: HttpUrl, headers: Map<String, String>): HttpResponse {
            val call = url.encodedPath.substringAfter("/json/") + (url.encodedQuery?.let { "?$it" } ?: "")
            calls += call
            val body = routes.firstOrNull { it.first in call }?.second?.let(::fixtureOrInline) ?: "{}"
            return HttpResponse(200, body)
        }

        private fun fixtureOrInline(s: String) = if (s.startsWith("{")) s else fixture(s)
    }

    private fun helpers(t: HttpTransport, premium: Boolean) = SportsDbClient {
        apiKey = if (premium) "9999999999" else SportsDbConfig.FREE_API_KEY
        transport = t
        requestsPerMinute = 0
    }.helpers

    /** Events in a fixture, read straight from the JSON, to check helpers against. */
    private fun fixtureEvents(path: String, key: String) =
        Json.parseToJsonElement(fixture(path)).jsonObject[key]!!.jsonArray.map { it.jsonObject }

    private fun Instant.inRange(from: Instant, to: Instant) = this >= from && this < to

    @Test fun currentSeasonAndSeasonEvents() = runTest {
        val premium = Routes("lookup/league/4328" to "v2/lookup_league.json", "schedule/league/4328/2026-2027" to "v2/schedule_league_season.json")
        val events = helpers(premium, premium = true).seasonEvents(4328)
        assertEquals(listOf("lookup/league/4328", "schedule/league/4328/2026-2027"), premium.calls)
        assertEquals(380, events.size)
        assertEquals(events.sortedBy { it.timestamp }, events)

        val free = Routes("lookupleague.php" to "v1-free/lookup_league.json", "eventsseason.php" to "v1-free/events_season.json")
        assertEquals(5, helpers(free, premium = false).seasonEvents(4328).size)
        assertTrue(free.calls.last().endsWith("eventsseason.php?id=4328&s=2026-2027"))
    }

    @Test fun upcomingLeagueEventsPremiumFiltersTheSeason() = runTest {
        val t = Routes("lookup/league/4328" to "v2/lookup_league.json", "schedule/league/" to "v2/schedule_league_season.json")
        val from = LocalDate.of(2026, 10, 17)
        val upcoming = helpers(t, premium = true).upcomingLeagueEvents(4328, days = 7, from = from)

        val expected = fixtureEvents("v2/schedule_league_season.json", "schedule")
            .map { LocalDate.parse(it["dateEvent"]!!.jsonPrimitive.content) }
            .count { !it.isBefore(from) && it.isBefore(from.plusDays(7)) }
        assertTrue(expected > 0)
        assertEquals(expected, upcoming.size)
    }

    @Test fun upcomingLeagueEventsFreeCallsOncePerDay() = runTest {
        val t = Routes("eventsday.php" to "v1-free/events_day.json")
        helpers(t, premium = false).upcomingLeagueEvents(4328, days = 3, from = LocalDate.of(2026, 10, 4))
        assertEquals(
            setOf("2026-10-04", "2026-10-05", "2026-10-06").map { "eventsday.php?d=$it&l=4328" }.toSet(),
            t.calls.map { it.substringAfterLast('/') }.toSet(),
        )
    }

    @Test fun teamSchedule() = runTest {
        val free = Routes("eventsnext.php" to "v1-free/events_next.json", "eventslast.php" to "v1-free/events_last.json")
        val schedule = helpers(free, premium = false).teamSchedule(133602)
        assertEquals(2, free.calls.size)
        assertEquals(schedule.map { it.id }.distinct(), schedule.map { it.id })
        assertEquals(schedule.sortedBy { it.timestamp }, schedule)

        val premium = Routes("schedule/full/team/133604" to "v2/schedule_full_team.json")
        assertEquals(48, helpers(premium, premium = true).teamSchedule(133604).size)
    }

    @Test fun eventsOnLocalDateSpansTwoUtcDays() = runTest {
        // Toronto's 4 October runs from 04:00 UTC on the 4th to 04:00 UTC on the 5th.
        val t = Routes("d=2026-10-04" to "v1-premium/events_day.json", "d=2026-10-05" to """{"events":null}""")
        val zone = ZoneId.of("America/Toronto")
        val date = LocalDate.of(2026, 10, 4)
        val events = helpers(t, premium = true).eventsOnLocalDate(date, zone)

        assertEquals(setOf("eventsday.php?d=2026-10-04", "eventsday.php?d=2026-10-05"), t.calls.map { it.substringAfterLast('/') }.toSet())
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        val expected = fixtureEvents("v1-premium/events_day.json", "events").count { e ->
            LocalDateTime.parse(e["strTimestamp"]!!.jsonPrimitive.content).toInstant(ZoneOffset.UTC).inRange(from, to)
        }
        assertTrue(expected in 1 until 901, "the local day should keep only part of the UTC day")
        assertEquals(expected, events.size)
        assertTrue(events.all { it.timestamp!!.inRange(from, to) })
    }

    @Test fun eventsOnLocalDateAheadOfUtc() = runTest {
        // Tokyo's 4 October runs from 15:00 UTC on the 3rd.
        val t = Routes()
        helpers(t, premium = false).eventsOnLocalDate(LocalDate.of(2026, 10, 4), ZoneId.of("Asia/Tokyo"), sport = "Soccer")
        assertEquals(
            setOf("eventsday.php?d=2026-10-03&s=Soccer", "eventsday.php?d=2026-10-04&s=Soccer"),
            t.calls.map { it.substringAfterLast('/') }.toSet(),
        )
    }

    @Test fun liveScores() = runTest {
        val leagueId = fixtureEvents("v1-free/livescore_soccer.json", "livescore").first()["idLeague"]!!.jsonPrimitive.content
        val free = Routes(
            "lookupleague.php" to """{"leagues":[{"idLeague":"$leagueId","strSport":"Soccer"}]}""",
            "livescore.php?s=Soccer" to "v1-free/livescore_soccer.json",
        )
        val scores = helpers(free, premium = false).liveScores(leagueId = leagueId.toLong())
        assertTrue(scores.isNotEmpty() && scores.all { it.leagueId == leagueId.toLong() })

        assertFailsWith<IllegalArgumentException> { helpers(Routes(), premium = false).liveScores() }

        val premium = Routes("livescore/all" to "v2/livescore_all.json")
        assertEquals(54, helpers(premium, premium = true).liveScores().size)
    }

    @Test fun leagueTeams() = runTest {
        val free = Routes("lookupleague.php" to "v1-free/lookup_league.json", "search_all_teams.php" to "v1-free/search_all_teams_league.json")
        assertEquals(10, helpers(free, premium = false).leagueTeams(4328).size)
        assertTrue(free.calls.last().endsWith("search_all_teams.php?l=English%20Premier%20League"))

        val premium = Routes("list/teams/4328" to "v2/list_teams.json")
        assertEquals(20, helpers(premium, premium = true).leagueTeams(4328).size)
    }

    @Test fun tvListings() = runTest {
        val from = LocalDate.of(2026, 10, 5)
        val premium = Routes("filter/tv/country/Canada" to "v2/filter_tv_country.json")
        val hockey = helpers(premium, premium = true).tvListings("Canada", sport = "ice hockey", days = 2, from = from)
        assertEquals(1, premium.calls.size)
        assertTrue(hockey.isNotEmpty())
        assertTrue(hockey.all { it.sport == "Ice Hockey" && it.date!! in setOf(from, from.plusDays(1)) })

        assertFailsWith<IllegalArgumentException> { helpers(Routes(), premium = false).tvListings("Canada") }
        val free = Routes("eventstv.php" to "v1-free/events_tv_country.json")
        helpers(free, premium = false).tvListings("Canada", sport = "Ice Hockey", days = 2, from = from)
        assertEquals(2, free.calls.size)
    }

    @Test fun roundEvents() = runTest {
        val free = Routes("eventsround.php" to "v1-free/events_round.json")
        val round = helpers(free, premium = false).roundEvents(4328, round = 1, season = "2026-2027")
        assertEquals(listOf("123/eventsround.php?id=4328&r=1&s=2026-2027"), free.calls)
        assertEquals(10, round.size)

        val premium = Routes("schedule/league/4328/2026-2027" to "v2/schedule_league_season.json")
        val filtered = helpers(premium, premium = true).roundEvents(4328, round = 1, season = "2026-2027")
        val expected = fixtureEvents("v2/schedule_league_season.json", "schedule").count { it["intRound"]!!.jsonPrimitive.content == "1" }
        assertEquals(expected, filtered.size)
        assertEquals(listOf("schedule/league/4328/2026-2027"), premium.calls, "premium must not call eventsround.php (it 404s)")
    }

    @Test fun keyThreeIsTreatedAsFree() = runTest {
        val t = Routes("eventsround.php" to "v1-free/events_round.json")
        SportsDbClient { apiKey = "3"; transport = t; requestsPerMinute = 0 }.helpers.roundEvents(4328, 1, "2026-2027")
        assertEquals(listOf("3/eventsround.php?id=4328&r=1&s=2026-2027"), t.calls)
    }

    @Test fun eventChannels() = runTest {
        val premium = Routes("lookup/event_tv/2494052" to "v2/lookup_event_tv.json")
        assertEquals(13, helpers(premium, premium = true).eventChannels(2494052).size)
        val free = Routes("lookuptv.php" to "v1-free/lookup_tv.json")
        assertEquals(2, helpers(free, premium = false).eventChannels(2494052).size)
    }
}
