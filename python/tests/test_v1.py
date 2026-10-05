"""Every v1 method against a real recorded response: the URL it builds, the record key it reads, the fields it parses."""

from __future__ import annotations

from collections.abc import Callable
from datetime import date, datetime, time, timezone
from typing import Any

import pytest

from thesportsdb_client import ApiMessageError, EventStatus, SportsDB

from .conftest import FakeTransport, client, fixture

D = date(2026, 10, 4)

# (fixture, expected call after the key, call, minimum records)
CASES: list[tuple[str, str, Callable[[SportsDB], list[Any]], int]] = [
    ("search_teams", "searchteams.php?t=Arsenal", lambda c: c.v1.search.teams("Arsenal"), 1),
    ("search_events", "searchevents.php?e=Arsenal%20vs%20Chelsea", lambda c: c.v1.search.events("Arsenal vs Chelsea"), 1),
    ("search_events_season", "searchevents.php?e=Arsenal_vs_Chelsea&s=2016-2017",
     lambda c: c.v1.search.events("Arsenal_vs_Chelsea", season="2016-2017"), 1),
    ("search_events_date", "searchevents.php?e=Arsenal%20vs%20Chelsea&d=2015-04-26",
     lambda c: c.v1.search.events("Arsenal vs Chelsea", date=date(2015, 4, 26)), 1),
    ("search_filename", "searchfilename.php?e=English%20Premier%20League%202015-04-26%20Arsenal%20vs%20Chelsea",
     lambda c: c.v1.search.events_by_filename("English Premier League 2015-04-26 Arsenal vs Chelsea"), 1),
    ("search_players", "searchplayers.php?p=Danny%20Welbeck", lambda c: c.v1.search.players("Danny Welbeck"), 1),
    ("search_venues", "searchvenues.php?v=Wembley", lambda c: c.v1.search.venues("Wembley"), 1),
    ("lookup_table", "lookuptable.php?l=4328", lambda c: c.v1.lookup.table(4328), 5),
    ("lookup_table_season", "lookuptable.php?l=4328&s=2024-2025", lambda c: c.v1.lookup.table(4328, "2024-2025"), 5),
    ("lookup_equipment", "lookupequipment.php?id=133597", lambda c: c.v1.lookup.equipment(133597), 1),
    ("lookup_honours", "lookuphonours.php?id=34147178", lambda c: c.v1.lookup.honours(34147178), 1),
    ("lookup_former_teams", "lookupformerteams.php?id=34147178", lambda c: c.v1.lookup.former_teams(34147178), 1),
    ("lookup_milestones", "lookupmilestones.php?id=34161397", lambda c: c.v1.lookup.milestones(34161397), 1),
    ("lookup_contracts", "lookupcontracts.php?id=34147178", lambda c: c.v1.lookup.contracts(34147178), 1),
    ("player_results", "playerresults.php?id=34160573", lambda c: c.v1.lookup.player_results(34160573), 1),
    ("lookup_player_stats", "lookupplayerstats.php?id=34146304", lambda c: c.v1.lookup.player_stats(34146304), 1),
    ("event_results", "eventresults.php?id=652890", lambda c: c.v1.lookup.event_results(652890), 1),
    ("lookup_lineup", "lookuplineup.php?id=1032723", lambda c: c.v1.lookup.lineup(1032723), 1),
    ("lookup_timeline", "lookuptimeline.php?id=1032718", lambda c: c.v1.lookup.timeline(1032718), 1),
    ("lookup_event_stats", "lookupeventstats.php?id=1032723", lambda c: c.v1.lookup.event_stats(1032723), 1),
    ("lookup_tv", "lookuptv.php?id=2494052", lambda c: c.v1.lookup.event_tv(2494052), 1),
    ("all_sports", "all_sports.php", lambda c: c.v1.list.sports(), 2),
    ("all_countries", "all_countries.php", lambda c: c.v1.list.countries(), 50),
    ("all_leagues", "all_leagues.php", lambda c: c.v1.list.leagues(), 1),
    ("search_all_leagues", "search_all_leagues.php?c=England&s=Soccer",
     lambda c: c.v1.list.leagues_in_country("England", "Soccer"), 1),
    ("search_all_seasons", "search_all_seasons.php?id=4328", lambda c: c.v1.list.seasons(4328), 1),
    ("search_all_seasons_poster", "search_all_seasons.php?id=4328&poster=1", lambda c: c.v1.list.seasons(4328, posters=True), 1),
    ("search_all_seasons_badge", "search_all_seasons.php?id=4328&badge=1", lambda c: c.v1.list.seasons(4328, badges=True), 1),
    ("search_all_seasons_description", "search_all_seasons.php?id=4328&description=1",
     lambda c: c.v1.list.seasons(4328, descriptions=True), 1),
    ("search_all_teams_league", "search_all_teams.php?l=English%20Premier%20League",
     lambda c: c.v1.list.teams_in_league("English Premier League"), 10),
    ("search_all_teams_country", "search_all_teams.php?s=Soccer&c=Spain", lambda c: c.v1.list.teams_in_country("Soccer", "Spain"), 10),
    ("lookup_all_players", "lookup_all_players.php?id=133604", lambda c: c.v1.list.players(133604), 10),
    ("events_next", "eventsnext.php?id=133602", lambda c: c.v1.schedule.team_next(133602), 1),
    ("events_last", "eventslast.php?id=133602", lambda c: c.v1.schedule.team_last(133602), 1),
    ("events_next_league", "eventsnextleague.php?id=4328", lambda c: c.v1.schedule.league_next(4328), 1),
    ("events_past_league", "eventspastleague.php?id=4328", lambda c: c.v1.schedule.league_past(4328), 1),
    ("events_day", "eventsday.php?d=2026-10-04", lambda c: c.v1.schedule.day(D), 3),
    ("events_day_sport", "eventsday.php?d=2026-10-04&s=Ice%20Hockey", lambda c: c.v1.schedule.day(D, sport="Ice Hockey"), 3),
    ("events_season", "eventsseason.php?id=4328&s=2026-2027", lambda c: c.v1.schedule.season(4328, "2026-2027"), 5),
    ("events_round", "eventsround.php?id=4328&r=1&s=2026-2027", lambda c: c.v1.schedule.round(4328, 1, "2026-2027"), 10),
    ("events_tv_day", "eventstv.php?d=2026-10-05", lambda c: c.v1.tv.day(date(2026, 10, 5)), 1),
    ("events_tv_country", "eventstv.php?d=2026-10-05&a=Canada&s=Ice%20Hockey",
     lambda c: c.v1.tv.day(date(2026, 10, 5), sport="Ice Hockey", country="Canada"), 1),
    ("events_tv_channel", "eventstv.php?c=TSN%201", lambda c: c.v1.tv.channel("TSN 1"), 1),
    ("events_tv_channel_id", "eventstv.php?id=8631", lambda c: c.v1.tv.channel_id(8631), 1),
    ("events_highlights", "eventshighlights.php?d=2026-10-04", lambda c: c.v1.video.highlights(D), 1),
    ("events_highlights_sport", "eventshighlights.php?d=2026-10-04&s=Soccer", lambda c: c.v1.video.highlights(D, sport="Soccer"), 1),
    ("livescore_soccer", "livescore.php?s=Soccer", lambda c: c.v1.live.sport("Soccer"), 1),
]


@pytest.mark.parametrize("name,expected_call,call,minimum", CASES, ids=[c[0] for c in CASES])
def test_endpoint(name: str, expected_call: str, call: Callable[[SportsDB], list[Any]], minimum: int) -> None:
    t = FakeTransport().respond(fixture(f"v1-free/{name}.json"))
    result = call(client(t))
    assert t.last_url == f"https://www.thesportsdb.com/api/v1/json/123/{expected_call}"
    assert len(result) >= minimum


def test_team_fields() -> None:
    team = client(FakeTransport().respond(fixture("v1-free/lookup_team.json"))).v1.lookup.team(133604)
    assert team is not None
    assert (team.id, team.name, team.short_name) == (133604, "Arsenal", "ARS")
    assert "Arsenal FC" in team.alternate_names
    assert team.leagues[0].id == 4328 and len(team.leagues) > 1
    assert {"EN", "DE"} <= set(team.descriptions)
    assert team.colours[0] == "#EF0107"
    assert team.is_locked is False


def test_event_season_times_are_utc() -> None:
    events = client(FakeTransport().respond(fixture("v1-free/events_season.json"))).v1.schedule.season(4328, "2026-2027")
    first = events[0]
    assert first.date == date(2026, 8, 21)
    assert first.time == time(19, 0)
    assert first.timestamp == datetime(2026, 8, 21, 19, 0, tzinfo=timezone.utc)


def test_older_events_have_no_status() -> None:
    event = client(FakeTransport().respond(fixture("v1-free/lookup_event.json"))).v1.lookup.event(441613)
    assert event is not None
    assert event.status_code is None and event.status is EventStatus.UNKNOWN
    assert event.home_score == 4
    # The 2017 FA Cup final is recorded with intRound 200: the "final" stage code.
    from thesportsdb_client import RoundStage

    final = client(FakeTransport().respond(fixture("v1-free/search_events_season.json"))).v1.search.events(
        "Arsenal_vs_Chelsea", season="2016-2017")[0]
    assert (final.round, final.stage, final.league) == (200, RoundStage.FINAL, "FA Cup")


def test_league_player_venue_lookups() -> None:
    t = FakeTransport().respond(fixture("v1-free/lookup_league.json")).respond(fixture("v1-free/lookup_player.json")) \
        .respond(fixture("v1-free/lookup_venue.json"))
    c = client(t)
    league = c.v1.lookup.league(4328)
    assert league is not None and league.first_event_date == date(1992, 8, 15) and league.current_season
    player = c.v1.lookup.player(34145937)
    assert player is not None and player.born is not None and player.external_ids.wikidata
    venue = c.v1.lookup.venue(16163)
    assert venue is not None and venue.capacity == 90000 and venue.coordinates == (51.555556, -0.279444)


def test_tv_listing_timestamp_with_space() -> None:
    tv = client(FakeTransport().respond(fixture("v1-free/lookup_tv.json"))).v1.lookup.event_tv(2494052)
    assert all(t.timestamp is not None and t.timestamp.tzinfo is timezone.utc for t in tv)


def test_channel_id_means_channel() -> None:
    tv = client(FakeTransport().respond(fixture("v1-free/events_tv_channel_id.json"))).v1.tv.channel_id(8631)
    assert all(t.channel_id == 8631 for t in tv)


def test_empty_results() -> None:
    t = FakeTransport().respond(fixture("v1-free/events_day_none.json")).respond(fixture("v1-free/events_tv_country_no_sport.json")) \
        .respond(fixture("v1-free/events_highlights_league.json"))
    c = client(t)
    assert c.v1.schedule.day(D, league_id=4328) == []  # {"events":null}
    assert c.v1.tv.channel("x") == []  # empty body
    assert c.v1.video.highlights(D, league_id=4328) == []


def test_rejected_parameter_is_an_error() -> None:
    t = FakeTransport().respond(fixture("v1-free/search_all_seasons_bad_param.json"))
    with pytest.raises(ApiMessageError) as e:
        client(t).v1.list.seasons(4328)
    assert e.value.api_message == "Invalid League ID passed"


def test_argument_rules_are_checked_before_any_request() -> None:
    t = FakeTransport()
    c = client(t)
    with pytest.raises(ValueError):
        c.v1.tv.day(D, country="Canada")
    with pytest.raises(ValueError):
        c.v1.search.events("x", season="2020", date=D)
    with pytest.raises(ValueError):
        c.v1.list.seasons(1, badges=True, posters=True)
    assert t.requests == []


def test_premium_results_are_bigger() -> None:
    t = FakeTransport().respond(fixture("v1-premium/events_day.json")).respond(fixture("v1-premium/lookup_table.json"))
    c = client(t, key="9999999999")
    assert len(c.v1.schedule.day(D)) > 500
    assert [s.rank for s in c.v1.lookup.table(4328)] == list(range(1, 21))
