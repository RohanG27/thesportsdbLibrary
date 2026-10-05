"""Client settings."""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass, field

from .cache import CachePolicy, ResponseCache
from .events import RequestEvent

FREE_API_KEY = "123"
"""The documented public key for development and testing."""

FREE_API_KEYS = frozenset({FREE_API_KEY, "3"})
"""Keys the API accepts without payment: ``123`` and the older ``3`` (same limits; measured 5 Oct 2026)."""


@dataclass(frozen=True, kw_only=True)
class Config:
    api_key: str = FREE_API_KEY
    requests_per_minute: int | None = None
    """Client-side limit shared by v1 and v2. None: 30 for a free key, 100 otherwise. 0 turns it off."""
    max_retries: int = 2
    """Retries for network errors and HTTP 5xx, with exponential backoff."""
    retry_backoff: float = 0.5
    """First backoff, in seconds; doubles on every retry."""
    retry_on_rate_limit: bool = True
    """On HTTP 429, wait (Retry-After, or rate_limit_wait) and retry once."""
    rate_limit_wait: float = 60.0
    cache: ResponseCache | None = None
    cache_policy: CachePolicy = field(default_factory=CachePolicy)
    timeout: float = 30.0
    """Seconds before a request gives up (connect and read)."""
    deduplicate_requests: bool = True
    """Identical calls made at the same time share one HTTP request."""
    request_listener: Callable[[RequestEvent], None] | None = None
    """Called once per call, after it finishes. Exceptions it raises are ignored."""
    base_url: str = "https://www.thesportsdb.com"
    user_agent: str = "sportsdb-python"

    @property
    def is_free_key(self) -> bool:
        return self.api_key in FREE_API_KEYS

    @property
    def effective_requests_per_minute(self) -> int:
        if self.requests_per_minute is not None:
            return self.requests_per_minute
        return 30 if self.is_free_key else 100
