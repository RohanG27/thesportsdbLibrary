package sportsdb.model

import kotlinx.serialization.json.JsonObject
import sportsdb.internal.bool
import sportsdb.internal.date
import sportsdb.internal.descriptions
import sportsdb.internal.double
import sportsdb.internal.id
import sportsdb.internal.int
import sportsdb.internal.localDateTime
import sportsdb.internal.locked
import sportsdb.internal.long
import sportsdb.internal.numbered
import sportsdb.internal.raw
import sportsdb.internal.socials
import sportsdb.internal.str
import sportsdb.internal.year
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A player, manager or other person. Search and list endpoints fill only some fields;
 * look the player up by id for the rest.
 */
public data class Player(
    /** `idPlayer` */ val id: Long?,
    /** `strPlayer` */ val name: String?,
    /** `strPlayerAlternate` */ val alternateName: String?,
    /** `strLastName` */ val lastName: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `idTeam2` */ val secondTeamId: Long?,
    /** `strTeam2`, often the national team */ val secondTeam: String?,
    /** `idTeamNational` */ val nationalTeamId: Long?,
    /** `idPlayerManager`: this person's record as a manager */ val managerId: Long?,
    /** `strSport` */ val sport: String?,
    /** `strNationality` */ val nationality: String?,
    /** `strPosition` */ val position: String?,
    /** `strNumber`: the shirt number, as text */ val number: String?,
    /** `strStatus`, e.g. `Active`, `Retired` */ val status: String?,
    /** `strGender` */ val gender: String?,
    /** `dateBorn` */ val born: LocalDate?,
    /** `strBirthLocation` */ val birthLocation: String?,
    /** `dateDied` */ val died: LocalDate?,
    /** `strDeathLocation` */ val deathLocation: String?,
    /** `dateSigned` */ val signed: LocalDate?,
    /** `strSigning`: the fee as text, e.g. `£25.20m` */ val signing: String?,
    /** `strWage`, as text */ val wage: String?,
    /** `strAgent` */ val agent: String?,
    /** `strHeight`, as text: `1.82 m (6 ft 0 in)` or `186 cm` */ val height: String?,
    /** `strWeight`, as text */ val weight: String?,
    /** `strSide`: `Left`/`Right` (preferred foot or hand) */ val side: String?,
    /** `strKit`: boot or equipment model */ val kit: String?,
    /** `strOutfitter` */ val outfitter: String?,
    /** `strCollege` */ val college: String?,
    /** `strEthnicity` */ val ethnicity: String?,
    /** `strThumb` */ val thumb: String?,
    /** `strCutout`: transparent head-and-shoulders */ val cutout: String?,
    /** `strRender`: transparent full body */ val render: String?,
    /** `strCartoon` */ val cartoon: String?,
    /** `strBanner` */ val banner: String?,
    /** `strPoster` */ val poster: String?,
    /** `strFanart1`..`strFanart4` */ val fanart: List<String>,
    /**
     * `strCreativeCommons`: whether the artwork is Creative Commons. TheSportsDB's terms
     * say artwork that isn't (false or null) must not be used in published apps.
     */
    val creativeCommons: Boolean?,
    /** `strCreativeCommonsAttribution`: the credit line to show */ val creativeCommonsAttribution: String?,
    /** `strDescriptionEN`, ... keyed by language code */ val descriptions: Map<String, String>,
    val socials: Socials,
    /** `intLoved` */ val loved: Int?,
    /** `strLocked` */ val isLocked: Boolean?,
    /** `relevance`: search score (v1 `searchplayers.php` only) */ val relevance: Double?,
    val externalIds: PlayerExternalIds,
    val raw: RawRecord,
) {
    val description: String? get() = descriptions["EN"]
}

/** Ids of the same player in other databases. */
public data class PlayerExternalIds(
    /** `idAPIfootball` */ val apiFootball: Long?,
    /** `idESPN` */ val espn: String?,
    /** `idGoogle`, e.g. `/g/11cpprmr81` */ val google: String?,
    /** `idTransferMkt` */ val transfermarkt: String?,
    /** `idWikidata`, e.g. `Q9144353` */ val wikidata: String?,
    /** `intSoccerXMLTeamID` */ val soccerXmlTeam: String?,
)

internal fun JsonObject.toPlayer() = Player(
    id = id("idPlayer"),
    name = str("strPlayer"),
    alternateName = str("strPlayerAlternate"),
    lastName = str("strLastName"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    secondTeamId = id("idTeam2"),
    secondTeam = str("strTeam2"),
    nationalTeamId = id("idTeamNational"),
    managerId = id("idPlayerManager"),
    sport = str("strSport"),
    nationality = str("strNationality"),
    position = str("strPosition"),
    number = str("strNumber"),
    status = str("strStatus"),
    gender = str("strGender"),
    born = date("dateBorn"),
    birthLocation = str("strBirthLocation"),
    died = date("dateDied"),
    deathLocation = str("strDeathLocation"),
    signed = date("dateSigned"),
    signing = str("strSigning"),
    wage = str("strWage"),
    agent = str("strAgent"),
    height = str("strHeight"),
    weight = str("strWeight"),
    side = str("strSide"),
    kit = str("strKit"),
    outfitter = str("strOutfitter"),
    college = str("strCollege"),
    ethnicity = str("strEthnicity"),
    thumb = str("strThumb"),
    cutout = str("strCutout"),
    render = str("strRender"),
    cartoon = str("strCartoon"),
    banner = str("strBanner"),
    poster = str("strPoster"),
    fanart = numbered("strFanart", 4),
    creativeCommons = bool("strCreativeCommons"),
    creativeCommonsAttribution = str("strCreativeCommonsAttribution"),
    descriptions = descriptions(),
    socials = socials(),
    loved = int("intLoved"),
    isLocked = locked(),
    relevance = double("relevance"),
    externalIds = PlayerExternalIds(
        apiFootball = id("idAPIfootball"),
        espn = str("idESPN"),
        google = str("idGoogle"),
        transfermarkt = str("idTransferMkt"),
        wikidata = str("idWikidata"),
        soccerXmlTeam = str("intSoccerXMLTeamID"),
    ),
    raw = raw(),
)

/** A trophy a player won (`lookuphonours.php`, `lookup/player_honours`). */
public data class Honour(
    /** `id`: this record */ val id: Long?,
    /** `idHonour`: the honour itself, shared by everyone who won it */ val honourId: Long?,
    /** `strHonour`, e.g. `Copa Libertadores` */ val name: String?,
    /** `strSeason` */ val season: String?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strTeamBadge` */ val teamBadge: String?,
    /** `idLeague` */ val leagueId: Long?,
    /** `strSport` */ val sport: String?,
    /** `strHonourLogo` */ val logo: String?,
    /** `strHonourTrophy` */ val trophy: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toHonour() = Honour(
    id = id("id"),
    honourId = id("idHonour"),
    name = str("strHonour"),
    season = str("strSeason"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    teamBadge = str("strTeamBadge"),
    leagueId = id("idLeague"),
    sport = str("strSport"),
    logo = str("strHonourLogo"),
    trophy = str("strHonourTrophy"),
    raw = raw(),
)

/** A team a player used to play for (`lookupformerteams.php`, `lookup/player_teams`). */
public data class FormerTeam(
    /** `id` */ val id: Long?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idFormerTeam`: the team's `idTeam` */ val teamId: Long?,
    /** `strFormerTeam` */ val team: String?,
    /** `strBadge` */ val badge: String?,
    /** `strJoined`: usually a year, as text */ val joined: String?,
    /** `strDeparted`: usually a year, as text */ val departed: String?,
    /** `strMoveType`, e.g. `Permanent`, `Loan` */ val moveType: String?,
    /** `intAppearances` */ val appearances: Int?,
    /** `intGoals` */ val goals: Int?,
    /** `strSport` */ val sport: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toFormerTeam() = FormerTeam(
    id = id("id"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idFormerTeam"),
    team = str("strFormerTeam"),
    badge = str("strBadge"),
    joined = str("strJoined"),
    departed = str("strDeparted"),
    moveType = str("strMoveType"),
    appearances = int("intAppearances"),
    goals = int("intGoals"),
    sport = str("strSport"),
    raw = raw(),
)

/** A career milestone or award (`lookupmilestones.php`, `lookup/player_milestones`). */
public data class Milestone(
    /** `id` */ val id: Long?,
    /** `idMilestone` */ val milestoneId: Long?,
    /** `strMilestone` */ val name: String?,
    /** `dateMilestone` */ val date: LocalDate?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strSport` */ val sport: String?,
    /** `strMilestoneLogo` */ val logo: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toMilestone() = Milestone(
    id = id("id"),
    milestoneId = id("idMilestone"),
    name = str("strMilestone"),
    date = date("dateMilestone"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    sport = str("strSport"),
    logo = str("strMilestoneLogo"),
    raw = raw(),
)

/** A player's contract (`lookupcontracts.php`, `lookup/player_contracts`). */
public data class Contract(
    /** `id` */ val id: Long?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strBadge` */ val badge: String?,
    /** `strYearStart` */ val yearStart: Int?,
    /** `strYearEnd` */ val yearEnd: Int?,
    /** `strWage`, as text */ val wage: String?,
    /** `strSport` */ val sport: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toContract() = Contract(
    id = id("id"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    badge = str("strBadge"),
    yearStart = year("strYearStart"),
    yearEnd = year("strYearEnd"),
    wage = str("strWage"),
    sport = str("strSport"),
    raw = raw(),
)

/**
 * One competitor's result in an individual-sport event: a race, a golf tournament, a
 * fight. Returned both per player (`playerresults.php`) and per event (`eventresults.php`).
 */
public data class EventResult(
    /** `idResult` */ val id: Long?,
    /** `idEvent` */ val eventId: Long?,
    /** `strEvent` */ val event: String?,
    /** `dateEvent` */ val date: LocalDate?,
    /** `strSeason` */ val season: String?,
    /** `strSport` */ val sport: String?,
    /** `strCountry` */ val country: String?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `intPosition`: finishing position */ val position: Int?,
    /** `intPoints` */ val points: Int?,
    /** `strResult`: free text */ val result: String?,
    /** `strDetail`: e.g. a time gap `+29.520` */ val detail: String?,
    val raw: RawRecord,
)

internal fun JsonObject.toEventResult() = EventResult(
    id = id("idResult"),
    eventId = id("idEvent"),
    event = str("strEvent"),
    date = date("dateEvent"),
    season = str("strSeason"),
    sport = str("strSport"),
    country = str("strCountry"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    position = int("intPosition"),
    points = int("intPoints"),
    result = str("strResult"),
    detail = str("strDetail"),
    raw = raw(),
)

/** One statistic for a player in one season (`lookupplayerstats.php`, `lookup/player_stats`). */
public data class PlayerStat(
    /** `id` */ val id: Long?,
    /** `idPlayer` */ val playerId: Long?,
    /** `strPlayer` */ val player: String?,
    /** `idTeam` */ val teamId: Long?,
    /** `strTeam` */ val team: String?,
    /** `strTeamBadge` */ val teamBadge: String?,
    /** `idLeague` */ val leagueId: Long?,
    /** `strLeague` */ val league: String?,
    /** `strLeagueBadge` */ val leagueBadge: String?,
    /** `strSeason` */ val season: String?,
    /** `strSport` */ val sport: String?,
    /** `strStatistic`, e.g. `Goals`, `Appearances`, `Mins Played` */ val statistic: String?,
    /** `strValue`, as text */ val value: String?,
    val raw: RawRecord,
) {
    /** [value] as a number, when it is one. */
    val numericValue: Double? get() = value?.trim()?.toDoubleOrNull()
}

internal fun JsonObject.toPlayerStat() = PlayerStat(
    id = id("id"),
    playerId = id("idPlayer"),
    player = str("strPlayer"),
    teamId = id("idTeam"),
    team = str("strTeam"),
    teamBadge = str("strTeamBadge"),
    leagueId = id("idLeague"),
    league = str("strLeague"),
    leagueBadge = str("strLeagueBadge"),
    season = str("strSeason"),
    sport = str("strSport"),
    statistic = str("strStatistic"),
    value = str("strValue"),
    raw = raw(),
)

/** A team kit (`lookupequipment.php`, `lookup/team_equipment`). */
public data class Equipment(
    /** `idEquipment` */ val id: Long?,
    /** `idTeam` */ val teamId: Long?,
    /** `strSeason` */ val season: String?,
    /** `strType`, e.g. `1st`, `2nd`, `GK` */ val type: String?,
    /** `strEquipment`: the image */ val image: String?,
    /** `strUsername`: who uploaded it */ val uploadedBy: String?,
    /** `date`: when it was added (zone not stated) */ val added: LocalDateTime?,
    val raw: RawRecord,
)

internal fun JsonObject.toEquipment() = Equipment(
    id = id("idEquipment"),
    teamId = id("idTeam"),
    season = str("strSeason"),
    type = str("strType"),
    image = str("strEquipment"),
    uploadedBy = str("strUsername"),
    added = localDateTime("date"),
    raw = raw(),
)

/** A stadium, arena or circuit. */
public data class Venue(
    /** `idVenue` */ val id: Long?,
    /** `strVenue` */ val name: String?,
    /** `strVenueAlternate` */ val alternateName: String?,
    /** `strVenueSponsor` */ val sponsorName: String?,
    /** `strSport` */ val sport: String?,
    /** `strLocation` */ val location: String?,
    /** `strCountry` */ val country: String?,
    /** `strTimezone`, as text, e.g. `UTC +00:00 Greenwich Mean Time (GMT)` */ val timezone: String?,
    /** `intCapacity` */ val capacity: Int?,
    /** `intFormedYear` */ val formedYear: Int?,
    /** `strArchitect` */ val architect: String?,
    /** `strCost`, as text */ val cost: String?,
    /** `strMap`: either `lat, lon` or an image URL */ val map: String?,
    /** `strThumb` */ val thumb: String?,
    /** `strLogo` */ val logo: String?,
    /** `strFanart1`..`strFanart4` */ val fanart: List<String>,
    /** `strCreativeCommons`: see [Player.creativeCommons] */ val creativeCommons: Boolean?,
    /** `strDescriptionEN`, ... */ val descriptions: Map<String, String>,
    val socials: Socials,
    /** `intLoved` */ val loved: Int?,
    /** `strLocked` */ val isLocked: Boolean?,
    /** `idDupe`: set when this record duplicates another venue */ val duplicateOf: Long?,
    val raw: RawRecord,
) {
    val description: String? get() = descriptions["EN"]

    /** [map] as latitude and longitude, when it holds coordinates. */
    val coordinates: Pair<Double, Double>?
        get() {
            val parts = map?.split(',')?.map { it.trim().toDoubleOrNull() } ?: return null
            val (lat, lon) = parts.takeIf { it.size == 2 } ?: return null
            return if (lat != null && lon != null) lat to lon else null
        }
}

internal fun JsonObject.toVenue() = Venue(
    id = id("idVenue"),
    name = str("strVenue"),
    alternateName = str("strVenueAlternate"),
    sponsorName = str("strVenueSponsor"),
    sport = str("strSport"),
    location = str("strLocation"),
    country = str("strCountry"),
    timezone = str("strTimezone"),
    capacity = int("intCapacity"),
    formedYear = year("intFormedYear"),
    architect = str("strArchitect"),
    cost = str("strCost"),
    map = str("strMap"),
    thumb = str("strThumb"),
    logo = str("strLogo"),
    fanart = numbered("strFanart", 4),
    creativeCommons = bool("strCreativeCommons"),
    descriptions = descriptions(),
    socials = socials(),
    loved = int("intLoved"),
    isLocked = locked(),
    duplicateOf = long("idDupe")?.takeIf { it != 0L },
    raw = raw(),
)
