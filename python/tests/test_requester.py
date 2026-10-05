"""Retries, rate limits, errors, redaction, caching and the request listener (blocking client)."""

from __future__ import annotations

import pytest

from sportsdb import (
    HTTPStatusError,
    InMemoryResponseCache,
    InvalidApiKeyError,
    NetworkError,
    RateLimitError,
    RequestEvent,
    Response,
    SportsDB,
)

from .conftest import FakeTransport, client, fixture

PAID = "5550001234"
OK = '{"sports":[{"idSport":"102","strSport":"Soccer"}]}'


def test_retries_server_errors_then_succeeds(no_sleep: list[float]) -> None:
    t = FakeTransport().respond("oops", 503).respond("oops", 502).respond(OK)
    assert len(client(t, retry_backoff=0.5).v1.list.sports()) == 1
    assert len(t.requests) == 3 and no_sleep == [0.5, 1.0]


def test_gives_up_after_max_retries(no_sleep: list[float]) -> None:
    t = FakeTransport(lambda: Response(500, "down"))
    with pytest.raises(HTTPStatusError) as e:
        client(t).v1.list.sports()
    assert e.value.status == 500 and len(t.requests) == 3


def test_network_errors_are_retried_and_wrapped(no_sleep: list[float]) -> None:
    assert len(client(FakeTransport().fail(ConnectionError("reset")).respond(OK)).v1.list.sports()) == 1
    down = FakeTransport()
    down.default = lambda: (_ for _ in ()).throw(ConnectionError("no route"))
    with pytest.raises(NetworkError):
        client(down).v1.list.sports()


def test_rate_limit_waits_for_retry_after_then_retries_once(no_sleep: list[float]) -> None:
    t = FakeTransport().respond("", 429, {"Retry-After": "7"}).respond(OK)
    client(t).v1.list.sports()
    assert no_sleep == [7.0] and len(t.requests) == 2

    always = FakeTransport(lambda: Response(429, ""))
    with pytest.raises(RateLimitError) as e:
        client(always, rate_limit_wait=60).v1.list.sports()
    assert len(always.requests) == 2 and e.value.retry_after is None


def test_rate_limit_retry_can_be_disabled(no_sleep: list[float]) -> None:
    t = FakeTransport().respond("", 429, {"Retry-After": "30"})
    with pytest.raises(RateLimitError) as e:
        client(t, retry_on_rate_limit=False).v1.list.sports()
    assert e.value.retry_after == 30 and len(t.requests) == 1


def test_client_side_rate_limit(no_sleep: list[float]) -> None:
    c = SportsDB(transport=FakeTransport(lambda: Response(200, OK)), requests_per_minute=3)
    for _ in range(4):
        c.v1.list.sports()
    assert no_sleep == [60.0]  # the 4th call waited for the window


def test_default_rate_limit_follows_the_key() -> None:
    assert SportsDB("123").config.effective_requests_per_minute == 30
    assert SportsDB("3").config.effective_requests_per_minute == 30
    assert SportsDB(PAID).config.effective_requests_per_minute == 100


def test_v1_key_never_appears_in_errors(no_sleep: list[float]) -> None:
    for transport, error in [
        (FakeTransport(lambda: Response(404, "nope")), HTTPStatusError),
        (FakeTransport().respond(fixture("v1-free/invalid_key.json"), 400), InvalidApiKeyError),
        (FakeTransport().fail(ConnectionError("boom")).fail(ConnectionError("boom")).fail(ConnectionError("boom")), NetworkError),
    ]:
        with pytest.raises(error) as e:
            client(transport, PAID).v1.lookup.team(1)
        assert PAID not in str(e.value)
    with pytest.raises(HTTPStatusError) as e:
        client(FakeTransport(lambda: Response(404, "nope")), PAID).v1.lookup.team(1)
    assert "/api/v1/json/***/lookupteam.php?id=1" in str(e.value)


def test_user_agent_is_sent() -> None:
    t = FakeTransport().respond(OK)
    client(t).v1.list.sports()
    assert t.requests[0][1]["User-Agent"] == "sportsdb-python"


def test_cache_serves_repeats_and_keeps_keys_out() -> None:
    cache = InMemoryResponseCache()
    t = FakeTransport().respond(OK)
    c = client(t, PAID, cache=cache)
    c.v1.list.sports()
    c.v1.list.sports()
    assert len(t.requests) == 1 and len(cache) == 1
    assert all(PAID not in k for k in cache._entries)


def test_live_scores_and_empty_bodies_are_not_cached() -> None:
    body = '{"livescore":[{"idLiveScore":"1"}]}'
    t = FakeTransport().respond(body).respond(body).respond("").respond(OK)
    c = client(t, cache=InMemoryResponseCache())
    c.v1.live.sport("Soccer")
    c.v1.live.sport("Soccer")
    assert len(t.requests) == 2
    assert c.v1.list.sports() == []
    assert len(c.v1.list.sports()) == 1


def test_listener_sees_retries_cache_hits_and_errors(no_sleep: list[float]) -> None:
    events: list[RequestEvent] = []
    t = FakeTransport().respond("busy", 503).respond(OK).respond("gone", 404)
    c = client(t, cache=InMemoryResponseCache(), request_listener=events.append)
    c.v1.list.sports()
    c.v1.list.sports()
    with pytest.raises(HTTPStatusError):
        c.v1.lookup.team(1)
    retried, cached, failed = events
    assert (retried.attempts, retried.status, retried.error) == (2, 200, None)
    assert cached.from_cache and cached.attempts == 0
    assert failed.status == 404 and isinstance(failed.error, HTTPStatusError)
    assert failed.url.endswith("/api/v1/json/***/lookupteam.php?id=1")


def test_a_failing_listener_does_not_break_calls() -> None:
    def broken(_: RequestEvent) -> None:
        raise RuntimeError("listener bug")

    assert len(client(FakeTransport().respond(OK), request_listener=broken).v1.list.sports()) == 1
