"""Typed records returned by the API.

Every model is a frozen, keyword-only dataclass built by the library from one API record.
Values are parsed leniently: anything missing, blank or malformed is ``None`` (or an empty
tuple/dict). Times in ``timestamp`` fields are timezone-aware UTC. Every model keeps the
original fields in ``raw``. Each field's comment names the API field it comes from.

Two records are equal when the API sent the same fields. Build them only through the client;
to test your own code, fake the HTTP layer with a transport instead.
"""

from __future__ import annotations

import re
from collections.abc import Mapping
from dataclasses import dataclass, field
from datetime import date as Date
from datetime import datetime as DateTime
from datetime import time as Time
from enum import Enum
from types import MappingProxyType
from typing import Any, ClassVar, TypeVar

from ._fields import Rec

R = TypeVar("R", bound="ApiRecord")


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class ApiRecord:
    """Base class of every record ([Team], [Event], ...)."""

    raw: Mapping[str, str | None] = field(default_factory=lambda: MappingProxyType({}))
    """Every field as the API sent it (blank strings as None). Use it for unmodelled fields: ``team.raw["strKeywords"]``."""

    _summary: ClassVar[tuple[str, ...]] = ()

    def __eq__(self, other: object) -> bool:
        return type(self) is type(other) and isinstance(other, ApiRecord) and dict(self.raw) == dict(other.raw)

    def __hash__(self) -> int:
        return hash((type(self), frozenset(self.raw.items())))

    def __repr__(self) -> str:
        parts = ", ".join(f"{name}={getattr(self, name)!r}" for name in self._summary)
        return f"{type(self).__name__}({parts})"

    @classmethod
    def _from(cls: type[R], raw: Mapping[str, str | None]) -> R:
        values = cls._read(Rec(raw))
        return cls(raw=MappingProxyType(dict(raw)), **values)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        raise NotImplementedError


@dataclass(frozen=True)
class Socials:
    """Web and social links. Often without a scheme (``www.facebook.com/Arsenal``)."""

    website: str | None = None  # strWebsite
    facebook: str | None = None  # strFacebook
    twitter: str | None = None  # strTwitter
    instagram: str | None = None  # strInstagram
    youtube: str | None = None  # strYoutube
    rss: str | None = None  # strRSS

    @staticmethod
    def _read(r: Rec) -> Socials:
        return Socials(r.s("strWebsite"), r.s("strFacebook"), r.s("strTwitter"), r.s("strInstagram"),
                       r.s("strYoutube"), r.s("strRSS"))


@dataclass(frozen=True)
class LeagueRef:
    """A league a team plays in: one of ``idLeague``/``strLeague`` .. ``idLeague7``/``strLeague7``."""

    id: int
    name: str | None


@dataclass(frozen=True)
class PlayerExternalIds:
    """Ids of the same player in other databases."""

    api_football: int | None = None  # idAPIfootball
    espn: str | None = None  # idESPN
    google: str | None = None  # idGoogle, e.g. /g/11cpprmr81
    transfermarkt: str | None = None  # idTransferMkt
    wikidata: str | None = None  # idWikidata, e.g. Q9144353
    soccer_xml_team: str | None = None  # intSoccerXMLTeamID


class ImageSize(Enum):
    """Sizes TheSportsDB serves by appending a path suffix to an image URL."""

    MEDIUM = "/medium"  # about 70% of the bytes
    SMALL = "/small"  # about 35%; good for cards
    TINY = "/tiny"  # about 10%; good for lists


def sized(url: str | None, size: ImageSize) -> str | None:
    """The same image at a smaller size.

    Only ``r2.thesportsdb.com`` and ``www.thesportsdb.com/images/media/`` images support the
    suffixes (others return 404, verified 5 Oct 2026); other URLs and None are returned unchanged.
    """
    if url is None or not url.startswith(("https://r2.thesportsdb.com/", "https://www.thesportsdb.com/images/media/")):
        return url
    for s in ImageSize:
        url = url.removesuffix(s.value)
    return url + size.value


class EventStatus(Enum):
    """A broad reading of a ``strStatus`` code, so you don't need every sport's codes.

    Covers the codes in TheSportsDB's data documentation (docs_api_data) for every sport.
    """

    NOT_STARTED = "not_started"
    IN_PLAY = "in_play"
    FINISHED = "finished"
    POSTPONED = "postponed"
    INTERRUPTED = "interrupted"  # suspended or interrupted (SUSP, INT, INTR): stopped, and may resume
    CANCELLED = "cancelled"
    ABANDONED = "abandoned"
    UNKNOWN = "unknown"  # None, or a code this library doesn't know

    @staticmethod
    def of(code: str | None) -> EventStatus:
        """Classifies a raw code (``NS``, ``2H``, ``Q3``, ``P2``, ``IN4``, ``S2``, ``FT``, ``PST``...)."""
        c = (code or "").strip().upper()
        if not c:
            return EventStatus.UNKNOWN
        for status, codes in _STATUS_CODES:
            if c in codes:
                return status
        if re.fullmatch(r"IN\d+|S\d", c):  # baseball innings, volleyball sets
            return EventStatus.IN_PLAY
        return EventStatus.UNKNOWN


_STATUS_CODES = (
    (EventStatus.NOT_STARTED, {"NS", "TBD", "NOT STARTED", "SCHEDULED"}),
    (EventStatus.FINISHED, {"FT", "AET", "PEN", "AOT", "AP", "AWD", "AW", "WO", "FINISHED", "MATCH FINISHED", "FINAL", "ENDED"}),
    (EventStatus.POSTPONED, {"PST", "POST", "POSTPONED", "DELAYED"}),
    (EventStatus.INTERRUPTED, {"SUSP", "INT", "INTR", "SUSPENDED", "INTERRUPTED"}),
    (EventStatus.CANCELLED, {"CANC", "CANCELLED", "CANCELED"}),
    (EventStatus.ABANDONED, {"ABD", "ABANDONED"}),
    (EventStatus.IN_PLAY, {"1H", "2H", "HT", "ET", "BT", "P", "PT", "LIVE", "BREAK", "Q1", "Q2", "Q3", "Q4", "OT",
                           "P1", "P2", "P3", "SO", "IN PROGRESS"}),
)


class RoundStage(Enum):
    """The stage a special ``intRound`` value stands for (docs_api_data). Other values are ordinary rounds."""

    QUARTER_FINAL = 125
    SEMI_FINAL = 150
    PLAYOFF = 160
    PLAYOFF_SEMI_FINAL = 170
    PLAYOFF_FINAL = 180
    FINAL = 200
    QUALIFIER = 400
    PRE_SEASON = 500

    @staticmethod
    def of(round: int | None) -> RoundStage | None:
        """The stage for an ``intRound`` value, or None for an ordinary round number."""
        try:
            return RoundStage(round) if round is not None else None
        except ValueError:
            return None


# ---------------------------------------------------------------------------------------------
# Catalogue


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Sport(ApiRecord):
    """A sport (``all_sports.php``, ``all/sports``)."""

    _summary = ("id", "name")
    id: int | None  # idSport
    name: str | None  # strSport, e.g. "Ice Hockey"; use it for sport filters
    format: str | None  # strFormat: TeamvsTeam or EventSport
    description: str | None  # strSportDescription
    thumb: str | None  # strSportThumb
    thumb_black_and_white: str | None  # strSportThumbBW
    icon: str | None  # strSportIconGreen

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idSport"), name=r.s("strSport"), format=r.s("strFormat"),
                    description=r.s("strSportDescription"), thumb=r.s("strSportThumb"),
                    thumb_black_and_white=r.s("strSportThumbBW"), icon=r.s("strSportIconGreen"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Country(ApiRecord):
    """A country (``all_countries.php``, ``all/countries``). v1 fills only name and flag32."""

    _summary = ("name", "code")
    name: str | None  # name_en; use it for country filters
    name_fr: str | None  # name_fr (v2)
    code: str | None  # code (v2), e.g. "AD"
    flag16: str | None  # flag_url_16 (v2)
    flag32: str | None  # flag_url_32
    flag64: str | None  # flag_url_64 (v2)
    api_football_id: int | None  # idAPIfootball (v2)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(name=r.s("name_en"), name_fr=r.s("name_fr"), code=r.s("code"), flag16=r.s("flag_url_16"),
                    flag32=r.s("flag_url_32"), flag64=r.s("flag_url_64"), api_football_id=r.id_("idAPIfootball"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class League(ApiRecord):
    """A league or cup. Lists fill only a few fields; look it up by id for the rest."""

    _summary = ("id", "name")
    id: int | None  # idLeague
    name: str | None  # strLeague
    alternate_names: tuple[str, ...]  # strLeagueAlternate, split on commas (premium only in all_leagues.php)
    sport: str | None  # strSport
    country: str | None  # strCountry
    gender: str | None  # strGender
    current_season: str | None  # strCurrentSeason, e.g. "2026-2027" or "2026"
    division: int | None  # intDivision
    is_cup: bool | None  # idCup
    formed_year: int | None  # intFormedYear
    first_event_date: Date | None  # dateFirstEvent
    is_complete: bool | None  # strComplete
    event_naming: str | None  # strNaming, e.g. "{strHomeTeam} vs {strAwayTeam}"
    tv_rights: str | None  # strTvRights
    badge: str | None  # strBadge
    logo: str | None  # strLogo
    banner: str | None  # strBanner
    poster: str | None  # strPoster
    trophy: str | None  # strTrophy
    fanart: tuple[str, ...]  # strFanart1..4
    descriptions: Mapping[str, str]  # strDescriptionEN, DE, ... keyed by language code
    socials: Socials
    is_locked: bool | None  # strLocked
    api_football_id: int | None  # idAPIfootball
    api_football_v3_id: int | None  # idAPIfootballv3

    @property
    def description(self) -> str | None:
        """The English description."""
        return self.descriptions.get("EN")

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(
            id=r.id_("idLeague"), name=r.s("strLeague"), alternate_names=r.csv("strLeagueAlternate"),
            sport=r.s("strSport"), country=r.s("strCountry"), gender=r.s("strGender"),
            current_season=r.s("strCurrentSeason"), division=r.i("intDivision"), is_cup=r.b("idCup"),
            formed_year=r.year("intFormedYear"), first_event_date=r.d("dateFirstEvent"),
            is_complete=r.b("strComplete"), event_naming=r.s("strNaming"), tv_rights=r.s("strTvRights"),
            badge=r.s("strBadge"), logo=r.s("strLogo"), banner=r.s("strBanner"), poster=r.s("strPoster"),
            trophy=r.s("strTrophy"), fanart=r.numbered("strFanart", 4),
            descriptions=MappingProxyType(r.descriptions()), socials=Socials._read(r), is_locked=r.locked(),
            api_football_id=r.id_("idAPIfootball"), api_football_v3_id=r.id_("idAPIfootballv3"),
        )


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Season(ApiRecord):
    """A season of a league (``search_all_seasons.php``, ``list/seasons``)."""

    _summary = ("name",)
    name: str  # strSeason, e.g. "2026-2027"
    badge: str | None  # strBadge (v2, or v1 with badges=True)
    poster: str | None  # strPoster (v2, or v1 with posters=True)
    description: str | None  # strDescriptionEN (v2, or v1 with descriptions=True)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(name=r.s("strSeason") or "", badge=r.s("strBadge"), poster=r.s("strPoster"),
                    description=r.s("strDescriptionEN"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Team(ApiRecord):
    """A team. Search and list endpoints fill only some fields; a lookup by id fills them all."""

    _summary = ("id", "name")
    id: int | None  # idTeam
    name: str | None  # strTeam
    short_name: str | None  # strTeamShort, e.g. "ARS"
    alternate_names: tuple[str, ...]  # strTeamAlternate, split on commas
    keywords: tuple[str, ...]  # strKeywords, split on commas (nicknames)
    sport: str | None  # strSport
    gender: str | None  # strGender
    country: str | None  # strCountry
    location: str | None  # strLocation
    formed_year: int | None  # intFormedYear
    league_id: int | None  # idLeague: the main league
    league: str | None  # strLeague
    leagues: tuple[LeagueRef, ...]  # idLeague..idLeague7 with their names
    division: str | None  # strDivision
    venue_id: int | None  # idVenue
    stadium: str | None  # strStadium
    colours: tuple[str, ...]  # strColour1..3, hex like "#EF0107"
    badge: str | None  # strBadge
    logo: str | None  # strLogo
    banner: str | None  # strBanner
    equipment: str | None  # strEquipment: the current kit image
    fanart: tuple[str, ...]  # strFanart1..4
    descriptions: Mapping[str, str]  # strDescriptionEN, ...
    socials: Socials
    loved: int | None  # intLoved
    is_locked: bool | None  # strLocked
    api_football_id: int | None  # idAPIfootball
    espn_id: str | None  # idESPN

    @property
    def description(self) -> str | None:
        return self.descriptions.get("EN")

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        leagues = []
        for n in range(1, 8):
            suffix = "" if n == 1 else str(n)
            if (lid := r.id_(f"idLeague{suffix}")) is not None:
                leagues.append(LeagueRef(lid, r.s(f"strLeague{suffix}")))
        return dict(
            id=r.id_("idTeam"), name=r.s("strTeam"), short_name=r.s("strTeamShort"),
            alternate_names=r.csv("strTeamAlternate"), keywords=r.csv("strKeywords"), sport=r.s("strSport"),
            gender=r.s("strGender"), country=r.s("strCountry"), location=r.s("strLocation"),
            formed_year=r.year("intFormedYear"), league_id=r.id_("idLeague"), league=r.s("strLeague"),
            leagues=tuple(leagues), division=r.s("strDivision"), venue_id=r.id_("idVenue"),
            stadium=r.s("strStadium"), colours=r.numbered("strColour", 3), badge=r.s("strBadge"),
            logo=r.s("strLogo"), banner=r.s("strBanner"), equipment=r.s("strEquipment"),
            fanart=r.numbered("strFanart", 4), descriptions=MappingProxyType(r.descriptions()),
            socials=Socials._read(r), loved=r.i("intLoved"), is_locked=r.locked(),
            api_football_id=r.id_("idAPIfootball"), espn_id=r.s("idESPN"),
        )


# ---------------------------------------------------------------------------------------------
# People


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Player(ApiRecord):
    """A player, manager or other person. Search and list endpoints fill only some fields."""

    _summary = ("id", "name", "team")
    id: int | None  # idPlayer
    name: str | None  # strPlayer
    alternate_name: str | None  # strPlayerAlternate
    last_name: str | None  # strLastName
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    second_team_id: int | None  # idTeam2
    second_team: str | None  # strTeam2, often the national team
    national_team_id: int | None  # idTeamNational
    manager_id: int | None  # idPlayerManager
    sport: str | None  # strSport
    nationality: str | None  # strNationality
    position: str | None  # strPosition
    number: str | None  # strNumber, as text
    status: str | None  # strStatus, e.g. Active, Retired
    gender: str | None  # strGender
    born: Date | None  # dateBorn
    birth_location: str | None  # strBirthLocation
    died: Date | None  # dateDied
    death_location: str | None  # strDeathLocation
    signed: Date | None  # dateSigned
    signing: str | None  # strSigning: the fee as text
    wage: str | None  # strWage, as text
    agent: str | None  # strAgent
    height: str | None  # strHeight, as text
    weight: str | None  # strWeight, as text
    side: str | None  # strSide: Left/Right
    kit: str | None  # strKit
    outfitter: str | None  # strOutfitter
    college: str | None  # strCollege
    ethnicity: str | None  # strEthnicity
    thumb: str | None  # strThumb
    cutout: str | None  # strCutout
    render: str | None  # strRender
    cartoon: str | None  # strCartoon
    banner: str | None  # strBanner
    poster: str | None  # strPoster
    fanart: tuple[str, ...]  # strFanart1..4
    creative_commons: bool | None
    """strCreativeCommons. TheSportsDB's terms: artwork that isn't Creative Commons must not be used in published apps."""
    creative_commons_attribution: str | None  # strCreativeCommonsAttribution: the credit to show
    descriptions: Mapping[str, str]
    socials: Socials
    loved: int | None  # intLoved
    is_locked: bool | None  # strLocked
    relevance: float | None  # relevance: search score (v1 searchplayers.php only)
    external_ids: PlayerExternalIds

    @property
    def description(self) -> str | None:
        return self.descriptions.get("EN")

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(
            id=r.id_("idPlayer"), name=r.s("strPlayer"), alternate_name=r.s("strPlayerAlternate"),
            last_name=r.s("strLastName"), team_id=r.id_("idTeam"), team=r.s("strTeam"),
            second_team_id=r.id_("idTeam2"), second_team=r.s("strTeam2"), national_team_id=r.id_("idTeamNational"),
            manager_id=r.id_("idPlayerManager"), sport=r.s("strSport"), nationality=r.s("strNationality"),
            position=r.s("strPosition"), number=r.s("strNumber"), status=r.s("strStatus"), gender=r.s("strGender"),
            born=r.d("dateBorn"), birth_location=r.s("strBirthLocation"), died=r.d("dateDied"),
            death_location=r.s("strDeathLocation"), signed=r.d("dateSigned"), signing=r.s("strSigning"),
            wage=r.s("strWage"), agent=r.s("strAgent"), height=r.s("strHeight"), weight=r.s("strWeight"),
            side=r.s("strSide"), kit=r.s("strKit"), outfitter=r.s("strOutfitter"), college=r.s("strCollege"),
            ethnicity=r.s("strEthnicity"), thumb=r.s("strThumb"), cutout=r.s("strCutout"), render=r.s("strRender"),
            cartoon=r.s("strCartoon"), banner=r.s("strBanner"), poster=r.s("strPoster"),
            fanart=r.numbered("strFanart", 4), creative_commons=r.b("strCreativeCommons"),
            creative_commons_attribution=r.s("strCreativeCommonsAttribution"),
            descriptions=MappingProxyType(r.descriptions()), socials=Socials._read(r), loved=r.i("intLoved"),
            is_locked=r.locked(), relevance=r.f("relevance"),
            external_ids=PlayerExternalIds(r.id_("idAPIfootball"), r.s("idESPN"), r.s("idGoogle"),
                                           r.s("idTransferMkt"), r.s("idWikidata"), r.s("intSoccerXMLTeamID")),
        )


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Honour(ApiRecord):
    """A trophy a player won (``lookuphonours.php``, ``lookup/player_honours``)."""

    _summary = ("player", "name", "season")
    id: int | None  # id
    honour_id: int | None  # idHonour
    name: str | None  # strHonour
    season: str | None  # strSeason
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    team_badge: str | None  # strTeamBadge
    league_id: int | None  # idLeague
    sport: str | None  # strSport
    logo: str | None  # strHonourLogo
    trophy: str | None  # strHonourTrophy

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), honour_id=r.id_("idHonour"), name=r.s("strHonour"), season=r.s("strSeason"),
                    player_id=r.id_("idPlayer"), player=r.s("strPlayer"), team_id=r.id_("idTeam"),
                    team=r.s("strTeam"), team_badge=r.s("strTeamBadge"), league_id=r.id_("idLeague"),
                    sport=r.s("strSport"), logo=r.s("strHonourLogo"), trophy=r.s("strHonourTrophy"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class FormerTeam(ApiRecord):
    """A team a player used to play for (``lookupformerteams.php``, ``lookup/player_teams``)."""

    _summary = ("player", "team", "joined", "departed")
    id: int | None  # id
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idFormerTeam
    team: str | None  # strFormerTeam
    badge: str | None  # strBadge
    joined: str | None  # strJoined, usually a year
    departed: str | None  # strDeparted, usually a year
    move_type: str | None  # strMoveType, e.g. Permanent, Loan
    appearances: int | None  # intAppearances
    goals: int | None  # intGoals
    sport: str | None  # strSport

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), player_id=r.id_("idPlayer"), player=r.s("strPlayer"),
                    team_id=r.id_("idFormerTeam"), team=r.s("strFormerTeam"), badge=r.s("strBadge"),
                    joined=r.s("strJoined"), departed=r.s("strDeparted"), move_type=r.s("strMoveType"),
                    appearances=r.i("intAppearances"), goals=r.i("intGoals"), sport=r.s("strSport"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Milestone(ApiRecord):
    """A career milestone or award."""

    _summary = ("player", "name", "date")
    id: int | None  # id
    milestone_id: int | None  # idMilestone
    name: str | None  # strMilestone
    date: Date | None  # dateMilestone
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    sport: str | None  # strSport
    logo: str | None  # strMilestoneLogo

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), milestone_id=r.id_("idMilestone"), name=r.s("strMilestone"),
                    date=r.d("dateMilestone"), player_id=r.id_("idPlayer"), player=r.s("strPlayer"),
                    team_id=r.id_("idTeam"), team=r.s("strTeam"), sport=r.s("strSport"),
                    logo=r.s("strMilestoneLogo"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Contract(ApiRecord):
    """A player's contract."""

    _summary = ("player", "team", "year_start", "year_end")
    id: int | None  # id
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    badge: str | None  # strBadge
    year_start: int | None  # strYearStart
    year_end: int | None  # strYearEnd
    wage: str | None  # strWage, as text
    sport: str | None  # strSport

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), player_id=r.id_("idPlayer"), player=r.s("strPlayer"), team_id=r.id_("idTeam"),
                    team=r.s("strTeam"), badge=r.s("strBadge"), year_start=r.year("strYearStart"),
                    year_end=r.year("strYearEnd"), wage=r.s("strWage"), sport=r.s("strSport"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class EventResult(ApiRecord):
    """One competitor's result in an individual-sport event (race, golf, fight)."""

    _summary = ("event_id", "player", "position")
    id: int | None  # idResult
    event_id: int | None  # idEvent
    event: str | None  # strEvent
    date: Date | None  # dateEvent
    season: str | None  # strSeason
    sport: str | None  # strSport
    country: str | None  # strCountry
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    position: int | None  # intPosition
    points: int | None  # intPoints
    result: str | None  # strResult
    detail: str | None  # strDetail, e.g. a time gap "+29.520"

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idResult"), event_id=r.id_("idEvent"), event=r.s("strEvent"), date=r.d("dateEvent"),
                    season=r.s("strSeason"), sport=r.s("strSport"), country=r.s("strCountry"),
                    player_id=r.id_("idPlayer"), player=r.s("strPlayer"), team_id=r.id_("idTeam"),
                    position=r.i("intPosition"), points=r.i("intPoints"), result=r.s("strResult"),
                    detail=r.s("strDetail"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class PlayerStat(ApiRecord):
    """One statistic for a player in one season."""

    _summary = ("player", "season", "statistic", "value")
    id: int | None  # id
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    team_badge: str | None  # strTeamBadge
    league_id: int | None  # idLeague
    league: str | None  # strLeague
    league_badge: str | None  # strLeagueBadge
    season: str | None  # strSeason
    sport: str | None  # strSport
    statistic: str | None  # strStatistic, e.g. Goals
    value: str | None  # strValue, as text

    @property
    def numeric_value(self) -> float | None:
        try:
            return float(self.value) if self.value is not None else None
        except ValueError:
            return None

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), player_id=r.id_("idPlayer"), player=r.s("strPlayer"), team_id=r.id_("idTeam"),
                    team=r.s("strTeam"), team_badge=r.s("strTeamBadge"), league_id=r.id_("idLeague"),
                    league=r.s("strLeague"), league_badge=r.s("strLeagueBadge"), season=r.s("strSeason"),
                    sport=r.s("strSport"), statistic=r.s("strStatistic"), value=r.s("strValue"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Equipment(ApiRecord):
    """A team kit."""

    _summary = ("team_id", "season", "type")
    id: int | None  # idEquipment
    team_id: int | None  # idTeam
    season: str | None  # strSeason
    type: str | None  # strType, e.g. 1st, 2nd, GK
    image: str | None  # strEquipment
    uploaded_by: str | None  # strUsername
    added: DateTime | None  # date (no zone stated; naive)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idEquipment"), team_id=r.id_("idTeam"), season=r.s("strSeason"), type=r.s("strType"),
                    image=r.s("strEquipment"), uploaded_by=r.s("strUsername"), added=r.ldt("date"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Venue(ApiRecord):
    """A stadium, arena or circuit."""

    _summary = ("id", "name")
    id: int | None  # idVenue
    name: str | None  # strVenue
    alternate_name: str | None  # strVenueAlternate
    sponsor_name: str | None  # strVenueSponsor
    sport: str | None  # strSport
    location: str | None  # strLocation
    country: str | None  # strCountry
    timezone: str | None  # strTimezone, as text
    capacity: int | None  # intCapacity
    formed_year: int | None  # intFormedYear
    architect: str | None  # strArchitect
    cost: str | None  # strCost, as text
    map: str | None  # strMap: "lat, lon" or an image URL
    thumb: str | None  # strThumb
    logo: str | None  # strLogo
    fanart: tuple[str, ...]  # strFanart1..4
    creative_commons: bool | None  # strCreativeCommons
    descriptions: Mapping[str, str]
    socials: Socials
    loved: int | None  # intLoved
    is_locked: bool | None  # strLocked
    duplicate_of: int | None  # idDupe

    @property
    def description(self) -> str | None:
        return self.descriptions.get("EN")

    @property
    def coordinates(self) -> tuple[float, float] | None:
        """``map`` as (latitude, longitude), when it holds coordinates."""
        parts = (self.map or "").split(",")
        if len(parts) != 2:
            return None
        try:
            return float(parts[0]), float(parts[1])
        except ValueError:
            return None

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(
            id=r.id_("idVenue"), name=r.s("strVenue"), alternate_name=r.s("strVenueAlternate"),
            sponsor_name=r.s("strVenueSponsor"), sport=r.s("strSport"), location=r.s("strLocation"),
            country=r.s("strCountry"), timezone=r.s("strTimezone"), capacity=r.i("intCapacity"),
            formed_year=r.year("intFormedYear"), architect=r.s("strArchitect"), cost=r.s("strCost"),
            map=r.s("strMap"), thumb=r.s("strThumb"), logo=r.s("strLogo"), fanart=r.numbered("strFanart", 4),
            creative_commons=r.b("strCreativeCommons"), descriptions=MappingProxyType(r.descriptions()),
            socials=Socials._read(r), loved=r.i("intLoved"), is_locked=r.locked(), duplicate_of=r.id_("idDupe"),
        )


# ---------------------------------------------------------------------------------------------
# Events


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Event(ApiRecord):
    """A game, match, race or other event. Also used for video highlights.

    ``timestamp``, ``date`` and ``time`` are UTC; ``local_date`` and ``local_time`` are the
    venue's local time and are often missing for future events.
    """

    _summary = ("id", "name", "timestamp", "status_code")
    id: int | None  # idEvent
    name: str | None  # strEvent, e.g. "Arsenal vs Chelsea"
    alternate_name: str | None  # strEventAlternate, e.g. "Chelsea @ Arsenal"
    filename: str | None  # strFilename
    sport: str | None  # strSport
    league_id: int | None  # idLeague
    league: str | None  # strLeague
    league_badge: str | None  # strLeagueBadge
    season: str | None  # strSeason
    round: int | None  # intRound: round number, or a stage code (125 quarter-final … 500 pre-season); see stage
    group: str | None  # strGroup
    home_team_id: int | None  # idHomeTeam
    home_team: str | None  # strHomeTeam
    home_team_badge: str | None  # strHomeTeamBadge
    away_team_id: int | None  # idAwayTeam
    away_team: str | None  # strAwayTeam
    away_team_badge: str | None  # strAwayTeamBadge
    home_score: int | None  # intHomeScore
    away_score: int | None  # intAwayScore
    home_score_extra: int | None  # intHomeScoreExtra
    away_score_extra: int | None  # intAwayScoreExtra
    timestamp: DateTime | None  # strTimestamp: start, UTC (aware)
    date: Date | None  # dateEvent (UTC)
    time: Time | None  # strTime (UTC)
    local_date: Date | None  # dateEventLocal
    local_time: Time | None  # strTimeLocal
    status_code: str | None  # strStatus: NS, 2H, FT, Q3, PST...; older events often have none
    is_postponed: bool | None  # strPostponed
    venue_id: int | None  # idVenue
    venue: str | None  # strVenue
    city: str | None  # strCity
    country: str | None  # strCountry
    spectators: int | None  # intSpectators
    official: str | None  # strOfficial
    result_text: str | None  # strResult
    description: str | None  # strDescriptionEN
    thumb: str | None  # strThumb
    poster: str | None  # strPoster
    banner: str | None  # strBanner
    square: str | None  # strSquare
    fanart: str | None  # strFanart
    map: str | None  # strMap
    video: str | None  # strVideo: highlights, usually YouTube
    tweet: str | None  # strTweet1
    weather: str | None  # strWeather
    rating: float | None  # intScore
    rating_votes: int | None  # intScoreVotes
    is_locked: bool | None  # strLocked
    api_football_id: int | None  # idAPIfootball

    @property
    def status(self) -> EventStatus:
        return EventStatus.of(self.status_code)

    @property
    def stage(self) -> RoundStage | None:
        """The stage when ``round`` is a stage code (e.g. 200 = final), or None for an ordinary round."""
        return RoundStage.of(self.round)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(
            id=r.id_("idEvent"), name=r.s("strEvent"), alternate_name=r.s("strEventAlternate"),
            filename=r.s("strFilename"), sport=r.s("strSport"), league_id=r.id_("idLeague"), league=r.s("strLeague"),
            league_badge=r.s("strLeagueBadge"), season=r.s("strSeason"), round=r.i("intRound"), group=r.s("strGroup"),
            home_team_id=r.id_("idHomeTeam"), home_team=r.s("strHomeTeam"), home_team_badge=r.s("strHomeTeamBadge"),
            away_team_id=r.id_("idAwayTeam"), away_team=r.s("strAwayTeam"), away_team_badge=r.s("strAwayTeamBadge"),
            home_score=r.i("intHomeScore"), away_score=r.i("intAwayScore"),
            home_score_extra=r.i("intHomeScoreExtra"), away_score_extra=r.i("intAwayScoreExtra"),
            timestamp=r.ts("strTimestamp"), date=r.d("dateEvent"), time=r.t("strTime"),
            local_date=r.d("dateEventLocal"), local_time=r.t("strTimeLocal"), status_code=r.s("strStatus"),
            is_postponed=r.b("strPostponed"), venue_id=r.id_("idVenue"), venue=r.s("strVenue"), city=r.s("strCity"),
            country=r.s("strCountry"), spectators=r.i("intSpectators"), official=r.s("strOfficial"),
            result_text=r.s("strResult"), description=r.s("strDescriptionEN"), thumb=r.s("strThumb"),
            poster=r.s("strPoster"), banner=r.s("strBanner"), square=r.s("strSquare"), fanart=r.s("strFanart"),
            map=r.s("strMap"), video=r.s("strVideo"), tweet=r.s("strTweet1"), weather=r.s("strWeather"),
            rating=r.f("intScore"), rating_votes=r.i("intScoreVotes"), is_locked=r.locked(),
            api_football_id=r.id_("idAPIfootball"),
        )


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class Standing(ApiRecord):
    """One row of a league table (``lookuptable.php``; v1 only)."""

    _summary = ("rank", "team", "points")
    id: int | None  # idStanding
    rank: int | None  # intRank
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    badge: str | None  # strBadge
    league_id: int | None  # idLeague
    league: str | None  # strLeague
    season: str | None  # strSeason
    group: str | None  # strGroup
    form: str | None  # strForm, e.g. "WWDLW"
    description: str | None  # strDescription, e.g. "Promotion - Champions League"
    played: int | None  # intPlayed
    won: int | None  # intWin
    drawn: int | None  # intDraw
    lost: int | None  # intLoss
    goals_for: int | None  # intGoalsFor
    goals_against: int | None  # intGoalsAgainst
    goal_difference: int | None  # intGoalDifference
    points: int | None  # intPoints
    updated: DateTime | None  # dateUpdated (naive)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idStanding"), rank=r.i("intRank"), team_id=r.id_("idTeam"), team=r.s("strTeam"),
                    badge=r.s("strBadge"), league_id=r.id_("idLeague"), league=r.s("strLeague"),
                    season=r.s("strSeason"), group=r.s("strGroup"), form=r.s("strForm"),
                    description=r.s("strDescription"), played=r.i("intPlayed"), won=r.i("intWin"),
                    drawn=r.i("intDraw"), lost=r.i("intLoss"), goals_for=r.i("intGoalsFor"),
                    goals_against=r.i("intGoalsAgainst"), goal_difference=r.i("intGoalDifference"),
                    points=r.i("intPoints"), updated=r.ldt("dateUpdated"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class LineupEntry(ApiRecord):
    """One player in an event's lineup."""

    _summary = ("event_id", "player", "team")
    id: int | None  # idLineup
    event_id: int | None  # idEvent
    event: str | None  # strEvent (v2)
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    is_home: bool | None  # strHome
    is_substitute: bool | None  # strSubstitute
    position: str | None  # strPosition
    position_short: str | None  # strPositionShort (v2; often null)
    formation: str | None  # strFormation (v2; often null)
    squad_number: int | None  # intSquadNumber
    country: str | None  # strCountry (v2)
    season: str | None  # strSeason (v2)
    thumb: str | None  # strThumb (v1)
    cutout: str | None  # strCutout
    render: str | None  # strRender (v1)
    api_football_id: int | None  # idAPIfootball (v2)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idLineup"), event_id=r.id_("idEvent"), event=r.s("strEvent"),
                    player_id=r.id_("idPlayer"), player=r.s("strPlayer"), team_id=r.id_("idTeam"),
                    team=r.s("strTeam"), is_home=r.b("strHome"), is_substitute=r.b("strSubstitute"),
                    position=r.s("strPosition"), position_short=r.s("strPositionShort"),
                    formation=r.s("strFormation"), squad_number=r.i("intSquadNumber"), country=r.s("strCountry"),
                    season=r.s("strSeason"), thumb=r.s("strThumb"), cutout=r.s("strCutout"),
                    render=r.s("strRender"), api_football_id=r.id_("idAPIfootball"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class TimelineEntry(ApiRecord):
    """A goal, card or substitution during an event."""

    _summary = ("event_id", "minute", "type", "player")
    id: int | None  # idTimeline
    event_id: int | None  # idEvent
    event: str | None  # strEvent
    date: Date | None  # dateEvent
    season: str | None  # strSeason
    minute: int | None  # intTime
    period: str | None  # strPeriod
    type: str | None  # strTimeline, e.g. Goal, Card, subst
    detail: str | None  # strTimelineDetail
    comment: str | None  # strComment
    player_id: int | None  # idPlayer
    player: str | None  # strPlayer
    cutout: str | None  # strCutout
    assist_id: int | None  # idAssist
    assist: str | None  # strAssist
    team_id: int | None  # idTeam
    team: str | None  # strTeam
    is_home: bool | None  # strHome
    api_football_id: int | None  # idAPIfootball

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idTimeline"), event_id=r.id_("idEvent"), event=r.s("strEvent"), date=r.d("dateEvent"),
                    season=r.s("strSeason"), minute=r.i("intTime"), period=r.s("strPeriod"),
                    type=r.s("strTimeline"), detail=r.s("strTimelineDetail"), comment=r.s("strComment"),
                    player_id=r.id_("idPlayer"), player=r.s("strPlayer"), cutout=r.s("strCutout"),
                    assist_id=r.id_("idAssist"), assist=r.s("strAssist"), team_id=r.id_("idTeam"),
                    team=r.s("strTeam"), is_home=r.b("strHome"), api_football_id=r.id_("idAPIfootball"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class EventStat(ApiRecord):
    """One team statistic for an event, home vs away."""

    _summary = ("event_id", "name", "home", "away")
    id: int | None  # idStatistic
    event_id: int | None  # idEvent
    event: str | None  # strEvent
    name: str | None  # strStat
    home: float | None  # intHome
    away: float | None  # intAway
    api_football_id: int | None  # idApiFootball (note the capitalisation)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idStatistic"), event_id=r.id_("idEvent"), event=r.s("strEvent"), name=r.s("strStat"),
                    home=r.f("intHome"), away=r.f("intAway"), api_football_id=r.id_("idApiFootball"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class TvListing(ApiRecord):
    """One broadcast of an event on one channel."""

    _summary = ("event_id", "channel", "timestamp")
    id: int | None  # id
    event_id: int | None  # idEvent
    event: str | None  # strEvent
    sport: str | None  # strSport
    season: str | None  # strSeason
    channel_id: int | None  # idChannel
    channel: str | None  # strChannel, e.g. "TSN 1"
    channel_logo: str | None  # strLogo
    country: str | None  # strCountry: the channel's country
    event_country: str | None  # strEventCountry
    timestamp: DateTime | None  # strTimeStamp (capital S, space-separated): UTC
    date: Date | None  # dateEvent
    time: Time | None  # strTime
    division: int | None  # intDivision
    event_thumb: str | None  # strEventThumb
    event_poster: str | None  # strEventPoster
    event_banner: str | None  # strEventBanner
    event_square: str | None  # strEventSquare

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("id"), event_id=r.id_("idEvent"), event=r.s("strEvent"), sport=r.s("strSport"),
                    season=r.s("strSeason"), channel_id=r.id_("idChannel"), channel=r.s("strChannel"),
                    channel_logo=r.s("strLogo"), country=r.s("strCountry"), event_country=r.s("strEventCountry"),
                    timestamp=r.ts("strTimeStamp") or r.ts("strTimestamp"), date=r.d("dateEvent"),
                    time=r.t("strTime"), division=r.i("intDivision"), event_thumb=r.s("strEventThumb"),
                    event_poster=r.s("strEventPoster"), event_banner=r.s("strEventBanner"),
                    event_square=r.s("strEventSquare"))


@dataclass(frozen=True, kw_only=True, eq=False, repr=False)
class LiveScore(ApiRecord):
    """A game in progress. Entries can be stale: check ``updated``."""

    _summary = ("event_id", "home_team", "home_score", "away_score", "away_team", "status_code")
    id: int | None  # idLiveScore
    event_id: int | None  # idEvent
    sport: str | None  # strSport
    league_id: int | None  # idLeague
    league: str | None  # strLeague
    division: int | None  # intDivision
    home_team_id: int | None  # idHomeTeam
    home_team: str | None  # strHomeTeam
    home_team_badge: str | None  # strHomeTeamBadge
    away_team_id: int | None  # idAwayTeam
    away_team: str | None  # strAwayTeam
    away_team_badge: str | None  # strAwayTeamBadge
    home_score: int | None  # intHomeScore
    away_score: int | None  # intAwayScore
    status_code: str | None  # strStatus
    progress: str | None  # strProgress: the minute, or "Final"
    event_time: Time | None  # strEventTime, HH:mm
    date: Date | None  # dateEvent
    timestamp: DateTime | None  # strTimestamp: kick-off, UTC
    updated: DateTime | None  # updated (naive; zone not stated)

    @property
    def status(self) -> EventStatus:
        return EventStatus.of(self.status_code)

    @classmethod
    def _read(cls, r: Rec) -> dict[str, Any]:
        return dict(id=r.id_("idLiveScore"), event_id=r.id_("idEvent"), sport=r.s("strSport"),
                    league_id=r.id_("idLeague"), league=r.s("strLeague"), division=r.i("intDivision"),
                    home_team_id=r.id_("idHomeTeam"), home_team=r.s("strHomeTeam"),
                    home_team_badge=r.s("strHomeTeamBadge"), away_team_id=r.id_("idAwayTeam"),
                    away_team=r.s("strAwayTeam"), away_team_badge=r.s("strAwayTeamBadge"),
                    home_score=r.i("intHomeScore"), away_score=r.i("intAwayScore"), status_code=r.s("strStatus"),
                    progress=r.s("strProgress"), event_time=r.t("strEventTime"), date=r.d("dateEvent"),
                    timestamp=r.ts("strTimestamp"), updated=r.ldt("updated"))
