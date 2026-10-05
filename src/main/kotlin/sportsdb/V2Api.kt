package sportsdb

import sportsdb.cache.Freshness.LIVE
import sportsdb.cache.Freshness.MEDIUM
import sportsdb.cache.Freshness.SLOW
import sportsdb.cache.Freshness.STATIC
import sportsdb.internal.Requester
import sportsdb.model.Contract
import sportsdb.model.Country
import sportsdb.model.Equipment
import sportsdb.model.Event
import sportsdb.model.EventResult
import sportsdb.model.EventStat
import sportsdb.model.FormerTeam
import sportsdb.model.Honour
import sportsdb.model.League
import sportsdb.model.LineupEntry
import sportsdb.model.LiveScore
import sportsdb.model.Milestone
import sportsdb.model.Player
import sportsdb.model.PlayerStat
import sportsdb.model.Season
import sportsdb.model.Sport
import sportsdb.model.Team
import sportsdb.model.TimelineEntry
import sportsdb.model.TvListing
import sportsdb.model.Venue
import sportsdb.model.toContract
import sportsdb.model.toCountry
import sportsdb.model.toEquipment
import sportsdb.model.toEvent
import sportsdb.model.toEventResult
import sportsdb.model.toEventStat
import sportsdb.model.toFormerTeam
import sportsdb.model.toHonour
import sportsdb.model.toLeague
import sportsdb.model.toLineupEntry
import sportsdb.model.toLiveScore
import sportsdb.model.toMilestone
import sportsdb.model.toPlayer
import sportsdb.model.toPlayerStat
import sportsdb.model.toSeason
import sportsdb.model.toSport
import sportsdb.model.toTeam
import sportsdb.model.toTimelineEntry
import sportsdb.model.toTvListing
import sportsdb.model.toVenue
import java.time.LocalDate

/**
 * The v2 API: `https://www.thesportsdb.com/api/v2/json/{group}/{name}/{param}`, with the
 * key in the `X-API-KEY` header. **Premium keys only**: with the free key every call
 * throws [PremiumRequiredException] without touching the network.
 *
 * Every response's record key is the group name (`search`, `lookup`, ...).
 */
public class V2Api internal constructor(r: Requester) {
    public val search: Search = Search(r)
    public val lookup: Lookup = Lookup(r)
    public val list: Lists = Lists(r)
    public val all: All = All(r)
    public val schedule: Schedule = Schedule(r)
    public val tv: Tv = Tv(r)
    public val live: Live = Live(r)

    /** `search/...`: up to 10 results, summary fields only. Look up by id for full records. */
    public class Search internal constructor(private val r: Requester) {
        public suspend fun leagues(name: String): List<League> =
            r.v2("search", SLOW, "search", "league", name).map { it.toLeague() }

        public suspend fun teams(name: String): List<Team> =
            r.v2("search", SLOW, "search", "team", name).map { it.toTeam() }

        public suspend fun players(name: String): List<Player> =
            r.v2("search", SLOW, "search", "player", name).map { it.toPlayer() }

        /**
         * Events by name. Needs the event's exact name as TheSportsDB writes it; a loose
         * `Arsenal vs Chelsea` finds nothing where v1 `searchevents.php` finds matches.
         */
        public suspend fun events(name: String): List<Event> =
            r.v2("search", MEDIUM, "search", "event", name).map { it.toEvent() }

        public suspend fun venues(name: String): List<Venue> =
            r.v2("search", SLOW, "search", "venue", name).map { it.toVenue() }
    }

    /** `lookup/...`: full records by id. */
    public class Lookup internal constructor(private val r: Requester) {
        public suspend fun league(id: Long): League? = one(SLOW, "league", id)?.toLeague()
        public suspend fun team(id: Long): Team? = one(SLOW, "team", id)?.toTeam()
        public suspend fun teamEquipment(teamId: Long): List<Equipment> = many(SLOW, "team_equipment", teamId).map { it.toEquipment() }
        public suspend fun player(id: Long): Player? = one(SLOW, "player", id)?.toPlayer()
        public suspend fun playerContracts(playerId: Long): List<Contract> = many(SLOW, "player_contracts", playerId).map { it.toContract() }
        public suspend fun playerResults(playerId: Long): List<EventResult> = many(MEDIUM, "player_results", playerId).map { it.toEventResult() }
        public suspend fun playerHonours(playerId: Long): List<Honour> = many(SLOW, "player_honours", playerId).map { it.toHonour() }
        public suspend fun playerMilestones(playerId: Long): List<Milestone> = many(SLOW, "player_milestones", playerId).map { it.toMilestone() }

        /** A player's former teams (v1 calls these `formerteams`). */
        public suspend fun playerTeams(playerId: Long): List<FormerTeam> = many(SLOW, "player_teams", playerId).map { it.toFormerTeam() }
        public suspend fun playerStats(playerId: Long): List<PlayerStat> = many(MEDIUM, "player_stats", playerId).map { it.toPlayerStat() }
        public suspend fun event(id: Long): Event? = one(MEDIUM, "event", id)?.toEvent()
        public suspend fun eventLineup(eventId: Long): List<LineupEntry> = many(MEDIUM, "event_lineup", eventId).map { it.toLineupEntry() }
        public suspend fun eventResults(eventId: Long): List<EventResult> = many(MEDIUM, "event_results", eventId).map { it.toEventResult() }
        public suspend fun eventStats(eventId: Long): List<EventStat> = many(MEDIUM, "event_stats", eventId).map { it.toEventStat() }
        public suspend fun eventTimeline(eventId: Long): List<TimelineEntry> = many(MEDIUM, "event_timeline", eventId).map { it.toTimelineEntry() }

        /** The channels showing an event. */
        public suspend fun eventTv(eventId: Long): List<TvListing> = many(MEDIUM, "event_tv", eventId).map { it.toTvListing() }

        /** An event with its highlight video in [Event.video]. */
        public suspend fun eventHighlights(eventId: Long): List<Event> = many(MEDIUM, "event_highlights", eventId).map { it.toEvent() }
        public suspend fun venue(id: Long): Venue? = one(SLOW, "venue", id)?.toVenue()

        private suspend fun many(f: sportsdb.cache.Freshness, name: String, id: Long) = r.v2("lookup", f, "lookup", name, id)
        private suspend fun one(f: sportsdb.cache.Freshness, name: String, id: Long) = many(f, name, id).firstOrNull()
    }

    /** `list/...` */
    public class Lists internal constructor(private val r: Requester) {
        /** A league's teams with badges and colours. No alternate names: look teams up by id for those. */
        public suspend fun teams(leagueId: Long): List<Team> = r.v2("list", SLOW, "list", "teams", leagueId).map { it.toTeam() }

        /** A league's seasons with badges, posters and descriptions. */
        public suspend fun seasons(leagueId: Long): List<Season> =
            r.v2("list", SLOW, "list", "seasons", leagueId).mapNotNull { it.toSeason() }

        /** A team's squad (summary fields). */
        public suspend fun players(teamId: Long): List<Player> = r.v2("list", SLOW, "list", "players", teamId).map { it.toPlayer() }
    }

    /** `all/...`: complete catalogues. */
    public class All internal constructor(private val r: Requester) {
        public suspend fun countries(): List<Country> = r.v2("all", STATIC, "all", "countries").map { it.toCountry() }
        public suspend fun sports(): List<Sport> = r.v2("all", STATIC, "all", "sports").map { it.toSport() }

        /** Every league (about 1,500): id, name, sport and alternate names only. */
        public suspend fun leagues(): List<League> = r.v2("all", STATIC, "all", "leagues").map { it.toLeague() }
    }

    /** `schedule/...`. Times in the returned events are UTC. */
    public class Schedule internal constructor(private val r: Requester) {
        public suspend fun leagueNext(leagueId: Long): List<Event> = get("next", "league", leagueId)
        public suspend fun leaguePrevious(leagueId: Long): List<Event> = get("previous", "league", leagueId)
        public suspend fun teamNext(teamId: Long): List<Event> = get("next", "team", teamId)
        public suspend fun teamPrevious(teamId: Long): List<Event> = get("previous", "team", teamId)
        public suspend fun venueNext(venueId: Long): List<Event> = get("next", "venue", venueId)
        public suspend fun venuePrevious(venueId: Long): List<Event> = get("previous", "venue", venueId)

        /** A team's whole schedule, past and future, across all its competitions. */
        public suspend fun teamFull(teamId: Long): List<Event> = get("full", "team", teamId)

        /**
         * A league's whole season in one call (up to 3,000 events). Get the current season
         * name from [League.currentSeason].
         */
        public suspend fun leagueSeason(leagueId: Long, season: String): List<Event> =
            r.v2("schedule", MEDIUM, "schedule", "league", leagueId, season).map { it.toEvent() }

        private suspend fun get(vararg path: Any) = r.v2("schedule", MEDIUM, "schedule", *path).map { it.toEvent() }
    }

    /** `filter/tv/...`: TV listings. */
    public class Tv internal constructor(private val r: Requester) {
        /** Every listing worldwide on a day. */
        public suspend fun day(date: LocalDate): List<TvListing> = get("day", date)

        /** About a week of listings for a country, e.g. `Canada`. */
        public suspend fun country(country: String): List<TvListing> = get("country", country)

        /** Listings for a sport, e.g. `Ice Hockey`. */
        public suspend fun sport(sport: String): List<TvListing> = get("sport", sport)

        /**
         * Listings for a channel by name. The name must use TheSportsDB's spelling
         * (`TSN 1`, not `TSN1`); partial names match too (`Sportsnet` finds `SportsNet West`).
         */
        public suspend fun channel(name: String): List<TvListing> = get("channel", name)

        /** Listings for a channel by [TvListing.channelId]. */
        public suspend fun channel(channelId: Long): List<TvListing> = get("channelid", channelId)

        private suspend fun get(kind: String, value: Any) =
            r.v2("filter", MEDIUM, "filter", "tv", kind, value).map { it.toTvListing() }
    }

    /** `livescore/...`. Never cached unless your [sportsdb.cache.CachePolicy] says so. */
    public class Live internal constructor(private val r: Requester) {
        /** Live games in a sport, e.g. `soccer`, `Ice Hockey`. */
        public suspend fun sport(sport: String): List<LiveScore> = get(sport)

        /** Live games in one league. Empty when nothing is in play. */
        public suspend fun league(leagueId: Long): List<LiveScore> = get(leagueId)

        /** Every live game in every sport. */
        public suspend fun all(): List<LiveScore> = get("all")

        private suspend fun get(param: Any) = r.v2("livescore", LIVE, "livescore", param).map { it.toLiveScore() }
    }
}
