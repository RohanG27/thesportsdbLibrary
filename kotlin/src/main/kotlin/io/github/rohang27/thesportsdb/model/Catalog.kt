package io.github.rohang27.thesportsdb.model

import kotlinx.serialization.json.JsonObject
import io.github.rohang27.thesportsdb.internal.bool
import io.github.rohang27.thesportsdb.internal.date
import io.github.rohang27.thesportsdb.internal.descriptions
import io.github.rohang27.thesportsdb.internal.id
import io.github.rohang27.thesportsdb.internal.int
import io.github.rohang27.thesportsdb.internal.list
import io.github.rohang27.thesportsdb.internal.locked
import io.github.rohang27.thesportsdb.internal.numbered
import io.github.rohang27.thesportsdb.internal.raw
import io.github.rohang27.thesportsdb.internal.socials
import io.github.rohang27.thesportsdb.internal.str
import io.github.rohang27.thesportsdb.internal.year
import java.time.LocalDate

/** A sport (`all_sports.php`, `all/sports`). */
public class Sport internal constructor(
    /** `idSport` */ public val id: Long?,
    /** `strSport`, e.g. `Ice Hockey`. Use this name for sport filters. */ public val name: String?,
    /** `strFormat`: `TeamvsTeam` or `EventSport`. */ public val format: String?,
    /** `strSportDescription` */ public val description: String?,
    /** `strSportThumb` */ public val thumb: String?,
    /** `strSportThumbBW` */ public val thumbBlackAndWhite: String?,
    /** `strSportIconGreen` */ public val icon: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Sport(id=$id, name=$name)"
}

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
public class Country internal constructor(
    /** `name_en`. Use this name for country filters. */ public val name: String?,
    /** `name_fr` (v2) */ public val nameFr: String?,
    /** `code` (v2), a two-letter code */ public val code: String?,
    /** `flag_url_16` (v2) */ public val flag16: String?,
    /** `flag_url_32` */ public val flag32: String?,
    /** `flag_url_64` (v2) */ public val flag64: String?,
    /** `idAPIfootball` (v2) */ public val apiFootballId: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Country(name=$name, code=$code)"
}

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
public class League internal constructor(
    /** `idLeague` */ public val id: Long?,
    /** `strLeague` */ public val name: String?,
    /** `strLeagueAlternate`, split on commas */ public val alternateNames: List<String>,
    /** `strSport` */ public val sport: String?,
    /** `strCountry` */ public val country: String?,
    /** `strGender` */ public val gender: String?,
    /** `strCurrentSeason`, e.g. `2026-2027` or `2026`. Pass it to season endpoints. */ public val currentSeason: String?,
    /** `intDivision`: 1 for a top flight; cups often use 0 or 99 */ public val division: Int?,
    /** `idCup`: true for a cup competition */ public val isCup: Boolean?,
    /** `intFormedYear` */ public val formedYear: Int?,
    /** `dateFirstEvent` */ public val firstEventDate: LocalDate?,
    /** `strComplete`: whether TheSportsDB considers its data complete */ public val isComplete: Boolean?,
    /** `strNaming`: the event-name template, e.g. `{strHomeTeam} vs {strAwayTeam}` */ public val eventNaming: String?,
    /** `strTvRights`: free text */ public val tvRights: String?,
    /** `strBadge` */ public val badge: String?,
    /** `strLogo` */ public val logo: String?,
    /** `strBanner` */ public val banner: String?,
    /** `strPoster` */ public val poster: String?,
    /** `strTrophy` */ public val trophy: String?,
    /** `strFanart1`..`strFanart4` */ public val fanart: List<String>,
    /** `strDescriptionEN`, `strDescriptionDE`, ... keyed by language code (`EN`, `DE`) */ public val descriptions: Map<String, String>,
    public val socials: Socials,
    /** `strLocked` */ public val isLocked: Boolean?,
    /** `idAPIfootball` */ public val apiFootballId: Long?,
    /** `idAPIfootballv3` */ public val apiFootballV3Id: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "League(id=$id, name=$name)"

    /** The English description, if any. */
    public val description: String? get() = descriptions["EN"]
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
public class Season internal constructor(
    /** `strSeason`, e.g. `2026-2027` */ public val name: String,
    /** `strBadge` (v2, or v1 with `badge=1`) */ public val badge: String?,
    /** `strPoster` (v2, or v1 with `poster=1`) */ public val poster: String?,
    /** `strDescriptionEN` (v2, or v1 with `description=1`) */ public val description: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Season(name=$name)"
}

/**
 * One season poster or badge uploaded for a league (v2 `list/seasonposters`). A season can have
 * several; each is a separate piece of artwork with its own id.
 */
public class SeasonPoster internal constructor(
    /** `idArt`: this artwork's id */ public val id: Long?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strSeason`, e.g. `2025-2026` */ public val season: String?,
    /** `strPoster` */ public val poster: String?,
    /** `strBadge` */ public val badge: String?,
    /** `strDescriptionEN` */ public val description: String?,
    /** `strUsername`: who uploaded it */ public val uploadedBy: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "SeasonPoster(id=$id, season=$season)"
}

internal fun JsonObject.toSeasonPoster() = SeasonPoster(
    id = id("idArt"),
    leagueId = id("idLeague"),
    season = str("strSeason"),
    poster = str("strPoster"),
    badge = str("strBadge"),
    description = str("strDescriptionEN"),
    uploadedBy = str("strUsername"),
    raw = raw(),
)

internal fun JsonObject.toSeason(): Season? = str("strSeason")?.let {
    Season(name = it, badge = str("strBadge"), poster = str("strPoster"), description = str("strDescriptionEN"), raw = raw())
}

/**
 * A team. Search and list endpoints fill only some fields (v2 `list/teams` has no
 * alternate names, for example); a lookup by id fills them all.
 */
public class Team internal constructor(
    /** `idTeam` */ public val id: Long?,
    /** `strTeam` */ public val name: String?,
    /** `strTeamShort`, e.g. `ARS` */ public val shortName: String?,
    /** `strTeamAlternate`, split on commas */ public val alternateNames: List<String>,
    /** `strKeywords`, split on commas (nicknames) */ public val keywords: List<String>,
    /** `strSport` */ public val sport: String?,
    /** `strGender` */ public val gender: String?,
    /** `strCountry` */ public val country: String?,
    /** `strLocation` */ public val location: String?,
    /** `intFormedYear` */ public val formedYear: Int?,
    /** `idLeague`/`strLeague`: the main league */ public val leagueId: Long?,
    /** `strLeague` */ public val league: String?,
    /** Every league and cup the team is entered in: `idLeague`..`idLeague7` with their names */ public val leagues: List<LeagueRef>,
    /** `strDivision` */ public val division: String?,
    /** `idVenue` */ public val venueId: Long?,
    /** `strStadium` */ public val stadium: String?,
    /** `strColour1`..`strColour3`, hex like `#EF0107` */ public val colours: List<String>,
    /** `strBadge` */ public val badge: String?,
    /** `strLogo` */ public val logo: String?,
    /** `strBanner` */ public val banner: String?,
    /** `strEquipment`: the current kit image */ public val equipment: String?,
    /** `strFanart1`..`strFanart4` */ public val fanart: List<String>,
    /** `strDescriptionEN`, `strDescriptionDE`, ... keyed by language code */ public val descriptions: Map<String, String>,
    public val socials: Socials,
    /** `intLoved` */ public val loved: Int?,
    /** `strLocked` */ public val isLocked: Boolean?,
    /** `idAPIfootball` */ public val apiFootballId: Long?,
    /** `idESPN` */ public val espnId: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Team(id=$id, name=$name)"

    /** The English description, if any. */
    public val description: String? get() = descriptions["EN"]
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
