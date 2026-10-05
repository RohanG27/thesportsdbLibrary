"""Per-call events for logging and metrics."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class RequestEvent:
    """One finished API call, passed to ``request_listener``."""

    url: str
    """The URL with any v1 key replaced by ``***``. Safe to log."""
    status: int | None
    """The last HTTP status; None if no response arrived or the result came from the cache."""
    attempts: int
    """HTTP requests made, counting retries; 0 when served by the cache or another caller's request."""
    from_cache: bool
    shared: bool
    """True when an identical call already in flight supplied the result."""
    duration: float
    """Seconds for the whole call, including rate-limit waits and retries."""
    error: BaseException | None
