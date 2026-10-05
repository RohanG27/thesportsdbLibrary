# Changelog

## 0.1.0 (unreleased)

First version.

- Every documented TheSportsDB endpoint: v1 (free and premium keys) and v2 (premium), plus v1's undocumented `livescore.php`.
- Typed, leniently parsed models with the original fields in `raw`.
- Client-side rate limiting by key tier, retries for 429, 5xx and network errors, optional caching by data freshness, and API keys redacted from every error and cache key.
- `SportsDbFutures`: a `CompletableFuture` API for Java, generated from the Kotlin API.
- Dokka API reference; tests against recorded free and premium responses, plus optional live tests.

### Known issues

- **Models are data classes.** Adding a property changes their `copy()` and `componentN()` signatures, which breaks binary compatibility. TheSportsDB adds fields over time, so this needs a decision before 1.0: switch to regular classes with `equals`/`hashCode`/`toString` (or a tool such as Poko), or accept breaking releases. The ABI check (`api/sportsdb-kotlin.api`) flags any such change.
- Placeholder Maven coordinates (`local.sportsdb`), and no license yet.
