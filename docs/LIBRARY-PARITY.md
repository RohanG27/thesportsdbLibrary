# Library parity: Kotlin and Python

Both client libraries implement the same behaviour, learned from the API (see [THESPORTSDB-API-BEHAVIOUR.md](THESPORTSDB-API-BEHAVIOUR.md)) and from reviewing older clients ([jsportsdb](https://github.com/Biacode/jsportsdb) and [TralahM/thesportsdb](https://github.com/TralahM/thesportsdb); see [python/REVIEW.md](../python/REVIEW.md)). Both are tested against the same recorded responses.

**Rule: a lesson is done only when it's in both libraries.** When you learn something new about the API:
1. Record a fixture (`tools/record-fixtures.sh`, then `python/tools/sync_fixtures.py`).
2. Note it in the behaviour doc.
3. Implement and test it in both libraries.
4. Add a row here.

| # | Behaviour | Why | Kotlin | Python |
|---|---|---|---|---|
| 1 | Records come from the endpoint's known key, falling back to any array-valued key | Keys vary by endpoint (`eventslast.php` → `results`, `search_all_leagues.php` → `countries`) | `internal/Envelope.kt` · `RequesterTest.recordKeyFallback` | `_envelope.py` · `test_parsing.test_envelopes` |
| 2 | Empty body, `{"key":null}` and `{"Message":"No data found"}` all mean "no results" | Measured | `Envelope.kt` · `V1FixtureTest.emptyResults` | `_envelope.py` · `test_v1.test_empty_results` |
| 3 | Text where the records belong is an error (`ApiMessageException` / `ApiMessageError`) | `{"seasons":"Invalid League ID passed"}`; upstream Python's `listSeasons` hit this | `Envelope.kt` · `V1FixtureTest.roundAndLegacyQuirks` | `_envelope.py` · `test_v1.test_rejected_parameter_is_an_error` |
| 4 | Lenient values: `""` → missing, numbers from strings **or** JSON numbers, case-insensitive flags, `locked`/`unlocked`, id/year `0` → missing, malformed → missing | Everything is text; v2 search sends numbers | `internal/Fields.kt` · `ParsingTest` | `_fields.py` · `test_parsing` |
| 5 | Timestamps are UTC, with a `T` or a space (`strTimeStamp` on TV) | Measured | `Fields.instant` · `ParsingTest.timestampsAreUtc` | `Rec.ts` (aware `datetime`) · `test_parsing.test_timestamps_are_utc` |
| 6 | Free keys are `123` **and** `3`; v2 with a free key fails before any request | Key `3` still works (found in the Python review) | `SportsDbConfig.FREE_API_KEYS` · `V2Test.freeKeysNeverCallV2` | `config.FREE_API_KEYS` · `test_v2.test_free_keys_never_call_v2` |
| 7 | Wrong key → `InvalidApiKey…` | HTTP 400 with a "premium" message | `Requester.kt` · `V2Test.invalidKeyIsReported` | `_requester.py` · `test_v2.test_invalid_key_and_premium_detection` |
| 8 | The v1 key never appears in errors, logs or cache keys; v2's key only in the `X-API-KEY` header | v1 puts the key in the URL | `RequesterTest.v1KeyNeverAppearsInErrors`, `cacheServesRepeatsAndKeepsKeysOut` | `test_requester.test_v1_key_never_appears_in_errors`, `test_cache_serves_repeats_and_keeps_keys_out` |
| 9 | Client-side rate limit by tier (30 free / 100 paid per minute) | Documented limits | `http/RateLimiter.kt` · `RateLimiterTest` | `AsyncRateLimiter` · `test_requester.test_client_side_rate_limit` |
| 10 | 429: wait for `Retry-After` (or 60 s), retry once; 5xx and network errors: retry with backoff | Documented 429 behaviour; neither older client had retries | `Requester.send` · `RequesterTest` | `_requester._send` · `test_requester` |
| 11 | A request timeout (30 s by default) | Upstream Python had none and could hang forever | `SportsDbConfig.timeout` · `RequesterTest.aSilentServerTimesOut` | `timeout=` · `test_timeout` |
| 12 | Optional cache, with TTLs by data freshness; empty bodies and live scores aren't cached | Data changes at very different rates | `cache/ResponseCache.kt` · `RequesterTest` | `cache.py` · `test_requester` |
| 13 | Identical calls in flight share one request; if the first caller is cancelled, a waiter takes over | Avoids bursts against the rate limit | `Requester.shared` · `ConcurrencyTest` | `_requester._shared` · `test_async` |
| 14 | A per-call event (redacted URL, status, attempts, cache/shared flags, duration, error); a failing listener can't break calls | Observability | `RequestEvent.kt` · `ConcurrencyTest.listener…` | `events.py` · `test_requester.test_listener…` |
| 15 | Models are read-only, equal when built from the same fields, short `toString`/`repr`, original fields in `raw` | Fields can be added without breaking callers | `ApiRecord` · `ModelTest` | `ApiRecord` (frozen, kw-only) · `test_models` |
| 16 | Endpoints and parameters as the API actually answers them: `lookuplineup.php` (not `…lineups`), `searchvenues.php?v=`, `eventsround.php?id=`, `search_all_seasons.php?id=`, league teams by **name** via `search_all_teams.php` (not `lookup_all_teams.php`, which returns another league) | Each one was broken in upstream Python | `V1Api.kt` · `V1FixtureTest` | `_async/v1.py` · `test_v1.test_endpoint` |
| 17 | `eventstv.php` with a country needs a sport; `?id=` is a **channel** id | Empty body otherwise; the original reference had `id=` wrong | `V1Api.Tv` · `V1FixtureTest.tvCountryNeedsSport` | `AsyncV1Tv` · `test_v1.test_argument_rules…` |
| 18 | Undocumented endpoints exposed with warnings: `livescore.php` (free keys get the full feed), `eventsround.php` (free keys only; 404 for premium) | Useful but unstable | `V1Api.Live`, `V1Api.Schedule.round` | `AsyncV1Live`, `AsyncV1Schedule.round` |
| 19 | Helpers route by tier: v2 for premium, v1 for free | One code path for any key | `Helpers.kt` · `HelpersTest` | `_async/helpers.py` · `test_helpers` |
| 20 | `eventsOnLocalDate` / `events_on_local_date` fetches every UTC day a local day overlaps | Events are filed under their UTC date | `HelpersTest.eventsOnLocalDate…` | `test_helpers.test_events_on_local_date…` |
| 21 | `roundEvents` / `round_events`: the v2 season filtered by round for premium keys, `eventsround.php` for free keys | `eventsround.php` returns 404 to premium keys | `HelpersTest.roundEvents` | `test_helpers.test_round_events_take_different_routes` |
| 22 | Image sizes only for `r2.thesportsdb.com` and `www…/images/media/` URLs | `/tiny` returns 404 elsewhere | `model/Common.kt sized` · `ParsingTest.imageSizes` | `models.sized` · `test_parsing.test_image_sizes` |
| 23 | `EventStatus` groups every sport's codes; older events often have no code | Measured | `EventStatus.of` · `ParsingTest.eventStatus` | `EventStatus.of` · `test_parsing.test_event_status` |
| 24 | Two APIs from one source, with a test that fails if the generated one is stale | Avoids hand-copying | Java `SportsDbFutures` from `tools/gen-futures.py` · `FuturesCoverageTest` | blocking `SportsDB` from `tools/unasync.py` · `test_generated` |

## Deliberate differences

| | Kotlin | Python |
|---|---|---|
| Second API | `CompletableFuture` for Java | Blocking `SportsDB` alongside `AsyncSportsDB` |
| Overloads | `leagues()` / `leagues(country, sport)`, `channel(name)` / `channel(id)` | `leagues()` / `leagues_in_country(...)`, `channel(name)` / `channel_id(id)` |
| Image sizes | `url.sized(ImageSize.TINY)` extension | `sized(url, ImageSize.TINY)` function (accepts `None`) |
| Durations | `kotlin.time.Duration` (Java: `java.time.Duration` setters) | seconds as `float` |
| Naive times (`dateUpdated`, `updated`) | `LocalDateTime` | naive `datetime` |
