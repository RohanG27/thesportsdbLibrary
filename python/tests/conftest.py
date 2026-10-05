from __future__ import annotations

import json
from collections.abc import Callable, Mapping
from pathlib import Path
from typing import Any

import pytest

import thesportsdb_client
from thesportsdb_client import Response

FIXTURES = Path(__file__).parent / "fixtures"
PREMIUM = "9999999999"  # a stand-in; fixtures never contain a real key


def fixture(path: str) -> str:
    return (FIXTURES / path).read_text()


def fixture_records(path: str, key: str) -> list[dict[str, Any]]:
    data: list[dict[str, Any]] = json.loads(fixture(path))[key]
    return data


class FakeTransport:
    """Records requests and answers from a queue, then from ``default``."""

    def __init__(self, default: Callable[[], Response] | None = None) -> None:
        self.default = default or (lambda: Response(200, "{}"))
        self.queue: list[Callable[[], Response]] = []
        self.requests: list[tuple[str, Mapping[str, str]]] = []

    def respond(self, body: str, status: int = 200, headers: Mapping[str, str] | None = None) -> FakeTransport:
        self.queue.append(lambda: Response(status, body, dict(headers or {})))
        return self

    def fail(self, error: OSError) -> FakeTransport:
        def raise_() -> Response:
            raise error
        self.queue.append(raise_)
        return self

    @property
    def last_url(self) -> str:
        return self.requests[-1][0]

    def get(self, url: str, headers: Mapping[str, str]) -> Response:
        self.requests.append((url, headers))
        return (self.queue.pop(0) if self.queue else self.default)()

    def close(self) -> None:
        pass


class Routes:
    """Answers by URL: the first route whose key is in the URL wins (a fixture path or inline JSON)."""

    def __init__(self, *routes: tuple[str, str]) -> None:
        self.routes = routes
        self.calls: list[str] = []

    def _answer(self, url: str) -> Response:
        call = url.split("/json/", 1)[1]
        self.calls.append(call)
        body = next((b for k, b in self.routes if k in call), "{}")
        return Response(200, body if body.startswith("{") else fixture(body))

    def get(self, url: str, headers: Mapping[str, str]) -> Response:
        return self._answer(url)

    def close(self) -> None:
        pass


class AsyncRoutes(Routes):
    async def get(self, url: str, headers: Mapping[str, str]) -> Response:  # type: ignore[override]
        return self._answer(url)

    async def aclose(self) -> None:
        pass


def client(transport: Any, key: str = "123", **options: Any) -> thesportsdb_client.SportsDB:
    options.setdefault("requests_per_minute", 0)
    options.setdefault("retry_backoff", 0)
    return thesportsdb_client.SportsDB(key, transport=transport, **options)


@pytest.fixture
def no_sleep(monkeypatch: pytest.MonkeyPatch) -> list[float]:
    """Replaces the blocking client's sleep and clock with a virtual clock; returns the sleeps."""
    from thesportsdb_client._sync import _compat

    clock = [0.0]
    sleeps: list[float] = []

    def sleep(s: float) -> None:
        sleeps.append(s)
        clock[0] += s

    monkeypatch.setattr(_compat, "sleep", sleep)
    monkeypatch.setattr(_compat, "now", lambda: clock[0])
    return sleeps


@pytest.fixture
def anyio_backend() -> str:
    return "asyncio"
