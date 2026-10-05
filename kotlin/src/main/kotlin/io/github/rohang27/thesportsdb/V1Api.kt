package io.github.rohang27.thesportsdb

import io.github.rohang27.thesportsdb.cache.Freshness.LIVE
import io.github.rohang27.thesportsdb.cache.Freshness.MEDIUM
import io.github.rohang27.thesportsdb.cache.Freshness.SLOW
import io.github.rohang27.thesportsdb.cache.Freshness.STATIC
import io.github.rohang27.thesportsdb.internal.Requester
import io.github.rohang27.thesportsdb.model.Contract
import io.github.rohang27.thesportsdb.model.Country
import io.github.rohang27.thesportsdb.model.Equipment
import io.github.rohang27.thesportsdb.model.Event
import io.github.rohang27.thesportsdb.model.EventResult
import io.github.rohang27.thesportsdb.model.EventStat
import io.github.rohang27.thesportsdb.model.FormerTeam
import io.github.rohang27.thesportsdb.model.Honour
import io.github.rohang27.thesportsdb.model.League
import io.github.rohang27.thesportsdb.model.LineupEntry
import io.github.rohang27.thesportsdb.model.LiveScore
import io.github.rohang27.thesportsdb.model.Milestone
import io.github.rohang27.thesportsdb.model.Player
import io.github.rohang27.thesportsdb.model.PlayerStat
import io.github.rohang27.thesportsdb.model.Season
import io.github.rohang27.thesportsdb.model.Sport
import io.github.rohang27.thesportsdb.model.Standing
import io.github.rohang27.thesportsdb.model.Team
import io.github.rohang27.thesportsdb.model.TimelineEntry
import io.github.rohang27.thesportsdb.model.TvListing
import io.github.rohang27.thesportsdb.model.Venue
import io.github.rohang27.thesportsdb.model.toContract
import io.github.rohang27.thesportsdb.model.toCountry
import io.github.rohang27.thesportsdb.model.toEquipment
import io.github.rohang27.thesportsdb.model.toEvent
import io.github.rohang27.thesportsdb.model.toEventResult
import io.github.rohang27.thesportsdb.model.toEventStat
import io.github.rohang27.thesportsdb.model.toFormerTeam
import io.github.rohang27.thesportsdb.model.toHonour
import io.github.rohang27.thesportsdb.model.toLeague
import io.github.rohang27.thesportsdb.model.toLineupEntry
import io.github.rohang27.thesportsdb.model.toLiveScore
import io.github.rohang27.thesportsdb.model.toMilestone
import io.github.rohang27.thesportsdb.model.toPlayer
import io.github.rohang27.thesportsdb.model.toPlayerStat
import io.github.rohang27.thesportsdb.model.toSeason
import io.github.rohang27.thesportsdb.model.toSport
import io.github.rohang27.thesportsdb.model.toStanding
import io.github.rohang27.thesportsdb.model.toTeam
import io.github.rohang27.thesportsdb.model.toTimelineEntry
import io.github.rohang27.thesportsdb.model.toTvListing
import io.github.rohang27.thesportsdb.model.toVenue
import java.time.LocalDate

/**
 * The v1 API: `https://www.thesportsdb.com/api/v1/json/{key}/{endpoint}.php`.
 *
 * Works with the free key `123` (small result limits, noted on each method) and with
 * premium keys (full results). Names may contain spaces; the library encodes them.
 */
public class V1Api internal constructor(r: Requester) {
    public val search: Search = Search(r)
    public val lookup: Lookup = Lookup(r)
    public val list: Lists = Lists(r)
    public val schedule: Schedule = Schedule(r)
    public val tv: Tv = Tv(r)
    public val video: Video = Video(r)
    public val live: Live = Live(r)

    /** Search endpoints. Free key: one result each. */
    public class Search internal constructor(private val r: Requester) {
        /** `searchteams.php?t=` — teams by name. Premium returns many matches (15 for "Arsenal"). */
        public suspend fun teams(name: String): List<Team> =
            r.v1("searchteams.php", "teams", SLOW, "t" to name).map { it.toTeam() }

        /**
         * `searchevents.php?e=` — events by name, `Home vs Away`. Narrow with a [season]
         * (`2016-2017`) or a [date]; pass at most one of them.
         */
        public suspend fun events(name: String, season: String? = null, date: LocalDate? = null): List<Event> {
            require(season == null || date == null) { "Pass season or date, not both" }
            return r.v1("searchevents.php", "event", MEDIUM, "e" to name, "s" to season, "d" to date)
                .map { it.toEvent() }
        }

        /**
         * `searchfilename.php?e=` — events by their [Event.filename], e.g.
         * `English Premier League 2015-04-26 Arsenal vs Chelsea`.
         */
        public suspend fun eventsByFilename(filename: String, season: String? = null): List<Event> =
            r.v1("searchfilename.php", "event", MEDIUM, "e" to filename, "s" to season).map { it.toEvent() }

        /** `searchplayers.php?p=` — players by name. Fills only a summary of each player. */
        public suspend fun players(name: String): List<Player> =
            r.v1("searchplayers.php", "player", SLOW, "p" to name).map { it.toPlayer() }

        /** `searchvenues.php?v=` — venues by name. */
        public suspend fun venues(name: String): List<Venue> =
            r.v1("searchvenues.php", "venues", SLOW, "v" to name).map { it.toVenue() }
    }

    /** Lookups by id. */
    public class Lookup internal constructor(private val r: Requester) {
        /** `lookupleague.php?id=` */
        public suspend fun league(id: Long): League? =
            r.v1("lookupleague.php", "leagues", SLOW, "id" to id).firstOrNull()?.toLeague()

        /**
         * `lookuptable.php?l=` — the league table. Free key: top 5 rows. Only some leagues
         * have tables (mostly featured soccer leagues). Omit [season] for the current one.
         */
        public suspend fun table(leagueId: Long, season: String? = null): List<Standing> =
            r.v1("lookuptable.php", "table", MEDIUM, "l" to leagueId, "s" to season).map { it.toStanding() }

        /** `lookupteam.php?id=` */
        public suspend fun team(id: Long): Team? =
            r.v1("lookupteam.php", "teams", SLOW, "id" to id).firstOrNull()?.toTeam()

        /** `lookupequipment.php?id=` — a team's kits over the seasons. Free key: 2. */
        public suspend fun equipment(teamId: Long): List<Equipment> =
            r.v1("lookupequipment.php", "equipment", SLOW, "id" to teamId).map { it.toEquipment() }

        /** `lookupplayer.php?id=` */
        public suspend fun player(id: Long): Player? =
            r.v1("lookupplayer.php", "players", SLOW, "id" to id).firstOrNull()?.toPlayer()

        /** `lookuphonours.php?id=` — a player's trophies. Free key: 5. */
        public suspend fun honours(playerId: Long): List<Honour> =
            r.v1("lookuphonours.php", "honours", SLOW, "id" to playerId).map { it.toHonour() }

        /** `lookupformerteams.php?id=` — a player's previous teams. Free key: 5. */
        public suspend fun formerTeams(playerId: Long): List<FormerTeam> =
            r.v1("lookupformerteams.php", "formerteams", SLOW, "id" to playerId).map { it.toFormerTeam() }

        /** `lookupmilestones.php?id=` — a player's milestones and awards. Free key: 5. */
        public suspend fun milestones(playerId: Long): List<Milestone> =
            r.v1("lookupmilestones.php", "milestones", SLOW, "id" to playerId).map { it.toMilestone() }

        /** `lookupcontracts.php?id=` — a player's contracts. Free key: 1. */
        public suspend fun contracts(playerId: Long): List<Contract> =
            r.v1("lookupcontracts.php", "contracts", SLOW, "id" to playerId).map { it.toContract() }

        /** `playerresults.php?id=` — an individual-sport player's results. Free key: 5. */
        public suspend fun playerResults(playerId: Long): List<EventResult> =
            r.v1("playerresults.php", "results", MEDIUM, "id" to playerId).map { it.toEventResult() }

        /** `lookupplayerstats.php?id=` — a player's season statistics. Free key: 10. */
        public suspend fun playerStats(playerId: Long): List<PlayerStat> =
            r.v1("lookupplayerstats.php", "playerstats", MEDIUM, "id" to playerId).map { it.toPlayerStat() }

        /** `lookupevent.php?id=` */
        public suspend fun event(id: Long): Event? =
            r.v1("lookupevent.php", "events", MEDIUM, "id" to id).firstOrNull()?.toEvent()

        /** `eventresults.php?id=` — every competitor's result in an individual-sport event. Free key: 5. */
        public suspend fun eventResults(eventId: Long): List<EventResult> =
            r.v1("eventresults.php", "results", MEDIUM, "id" to eventId).map { it.toEventResult() }

        /** `lookuplineup.php?id=` — an event's lineups. Free key: 5. */
        public suspend fun lineup(eventId: Long): List<LineupEntry> =
            r.v1("lookuplineup.php", "lineup", MEDIUM, "id" to eventId).map { it.toLineupEntry() }

        /** `lookuptimeline.php?id=` — goals, cards and substitutions. Free key: 5. */
        public suspend fun timeline(eventId: Long): List<TimelineEntry> =
            r.v1("lookuptimeline.php", "timeline", MEDIUM, "id" to eventId).map { it.toTimelineEntry() }

        /** `lookupeventstats.php?id=` — team statistics for an event. Free key: 5. */
        public suspend fun eventStats(eventId: Long): List<EventStat> =
            r.v1("lookupeventstats.php", "eventstats", MEDIUM, "id" to eventId).map { it.toEventStat() }

        /** `lookuptv.php?id=` — the channels showing an event. Free key: 2. */
        public suspend fun eventTv(eventId: Long): List<TvListing> =
            r.v1("lookuptv.php", "tvevent", MEDIUM, "id" to eventId).map { it.toTvListing() }

        /** `lookupvenue.php?id=` */
        public suspend fun venue(id: Long): Venue? =
            r.v1("lookupvenue.php", "venues", SLOW, "id" to id).firstOrNull()?.toVenue()
    }

    /** Lists. */
    public class Lists internal constructor(private val r: Requester) {
        /** `all_sports.php`. Free key: 2. */
        public suspend fun sports(): List<Sport> = r.v1("all_sports.php", "sports", STATIC).map { it.toSport() }

        /** `all_countries.php`: names and flags. Free key: 50. */
        public suspend fun countries(): List<Country> =
            r.v1("all_countries.php", "countries", STATIC).map { it.toCountry() }

        /** `all_leagues.php`: id, name and sport (plus alternate names with a premium key). Free key: 5. */
        public suspend fun leagues(): List<League> = r.v1("all_leagues.php", "leagues", STATIC).map { it.toLeague() }

        /**
         * `search_all_leagues.php?c=&s=` — full league records in a country, optionally for
         * one sport. Free key: 5–10.
         */
        public suspend fun leagues(country: String, sport: String? = null): List<League> =
            // The record key really is "countries".
            r.v1("search_all_leagues.php", "countries", SLOW, "c" to country, "s" to sport).map { it.toLeague() }

        /**
         * `search_all_seasons.php?id=` — a league's seasons. Season names alone unless you
         * ask for [badges], [posters] or [descriptions] (one per request). Free key: 5.
         */
        public suspend fun seasons(
            leagueId: Long,
            badges: Boolean = false,
            posters: Boolean = false,
            descriptions: Boolean = false,
        ): List<Season> {
            require(listOf(badges, posters, descriptions).count { it } <= 1) {
                "Ask for one of badges, posters or descriptions per request"
            }
            fun flag(on: Boolean) = if (on) 1 else null
            return r.v1(
                "search_all_seasons.php", "seasons", SLOW,
                "id" to leagueId, "badge" to flag(badges), "poster" to flag(posters), "description" to flag(descriptions),
            ).mapNotNull { it.toSeason() }
        }

        /** `search_all_teams.php?l=` — every team in a league, by league **name**. Free key: 10. */
        public suspend fun teamsInLeague(leagueName: String): List<Team> =
            r.v1("search_all_teams.php", "teams", SLOW, "l" to leagueName).map { it.toTeam() }

        /** `search_all_teams.php?s=&c=` — every team of a sport in a country. Free key: 10. */
        public suspend fun teamsInCountry(sport: String, country: String): List<Team> =
            r.v1("search_all_teams.php", "teams", SLOW, "s" to sport, "c" to country).map { it.toTeam() }

        /** `lookup_all_players.php?id=` — a team's squad. Free key: 10. */
        public suspend fun players(teamId: Long): List<Player> =
            r.v1("lookup_all_players.php", "player", SLOW, "id" to teamId).map { it.toPlayer() }
    }

    /** Schedules and results. All times in the returned events are UTC. */
    public class Schedule internal constructor(private val r: Requester) {
        /** `eventsnext.php?id=` — a team's next events. Free key: 1, and home games only. */
        public suspend fun teamNext(teamId: Long): List<Event> =
            r.v1("eventsnext.php", "events", MEDIUM, "id" to teamId).map { it.toEvent() }

        /** `eventslast.php?id=` — a team's last results. Free key: 1, and home games only. */
        public suspend fun teamLast(teamId: Long): List<Event> =
            // The record key here is "results", but the records are events.
            r.v1("eventslast.php", "results", MEDIUM, "id" to teamId).map { it.toEvent() }

        /** `eventsnextleague.php?id=` — a league's next events. Free key: 1. */
        public suspend fun leagueNext(leagueId: Long): List<Event> =
            r.v1("eventsnextleague.php", "events", MEDIUM, "id" to leagueId).map { it.toEvent() }

        /** `eventspastleague.php?id=` — a league's latest results. Free key: 1. */
        public suspend fun leaguePast(leagueId: Long): List<Event> =
            r.v1("eventspastleague.php", "events", MEDIUM, "id" to leagueId).map { it.toEvent() }

        /**
         * `eventsday.php?d=` — every event on a (UTC) day, optionally for one [sport] or one
         * league ([leagueId] or [leagueName]). Free key: 3. Premium: the whole day (900+).
         */
        public suspend fun day(
            date: LocalDate,
            sport: String? = null,
            leagueId: Long? = null,
            leagueName: String? = null,
        ): List<Event> {
            require(leagueId == null || leagueName == null) { "Pass leagueId or leagueName, not both" }
            return r.v1("eventsday.php", "events", MEDIUM, "d" to date, "s" to sport, "l" to (leagueId ?: leagueName))
                .map { it.toEvent() }
        }

        /**
         * `eventsround.php?id=&r=&s=` — every event in one round (matchday) of a season.
         * **Undocumented, and free keys only:** it answers the free keys (a whole round, e.g. 10
         * games) but returns HTTP 404 to premium keys. With a premium key, use
         * [SportsDbClient.helpers]' `roundEvents`, which filters the v2 season instead.
         */
        public suspend fun round(leagueId: Long, round: Int, season: String): List<Event> =
            r.v1("eventsround.php", "events", MEDIUM, "id" to leagueId, "r" to round, "s" to season).map { it.toEvent() }

        /** `eventsseason.php?id=&s=` — a whole season in one call (premium). Free key: 5–15. */
        public suspend fun season(leagueId: Long, season: String): List<Event> =
            r.v1("eventsseason.php", "events", MEDIUM, "id" to leagueId, "s" to season).map { it.toEvent() }
    }

    /** TV listings. Free key: 1 per call. */
    public class Tv internal constructor(private val r: Requester) {
        /**
         * `eventstv.php?d=` — TV listings for a day, optionally narrowed to a [sport], or to
         * a [country] **and** sport. The API returns an empty body when given a country
         * without a sport, so that combination is rejected here.
         */
        public suspend fun day(date: LocalDate, sport: String? = null, country: String? = null): List<TvListing> {
            require(country == null || sport != null) { "eventstv.php needs a sport when filtering by country" }
            return r.v1("eventstv.php", "tvevents", MEDIUM, "d" to date, "a" to country, "s" to sport)
                .map { it.toTvListing() }
        }

        /** `eventstv.php?c=` — upcoming listings on one channel, by name (e.g. `TSN 1`). */
        public suspend fun channel(name: String): List<TvListing> =
            r.v1("eventstv.php", "tvevents", MEDIUM, "c" to name).map { it.toTvListing() }

        /** `eventstv.php?id=` — upcoming listings on one channel, by [TvListing.channelId]. */
        public suspend fun channel(channelId: Long): List<TvListing> =
            r.v1("eventstv.php", "tvevents", MEDIUM, "id" to channelId).map { it.toTvListing() }
    }

    /** Video highlights. */
    public class Video internal constructor(private val r: Requester) {
        /**
         * `eventshighlights.php?d=` — events with highlight videos ([Event.video]) on a day,
         * optionally for one [leagueId] or [sport]. Free key: 2.
         */
        public suspend fun highlights(date: LocalDate, leagueId: Long? = null, sport: String? = null): List<Event> =
            r.v1("eventshighlights.php", "tvhighlights", MEDIUM, "d" to date, "l" to leagueId, "s" to sport)
                .map { it.toEvent() }
    }

    /**
     * Live scores on v1. **Not in the official v1 documentation** (v2 has the documented
     * live score endpoints), but it answers, including with the free key. It may change.
     */
    public class Live internal constructor(private val r: Requester) {
        /** `livescore.php?s=` — live games for a sport, e.g. `Soccer`. */
        public suspend fun sport(sport: String): List<LiveScore> =
            r.v1("livescore.php", "livescore", LIVE, "s" to sport).map { it.toLiveScore() }
    }
}
