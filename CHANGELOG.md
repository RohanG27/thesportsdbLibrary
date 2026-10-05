# Changelog

## 0.1.0 (unreleased)

First version.

- Every documented TheSportsDB endpoint: v1 (free and premium keys) and v2 (premium), plus v1's undocumented `livescore.php`.
- Typed, leniently parsed models with the original fields in `raw`.
- Client-side rate limiting by key tier, retries for 429, 5xx and network errors, optional caching by data freshness, and API keys redacted from every error and cache key.
- `SportsDbFutures`: a `CompletableFuture` API for Java, generated from the Kotlin API.
- Dokka API reference; tests against recorded free and premium responses, plus optional live tests.
- Models are read-only `ApiRecord` subclasses with internal constructors, equal when built from the same API fields, so fields can be added without breaking binary compatibility.
- Identical calls in flight share one HTTP request (`deduplicateRequests`).
- `requestListener`: a `RequestEvent` per call, for logging and metrics.
- `client.helpers`: common tasks in one call (season fixtures, upcoming events, a team's schedule, events on a local calendar day, live scores, league teams, event channels, TV listings), using v2 with a premium key and v1 with the free key.

### Fixed during review

- The older free key `3` is recognised as free (`SportsDbConfig.FREE_API_KEYS`), so helpers use v1 for it and v2 calls fail fast.
- A rejected parameter (`{"seasons":"Invalid League ID passed"}`) raises `ApiMessageException` with the API's text, instead of a generic parse error.
- Added `v1.schedule.round` (`eventsround.php`, undocumented, free keys only) and the `helpers.roundEvents` helper, which uses the v2 season schedule on premium keys.

- `timeout` (30 s by default) for the default transport; previously OkHttp's fixed 10 s defaults applied. `transport` is now nullable (null = built from `timeout`).

### Known issues

- Placeholder Maven coordinates (`local.sportsdb`), and no license yet.
