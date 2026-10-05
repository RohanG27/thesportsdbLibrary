"""Every v2 method against a real premium response, plus v2's key handling."""

from __future__ import annotations

from collections.abc import Callable
from datetime import date, datetime, timezone
from typing import Any

import pytest

from sportsdb import EventStatus, InvalidApiKeyError, PremiumRequiredError, SportsDB

from .conftest import PREMIUM, FakeTransport, client, fixture

CASES: list[tuple[str, str, Callable[[SportsDB], Any], int]] = [
    ("search_team", "search/team/Arsenal", lambda c: c.v2.search.teams("Arsenal"), 12),
    ("search_league", "search/league/English%20Premier%20League", lambda c: c.v2.search.leagues("English Premier League"), 2),
    ("search_player", "search/player/Danny%20Welbeck", lambda c: c.v2.search.players("Danny Welbeck"), 1),
    ("search_venue", "search/venue/Wembley", lambda c: c.v2.search.venues("Wembley"), 3),
    ("lookup_team_equipment", "lookup/team_equipment/133597", lambda c: c.v2.lookup.team_equipment(133597), 18),
    ("lookup_player_contracts", "lookup/player_contracts/34147178", lambda c: c.v2.lookup.player_contracts(34147178), 1),
    ("lookup_player_results", "lookup/player_results/34160573", lambda c: c.v2.lookup.player_results(34160573), 24),
    ("lookup_player_honours", "lookup/player_honours/34147178", lambda c: c.v2.lookup.player_honours(34147178), 5),
    ("lookup_player_milestones", "lookup/player_milestones/34161397", lambda c: c.v2.lookup.player_milestones(34161397), 3),
    ("lookup_player_teams", "lookup/player_teams/34147178", lambda c: c.v2.lookup.player_teams(34147178), 6),
    ("lookup_player_stats", "lookup/player_stats/34146304", lambda c: c.v2.lookup.player_stats(34146304), 315),
    ("lookup_event_lineup", "lookup/event_lineup/1032723", lambda c: c.v2.lookup.event_lineup(1032723), 22),
    ("lookup_event_results", "lookup/event_results/652890", lambda c: c.v2.lookup.event_results(652890), 22),
    ("lookup_event_stats", "lookup/event_stats/1032723", lambda c: c.v2.lookup.event_stats(1032723), 16),
    ("lookup_event_timeline", "lookup/event_timeline/1032718", lambda c: c.v2.lookup.event_timeline(1032718), 10),
    ("lookup_event_tv", "lookup/event_tv/2494052", lambda c: c.v2.lookup.event_tv(2494052), 13),
    ("lookup_event_highlights", "lookup/event_highlights/441613", lambda c: c.v2.lookup.event_highlights(441613), 1),
    ("list_teams", "list/teams/4328", lambda c: c.v2.list.teams(4328), 20),
    ("list_seasons", "list/seasons/4328", lambda c: c.v2.list.seasons(4328), 35),
    ("list_players", "list/players/133604", lambda c: c.v2.list.players(133604), 27),
    ("all_countries", "all/countries", lambda c: c.v2.all.countries(), 256),
    ("all_sports", "all/sports", lambda c: c.v2.all.sports(), 37),
    ("all_leagues", "all/leagues", lambda c: c.v2.all.leagues(), 1547),
    ("schedule_next_league", "schedule/next/league/4328", lambda c: c.v2.schedule.league_next(4328), 20),
    ("schedule_previous_league", "schedule/previous/league/4328", lambda c: c.v2.schedule.league_previous(4328), 20),
    ("schedule_next_team", "schedule/next/team/133604", lambda c: c.v2.schedule.team_next(133604), 10),
    ("schedule_previous_team", "schedule/previous/team/133604", lambda c: c.v2.schedule.team_previous(133604), 10),
    ("schedule_next_venue", "schedule/next/venue/16163", lambda c: c.v2.schedule.venue_next(16163), 3),
    ("schedule_previous_venue", "schedule/previous/venue/16163", lambda c: c.v2.schedule.venue_previous(16163), 10),
    ("schedule_full_team", "schedule/full/team/133604", lambda c: c.v2.schedule.team_full(133604), 48),
    ("schedule_league_season", "schedule/league/4328/2026-2027", lambda c: c.v2.schedule.league_season(4328, "2026-2027"), 380),
    ("filter_tv_day", "filter/tv/day/2026-10-05", lambda c: c.v2.tv.day(date(2026, 10, 5)), 287),
    ("filter_tv_country", "filter/tv/country/Canada", lambda c: c.v2.tv.country("Canada"), 90),
    ("filter_tv_sport", "filter/tv/sport/Ice%20Hockey", lambda c: c.v2.tv.sport("Ice Hockey"), 189),
    ("filter_tv_channel", "filter/tv/channel/TSN%201", lambda c: c.v2.tv.channel("TSN 1"), 3),
    ("filter_tv_channel_id", "filter/tv/channelid/8631", lambda c: c.v2.tv.channel_id(8631), 4),
    ("livescore_all", "livescore/all", lambda c: c.v2.live.all(), 54),
    ("livescore_soccer", "livescore/soccer", lambda c: c.v2.live.sport("soccer"), 17),
]


@pytest.mark.parametrize("name,path,call,count", CASES, ids=[c[0] for c in CASES])
def test_endpoint(name: str, path: str, call: Callable[[SportsDB], Any], count: int) -> None:
    t = FakeTransport().respond(fixture(f"v2/{name}.json"))
    result = call(client(t, PREMIUM))
    url, headers = t.requests[-1]
    assert url == f"https://www.thesportsdb.com/api/v2/json/{path}"
    assert headers["X-API-KEY"] == PREMIUM and PREMIUM not in url
    assert len(result) == count


@pytest.mark.parametrize("name,path,attr", [
    ("lookup_league", "lookup/league/4328", "league"), ("lookup_team", "lookup/team/133604", "team"),
    ("lookup_player", "lookup/player/34145937", "player"), ("lookup_event", "lookup/event/441613", "event"),
    ("lookup_venue", "lookup/venue/16163", "venue"),
])
def test_single_lookups(name: str, path: str, attr: str) -> None:
    t = FakeTransport().respond(fixture(f"v2/{name}.json"))
    found = getattr(client(t, PREMIUM).v2.lookup, attr)(int(path.rsplit("/", 1)[1]))
    assert found is not None and t.last_url.endswith(path)


def test_search_ids_arrive_as_json_numbers() -> None:
    teams = client(FakeTransport().respond(fixture("v2/search_team.json")), PREMIUM).v2.search.teams("Arsenal")
    assert teams[0].id == 133604 and teams[0].league_id == 4328
    assert teams[0].raw["idTeam"] == "133604"


def test_v2_specifics() -> None:
    t = FakeTransport().respond(fixture("v2/all_countries.json")).respond(fixture("v2/schedule_full_team.json")) \
        .respond(fixture("v2/livescore_all.json")).respond(fixture("v2/search_event_none.json")) \
        .respond(fixture("v2/livescore_league.json"))
    c = client(t, PREMIUM)
    andorra = next(x for x in c.v2.all.countries() if x.name == "Andorra")
    assert (andorra.code, andorra.name_fr, andorra.api_football_id) == ("AD", "Andorre", None)
    first = c.v2.schedule.team_full(133604)[0]
    assert first.timestamp == datetime(2027, 5, 30, 15, 0, tzinfo=timezone.utc) and first.status is EventStatus.NOT_STARTED
    live = c.v2.live.all()[0]
    assert (live.status_code, live.status, live.home_score) == ("P3", EventStatus.IN_PLAY, 2)
    assert c.v2.search.events("Arsenal vs Chelsea") == []  # {"Message":"No data found"}
    assert c.v2.live.league(4328) == []


@pytest.mark.parametrize("key", ["123", "3"])
def test_free_keys_never_call_v2(key: str) -> None:
    t = FakeTransport()
    c = client(t, key)
    with pytest.raises(PremiumRequiredError):
        c.v2.all.sports()
    assert t.requests == [] and c.is_premium_key() is False


def test_invalid_key_and_premium_detection() -> None:
    bad = FakeTransport().respond(fixture("v2/invalid_key.json"), 400)
    with pytest.raises(InvalidApiKeyError):
        client(bad, "1234567890").v2.lookup.league(4328)
    assert client(FakeTransport().respond(fixture("v2/lookup_league.json")), PREMIUM).is_premium_key()
    assert not client(FakeTransport().respond(fixture("v2/invalid_key.json"), 400), "1234567890").is_premium_key()
