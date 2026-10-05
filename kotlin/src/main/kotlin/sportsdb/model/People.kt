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
public class Player internal constructor(
    /** `idPlayer` */ public val id: Long?,
    /** `strPlayer` */ public val name: String?,
    /** `strPlayerAlternate` */ public val alternateName: String?,
    /** `strLastName` */ public val lastName: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `idTeam2` */ public val secondTeamId: Long?,
    /** `strTeam2`, often the national team */ public val secondTeam: String?,
    /** `idTeamNational` */ public val nationalTeamId: Long?,
    /** `idPlayerManager`: this person's record as a manager */ public val managerId: Long?,
    /** `strSport` */ public val sport: String?,
    /** `strNationality` */ public val nationality: String?,
    /** `strPosition` */ public val position: String?,
    /** `strNumber`: the shirt number, as text */ public val number: String?,
    /** `strStatus`, e.g. `Active`, `Retired` */ public val status: String?,
    /** `strGender` */ public val gender: String?,
    /** `dateBorn` */ public val born: LocalDate?,
    /** `strBirthLocation` */ public val birthLocation: String?,
    /** `dateDied` */ public val died: LocalDate?,
    /** `strDeathLocation` */ public val deathLocation: String?,
    /** `dateSigned` */ public val signed: LocalDate?,
    /** `strSigning`: the fee as text, e.g. `£25.20m` */ public val signing: String?,
    /** `strWage`, as text */ public val wage: String?,
    /** `strAgent` */ public val agent: String?,
    /** `strHeight`, as text: `1.82 m (6 ft 0 in)` or `186 cm` */ public val height: String?,
    /** `strWeight`, as text */ public val weight: String?,
    /** `strSide`: `Left`/`Right` (preferred foot or hand) */ public val side: String?,
    /** `strKit`: boot or equipment model */ public val kit: String?,
    /** `strOutfitter` */ public val outfitter: String?,
    /** `strCollege` */ public val college: String?,
    /** `strEthnicity` */ public val ethnicity: String?,
    /** `strThumb` */ public val thumb: String?,
    /** `strCutout`: transparent head-and-shoulders */ public val cutout: String?,
    /** `strRender`: transparent full body */ public val render: String?,
    /** `strCartoon` */ public val cartoon: String?,
    /** `strBanner` */ public val banner: String?,
    /** `strPoster` */ public val poster: String?,
    /** `strFanart1`..`strFanart4` */ public val fanart: List<String>,
    /**
     * `strCreativeCommons`: whether the artwork is Creative Commons. TheSportsDB's terms
     * say artwork that isn't (false or null) must not be used in published apps.
     */
    public val creativeCommons: Boolean?,
    /** `strCreativeCommonsAttribution`: the credit line to show */ public val creativeCommonsAttribution: String?,
    /** `strDescriptionEN`, ... keyed by language code */ public val descriptions: Map<String, String>,
    public val socials: Socials,
    /** `intLoved` */ public val loved: Int?,
    /** `strLocked` */ public val isLocked: Boolean?,
    /** `relevance`: search score (v1 `searchplayers.php` only) */ public val relevance: Double?,
    public val externalIds: PlayerExternalIds,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Player(id=$id, name=$name, team=$team)"

    public val description: String? get() = descriptions["EN"]
}

/** Ids of the same player in other databases. */
public class PlayerExternalIds internal constructor(
    /** `idAPIfootball` */ public val apiFootball: Long?,
    /** `idESPN` */ public val espn: String?,
    /** `idGoogle`, e.g. `/g/11cpprmr81` */ public val google: String?,
    /** `idTransferMkt` */ public val transfermarkt: String?,
    /** `idWikidata`, e.g. `Q9144353` */ public val wikidata: String?,
    /** `intSoccerXMLTeamID` */ public val soccerXmlTeam: String?,
) {
    private val all get() = listOf(apiFootball, espn, google, transfermarkt, wikidata, soccerXmlTeam)
    override fun equals(other: Any?): Boolean = other is PlayerExternalIds && other.all == all
    override fun hashCode(): Int = all.hashCode()
    override fun toString(): String =
        "PlayerExternalIds(apiFootball=$apiFootball, espn=$espn, google=$google, transfermarkt=$transfermarkt, " +
            "wikidata=$wikidata, soccerXmlTeam=$soccerXmlTeam)"
}

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
public class Honour internal constructor(
    /** `id`: this record */ public val id: Long?,
    /** `idHonour`: the honour itself, shared by everyone who won it */ public val honourId: Long?,
    /** `strHonour`, e.g. `Copa Libertadores` */ public val name: String?,
    /** `strSeason` */ public val season: String?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strTeamBadge` */ public val teamBadge: String?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strSport` */ public val sport: String?,
    /** `strHonourLogo` */ public val logo: String?,
    /** `strHonourTrophy` */ public val trophy: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Honour(player=$player, name=$name, season=$season)"
}

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
public class FormerTeam internal constructor(
    /** `id` */ public val id: Long?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idFormerTeam`: the team's `idTeam` */ public val teamId: Long?,
    /** `strFormerTeam` */ public val team: String?,
    /** `strBadge` */ public val badge: String?,
    /** `strJoined`: usually a year, as text */ public val joined: String?,
    /** `strDeparted`: usually a year, as text */ public val departed: String?,
    /** `strMoveType`, e.g. `Permanent`, `Loan` */ public val moveType: String?,
    /** `intAppearances` */ public val appearances: Int?,
    /** `intGoals` */ public val goals: Int?,
    /** `strSport` */ public val sport: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "FormerTeam(player=$player, team=$team, joined=$joined, departed=$departed)"
}

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
public class Milestone internal constructor(
    /** `id` */ public val id: Long?,
    /** `idMilestone` */ public val milestoneId: Long?,
    /** `strMilestone` */ public val name: String?,
    /** `dateMilestone` */ public val date: LocalDate?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strSport` */ public val sport: String?,
    /** `strMilestoneLogo` */ public val logo: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Milestone(player=$player, name=$name, date=$date)"
}

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
public class Contract internal constructor(
    /** `id` */ public val id: Long?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strBadge` */ public val badge: String?,
    /** `strYearStart` */ public val yearStart: Int?,
    /** `strYearEnd` */ public val yearEnd: Int?,
    /** `strWage`, as text */ public val wage: String?,
    /** `strSport` */ public val sport: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Contract(player=$player, team=$team, yearStart=$yearStart, yearEnd=$yearEnd)"
}

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
public class EventResult internal constructor(
    /** `idResult` */ public val id: Long?,
    /** `idEvent` */ public val eventId: Long?,
    /** `strEvent` */ public val event: String?,
    /** `dateEvent` */ public val date: LocalDate?,
    /** `strSeason` */ public val season: String?,
    /** `strSport` */ public val sport: String?,
    /** `strCountry` */ public val country: String?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `intPosition`: finishing position */ public val position: Int?,
    /** `intPoints` */ public val points: Int?,
    /** `strResult`: free text */ public val result: String?,
    /** `strDetail`: e.g. a time gap `+29.520` */ public val detail: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "EventResult(eventId=$eventId, player=$player, position=$position)"
}

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
public class PlayerStat internal constructor(
    /** `id` */ public val id: Long?,
    /** `idPlayer` */ public val playerId: Long?,
    /** `strPlayer` */ public val player: String?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strTeam` */ public val team: String?,
    /** `strTeamBadge` */ public val teamBadge: String?,
    /** `idLeague` */ public val leagueId: Long?,
    /** `strLeague` */ public val league: String?,
    /** `strLeagueBadge` */ public val leagueBadge: String?,
    /** `strSeason` */ public val season: String?,
    /** `strSport` */ public val sport: String?,
    /** `strStatistic`, e.g. `Goals`, `Appearances`, `Mins Played` */ public val statistic: String?,
    /** `strValue`, as text */ public val value: String?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "PlayerStat(player=$player, season=$season, statistic=$statistic, value=$value)"

    /** [value] as a number, when it is one. */
    public val numericValue: Double? get() = value?.trim()?.toDoubleOrNull()
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
public class Equipment internal constructor(
    /** `idEquipment` */ public val id: Long?,
    /** `idTeam` */ public val teamId: Long?,
    /** `strSeason` */ public val season: String?,
    /** `strType`, e.g. `1st`, `2nd`, `GK` */ public val type: String?,
    /** `strEquipment`: the image */ public val image: String?,
    /** `strUsername`: who uploaded it */ public val uploadedBy: String?,
    /** `date`: when it was added (zone not stated) */ public val added: LocalDateTime?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Equipment(teamId=$teamId, season=$season, type=$type)"
}

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
public class Venue internal constructor(
    /** `idVenue` */ public val id: Long?,
    /** `strVenue` */ public val name: String?,
    /** `strVenueAlternate` */ public val alternateName: String?,
    /** `strVenueSponsor` */ public val sponsorName: String?,
    /** `strSport` */ public val sport: String?,
    /** `strLocation` */ public val location: String?,
    /** `strCountry` */ public val country: String?,
    /** `strTimezone`, as text, e.g. `UTC +00:00 Greenwich Mean Time (GMT)` */ public val timezone: String?,
    /** `intCapacity` */ public val capacity: Int?,
    /** `intFormedYear` */ public val formedYear: Int?,
    /** `strArchitect` */ public val architect: String?,
    /** `strCost`, as text */ public val cost: String?,
    /** `strMap`: either `lat, lon` or an image URL */ public val map: String?,
    /** `strThumb` */ public val thumb: String?,
    /** `strLogo` */ public val logo: String?,
    /** `strFanart1`..`strFanart4` */ public val fanart: List<String>,
    /** `strCreativeCommons`: see [Player.creativeCommons] */ public val creativeCommons: Boolean?,
    /** `strDescriptionEN`, ... */ public val descriptions: Map<String, String>,
    public val socials: Socials,
    /** `intLoved` */ public val loved: Int?,
    /** `strLocked` */ public val isLocked: Boolean?,
    /** `idDupe`: set when this record duplicates another venue */ public val duplicateOf: Long?,
    raw: RawRecord,
) : ApiRecord(raw) {
    override fun toString(): String = "Venue(id=$id, name=$name)"

    public val description: String? get() = descriptions["EN"]

    /** [map] as latitude and longitude, when it holds coordinates. */
    public val coordinates: Pair<Double, Double>?
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
