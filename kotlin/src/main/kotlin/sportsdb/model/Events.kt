package sportsdb.model

import kotlinx.serialization.json.JsonObject
import sportsdb.internal.bool
import sportsdb.internal.date
import sportsdb.internal.double
import sportsdb.internal.id
import sportsdb.internal.instant
import sportsdb.internal.int
import sportsdb.internal.localDateTime
import sportsdb.internal.locked
import sportsdb.internal.raw
import sportsdb.internal.str
import sportsdb.internal.time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A game, match, race or other event. Also used for video highlights.
 *
 * Schedules and lists fill fewer fields than a lookup by id: v1 `eventsseason.php` and v2
 * `schedule/...` leave out descriptions, officials, spectators and the like.
 *
 * **Times:** [timestamp], [date] and [time] are UTC. [localDate] and [localTime] are the
 * venue's local time and are often missing for future events.
 */
public class Event internal constructor(
    /** `idEvent` */ public val id: Long?,
    /** `strEvent`, e.g. `Arsenal vs Chelsea` */ public val name: String?,
    /** `strEventAlternate`, e.g. `Chelsea @ Arsenal` */ public val alternateName: String?,
    /** `strFilename`: `{league} {date} {home} vs {away}`, searchable with `searchfilename.php` */ public val filename: String?,
    /** `strSport` */ public val sport: String?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strLeague` */ public val league: String?,
    /** `strLeagueBadge` */ public val leagueBadge: String?,
    /** `strSeason` */ public val season: String?,
    /** `intRound`: matchday or round number, or a stage code (125 quarter-final … 500 pre-season); see [stage] */ public val round: Int?,
    /** `strGroup`: a cup group or conference */ public val group: String?,
    /** `idHomeTeam` */ public val homeTeamId: Long?,
    /** `strHomeTeam` */ public val homeTeam: String?,
    /** `strHomeTeamBadge` */ public val homeTeamBadge: String?,
    /** `idAwayTeam` */ public val awayTeamId: Long?,
    /** `strAwayTeam` */ public val awayTeam: String?,
    /** `strAwayTeamBadge` */ public val awayTeamBadge: String?,
    /** `intHomeScore`: null until the event has a score */ public val homeScore: Int?,
    /** `intAwayScore` */ public val awayScore: Int?,
    /** `intHomeScoreExtra`: extra time or shoot-out */ public val homeScoreExtra: Int?,
    /** `intAwayScoreExtra` */ public val awayScoreExtra: Int?,
    /** `strTimestamp`: the start time (UTC) */ public val timestamp: Instant?,
    /** `dateEvent` (UTC) */ public val date: LocalDate?,
    /** `strTime` (UTC) */ public val time: LocalTime?,
    /** `dateEventLocal`: the venue's local date */ public val localDate: LocalDate?,
    /** `strTimeLocal`: the venue's local time */ public val localTime: LocalTime?,
    /** `strStatus`: the raw code (`NS`, `2H`, `FT`, `Q3`, `PST`...); see [status] */ public val statusCode: String?,
    /** `strPostponed` */ public val isPostponed: Boolean?,
    /** `idVenue` */ public val venueId: Long?,
    /** `strVenue` */ public val venue: String?,
    /** `strCity` */ public val city: String?,
    /** `strCountry` */ public val country: String?,
    /** `intSpectators` */ public val spectators: Int?,
    /** `strOfficial`: the referee */ public val official: String?,
    /** `strResult`: free-text result, sometimes with HTML `<br>` */ public val resultText: String?,
    /** `strDescriptionEN` */ public val description: String?,
    /** `strThumb` */ public val thumb: String?,
    /** `strPoster` */ public val poster: String?,
    /** `strBanner` */ public val banner: String?,
    /** `strSquare` */ public val square: String?,
    /** `strFanart` */ public val fanart: String?,
    /** `strMap` */ public val map: String?,
    /** `strVideo`: a highlights video, usually YouTube */ public val video: String?,
    /** `strTweet1` */ public val tweet: String?,
    /** `strWeather` */ public val weather: String?,
    /** `intScore`: the users' rating of the event */ public val rating: Double?,
    /** `intScoreVotes` */ public val ratingVotes: Int?,
    /** `strLocked` */ public val isLocked: Boolean?,
    /** `idAPIfootball` */ public val apiFootballId: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Event(id=$id, name=$name, timestamp=$timestamp, statusCode=$statusCode)"

    /** [statusCode] read as a broad status. */
    public val status: EventStatus get() = EventStatus.of(statusCode)

    /** The stage when [round] is a stage code (e.g. 200 = final), or null for an ordinary round. */
    public val stage: RoundStage? get() = RoundStage.of(round)
}

internal fun JsonObject.toEvent() = Event(
    id = id("idEvent"),
    name = str("strEvent"),
    alternateName = str("strEventAlternate"),
    filename = str("strFilename"),
    sport = str("strSport"),
    leagueId = id("idLeague"),
    league = str("strLeague"),
    leagueBadge = str("strLeagueBadge"),
    season = str("strSeason"),
    round = int("intRound"),
    group = str("strGroup"),
    homeTeamId = id("idHomeTeam"),
    homeTeam = str("strHomeTeam"),
    homeTeamBadge = str("strHomeTeamBadge"),
    awayTeamId = id("idAwayTeam"),
    awayTeam = str("strAwayTeam"),
    awayTeamBadge = str("strAwayTeamBadge"),
    homeScore = int("intHomeScore"),
    awayScore = int("intAwayScore"),
    homeScoreExtra = int("intHomeScoreExtra"),
    awayScoreExtra = int("intAwayScoreExtra"),
    timestamp = instant("strTimestamp"),
    date = date("dateEvent"),
    time = time("strTime"),
    localDate = date("dateEventLocal"),
    localTime = time("strTimeLocal"),
    statusCode = str("strStatus"),
    isPostponed = bool("strPostponed"),
    venueId = id("idVenue"),
    venue = str("strVenue"),
    city = str("strCity"),
    country = str("strCountry"),
    spectators = int("intSpectators"),
    official = str("strOfficial"),
    resultText = str("strResult"),
    description = str("strDescriptionEN"),
    thumb = str("strThumb"),
    poster = str("strPoster"),
    banner = str("strBanner"),
    square = str("strSquare"),
    fanart = str("strFanart"),
    map = str("strMap"),
    video = str("strVideo"),
    tweet = str("strTweet1"),
    weather = str("strWeather"),
    rating = double("intScore"),
    ratingVotes = int("intScoreVotes"),
    isLocked = locked(),
    apiFootballId = id("idAPIfootball"),
    raw = raw(),
)

/** One row of a league table (`lookuptable.php`). v1 only; there is no v2 equivalent. */
public class Standing internal constructor(
    /** `idStanding` */ public val id: Long?,
    /** `intRank` */ public val rank: Int?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strBadge` */ public val badge: String?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strLeague` */ public val league: String?,
    /** `strSeason` */ public val season: String?,
    /** `strGroup` */ public val group: String?,
    /** `strForm`: recent results, newest last, e.g. `WWDLW` */ public val form: String?,
    /** `strDescription`: what this position leads to, e.g. `Promotion - Champions League` */ public val description: String?,
    /** `intPlayed` */ public val played: Int?,
    /** `intWin` */ public val won: Int?,
    /** `intDraw` */ public val drawn: Int?,
    /** `intLoss` */ public val lost: Int?,
    /** `intGoalsFor` */ public val goalsFor: Int?,
    /** `intGoalsAgainst` */ public val goalsAgainst: Int?,
    /** `intGoalDifference` */ public val goalDifference: Int?,
    /** `intPoints` */ public val points: Int?,
    /** `dateUpdated` (zone not stated) */ public val updated: LocalDateTime?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Standing(rank=$rank, team=$team, points=$points)"
}

internal fun JsonObject.toStanding() = Standing(
    id = id("idStanding"),
    rank = int("intRank"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    badge = str("strBadge"),
    leagueId = id("idLeague"),
    league = str("strLeague"),
    season = str("strSeason"),
    group = str("strGroup"),
    form = str("strForm"),
    description = str("strDescription"),
    played = int("intPlayed"),
    won = int("intWin"),
    drawn = int("intDraw"),
    lost = int("intLoss"),
    goalsFor = int("intGoalsFor"),
    goalsAgainst = int("intGoalsAgainst"),
    goalDifference = int("intGoalDifference"),
    points = int("intPoints"),
    updated = localDateTime("dateUpdated"),
    raw = raw(),
)

/** One player in an event's lineup (`lookuplineup.php`, `lookup/event_lineup`). */
public class LineupEntry internal constructor(
    /** `idLineup` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strEvent` (v2) */ public val event: String?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strHome`: true for the home side */ public val isHome: Boolean?,
    /** `strSubstitute`: true for the bench */ public val isSubstitute: Boolean?,
    /** `strPosition` */ public val position: String?,
    /** `strPositionShort` (v2), e.g. `G`, `D` */ public val positionShort: String?,
    /** `strFormation` (v2), e.g. `4-3-3` */ public val formation: String?,
    /** `intSquadNumber` */ public val squadNumber: Int?,
    /** `strCountry` (v2) */ public val country: String?,
    /** `strSeason` (v2) */ public val season: String?,
    /** `strThumb` (v1) */ public val thumb: String?,
    /** `strCutout` */ public val cutout: String?,
    /** `strRender` (v1) */ public val render: String?,
    /** `idAPIfootball` (v2) */ public val apiFootballId: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "LineupEntry(eventId=$eventId, player=$player, team=$team)"
}

internal fun JsonObject.toLineupEntry() = LineupEntry(
    id = id("idLineup"),
    eventId = id("idEvent"),
    event = str("strEvent"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    isHome = bool("strHome"),
    isSubstitute = bool("strSubstitute"),
    position = str("strPosition"),
    positionShort = str("strPositionShort"),
    formation = str("strFormation"),
    squadNumber = int("intSquadNumber"),
    country = str("strCountry"),
    season = str("strSeason"),
    thumb = str("strThumb"),
    cutout = str("strCutout"),
    render = str("strRender"),
    apiFootballId = id("idAPIfootball"),
    raw = raw(),
)

/** A goal, card or substitution during an event (`lookuptimeline.php`, `lookup/event_timeline`). */
public class TimelineEntry internal constructor(
    /** `idTimeline` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strEvent` */ public val event: String?,
    /** `dateEvent` */ public val date: LocalDate?,
    /** `strSeason` */ public val season: String?,
    /** `intTime`: the minute */ public val minute: Int?,
    /** `strPeriod` */ public val period: String?,
    /** `strTimeline`: the kind of entry, e.g. `Goal`, `Card`, `subst` */ public val type: String?,
    /** `strTimelineDetail`, e.g. `Yellow Card`, `Normal Goal`, or the player coming on */ public val detail: String?,
    /** `strComment` */ public val comment: String?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `strCutout` */ public val cutout: String?,
    /** `idAssist` */ public val assistId: Long?,
    /** `strAssist` */ public val assist: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strHome` */ public val isHome: Boolean?,
    /** `idAPIfootball` */ public val apiFootballId: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "TimelineEntry(eventId=$eventId, minute=$minute, type=$type, player=$player)"
}

internal fun JsonObject.toTimelineEntry() = TimelineEntry(
    id = id("idTimeline"),
    eventId = id("idEvent"),
    event = str("strEvent"),
    date = date("dateEvent"),
    season = str("strSeason"),
    minute = int("intTime"),
    period = str("strPeriod"),
    type = str("strTimeline"),
    detail = str("strTimelineDetail"),
    comment = str("strComment"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    cutout = str("strCutout"),
    assistId = id("idAssist"),
    assist = str("strAssist"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    isHome = bool("strHome"),
    apiFootballId = id("idAPIfootball"),
    raw = raw(),
)

/** One team statistic for an event, home vs away (`lookupeventstats.php`, `lookup/event_stats`). */
public class EventStat internal constructor(
    /** `idStatistic` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strEvent` */ public val event: String?,
    /** `strStat`, e.g. `Shots on Goal`, `Ball Possession` */ public val name: String?,
    /** `intHome` */ public val home: Double?,
    /** `intAway` */ public val away: Double?,
    /** `idApiFootball` (note the different capitalisation in the API) */ public val apiFootballId: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "EventStat(eventId=$eventId, name=$name, home=$home, away=$away)"
}

internal fun JsonObject.toEventStat() = EventStat(
    id = id("idStatistic"),
    eventId = id("idEvent"),
    event = str("strEvent"),
    name = str("strStat"),
    home = double("intHome"),
    away = double("intAway"),
    apiFootballId = id("idApiFootball"),
    raw = raw(),
)

/** One broadcast of an event on one channel (v1 `lookuptv.php`/`eventstv.php`, v2 `lookup/event_tv`/`filter/tv`). */
public class TvListing internal constructor(
    /** `id` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strEvent` */ public val event: String?,
    /** `strSport` */ public val sport: String?,
    /** `strSeason` */ public val season: String?,
    /** `idChannel` */ public val channelId: Long?,
    /** `strChannel`, e.g. `TSN 1` */ public val channel: String?,
    /** `strLogo`: the channel logo */ public val channelLogo: String?,
    /** `strCountry`: the channel's country */ public val country: String?,
    /** `strEventCountry`: where the event takes place */ public val eventCountry: String?,
    /** `strTimeStamp` (capital S, space-separated in the API): broadcast start, UTC */ public val timestamp: Instant?,
    /** `dateEvent` */ public val date: LocalDate?,
    /** `strTime` */ public val time: LocalTime?,
    /** `intDivision` */ public val division: Int?,
    /** `strEventThumb` */ public val eventThumb: String?,
    /** `strEventPoster` */ public val eventPoster: String?,
    /** `strEventBanner` */ public val eventBanner: String?,
    /** `strEventSquare` */ public val eventSquare: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "TvListing(eventId=$eventId, channel=$channel, timestamp=$timestamp)"
}

internal fun JsonObject.toTvListing() = TvListing(
    id = id("id"),
    eventId = id("idEvent"),
    event = str("strEvent"),
    sport = str("strSport"),
    season = str("strSeason"),
    channelId = id("idChannel"),
    channel = str("strChannel"),
    channelLogo = str("strLogo"),
    country = str("strCountry"),
    eventCountry = str("strEventCountry"),
    timestamp = instant("strTimeStamp") ?: instant("strTimestamp"),
    date = date("dateEvent"),
    time = time("strTime"),
    division = int("intDivision"),
    eventThumb = str("strEventThumb"),
    eventPoster = str("strEventPoster"),
    eventBanner = str("strEventBanner"),
    eventSquare = str("strEventSquare"),
    raw = raw(),
)

/** A game in progress (`livescore.php`, `livescore/...`). */
public class LiveScore internal constructor(
    /** `idLiveScore` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strSport` */ public val sport: String?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strLeague` */ public val league: String?,
    /** `intDivision` */ public val division: Int?,
    /** `idHomeTeam` */ public val homeTeamId: Long?,
    /** `strHomeTeam` */ public val homeTeam: String?,
    /** `strHomeTeamBadge` */ public val homeTeamBadge: String?,
    /** `idAwayTeam` */ public val awayTeamId: Long?,
    /** `strAwayTeam` */ public val awayTeam: String?,
    /** `strAwayTeamBadge` */ public val awayTeamBadge: String?,
    /** `intHomeScore` */ public val homeScore: Int?,
    /** `intAwayScore` */ public val awayScore: Int?,
    /** `strStatus`: the raw code; see [status] */ public val statusCode: String?,
    /** `strProgress`: the minute (soccer) or `Final` */ public val progress: String?,
    /** `strEventTime`: kick-off, `HH:mm` */ public val eventTime: LocalTime?,
    /** `dateEvent` */ public val date: LocalDate?,
    /** `strTimestamp`: kick-off, UTC */ public val timestamp: Instant?,
    /** `updated`: when this score was last refreshed (zone not stated) */ public val updated: LocalDateTime?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "LiveScore(eventId=$eventId, homeTeam=$homeTeam, homeScore=$homeScore, awayScore=$awayScore, awayTeam=$awayTeam, statusCode=$statusCode)"

    public val status: EventStatus get() = EventStatus.of(statusCode)
}

internal fun JsonObject.toLiveScore() = LiveScore(
    id = id("idLiveScore"),
    eventId = id("idEvent"),
    sport = str("strSport"),
    leagueId = id("idLeague"),
    league = str("strLeague"),
    division = int("intDivision"),
    homeTeamId = id("idHomeTeam"),
    homeTeam = str("strHomeTeam"),
    homeTeamBadge = str("strHomeTeamBadge"),
    awayTeamId = id("idAwayTeam"),
    awayTeam = str("strAwayTeam"),
    awayTeamBadge = str("strAwayTeamBadge"),
    homeScore = int("intHomeScore"),
    awayScore = int("intAwayScore"),
    statusCode = str("strStatus"),
    progress = str("strProgress"),
    eventTime = time("strEventTime"),
    date = date("dateEvent"),
    timestamp = instant("strTimestamp"),
    updated = localDateTime("updated"),
    raw = raw(),
)
