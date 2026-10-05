"""Helpers route to v2 for premium keys and v1 for free keys (blocking client)."""

from __future__ import annotations

from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo

import pytest

from thesportsdb_client import SportsDB

from .conftest import PREMIUM, Routes, fixture_records


def helpers(t: Routes, premium: bool):  # type: ignore[no-untyped-def]
    return SportsDB(PREMIUM if premium else "123", transport=t, requests_per_minute=0).helpers


def test_current_season_and_season_events() -> None:
    p = Routes(("lookup/league/4328", "v2/lookup_league.json"), ("schedule/league/4328/2026-2027", "v2/schedule_league_season.json"))
    events = helpers(p, True).season_events(4328)
    assert p.calls == ["lookup/league/4328", "schedule/league/4328/2026-2027"]
    assert len(events) == 380 and events == sorted(events, key=lambda e: e.timestamp)  # type: ignore[arg-type, return-value]
    f = Routes(("lookupleague.php", "v1-free/lookup_league.json"), ("eventsseason.php", "v1-free/events_season.json"))
    assert len(helpers(f, False).season_events(4328)) == 5


def test_round_events_take_different_routes() -> None:
    f = Routes(("eventsround.php", "v1-free/events_round.json"))
    assert len(helpers(f, False).round_events(4328, 1, "2026-2027")) == 10
    assert f.calls == ["123/eventsround.php?id=4328&r=1&s=2026-2027"]
    p = Routes(("schedule/league/4328/2026-2027", "v2/schedule_league_season.json"))
    expected = sum(1 for e in fixture_records("v2/schedule_league_season.json", "schedule") if e["intRound"] == "1")
    assert len(helpers(p, True).round_events(4328, 1, "2026-2027")) == expected
    assert p.calls == ["schedule/league/4328/2026-2027"], "premium must not call eventsround.php (it 404s)"


def test_key_three_is_free() -> None:
    t = Routes(("eventsround.php", "v1-free/events_round.json"))
    SportsDB("3", transport=t, requests_per_minute=0).helpers.round_events(4328, 1, "2026-2027")
    assert t.calls == ["3/eventsround.php?id=4328&r=1&s=2026-2027"]


def test_upcoming_league_events() -> None:
    start = date(2026, 10, 17)
    p = Routes(("lookup/league/4328", "v2/lookup_league.json"), ("schedule/league/", "v2/schedule_league_season.json"))
    upcoming = helpers(p, True).upcoming_league_events(4328, days=7, start=start)
    expected = sum(1 for e in fixture_records("v2/schedule_league_season.json", "schedule")
                   if start <= date.fromisoformat(e["dateEvent"]) < start + timedelta(days=7))
    assert expected > 0 and len(upcoming) == expected
    f = Routes(("eventsday.php", "v1-free/events_day.json"))
    helpers(f, False).upcoming_league_events(4328, days=3, start=date(2026, 10, 4))
    assert sorted(c.rsplit("/", 1)[1] for c in f.calls) == [f"eventsday.php?d=2026-10-0{d}&l=4328" for d in (4, 5, 6)]


def test_team_schedule() -> None:
    f = Routes(("eventsnext.php", "v1-free/events_next.json"), ("eventslast.php", "v1-free/events_last.json"))
    s = helpers(f, False).team_schedule(133602)
    assert len(f.calls) == 2 and len({e.id for e in s}) == len(s)
    assert len(helpers(Routes(("schedule/full/team/133604", "v2/schedule_full_team.json")), True).team_schedule(133604)) == 48


def test_events_on_local_date_spans_two_utc_days() -> None:
    t = Routes(("d=2026-10-04", "v1-premium/events_day.json"), ("d=2026-10-05", '{"events":null}'))
    tz, day = ZoneInfo("America/Toronto"), date(2026, 10, 4)
    events = helpers(t, True).events_on_local_date(day, tz)
    assert sorted(c.rsplit("/", 1)[1] for c in t.calls) == ["eventsday.php?d=2026-10-04", "eventsday.php?d=2026-10-05"]
    start = datetime(2026, 10, 4, tzinfo=tz).astimezone(timezone.utc)
    end = datetime(2026, 10, 5, tzinfo=tz).astimezone(timezone.utc)
    expected = sum(1 for e in fixture_records("v1-premium/events_day.json", "events")
                   if start <= datetime.fromisoformat(e["strTimestamp"]).replace(tzinfo=timezone.utc) < end)
    assert 0 < expected < 901 and len(events) == expected
    assert all(start <= e.timestamp < end for e in events if e.timestamp)  # type: ignore[operator]


def test_events_on_local_date_ahead_of_utc() -> None:
    t = Routes()
    helpers(t, False).events_on_local_date(date(2026, 10, 4), ZoneInfo("Asia/Tokyo"), sport="Soccer")
    assert sorted(c.rsplit("/", 1)[1] for c in t.calls) == [
        "eventsday.php?d=2026-10-03&s=Soccer",
        "eventsday.php?d=2026-10-04&s=Soccer",
    ]


def test_live_scores() -> None:
    league = fixture_records("v1-free/livescore_soccer.json", "livescore")[0]["idLeague"]
    f = Routes(("lookupleague.php", f'{{"leagues":[{{"idLeague":"{league}","strSport":"Soccer"}}]}}'),
               ("livescore.php?s=Soccer", "v1-free/livescore_soccer.json"))
    scores = helpers(f, False).live_scores(league_id=int(league))
    assert scores and all(s.league_id == int(league) for s in scores)
    with pytest.raises(ValueError):
        helpers(Routes(), False).live_scores()
    assert len(helpers(Routes(("livescore/all", "v2/livescore_all.json")), True).live_scores()) == 54


def test_league_teams_and_channels() -> None:
    f = Routes(("lookupleague.php", "v1-free/lookup_league.json"), ("search_all_teams.php", "v1-free/search_all_teams_league.json"))
    assert len(helpers(f, False).league_teams(4328)) == 10
    assert f.calls[-1].endswith("search_all_teams.php?l=English%20Premier%20League")
    assert len(helpers(Routes(("list/teams/4328", "v2/list_teams.json")), True).league_teams(4328)) == 20
    assert len(helpers(Routes(("lookup/event_tv/2494052", "v2/lookup_event_tv.json")), True).event_channels(2494052)) == 13
    assert len(helpers(Routes(("lookuptv.php", "v1-free/lookup_tv.json")), False).event_channels(2494052)) == 2


def test_tv_listings() -> None:
    start = date(2026, 10, 5)
    p = Routes(("filter/tv/country/Canada", "v2/filter_tv_country.json"))
    hockey = helpers(p, True).tv_listings("Canada", sport="ice hockey", days=2, start=start)
    assert len(p.calls) == 1 and hockey
    assert all(t.sport == "Ice Hockey" and t.date in (start, start + timedelta(days=1)) for t in hockey)
    with pytest.raises(ValueError):
        helpers(Routes(), False).tv_listings("Canada")
    f = Routes(("eventstv.php", "v1-free/events_tv_country.json"))
    helpers(f, False).tv_listings("Canada", sport="Ice Hockey", days=2, start=start)
    assert len(f.calls) == 2
