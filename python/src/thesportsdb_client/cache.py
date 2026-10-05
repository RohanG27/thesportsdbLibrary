"""Optional response caching, with a time-to-live per kind of data."""

from __future__ import annotations

import threading
import time
from collections import OrderedDict
from collections.abc import Callable
from dataclasses import dataclass
from enum import Enum
from typing import Protocol


class Freshness(Enum):
    """How quickly an endpoint's data changes. Every endpoint is tagged with one."""

    STATIC = "static"  # sports, countries, the league list
    SLOW = "slow"  # teams, players, venues, seasons
    MEDIUM = "medium"  # schedules, standings, TV listings, highlights
    LIVE = "live"  # live scores


@dataclass(frozen=True)
class CachePolicy:
    """Time-to-live in seconds for each Freshness. Zero or less means don't cache."""

    static: float = 7 * 24 * 3600
    slow: float = 24 * 3600
    medium: float = 3600
    live: float = 0

    def ttl(self, freshness: Freshness) -> float:
        return {Freshness.STATIC: self.static, Freshness.SLOW: self.slow,
                Freshness.MEDIUM: self.medium, Freshness.LIVE: self.live}[freshness]


class ResponseCache(Protocol):
    """Stores raw response bodies. Keys never contain an API key. Implement it for Redis, disk, etc."""

    def get(self, key: str) -> str | None: ...

    def put(self, key: str, body: str, ttl: float) -> None: ...


class InMemoryResponseCache:
    """A bounded, thread-safe, in-memory LRU cache with per-entry expiry."""

    def __init__(self, max_entries: int = 1000, clock: Callable[[], float] = time.monotonic) -> None:
        self._max = max_entries
        self._clock = clock
        self._lock = threading.Lock()
        self._entries: OrderedDict[str, tuple[str, float]] = OrderedDict()

    def get(self, key: str) -> str | None:
        with self._lock:
            entry = self._entries.get(key)
            if entry is None:
                return None
            body, expires = entry
            if expires <= self._clock():
                del self._entries[key]
                return None
            self._entries.move_to_end(key)
            return body

    def put(self, key: str, body: str, ttl: float) -> None:
        if ttl <= 0:
            return
        with self._lock:
            self._entries[key] = (body, self._clock() + ttl)
            self._entries.move_to_end(key)
            while len(self._entries) > self._max:
                self._entries.popitem(last=False)

    def clear(self) -> None:
        with self._lock:
            self._entries.clear()

    def __len__(self) -> int:
        with self._lock:
            return len(self._entries)
