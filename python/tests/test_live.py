"""Calls the real API. Excluded by default; run with ``pytest -m live``.
Uses the free key unless THESPORTSDB_API_KEY is set (then v2 too)."""

from __future__ import annotations

import os
from datetime import date
from zoneinfo import ZoneInfo

import pytest

from sportsdb import FREE_API_KEY, AsyncSportsDB, SportsDB

pytestmark = pytest.mark.live
KEY = os.environ.get("THESPORTSDB_API_KEY", "").strip() or FREE_API_KEY


@pytest.fixture(scope="module")
def db() -> SportsDB:
    return SportsDB(KEY)


def test_lookup_team(db: SportsDB) -> None:
    team = db.v1.lookup.team(133604)
    assert team is not None and team.name == "Arsenal" and team.leagues


def test_names_with_spaces(db: SportsDB) -> None:
    assert db.v1.search.teams("Toronto Maple Leafs")[0].name == "Toronto Maple Leafs"


def test_empty_day(db: SportsDB) -> None:
    assert db.v1.schedule.day(date(2026, 10, 4), league_id=4328) == []


def test_premium_detection(db: SportsDB) -> None:
    assert db.is_premium_key() == (KEY != FREE_API_KEY)


def test_helpers(db: SportsDB) -> None:
    h = db.helpers
    season = h.current_season(4328)
    assert season
    events = h.season_events(4328, season)
    assert events
    tz = ZoneInfo("America/Toronto")
    local = h.events_on_local_date(date(2026, 10, 4), tz, sport="Soccer")
    assert all(e.timestamp is None or e.timestamp.astimezone(tz).date() == date(2026, 10, 4) for e in local)


def test_v2(db: SportsDB) -> None:
    if KEY == FREE_API_KEY:
        pytest.skip("set THESPORTSDB_API_KEY to test v2")
    league = db.v2.lookup.league(4328)
    assert league is not None and league.current_season
    assert len(db.v2.schedule.league_season(4328, league.current_season)) > 100


@pytest.mark.anyio
async def test_async_client() -> None:
    async with AsyncSportsDB(KEY) as adb:
        team = await adb.v1.lookup.team(133604)
        assert team is not None and team.name == "Arsenal"


@pytest.fixture
def anyio_backend() -> str:
    return "asyncio"
