package sportsdb

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import sportsdb.model.Event
import sportsdb.model.LiveScore
import sportsdb.model.Team
import sportsdb.model.TvListing
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Common tasks in one call. Each helper picks the best endpoint for your key: v2 with a
 * premium key, v1 with the free key `123` (and then the free key's small result limits
 * apply, as noted on each method). Any key other than `123` that the API accepts is a paid
 * key, so no extra call is made to find out.
 *
 * Returned events are sorted by start time (events without a time last).
 */
public class Helpers internal constructor(private val client: SportsDbClient) {
    private val premium get() = !client.config.isFreeKey
    private val v1 get() = client.v1
    private val v2 get() = client.v2

    /** A league's current season name, e.g. `2026-2027` or `2026`; null if the league is unknown. */
    public suspend fun currentSeason(leagueId: Long): String? =
        (if (premium) v2.lookup.league(leagueId) else v1.lookup.league(leagueId))?.currentSeason

    /**
     * Every event of a league's season ([season] defaults to the current one).
     * Premium: the whole season in one call. Free key: only the first 5 events.
     */
    public suspend fun seasonEvents(leagueId: Long, season: String? = null): List<Event> {
        val name = season ?: currentSeason(leagueId) ?: return emptyList()
        val events = if (premium) v2.schedule.leagueSeason(leagueId, name) else v1.schedule.season(leagueId, name)
        return events.sortedByStart()
    }

    /**
     * Every event in one round (matchday) of a season ([season] defaults to the current one).
     * Premium: filtered from the season schedule. Free key: v1's undocumented `eventsround.php`,
     * which returns the whole round (premium keys get 404 from it, hence the two routes).
     */
    public suspend fun roundEvents(leagueId: Long, round: Int, season: String? = null): List<Event> {
        if (premium) return seasonEvents(leagueId, season).filter { it.round == round }
        val name = season ?: currentSeason(leagueId) ?: return emptyList()
        return v1.schedule.round(leagueId, round, name).sortedByStart()
    }

    /**
     * A league's events starting in the next [days] UTC days, beginning with [from].
     * Premium: filtered from the season schedule (one or two calls). Free key: one
     * `eventsday` call per day, each limited to 3 events.
     */
    public suspend fun upcomingLeagueEvents(
        leagueId: Long,
        days: Int = 7,
        from: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): List<Event> {
        require(days in 1..31) { "days must be between 1 and 31" }
        val dates = (0 until days).map { from.plusDays(it.toLong()) }
        if (!premium) {
            return eachDay(dates) { v1.schedule.day(it, leagueId = leagueId) }.distinctById().sortedByStart()
        }
        val wanted = dates.toSet()
        return seasonEvents(leagueId).filter { it.date in wanted }
    }

    /** A league's latest results. Premium: up to about 20. Free key: 1. */
    public suspend fun recentLeagueResults(leagueId: Long): List<Event> =
        (if (premium) v2.schedule.leaguePrevious(leagueId) else v1.schedule.leaguePast(leagueId)).sortedByStart()

    /**
     * A team's schedule across all competitions, past and future.
     * Premium: the full schedule (one call). Free key: the next and the last event only,
     * and only home games (a free-key limit).
     */
    public suspend fun teamSchedule(teamId: Long): List<Event> {
        if (premium) return v2.schedule.teamFull(teamId).sortedByStart()
        return coroutineScope {
            val next = async { v1.schedule.teamNext(teamId) }
            val last = async { v1.schedule.teamLast(teamId) }
            (last.await() + next.await()).distinctById().sortedByStart()
        }
    }

    /**
     * Events on a calendar day in your time zone. The API files events under their UTC date,
     * so a local day can span two API days; this fetches both and keeps the events that start
     * on [date] in [zone]. Optionally narrowed to a [sport] or a [leagueId].
     * Free key: at most 3 events per UTC day.
     */
    public suspend fun eventsOnLocalDate(
        date: LocalDate,
        zone: ZoneId,
        sport: String? = null,
        leagueId: Long? = null,
    ): List<Event> {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val utcDays = generateSequence(start.atZone(ZoneOffset.UTC).toLocalDate()) { it.plusDays(1) }
            .takeWhile { !it.atStartOfDay(ZoneOffset.UTC).toInstant().isAfter(end.minusNanos(1)) }
            .toList()
        return eachDay(utcDays) { v1.schedule.day(it, sport = sport, leagueId = leagueId) }
            .distinctById()
            .filter { e -> e.timestamp?.let { it >= start && it < end } ?: (e.date == date) }
            .sortedByStart()
    }

    /**
     * Games in progress, optionally for one [sport] (e.g. `Soccer`) or one [leagueId].
     * Premium: v2 live scores. Free key: v1's undocumented live feed, which needs a sport;
     * for a league, the league's sport is looked up and the results filtered.
     *
     * Entries can be stale (a finished game may linger); check [LiveScore.updated].
     */
    public suspend fun liveScores(sport: String? = null, leagueId: Long? = null): List<LiveScore> {
        if (premium) {
            return when {
                leagueId != null -> v2.live.league(leagueId)
                sport != null -> v2.live.sport(sport)
                else -> v2.live.all()
            }
        }
        val sportName = sport ?: leagueId?.let { v1.lookup.league(it)?.sport }
            ?: throw IllegalArgumentException("With the free key, live scores need a sport or a leagueId")
        val scores = v1.live.sport(sportName)
        return if (leagueId != null) scores.filter { it.leagueId == leagueId } else scores
    }

    /**
     * A league's teams, with badges.
     * Premium: one call. Free key: the league's name is looked up, then up to 10 teams.
     */
    public suspend fun leagueTeams(leagueId: Long): List<Team> {
        if (premium) return v2.list.teams(leagueId)
        val name = v1.lookup.league(leagueId)?.name ?: return emptyList()
        return v1.list.teamsInLeague(name)
    }

    /** The channels showing an event. Free key: at most 2. */
    public suspend fun eventChannels(eventId: Long): List<TvListing> =
        if (premium) v2.lookup.eventTv(eventId) else v1.lookup.eventTv(eventId)

    /**
     * A country's TV listings for the next [days] days from [from], optionally for one [sport].
     * Premium: about a week of the country's listings in one call, filtered here.
     * Free key: one call per day, which needs a [sport], and returns 1 listing per call.
     */
    public suspend fun tvListings(
        country: String,
        sport: String? = null,
        days: Int = 7,
        from: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): List<TvListing> {
        require(days in 1..31) { "days must be between 1 and 31" }
        val dates = (0 until days).map { from.plusDays(it.toLong()) }
        val listings = if (premium) {
            val wanted = dates.toSet()
            v2.tv.country(country).filter { it.date in wanted && (sport == null || it.sport.equals(sport, ignoreCase = true)) }
        } else {
            requireNotNull(sport) { "With the free key, TV listings for a country need a sport" }
            eachDay(dates) { v1.tv.day(it, sport = sport, country = country) }
        }
        return listings.sortedWith(compareBy(nullsLast()) { it.timestamp })
    }

    private suspend fun <T> eachDay(dates: List<LocalDate>, call: suspend (LocalDate) -> List<T>): List<T> =
        coroutineScope { dates.map { async { call(it) } }.awaitAll().flatten() }
}

private fun List<Event>.sortedByStart() = sortedWith(compareBy(nullsLast()) { it.timestamp })

private fun List<Event>.distinctById() = distinctBy { it.id ?: it.raw }
