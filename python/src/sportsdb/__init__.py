"""A Python client for the TheSportsDB API, v1 and v2.

``SportsDB`` (blocking) and ``AsyncSportsDB`` (asyncio or trio) share one implementation:
every endpoint, typed models, helpers for common tasks, client-side rate limiting, retries,
optional caching and de-duplication of identical calls in flight. API keys never appear in
errors or cache keys.
"""

from ._async._transport import AsyncTransport, HttpxAsyncTransport, Response
from ._async.client import AsyncSportsDB
from ._sync._transport import HttpxTransport, Transport
from ._sync.client import SportsDB
from .cache import CachePolicy, Freshness, InMemoryResponseCache, ResponseCache
from .config import FREE_API_KEY, FREE_API_KEYS, Config
from .errors import (
    ApiMessageError,
    HTTPStatusError,
    InvalidApiKeyError,
    NetworkError,
    PremiumRequiredError,
    RateLimitError,
    ResponseParseError,
    SportsDBError,
)
from .events import RequestEvent
from .models import (
    ApiRecord,
    Contract,
    Country,
    Equipment,
    Event,
    EventResult,
    EventStat,
    EventStatus,
    FormerTeam,
    Honour,
    ImageSize,
    League,
    LeagueRef,
    LineupEntry,
    LiveScore,
    Milestone,
    Player,
    PlayerExternalIds,
    PlayerStat,
    RoundStage,
    Season,
    Socials,
    Sport,
    Standing,
    Team,
    TimelineEntry,
    TvListing,
    Venue,
    sized,
)

__version__ = "0.1.0.dev0"

__all__ = [
    "AsyncSportsDB", "SportsDB", "Config", "FREE_API_KEY", "FREE_API_KEYS",
    "AsyncTransport", "Transport", "HttpxAsyncTransport", "HttpxTransport", "Response",
    "CachePolicy", "Freshness", "InMemoryResponseCache", "ResponseCache", "RequestEvent",
    "SportsDBError", "InvalidApiKeyError", "PremiumRequiredError", "RateLimitError", "HTTPStatusError",
    "ResponseParseError", "ApiMessageError", "NetworkError",
    "ApiRecord", "Contract", "Country", "Equipment", "Event", "EventResult", "EventStat", "EventStatus",
    "FormerTeam", "Honour", "ImageSize", "League", "LeagueRef", "LineupEntry", "LiveScore", "Milestone",
    "Player", "PlayerExternalIds", "PlayerStat", "RoundStage", "Season", "Socials", "Sport", "Standing", "Team",
    "TimelineEntry", "TvListing", "Venue", "sized",
]
