"""Blocking primitives. Hand-written twin of ``_async/_compat.py`` (not generated)."""

from __future__ import annotations

import threading
import time
from collections.abc import Callable, Sequence
from typing import TypeVar

T = TypeVar("T")

Lock = threading.Lock
Event = threading.Event


def now() -> float:
    return time.monotonic()


def sleep(seconds: float) -> None:
    time.sleep(seconds)


def gather(calls: Sequence[Callable[[], T]]) -> list[T]:
    """Runs the calls one after another (the rate limiter would serialise them anyway)."""
    return [call() for call in calls]
