package sportsdb.model

import kotlinx.serialization.json.JsonObject
import sportsdb.internal.bool
import sportsdb.internal.date
import sportsdb.internal.descriptions
import sportsdb.internal.id
import sportsdb.internal.int
import sportsdb.internal.list
import sportsdb.internal.locked
import sportsdb.internal.numbered
import sportsdb.internal.raw
import sportsdb.internal.socials
import sportsdb.internal.str
import sportsdb.internal.year
import java.time.LocalDate

/** A sport (`all_sports.php`, `all/sports`). */
public data class Sport(
    /** `idSport` */ val id: Long?,
    /** `strSport`, e.g. `Ice Hockey`. Use this name for sport filters. */ val name: String?,
    /** `strFormat`: `TeamvsTeam` or `EventSport`. */ val format: String?,
    /** `strSportDescription` */ val description: String?,
    /** `strSportThumb` */ val thumb: String?,
    /** `strSportThumbBW` */ val thumbBlackAndWhite: String?,
    /** `strSportIconGreen` */ val icon: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toSport() = Sport(
    id = id("idSport"),
    name = str("strSport"),
    format = str("strFormat"),
    description = str("strSportDescription"),
    thumb = str("strSportThumb"),
    thumbBlackAndWhite = str("strSportThumbBW"),
    icon = str("strSportIconGreen"),
    raw = raw(),
)

/** A country (`all_countries.php`, `all/countries`). v1 returns only [name] and [flag32]. */
public data class Country(
    /** `name_en`. Use this name for country filters. */ val name: String?,
    /** `name_fr` (v2) */ val nameFr: String?,
    /** `code` (v2), a two-letter code */ val code: String?,
    /** `flag_url_16` (v2) */ val flag16: String?,
    /** `flag_url_32` */ val flag32: String?,
    /** `flag_url_64` (v2) */ val flag64: String?,
    /** `idAPIfootball` (v2) */ val apiFootballId: Long?,
    val raw: RawRecord,
)

internal fun JsonObject.toCountry() = Country(
    name = str("name_en"),
    nameFr = str("name_fr"),
    code = str("code"),
    flag16 = str("flag_url_16"),
    flag32 = str("flag_url_32"),
    flag64 = str("flag_url_64"),
    apiFootballId = id("idAPIfootball"),
    raw = raw(),
)

/**
 * A league or cup. Lists (`all_leagues.php`, v2 `search/league`) fill only a few fields;
 * look it up by id for the rest.
 */
public data class League(
    /** `idLeague` */ val id: Long?,
    /** `strLeague` */ val name: String?,
    /** `strLeagueAlternate`, split on commas */ val alternateNames: List<String>,
    /** `strSport` */ val sport: String?,
    /** `strCountry` */ val country: String?,
    /** `strGender` */ val gender: String?,
    /** `strCurrentSeason`, e.g. `2026-2027` or `2026`. Pass it to season endpoints. */ val currentSeason: String?,
    /** `intDivision`: 1 for a top flight; cups often use 0 or 99 */ val division: Int?,
    /** `idCup`: true for a cup competition */ val isCup: Boolean?,
    /** `intFormedYear` */ val formedYear: Int?,
    /** `dateFirstEvent` */ val firstEventDate: LocalDate?,
    /** `strComplete`: whether TheSportsDB considers its data complete */ val isComplete: Boolean?,
    /** `strNaming`: the event-name template, e.g. `{strHomeTeam} vs {strAwayTeam}` */ val eventNaming: String?,
    /** `strTvRights`: free text */ val tvRights: String?,
    /** `strBadge` */ val badge: String?,
    /** `strLogo` */ val logo: String?,
    /** `strBanner` */ val banner: String?,
    /** `strPoster` */ val poster: String?,
    /** `strTrophy` */ val trophy: String?,
    /** `strFanart1`..`strFanart4` */ val fanart: List<String>,
    /** `strDescriptionEN`, `strDescriptionDE`, ... keyed by language code (`EN`, `DE`) */ val descriptions: Map<String, String>,
    val socials: Socials,
    /** `strLocked` */ val isLocked: Boolean?,
    /** `idAPIfootball` */ val apiFootballId: Long?,
    /** `idAPIfootballv3` */ val apiFootballV3Id: Long?,
    val raw: RawRecord,
) {
    /** The English description, if any. */
    val description: String? get() = descriptions["EN"]
}

internal fun JsonObject.toLeague() = League(
    id = id("idLeague"),
    name = str("strLeague"),
    alternateNames = list("strLeagueAlternate"),
    sport = str("strSport"),
    country = str("strCountry"),
    gender = str("strGender"),
    currentSeason = str("strCurrentSeason"),
    division = int("intDivision"),
    isCup = bool("idCup"),
    formedYear = year("intFormedYear"),
    firstEventDate = date("dateFirstEvent"),
    isComplete = bool("strComplete"),
    eventNaming = str("strNaming"),
    tvRights = str("strTvRights"),
    badge = str("strBadge"),
    logo = str("strLogo"),
    banner = str("strBanner"),
    poster = str("strPoster"),
    trophy = str("strTrophy"),
    fanart = numbered("strFanart", 4),
    descriptions = descriptions(),
    socials = socials(),
    isLocked = locked(),
    apiFootballId = id("idAPIfootball"),
    apiFootballV3Id = id("idAPIfootballv3"),
    raw = raw(),
)

/** A season of a league (`search_all_seasons.php`, `list/seasons`). */
public data class Season(
    /** `strSeason`, e.g. `2026-2027` */ val name: String,
    /** `strBadge` (v2, or v1 with `badge=1`) */ val badge: String?,
    /** `strPoster` (v2, or v1 with `poster=1`) */ val poster: String?,
    /** `strDescriptionEN` (v2, or v1 with `description=1`) */ val description: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toSeason(): Season? = str("strSeason")?.let {
    Season(name = it, badge = str("strBadge"), poster = str("strPoster"), description = str("strDescriptionEN"), raw = raw())
}

/**
 * A team. Search and list endpoints fill only some fields (v2 `list/teams` has no
 * alternate names, for example); a lookup by id fills them all.
 */
public data class Team(
    /** `idTeam` */ val id: Long?,
    /** `strTeam` */ val name: String?,
    /** `strTeamShort`, e.g. `ARS` */ val shortName: String?,
    /** `strTeamAlternate`, split on commas */ val alternateNames: List<String>,
    /** `strKeywords`, split on commas (nicknames) */ val keywords: List<String>,
    /** `strSport` */ val sport: String?,
    /** `strGender` */ val gender: String?,
    /** `strCountry` */ val country: String?,
    /** `strLocation` */ val location: String?,
    /** `intFormedYear` */ val formedYear: Int?,
    /** `idLeague`/`strLeague`: the main league */ val leagueId: Long?,
    /** `strLeague` */ val league: String?,
    /** Every league and cup the team is entered in: `idLeague`..`idLeague7` with their names */ val leagues: List<LeagueRef>,
    /** `strDivision` */ val division: String?,
    /** `idVenue` */ val venueId: Long?,
    /** `strStadium` */ val stadium: String?,
    /** `strColour1`..`strColour3`, hex like `#EF0107` */ val colours: List<String>,
    /** `strBadge` */ val badge: String?,
    /** `strLogo` */ val logo: String?,
    /** `strBanner` */ val banner: String?,
    /** `strEquipment`: the current kit image */ val equipment: String?,
    /** `strFanart1`..`strFanart4` */ val fanart: List<String>,
    /** `strDescriptionEN`, `strDescriptionDE`, ... keyed by language code */ val descriptions: Map<String, String>,
    val socials: Socials,
    /** `intLoved` */ val loved: Int?,
    /** `strLocked` */ val isLocked: Boolean?,
    /** `idAPIfootball` */ val apiFootballId: Long?,
    /** `idESPN` */ val espnId: String?,
    val raw: RawRecord,
) {
    /** The English description, if any. */
    val description: String? get() = descriptions["EN"]
}

internal fun JsonObject.toTeam() = Team(
    id = id("idTeam"),
    name = str("strTeam"),
    shortName = str("strTeamShort"),
    alternateNames = list("strTeamAlternate"),
    keywords = list("strKeywords"),
    sport = str("strSport"),
    gender = str("strGender"),
    country = str("strCountry"),
    location = str("strLocation"),
    formedYear = year("intFormedYear"),
    leagueId = id("idLeague"),
    league = str("strLeague"),
    leagues = (1..7).mapNotNull { n ->
        val suffix = if (n == 1) "" else "$n"
        id("idLeague$suffix")?.let { LeagueRef(it, str("strLeague$suffix")) }
    },
    division = str("strDivision"),
    venueId = id("idVenue"),
    stadium = str("strStadium"),
    colours = numbered("strColour", 3),
    badge = str("strBadge"),
    logo = str("strLogo"),
    banner = str("strBanner"),
    equipment = str("strEquipment"),
    fanart = numbered("strFanart", 4),
    descriptions = descriptions(),
    socials = socials(),
    loved = int("intLoved"),
    isLocked = locked(),
    apiFootballId = id("idAPIfootball"),
    espnId = str("idESPN"),
    raw = raw(),
)
