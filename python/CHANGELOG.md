# Changelog

## 0.1.0 (unreleased)

First version.

- Every documented TheSportsDB endpoint: v1 (free and premium keys) and v2 (premium), plus the useful undocumented ones.
- Blocking (`SportsDB`) and async (`AsyncSportsDB`, asyncio or trio) clients sharing one implementation.
- Typed, read-only, leniently parsed models with timezone-aware UTC datetimes and the original fields in `raw`.
- Client-side rate limiting by key tier, retries for 429, 5xx and network errors, optional caching by data freshness, and API keys redacted from every error and cache key.
- Helpers that use v2 with a premium key and v1 with the free key.
