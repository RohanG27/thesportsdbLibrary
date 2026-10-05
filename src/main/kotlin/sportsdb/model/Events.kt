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
public data class Event(
    /** `idEvent` */ val id: Long?,
    /** `strEvent`, e.g. `Arsenal vs Chelsea` */ val name: String?,
    /** `strEventAlternate`, e.g. `Chelsea @ Arsenal` */ val alternateName: String?,
    /** `strFilename`: `{league} {date} {home} vs {away}`, searchable with `searchfilename.php` */ val filename: String?,
    /** `strSport` */ val sport: String?,
    /** `idLeague` */ val leagueId: Long?,
    /** `strLeague` */ val league: String?,
    /** `strLeagueBadge` */ val leagueBadge: String?,
    /** `strSeason` */ val season: String?,
    /** `intRound`: matchday or round number */ val round: Int?,
    /** `strGroup`: a cup group or conference */ val group: String?,
    /** `idHomeTeam` */ val homeTeamId: Long?,
    /** `strHomeTeam` */ val homeTeam: String?,
    /** `strHomeTeamBadge` */ val homeTeamBadge: String?,
    /** `idAwayTeam` */ val awayTeamId: Long?,
    /** `strAwayTeam` */ val awayTeam: String?,
    /** `strAwayTeamBadge` */ val awayTeamBadge: String?,
    /** `intHomeScore`: null until the event has a score */ val homeScore: Int?,
    /** `intAwayScore` */ val awayScore: Int?,
    /** `intHomeScoreExtra`: extra time or shoot-out */ val homeScoreExtra: Int?,
    /** `intAwayScoreExtra` */ val awayScoreExtra: Int?,
    /** `strTimestamp`: the start time (UTC) */ val timestamp: Instant?,
    /** `dateEvent` (UTC) */ val date: LocalDate?,
    /** `strTime` (UTC) */ val time: LocalTime?,
    /** `dateEventLocal`: the venue's local date */ val localDate: LocalDate?,
    /** `strTimeLocal`: the venue's local time */ val localTime: LocalTime?,
    /** `strStatus`: the raw code (`NS`, `2H`, `FT`, `Q3`, `PST`...); see [status] */ val statusCode: String?,
    /** `strPostponed` */ val isPostponed: Boolean?,
    /** `idVenue` */ val venueId: Long?,
    /** `strVenue` */ val venue: String?,
    /** `strCity` */ val city: String?,
    /** `strCountry` */ val country: String?,
    /** `intSpectators` */ val spectators: Int?,
    /** `strOfficial`: the referee */ val official: String?,
    /** `strResult`: free-text result, sometimes with HTML `<br>` */ val resultText: String?,
    /** `strDescriptionEN` */ val description: String?,
    /** `strThumb` */ val thumb: String?,
    /** `strPoster` */ val poster: String?,
    /** `strBanner` */ val banner: String?,
    /** `strSquare` */ val square: String?,
    /** `strFanart` */ val fanart: String?,
    /** `strMap` */ val map: String?,
    /** `strVideo`: a highlights video, usually YouTube */ val video: String?,
    /** `strTweet1` */ val tweet: String?,
    /** `strWeather` */ val weather: String?,
    /** `intScore`: the users' rating of the event */ val rating: Double?,
    /** `intScoreVotes` */ val ratingVotes: Int?,
    /** `strLocked` */ val isLocked: Boolean?,
    /** `idAPIfootball` */ val apiFootballId: Long?,
    val raw: RawRecord,
) {
    /** [statusCode] read as a broad status. */
    val status: EventStatus get() = EventStatus.of(statusCode)
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
public data class Standing(
    /** `idStanding` */ val id: Long?,
    /** `intRank` */ val rank: Int?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strBadge` */ val badge: String?,
    /** `idLeague` */ val leagueId: Long?,
    /** `strLeague` */ val league: String?,
    /** `strSeason` */ val season: String?,
    /** `strGroup` */ val group: String?,
    /** `strForm`: recent results, newest last, e.g. `WWDLW` */ val form: String?,
    /** `strDescription`: what this position leads to, e.g. `Promotion - Champions League` */ val description: String?,
    /** `intPlayed` */ val played: Int?,
    /** `intWin` */ val won: Int?,
    /** `intDraw` */ val drawn: Int?,
    /** `intLoss` */ val lost: Int?,
    /** `intGoalsFor` */ val goalsFor: Int?,
    /** `intGoalsAgainst` */ val goalsAgainst: Int?,
    /** `intGoalDifference` */ val goalDifference: Int?,
    /** `intPoints` */ val points: Int?,
    /** `dateUpdated` (zone not stated) */ val updated: LocalDateTime?,
    val raw: RawRecord,
)

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
public data class LineupEntry(
    /** `idLineup` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strEvent` (v2) */ val event: String?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strHome`: true for the home side */ val isHome: Boolean?,
    /** `strSubstitute`: true for the bench */ val isSubstitute: Boolean?,
    /** `strPosition` */ val position: String?,
    /** `strPositionShort` (v2), e.g. `G`, `D` */ val positionShort: String?,
    /** `strFormation` (v2), e.g. `4-3-3` */ val formation: String?,
    /** `intSquadNumber` */ val squadNumber: Int?,
    /** `strCountry` (v2) */ val country: String?,
    /** `strSeason` (v2) */ val season: String?,
    /** `strThumb` (v1) */ val thumb: String?,
    /** `strCutout` */ val cutout: String?,
    /** `strRender` (v1) */ val render: String?,
    /** `idAPIfootball` (v2) */ val apiFootballId: Long?,
    val raw: RawRecord,
)

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
public data class TimelineEntry(
    /** `idTimeline` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strEvent` */ val event: String?,
    /** `dateEvent` */ val date: LocalDate?,
    /** `strSeason` */ val season: String?,
    /** `intTime`: the minute */ val minute: Int?,
    /** `strPeriod` */ val period: String?,
    /** `strTimeline`: the kind of entry, e.g. `Goal`, `Card`, `subst` */ val type: String?,
    /** `strTimelineDetail`, e.g. `Yellow Card`, `Normal Goal`, or the player coming on */ val detail: String?,
    /** `strComment` */ val comment: String?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `strCutout` */ val cutout: String?,
    /** `idAssist` */ val assistId: Long?,
    /** `strAssist` */ val assist: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strHome` */ val isHome: Boolean?,
    /** `idAPIfootball` */ val apiFootballId: Long?,
    val raw: RawRecord,
)

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
public data class EventStat(
    /** `idStatistic` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strEvent` */ val event: String?,
    /** `strStat`, e.g. `Shots on Goal`, `Ball Possession` */ val name: String?,
    /** `intHome` */ val home: Double?,
    /** `intAway` */ val away: Double?,
    /** `idApiFootball` (note the different capitalisation in the API) */ val apiFootballId: Long?,
    val raw: RawRecord,
)

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
public data class TvListing(
    /** `id` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strEvent` */ val event: String?,
    /** `strSport` */ val sport: String?,
    /** `strSeason` */ val season: String?,
    /** `idChannel` */ val channelId: Long?,
    /** `strChannel`, e.g. `TSN 1` */ val channel: String?,
    /** `strLogo`: the channel logo */ val channelLogo: String?,
    /** `strCountry`: the channel's country */ val country: String?,
    /** `strEventCountry`: where the event takes place */ val eventCountry: String?,
    /** `strTimeStamp` (capital S, space-separated in the API): broadcast start, UTC */ val timestamp: Instant?,
    /** `dateEvent` */ val date: LocalDate?,
    /** `strTime` */ val time: LocalTime?,
    /** `intDivision` */ val division: Int?,
    /** `strEventThumb` */ val eventThumb: String?,
    /** `strEventPoster` */ val eventPoster: String?,
    /** `strEventBanner` */ val eventBanner: String?,
    /** `strEventSquare` */ val eventSquare: String?,
    val raw: RawRecord,
)

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
public data class LiveScore(
    /** `idLiveScore` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strSport` */ val sport: String?,
    /** `idLeague` */ val leagueId: Long?,
    /** `strLeague` */ val league: String?,
    /** `intDivision` */ val division: Int?,
    /** `idHomeTeam` */ val homeTeamId: Long?,
    /** `strHomeTeam` */ val homeTeam: String?,
    /** `strHomeTeamBadge` */ val homeTeamBadge: String?,
    /** `idAwayTeam` */ val awayTeamId: Long?,
    /** `strAwayTeam` */ val awayTeam: String?,
    /** `strAwayTeamBadge` */ val awayTeamBadge: String?,
    /** `intHomeScore` */ val homeScore: Int?,
    /** `intAwayScore` */ val awayScore: Int?,
    /** `strStatus`: the raw code; see [status] */ val statusCode: String?,
    /** `strProgress`: the minute (soccer) or `Final` */ val progress: String?,
    /** `strEventTime`: kick-off, `HH:mm` */ val eventTime: LocalTime?,
    /** `dateEvent` */ val date: LocalDate?,
    /** `strTimestamp`: kick-off, UTC */ val timestamp: Instant?,
    /** `updated`: when this score was last refreshed (zone not stated) */ val updated: LocalDateTime?,
    val raw: RawRecord,
) {
    val status: EventStatus get() = EventStatus.of(statusCode)
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
