#!/usr/bin/env python3
"""Builds docs-site/openapi/v1.yaml and v2.yaml, OpenAPI 3.1 descriptions of TheSportsDB's API.

Three sources:
- ENDPOINTS below: paths, parameters, record keys and documented limits (written by hand from the
  official documentation and the measurements in docs/THESPORTSDB-API-BEHAVIOUR.md).
- The recorded responses in kotlin/src/test/resources/fixtures/: one schema per record type with every
  field ever seen, the measured free/premium counts, and real (trimmed) examples.
- The field comments in python/src/thesportsdb_client/models.py, for field descriptions.

    docs-site/.venv/bin/python docs-site/tools/build_openapi.py
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "kotlin" / "src" / "test" / "resources" / "fixtures"
MODELS = ROOT / "python" / "src" / "thesportsdb_client" / "models.py"
OUT = ROOT / "docs-site" / "openapi"
MEASURED = "5 Oct 2026"


@dataclass
class P:
    name: str
    description: str
    example: Any
    required: bool = True
    type: str = "string"
    fmt: str | None = None


@dataclass
class E:
    path: str
    op: str
    tag: str
    summary: str
    key: str
    schema: str
    params: list[P] = field(default_factory=list)
    fixture: str | None = None  # file name without .json, in v1-free/v1-premium (v1) or v2 (v2)
    documented: str | None = None  # the official page's limit: "free / premium" (v1) or "N" (v2)
    notes: str = ""
    undocumented: bool = False
    free_only: bool = False
    single: bool = False  # a lookup by id: one record


def id_(name: str, what: str, example: int) -> P:
    return P(name, f"The {what} id.", example, type="integer")


DATE = P("d", "A day, `YYYY-MM-DD`. Events are filed under their **UTC** date.", "2026-10-04", fmt="date")
SEASON_OPT = P("s", "A season name, e.g. `2026-2027` (or `2026` for single-year leagues).", "2026-2027", required=False)

V1: list[E] = [
    # Search
    E("searchteams.php", "searchTeams", "Search", "Search teams by name", "teams", "Team",
      [P("t", "Team name. Spaces as `%20` or `_`.", "Arsenal")], "search_teams", "1 / 100",
      "The free keys only find the exact team (1 record)."),
    E("searchevents.php", "searchEvents", "Search", "Search events by name", "event", "Event",
      [P("e", "Event name, `Home vs Away`.", "Arsenal_vs_Chelsea"), P("s", "Narrow to a season.", "2016-2017", required=False),
       P("d", "Narrow to a date, `YYYY-MM-DD`.", "2015-04-26", required=False, fmt="date"),
       P("f", "Alternatively, an event filename (same as `searchfilename.php`).", "English_Premier_League_2015-04-26_Arsenal_vs_Chelsea",
         required=False)],
      "search_events", "1 / 10", "Pass at most one of `s` and `d`."),
    E("searchfilename.php", "searchEventsByFilename", "Search", "Search events by filename", "event", "Event",
      [P("e", "Event filename: `{League} {YYYY-MM-DD} {Home} vs {Away}`.", "English_Premier_League_2015-04-26_Arsenal_vs_Chelsea"),
       P("s", "Narrow to a season.", "2014-2015", required=False)], "search_filename", "1 / 10"),
    E("searchplayers.php", "searchPlayers", "Search", "Search players by name", "player", "Player",
      [P("p", "Player name.", "Danny_Welbeck")], "search_players", "1 / 10", "Returns summary fields plus a `relevance` score."),
    E("searchvenues.php", "searchVenues", "Search", "Search venues by name", "venues", "Venue",
      [P("v", "Venue name.", "Wembley")], "search_venues", "1 / 10"),
    # Lookup
    E("lookupleague.php", "lookupLeague", "Lookup", "Look up a league", "leagues", "League",
      [id_("id", "league", 4328)], "lookup_league", "1 / 1", single=True),
    E("lookuptable.php", "lookupTable", "Lookup", "League table (standings)", "table", "Standing",
      [P("l", "The league id.", 4328, type="integer"), SEASON_OPT], "lookup_table", "5 / 100",
      "Only some leagues have tables, mostly featured soccer leagues. Omit `s` for the current season. "
      "There is no v2 equivalent: use this endpoint with a premium key."),
    E("lookupteam.php", "lookupTeam", "Lookup", "Look up a team", "teams", "Team", [id_("id", "team", 133604)],
      "lookup_team", "1 / 1", single=True),
    E("lookupequipment.php", "lookupEquipment", "Lookup", "A team's kits", "equipment", "Equipment",
      [id_("id", "team", 133597)], "lookup_equipment", "2 / 100"),
    E("lookupplayer.php", "lookupPlayer", "Lookup", "Look up a player", "players", "Player", [id_("id", "player", 34145937)],
      "lookup_player", "1 / 1", single=True),
    E("lookuphonours.php", "lookupHonours", "Lookup", "A player's honours", "honours", "Honour",
      [id_("id", "player", 34147178)], "lookup_honours", "5 / 500"),
    E("lookupformerteams.php", "lookupFormerTeams", "Lookup", "A player's former teams", "formerteams", "FormerTeam",
      [id_("id", "player", 34147178)], "lookup_former_teams", "5 / 100"),
    E("lookupmilestones.php", "lookupMilestones", "Lookup", "A player's milestones", "milestones", "Milestone",
      [id_("id", "player", 34161397)], "lookup_milestones", "5 / 100"),
    E("lookupcontracts.php", "lookupContracts", "Lookup", "A player's contracts", "contracts", "Contract",
      [id_("id", "player", 34147178)], "lookup_contracts", "1 / 100"),
    E("playerresults.php", "playerResults", "Lookup", "A player's results (individual sports)", "results", "EventResult",
      [id_("id", "player", 34160573)], "player_results", "5 / 500", "For races, golf, fights and similar."),
    E("lookupplayerstats.php", "lookupPlayerStats", "Lookup", "A player's season statistics", "playerstats", "PlayerStat",
      [id_("id", "player", 34146304)], "lookup_player_stats", "10 / 10000", "One record per statistic per season."),
    E("lookupevent.php", "lookupEvent", "Lookup", "Look up an event", "events", "Event", [id_("id", "event", 441613)],
      "lookup_event", "1 / 1", single=True),
    E("eventresults.php", "eventResults", "Lookup", "An event's results (individual sports)", "results", "EventResult",
      [id_("id", "event", 652890)], "event_results", "5 / 100"),
    E("lookuplineup.php", "lookupLineup", "Lookup", "An event's lineups", "lineup", "LineupEntry",
      [id_("id", "event", 1032723)], "lookup_lineup", "5 / 100", "The old name `lookuplineups.php` returns 404."),
    E("lookuptimeline.php", "lookupTimeline", "Lookup", "An event's timeline", "timeline", "TimelineEntry",
      [id_("id", "event", 1032718)], "lookup_timeline", "5 / 100", "Goals, cards and substitutions."),
    E("lookupeventstats.php", "lookupEventStats", "Lookup", "An event's team statistics", "eventstats", "EventStat",
      [id_("id", "event", 1032723)], "lookup_event_stats", "5 / 100"),
    E("lookuptv.php", "lookupEventTv", "Lookup", "The channels showing an event", "tvevent", "TvListing",
      [id_("id", "event", 2494052)], "lookup_tv", "2 / 100"),
    E("lookupvenue.php", "lookupVenue", "Lookup", "Look up a venue", "venues", "Venue", [id_("id", "venue", 16163)],
      "lookup_venue", "1 / 1", single=True),
    # Lists
    E("all_sports.php", "allSports", "Lists", "All sports", "sports", "Sport", [], "all_sports", "2 / 50"),
    E("all_countries.php", "allCountries", "Lists", "All countries", "countries", "Country", [], "all_countries", "50 / 500",
      "Name and 32 px flag only; v2 `all/countries` adds codes and more flag sizes."),
    E("all_leagues.php", "allLeagues", "Lists", "All leagues", "leagues", "League", [], "all_leagues", "10 / 3000",
      "Id, name and sport. `strLeagueAlternate` is included **only with a premium key**."),
    E("search_all_leagues.php", "leaguesInCountry", "Lists", "Leagues in a country", "countries", "League",
      [P("c", "Country name, as in `all_countries.php`.", "England"), P("s", "Sport name.", "Soccer", required=False)],
      "search_all_leagues", "10 / 100", "Full league records. The record key really is `countries`."),
    E("search_all_seasons.php", "leagueSeasons", "Lists", "A league's seasons", "seasons", "Season",
      [P("id", "The league id.", 4328, type="integer"),
       P("badge", "`1` to add `strBadge`.", 1, required=False, type="integer"),
       P("poster", "`1` to add `strPoster`.", 1, required=False, type="integer"),
       P("description", "`1` to add `strDescriptionEN`.", 1, required=False, type="integer")],
      "search_all_seasons", "5 / 500", "Season names only unless one of `badge`, `poster` or `description` is set. "
      "The parameter is `id`; `l=` returns `{\"seasons\":\"Invalid League ID passed\"}`."),
    E("search_all_teams.php", "allTeams", "Lists", "Teams in a league, or of a sport in a country", "teams", "Team",
      [P("l", "League **name** (not id).", "English_Premier_League", required=False),
       P("s", "Sport name (with `c`).", "Soccer", required=False), P("c", "Country name (with `s`).", "Spain", required=False)],
      "search_all_teams_league", "10 / 3000", "Pass `l`, or `s` and `c`."),
    E("lookup_all_players.php", "teamPlayers", "Lists", "A team's squad", "player", "Player",
      [id_("id", "team", 133604)], "lookup_all_players", "10 / 3000"),
    # Schedules
    E("eventsnext.php", "teamNextEvents", "Schedules", "A team's next events", "events", "Event",
      [id_("id", "team", 133602)], "events_next", "1 / 10", "Free keys: home games only."),
    E("eventslast.php", "teamLastEvents", "Schedules", "A team's last results", "results", "Event",
      [id_("id", "team", 133602)], "events_last", "1 / 10", "Free keys: home games only. The record key is `results`, "
      "but the records are events."),
    E("eventsnextleague.php", "leagueNextEvents", "Schedules", "A league's next events", "events", "Event",
      [id_("id", "league", 4328)], "events_next_league", "1 / 20"),
    E("eventspastleague.php", "leaguePastEvents", "Schedules", "A league's latest results", "events", "Event",
      [id_("id", "league", 4328)], "events_past_league", "1 / 20"),
    E("eventsday.php", "eventsOnDay", "Schedules", "Every event on a day", "events", "Event",
      [DATE, P("s", "Sport name.", "Ice_Hockey", required=False),
       P("l", "League id or name.", "4328", required=False)], "events_day", "3 / 1500",
      "The day is a **UTC** date: evening games in the Americas appear on the next day. "
      "A day with no matching events returns `{\"events\":null}`."),
    E("eventsseason.php", "seasonEvents", "Schedules", "A league's whole season", "events", "Event",
      [id_("id", "league", 4328), P("s", "Season name, from `strCurrentSeason`.", "2026-2027")], "events_season", "15 / 3000",
      "Fewer fields than a lookup (no descriptions, officials or spectators)."),
    E("eventsround.php", "roundEvents", "Schedules", "One round of a season", "events", "Event",
      [id_("id", "league", 4328), P("r", "Round (matchday) number.", 1, type="integer"), P("s", "Season name.", "2026-2027")],
      "events_round", None, "Returns the whole round. `l=` instead of `id=` returns "
      "`{\"events\":\"Invalid League ID or no round passed\"}`. With a premium key, filter v2 "
      "`schedule/league/{id}/{season}` by `intRound` instead.", undocumented=True, free_only=True),
    # TV
    E("eventstv.php", "tvListings", "TV", "TV listings", "tvevents", "TvListing",
      [P("d", "A day, `YYYY-MM-DD`.", "2026-10-05", required=False, fmt="date"),
       P("s", "Sport name (with `d`).", "Ice_Hockey", required=False),
       P("a", "Country of the channel (with `d` **and** `s`).", "Canada", required=False),
       P("c", "Channel name (alone), e.g. `TSN_1`.", "TSN_1", required=False),
       P("id", "Channel id, `idChannel` (alone). **Not** an event id.", 8631, required=False, type="integer")],
      "events_tv_day", "1 / 1500",
      "Use one form: `d` (optionally with `s`, or `s` and `a`), `c`, or `id`. "
      "**`a` without `s` returns an empty body.** For an event's channels use `lookuptv.php`."),
    # Video
    E("eventshighlights.php", "highlights", "Video", "Events with highlight videos", "tvhighlights", "Event",
      [DATE, P("l", "League id.", 4328, required=False, type="integer"), P("s", "Sport name.", "Soccer", required=False)],
      "events_highlights", "2 / 50", "The video URL is in `strVideo`. Premium results stop at the documented 50."),
    # Live
    E("livescore.php", "liveScores", "Live scores", "Games in progress", "livescore", "LiveScore",
      [P("s", "Sport name.", "Soccer")], "livescore_soccer", None,
      "The free keys get the full feed. `l=` (league) is **ignored**: every live game is returned. "
      "Entries can be stale; check `updated`. v2 `livescore/...` is the documented equivalent.", undocumented=True),
]

V2: list[E] = [
    *[E(f"search/{kind}/{{name}}", f"search{title}", "Search", f"Search {title.lower()}", "search", schema,
        [P("name", f"{title[:-1] if title.endswith('s') else title} name; spaces as `%20`.", ex)], f"search_{kind}", "10", note)
      for kind, title, schema, ex, note in [
          ("league", "Leagues", "League", "English Premier League", ""),
          ("team", "Teams", "Team", "Arsenal", "Ids are JSON numbers here, strings everywhere else."),
          ("player", "Players", "Player", "Danny Welbeck", ""),
          ("event", "Events", "Event", "Arsenal vs Chelsea", "Needs the exact stored event name; `Arsenal vs Chelsea` "
           "returns `No data found` (v1 `searchevents.php` is looser)."),
          ("venue", "Venues", "Venue", "Wembley", ""),
      ]],
    *[E(f"lookup/{kind}/{{id}}", f"lookup{''.join(w.title() for w in kind.split('_'))}", "Lookup", summary, "lookup", schema,
        [P("id", f"The {what} id.", ex, type="integer")], f"lookup_{kind}", "1", note, single=single)
      for kind, summary, schema, what, ex, note, single in [
          ("league", "Look up a league", "League", "league", 4328, "", True),
          ("team", "Look up a team", "Team", "team", 133604, "", True),
          ("team_equipment", "A team's kits", "Equipment", "team", 133597, "", False),
          ("player", "Look up a player", "Player", "player", 34145937, "", True),
          ("player_contracts", "A player's contracts", "Contract", "player", 34147178, "", False),
          ("player_results", "A player's results (individual sports)", "EventResult", "player", 34160573, "", False),
          ("player_honours", "A player's honours", "Honour", "player", 34147178, "", False),
          ("player_milestones", "A player's milestones", "Milestone", "player", 34161397, "", False),
          ("player_teams", "A player's former teams", "FormerTeam", "player", 34147178, "", False),
          ("player_stats", "A player's season statistics", "PlayerStat", "player", 34146304, "", False),
          ("event", "Look up an event", "Event", "event", 441613, "", True),
          ("event_lineup", "An event's lineups", "LineupEntry", "event", 1032723,
           "Adds `strFormation` and `strPositionShort` (often null) to v1's fields.", False),
          ("event_results", "An event's results (individual sports)", "EventResult", "event", 652890, "", False),
          ("event_stats", "An event's team statistics", "EventStat", "event", 1032723, "", False),
          ("event_timeline", "An event's timeline", "TimelineEntry", "event", 1032718, "", False),
          ("event_tv", "The channels showing an event", "TvListing", "event", 2494052, "", False),
          ("event_highlights", "An event's highlight video", "Event", "event", 441613, "The video URL is in `strVideo`.", False),
          ("venue", "Look up a venue", "Venue", "venue", 16163, "", True),
      ]],
    E("list/teams/{id}", "listTeams", "Lists", "A league's teams", "list", "Team", [id_("id", "league", 4328)], "list_teams", "100",
      "Badges and colours, but no alternate names: look teams up by id for those."),
    E("list/seasons/{id}", "listSeasons", "Lists", "A league's seasons", "list", "Season", [id_("id", "league", 4328)],
      "list_seasons", "100", "With badge, poster and description."),
    E("list/players/{id}", "listPlayers", "Lists", "A team's squad", "list", "Player", [id_("id", "team", 133604)],
      "list_players", "100"),
    E("list/seasonposters/{id}", "listSeasonPosters", "Lists", "A league's season posters", "list", "SeasonPoster",
      [id_("id", "league", 4328)], "list_seasonposters", None,
      "Every season poster and badge uploaded for the league, with the uploader; several per season are possible. "
      "In TheSportsDB's OpenAPI description but not on its HTML documentation page."),
    E("all/countries", "allCountries", "All", "All countries", "all", "Country", [], "all_countries", "500"),
    E("all/sports", "allSports", "All", "All sports", "all", "Sport", [], "all_sports", "500"),
    E("all/leagues", "allLeagues", "All", "All leagues", "all", "League", [], "all_leagues", "3000"),
    *[E(f"schedule/{when}/{what}/{{id}}", f"schedule{when.title()}{what.title()}", "Schedules",
        f"{'Next' if when == 'next' else 'Previous'} events of a {what}", "schedule", "Event",
        [P("id", f"The {what} id.", ex, type="integer")], f"schedule_{when}_{what}", "10", "")
      for when in ("next", "previous") for what, ex in (("league", 4328), ("team", 133604), ("venue", 16163))],
    E("schedule/full/team/{id}", "scheduleFullTeam", "Schedules", "A team's full schedule", "schedule", "Event",
      [id_("id", "team", 133604)], "schedule_full_team", "250", "Past and future, across all competitions."),
    E("schedule/league/{id}/{season}", "scheduleLeagueSeason", "Schedules", "A league's whole season", "schedule", "Event",
      [id_("id", "league", 4328), P("season", "Season name, from `strCurrentSeason`.", "2026-2027")],
      "schedule_league_season", "3000", "Every event of the season in one call."),
    *[E(f"filter/tv/{kind}/{{{param}}}", f"tvBy{kind.title()}", "TV", summary, "filter", "TvListing",
        [P(param, desc, ex, type=t, fmt=f)], fx, "100", note)
      for kind, param, summary, desc, ex, t, f, fx, note in [
          ("day", "date", "Every TV listing on a day", "A day, `YYYY-MM-DD`.", "2026-10-05", "string", "date", "filter_tv_day",
           "Worldwide."),
          ("country", "country", "A country's TV listings", "Country of the channel.", "Canada", "string", None, "filter_tv_country",
           "About a week of listings."),
          ("sport", "sport", "A sport's TV listings", "Sport name; spaces as `%20`.", "Ice Hockey", "string", None, "filter_tv_sport", ""),
          ("channel", "channel", "A channel's TV listings, by name", "Channel name in TheSportsDB's spelling (`TSN 1`, not "
           "`TSN1`); partial names match.", "TSN 1", "string", None, "filter_tv_channel", ""),
          ("channelid", "id", "A channel's TV listings, by id", "The channel id, `idChannel`.", 8631, "integer", None,
           "filter_tv_channel_id", ""),
      ]],
    E("livescore/{sportOrLeague}", "liveScores", "Live scores", "Games in progress in a sport or a league", "livescore",
      "LiveScore", [P("sportOrLeague", "A sport name (e.g. `soccer`) or a league id (e.g. `4328`).", "soccer")],
      "livescore_soccer", "100",
      "A league with nothing in play returns `No data found`. Entries can be stale; check `updated`."),
    E("livescore/all", "liveScoresAll", "Live scores", "Every game in progress", "livescore", "LiveScore", [], "livescore_all", "500"),
]

TAGS = {
    "Search": "Find records by name.",
    "Lookup": "Fetch records by id, and the details attached to a team, player or event.",
    "Lists": "Lists of leagues, seasons, teams and players.",
    "All": "Complete catalogues of countries, sports and leagues.",
    "Schedules": "Fixtures and results. Times are UTC.",
    "TV": "TV listings: which channels show which events.",
    "Video": "Highlight videos.",
    "Live scores": "Games in progress.",
}

# ---------------------------------------------------------------------------------------------


def field_docs() -> dict[str, dict[str, str]]:
    """{schema: {apiField: description}} from the comments in python/src/thesportsdb_client/models.py."""
    src = MODELS.read_text()
    parts = re.split(r"\nclass (\w+)\(ApiRecord\):", src)
    docs: dict[str, dict[str, str]] = {}
    for i in range(1, len(parts), 2):
        out: dict[str, str] = {}
        for m in re.finditer(r"^    (\w+): [^\n#=]+?(?:  # (\w+)(.*))?$", parts[i + 1], re.M):
            pyname, api, rest = m.group(1), m.group(2), (m.group(3) or "")
            if not api:
                continue
            text = re.sub(r"\s*\((?:aware|naive[^)]*)\)", "", rest).strip(" ,:;")
            text = text[0].upper() + text[1:] if text else pyname.replace("_", " ").capitalize()
            out[api] = text if text.endswith((".", ")")) else text + "."
        docs[parts[i]] = out
    return docs


GENERIC = {
    "strWebsite": "Website, often without a scheme.", "strFacebook": "Facebook page, often without a scheme.",
    "strTwitter": "Twitter/X profile.", "strInstagram": "Instagram profile.", "strYoutube": "YouTube channel.",
    "strRSS": "RSS feed URL.", "strLocked": "`locked` or `unlocked`: whether the record is protected from edits.",
}


GLOSSARY: dict[str, Any] = yaml.safe_load((ROOT / "docs-site" / "fields.yaml").read_text())
UNDESCRIBED: set[str] = set()


def describe(schema: str, api_field: str, docs: dict[str, dict[str, str]]) -> str:
    entry = GLOSSARY.get(api_field)
    if isinstance(entry, dict):
        return str(entry.get(schema, entry["default"]))
    if isinstance(entry, str):
        return entry
    if m := re.fullmatch(r"strDescription([A-Z]{2})", api_field):
        return f"Description in language `{m.group(1)}`."
    if m := re.fullmatch(r"strFanart(\d)", api_field):
        return f"Fan art image {m.group(1)}."
    if m := re.fullmatch(r"strColour(\d)", api_field):
        return f"Colour {m.group(1)}, hex like `#EF0107`."
    if m := re.fullmatch(r"(id|str)League(\d)", api_field):
        return f"{'Id' if m.group(1) == 'id' else 'Name'} of the team's league or cup number {m.group(2)}."
    if api_field in GENERIC:
        return GENERIC[api_field]
    UNDESCRIBED.add(api_field)
    return docs.get(schema, {}).get(api_field, "")


def load(path: Path) -> Any:
    text = path.read_text() if path.exists() else ""
    if not text.strip():
        return None
    try:
        return json.loads(text)
    except ValueError:
        return None


def records(data: Any) -> list[dict[str, Any]]:
    if isinstance(data, dict):
        for v in data.values():
            if isinstance(v, list):
                return v
    return []


def trim(value: Any) -> Any:
    if isinstance(value, str) and len(value) > 160:
        return value[:157] + "…"
    return value


def example(data: Any, key: str, limit: int = 2) -> Any:
    if not isinstance(data, dict) or not isinstance(data.get(key), list):
        return data
    return {key: [{k: trim(v) for k, v in r.items()} for r in data[key][:limit]]}


def build_schemas(endpoints: list[E], dirs: list[str], docs: dict[str, dict[str, str]]) -> dict[str, Any]:
    seen: dict[str, dict[str, Any]] = {}  # schema -> field -> first non-null value
    ints: dict[str, set[str]] = {}
    for e in endpoints:
        for d in dirs:
            for r in records(load(FIXTURES / d / f"{e.fixture}.json")):
                fields = seen.setdefault(e.schema, {})
                for k, v in r.items():
                    if fields.get(k) in (None, "") and v not in (None, ""):
                        fields[k] = v
                    else:
                        fields.setdefault(k, v)
                    if isinstance(v, int) and not isinstance(v, bool):
                        ints.setdefault(e.schema, set()).add(k)
    out: dict[str, Any] = {}
    for schema, fields in sorted(seen.items()):
        props: dict[str, Any] = {}
        for name in sorted(fields, key=str.lower):
            prop: dict[str, Any] = {"type": ["string", "integer", "null"] if name in ints.get(schema, set()) else ["string", "null"]}
            if d := describe(schema, name, docs):
                prop["description"] = d
            if fields[name] not in (None, ""):
                prop["examples"] = [trim(fields[name])]
            props[name] = prop
        out[schema] = {"type": "object", "description": f"{schema} record. Every value is a string or null; "
                       "`\"\"` also means no value.", "properties": props}
    return out


def counts(e: E, version: str) -> str:
    def n(d: str) -> str:
        data = load(FIXTURES / d / f"{e.fixture}.json")
        if data is None:
            p = FIXTURES / d / f"{e.fixture}.json"
            return "HTTP 404" if p.exists() and "<html" in p.read_text()[:300].lower() else "—"
        v = data.get(e.key) if isinstance(data, dict) else None
        return str(len(v)) if isinstance(v, list) else "none"
    if version == "v1":
        return f"Records measured {MEASURED}: free key **{n('v1-free')}**, premium key **{n('v1-premium')}**."
    return f"Records measured {MEASURED}: **{n('v2')}**."


def operation(e: E, version: str) -> dict[str, Any]:
    lines = [e.notes] if e.notes else []
    if e.undocumented:
        lines.append("**Not in the official documentation.**" + (" Answers the free keys; **premium keys get HTTP 404**."
                                                               if e.free_only else ""))
    lines.append(counts(e, version))
    if e.documented:
        lines.append(f"Documented limit: {e.documented}" + (" (free / premium)." if "/" in e.documented else "."))
    params: list[dict[str, Any]] = [{"$ref": "#/components/parameters/apiKey"}] if version == "v1" else []
    for p in e.params:
        schema: dict[str, Any] = {"type": p.type}
        if p.fmt:
            schema["format"] = p.fmt
        params.append({"name": p.name, "in": "path" if f"{{{p.name}}}" in e.path else "query",
                       "required": True if f"{{{p.name}}}" in e.path else p.required,
                       "description": p.description, "schema": schema, "example": p.example})
    data = load(FIXTURES / ("v1-premium" if version == "v1" else "v2") / f"{e.fixture}.json") \
        or load(FIXTURES / ("v1-free" if version == "v1" else "v2") / f"{e.fixture}.json")
    items: dict[str, Any] = {"type": "array", "items": {"$ref": f"#/components/schemas/{e.schema}"}}
    if e.single:
        items["maxItems"] = 1
    alternatives: list[dict[str, Any]] = [items, {"type": "null", "description": "No results."}]
    if version == "v1":
        alternatives.append({"type": "string", "description": "A rejected parameter, e.g. `Invalid League ID passed`."})
    envelope = {"type": "object", "properties": {e.key: {"oneOf": alternatives}}}
    if version == "v2":
        envelope = {"oneOf": [envelope, {"$ref": "#/components/schemas/NoData"}]}
    content: dict[str, Any] = {"schema": envelope}
    if data is not None:
        content["example"] = example(data, e.key)
    responses: dict[str, Any] = {
        "200": {"description": "The records under `" + e.key + "`." + (" An empty body also means no results." if version == "v1"
                                                                        else ""), "content": {"application/json": content}},
        "400": {"$ref": "#/components/responses/InvalidKey"},
        "429": {"$ref": "#/components/responses/RateLimited"},
    }
    if e.free_only:
        responses["404"] = {"description": "Returned to premium keys (an HTML page)."}
    op: dict[str, Any] = {"operationId": e.op, "tags": [e.tag], "summary": e.summary, "description": "\n\n".join(lines),
                          "parameters": params, "responses": responses}
    if e.undocumented:
        op["x-undocumented"] = True
    if version == "v1":
        op["security"] = []  # the key is the apiKey path parameter, not a security scheme
    return op


def spec(version: str, endpoints: list[E]) -> dict[str, Any]:
    docs = field_docs()
    dirs = ["v1-premium", "v1-free"] if version == "v1" else ["v2"]
    paths = {}
    for e in endpoints:
        path = f"/api/v1/json/{{apiKey}}/{e.path}" if version == "v1" else f"/api/v2/json/{e.path}"
        paths[path] = {"get": operation(e, version)}
    schemas = build_schemas(endpoints, dirs, docs)
    if version == "v2":
        schemas["NoData"] = {"type": "object", "description": "v2's \"no results\" answer (HTTP 200).",
                             "properties": {"Message": {"type": "string", "const": "No data found"}}, "required": ["Message"]}
    schemas["Message"] = {"type": "object", "properties": {"Message": {"type": "string", "description": str(GLOSSARY["Message"])}},
                          "example": {"Message": "Invalid Premium API key: Signup here: https://www.thesportsdb.com/pricing"}}
    components: dict[str, Any] = {
        "schemas": schemas,
        "responses": {
            "InvalidKey": {"description": "The key isn't accepted" + (" (only `123`, `3` and paid keys work)." if version == "v1"
                                                                       else " (v2 needs a premium key)."),
                           "content": {"application/json": {"schema": {"$ref": "#/components/schemas/Message"}}}},
            "RateLimited": {"description": "Over the rate limit (30 a minute free, 100 premium, 120 business). Wait a minute."},
        },
    }
    if version == "v1":
        components["parameters"] = {"apiKey": {
            "name": "apiKey", "in": "path", "required": True, "schema": {"type": "string"}, "example": "123",
            "description": "Your API key. `123` is the free key for development and testing (`3` also works, with the same "
                           "limits). The key is in the URL, so treat v1 URLs as secrets and don't log them."}}
        security: list[Any] = []
    else:
        components["securitySchemes"] = {"apiKey": {"type": "apiKey", "in": "header", "name": "X-API-KEY",
                                                     "description": "A premium key. Free keys get HTTP 400."}}
        security = [{"apiKey": []}]
    intro = (ROOT / "docs-site" / "openapi" / f"intro-{version}.md").read_text()
    tags = []
    for e in endpoints:
        if e.tag not in [t["name"] for t in tags]:
            tags.append({"name": e.tag, "description": TAGS[e.tag]})
    result: dict[str, Any] = {
        "openapi": "3.1.0",
        "info": {"title": "TheSportsDB API", "version": f"{version} · measured {MEASURED}", "description": intro,
                 "termsOfService": "https://www.thesportsdb.com/docs_terms_of_use.php"},
        "servers": [{"url": "https://www.thesportsdb.com"}],
        "tags": tags,
        "paths": paths,
        "components": components,
    }
    if security:
        result["security"] = security
    return result


class Dumper(yaml.SafeDumper):
    pass


def _str(dumper: yaml.SafeDumper, value: str) -> yaml.Node:
    style = "|" if "\n" in value else None
    return dumper.represent_scalar("tag:yaml.org,2002:str", value, style=style)


Dumper.add_representer(str, _str)

if __name__ == "__main__":
    for version, endpoints in (("v1", V1), ("v2", V2)):
        path = OUT / f"{version}.yaml"
        header = f"# GENERATED by docs-site/tools/build_openapi.py from the recorded responses. Do not edit.\n"
        path.write_text(header + yaml.dump(spec(version, endpoints), Dumper=Dumper, sort_keys=False, allow_unicode=True, width=120))
        print(f"wrote {path.relative_to(ROOT)} ({len(endpoints)} endpoints)")
    if UNDESCRIBED:
        raise SystemExit("fields missing from docs-site/fields.yaml: " + ", ".join(sorted(UNDESCRIBED)))
