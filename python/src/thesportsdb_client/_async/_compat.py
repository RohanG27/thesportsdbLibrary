"""Async primitives. Hand-written; ``_sync/_compat.py`` is its blocking twin (not generated).

anyio (an httpx dependency) keeps the async client working on both asyncio and trio.
"""

from __future__ import annotations

import time
from collections.abc import Awaitable, Callable, Sequence
from typing import TypeVar

import anyio

T = TypeVar("T")

Lock = anyio.Lock
Event = anyio.Event


def now() -> float:
    return time.monotonic()


async def sleep(seconds: float) -> None:
    await anyio.sleep(seconds)


async def gather(calls: Sequence[Callable[[], Awaitable[T]]]) -> list[T]:
    """Runs the calls concurrently; results in call order. The first failure cancels the rest."""
    results: list[T | None] = [None] * len(calls)

    async def run(i: int, call: Callable[[], Awaitable[T]]) -> None:
        results[i] = await call()

    async with anyio.create_task_group() as tg:
        for i, call in enumerate(calls):
            tg.start_soon(run, i, call)
    return results  # type: ignore[return-value]
