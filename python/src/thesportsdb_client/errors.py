"""Exceptions. Every error the library raises extends :class:`SportsDBError`.

Messages never contain an API key: v1 URLs (which carry the key in their path) are shown
with ``***`` in its place.
"""

from __future__ import annotations


class SportsDBError(Exception):
    """Base class for every error this library raises."""


class InvalidApiKeyError(SportsDBError):
    """The API rejected the key (HTTP 400 ``Invalid Premium API key``). Only ``123``, ``3`` and paid keys work."""


class PremiumRequiredError(SportsDBError):
    """A v2 endpoint was called with a free key. v2 accepts premium keys only. Raised before any request."""


class RateLimitError(SportsDBError):
    """HTTP 429 after the retry was used up or disabled. ``retry_after`` is the server's wait in seconds, if given."""

    def __init__(self, message: str, retry_after: float | None) -> None:
        super().__init__(message)
        self.retry_after = retry_after


class HTTPStatusError(SportsDBError):
    """Any other non-2xx HTTP response, after retries for 5xx."""

    def __init__(self, message: str, status: int, body_snippet: str) -> None:
        super().__init__(message)
        self.status = status
        self.body_snippet = body_snippet


class ResponseParseError(SportsDBError):
    """The body was not a TheSportsDB envelope (e.g. an HTML error page)."""


class ApiMessageError(SportsDBError):
    """The API answered with a message instead of data.

    Either an unrecognised ``{"Message": ...}`` body, or a rejected parameter, which v1
    reports as text where the records belong: ``{"seasons": "Invalid League ID passed"}``.
    """

    def __init__(self, message: str, api_message: str) -> None:
        super().__init__(message)
        self.api_message = api_message


class NetworkError(SportsDBError):
    """No HTTP response arrived (DNS, connection, timeout), after retries."""
