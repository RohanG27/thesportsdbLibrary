"""The async client: same behaviour, plus concurrency (de-duplication, cancellation, parallel helpers)."""

from __future__ import annotations

from collections.abc import Mapping
from datetime import date
from zoneinfo import ZoneInfo

import anyio
import pytest

from thesportsdb_client import AsyncSportsDB, HTTPStatusError, PremiumRequiredError, RequestEvent, Response

from .conftest import PREMIUM, AsyncRoutes, fixture

pytestmark = pytest.mark.anyio
SPORTS = '{"sports":[{"idSport":"102","strSport":"Soccer"}]}'


class Gated:
    """Holds every request until released."""

    def __init__(self, status: int = 200, body: str = SPORTS) -> None:
        self.gate = anyio.Event()
        self.urls: list[str] = []
        self.response = Response(status, body)

    async def get(self, url: str, headers: Mapping[str, str]) -> Response:
        self.urls.append(url)
        await self.gate.wait()
        return self.response

    async def aclose(self) -> None:
        pass


def make(transport: object, **options: object) -> AsyncSportsDB:
    return AsyncSportsDB(transport=transport, requests_per_minute=0, retry_backoff=0, **options)  # type: ignore[arg-type]


async def test_lookup_and_v2_rules() -> None:
    t = AsyncRoutes(("lookupteam.php", "v1-free/lookup_team.json"))
    async with make(t) as db:
        team = await db.v1.lookup.team(133604)
        assert team is not None and team.name == "Arsenal"
        with pytest.raises(PremiumRequiredError):
            await db.v2.all.sports()


async def test_identical_calls_share_one_request() -> None:
    t = Gated()
    events: list[RequestEvent] = []
    db = make(t, request_listener=events.append)
    results: list[object] = []

    async def call() -> None:
        results.append(await db.v1.list.sports())

    async with anyio.create_task_group() as tg:
        for _ in range(10):
            tg.start_soon(call)
        await anyio.wait_all_tasks_blocked()
        assert len(t.urls) == 1
        t.gate.set()
    assert all(r == results[0] for r in results)
    assert sum(1 for e in events if not e.shared and e.attempts == 1) == 1
    assert sum(1 for e in events if e.shared and e.attempts == 0 and e.status == 200) == 9


async def test_different_calls_are_not_merged_and_dedup_can_be_off() -> None:
    t = Gated()
    db = make(t)
    async with anyio.create_task_group() as tg:
        tg.start_soon(db.v1.list.sports)
        tg.start_soon(db.v1.lookup.team, 1)
        await anyio.wait_all_tasks_blocked()
        assert len(t.urls) == 2
        t.gate.set()
    t2 = Gated()
    db2 = make(t2, deduplicate_requests=False)
    async with anyio.create_task_group() as tg:
        for _ in range(3):
            tg.start_soon(db2.v1.list.sports)
        await anyio.wait_all_tasks_blocked()
        assert len(t2.urls) == 3
        t2.gate.set()


async def test_errors_reach_every_waiter() -> None:
    t = Gated(404, "nope")
    db = make(t)
    errors: list[BaseException] = []

    async def call() -> None:
        try:
            await db.v1.list.sports()
        except HTTPStatusError as e:
            errors.append(e)

    async with anyio.create_task_group() as tg:
        for _ in range(3):
            tg.start_soon(call)
        await anyio.wait_all_tasks_blocked()
        t.gate.set()
    assert len(t.urls) == 1 and len(errors) == 3


async def test_a_cancelled_first_caller_hands_over_to_a_waiter() -> None:
    t = Gated()
    db = make(t)
    result: list[object] = []

    async def second() -> None:
        result.append(await db.v1.list.sports())

    async with anyio.create_task_group() as outer:
        async with anyio.create_task_group() as first:
            first.start_soon(db.v1.list.sports)
            await anyio.wait_all_tasks_blocked()
            outer.start_soon(second)
            await anyio.wait_all_tasks_blocked()
            first.cancel_scope.cancel()
        await anyio.wait_all_tasks_blocked()
        assert len(t.urls) == 2  # the waiter made its own request instead of being cancelled
        t.gate.set()
    assert len(result) == 1


async def test_helpers_run_days_concurrently() -> None:
    t = AsyncRoutes(("d=2026-10-04", "v1-premium/events_day.json"), ("d=2026-10-05", '{"events":null}'))
    db = AsyncSportsDB(PREMIUM, transport=t, requests_per_minute=0)
    events = await db.helpers.events_on_local_date(date(2026, 10, 4), ZoneInfo("America/Toronto"))
    assert len(t.calls) == 2 and events


async def test_closing_leaves_a_passed_in_transport_open() -> None:
    closed: list[bool] = []

    class T(AsyncRoutes):
        async def aclose(self) -> None:
            closed.append(True)

    async with make(T()):
        pass
    assert closed == []


async def test_matches_the_blocking_client() -> None:
    from thesportsdb_client import SportsDB

    from .conftest import Routes

    sync = SportsDB(transport=Routes(("lookuptable.php", "v1-free/lookup_table.json")), requests_per_minute=0)
    async with make(AsyncRoutes(("lookuptable.php", "v1-free/lookup_table.json"))) as db:
        assert await db.v1.lookup.table(4328) == sync.v1.lookup.table(4328)
    assert fixture("v1-free/lookup_table.json")
