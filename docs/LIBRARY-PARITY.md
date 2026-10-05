# Library parity: Kotlin, Python, PHP and JavaScript

All four client libraries implement the same behaviour, learned from the API (see [THESPORTSDB-API-BEHAVIOUR.md](THESPORTSDB-API-BEHAVIOUR.md)) and from reviewing older clients ([jsportsdb](https://github.com/Biacode/jsportsdb) and [TralahM/thesportsdb](https://github.com/TralahM/thesportsdb); see [python/REVIEW.md](../python/REVIEW.md)). All are tested against the same recorded responses.

**Rule: a lesson is done only when it's in every library.** When you learn something new about the API:
1. Record a fixture (`tools/record-fixtures.sh`, then the `sync_fixtures`/`sync-fixtures` script in `python/`, `php/` and `javascript/`).
2. Note it in the behaviour doc.
3. Implement and test it in every library.
4. Add a row here.

| # | Behaviour | Why | Kotlin | Python | PHP | JavaScript |
|---|---|---|---|---|---|---|
| 1 | Records come from the endpoint's known key, falling back to any array-valued key | Keys vary by endpoint (`eventslast.php` → `results`, `search_all_leagues.php` → `countries`) | `internal/Envelope.kt` · `RequesterTest.recordKeyFallback` | `_envelope.py` · `test_parsing.test_envelopes` | `Internal/Envelope.php` · `ParsingTest::testEnvelopes` | `envelope.ts` · `parsing.test.ts` "handles every form" |
| 2 | Empty body, `{"key":null}` and `{"Message":"No data found"}` all mean "no results" | Measured | `Envelope.kt` · `V1FixtureTest.emptyResults` | `_envelope.py` · `test_v1.test_empty_results` | `Envelope.php` · `V1Test::testEmptyResultsAndErrors` | `envelope.ts` · `v1.test.ts` "treats empty results…" |
| 3 | Text where the records belong is an error (`ApiMessageException` / `ApiMessageError`) | `{"seasons":"Invalid League ID passed"}`; upstream Python's `listSeasons` hit this | `Envelope.kt` · `V1FixtureTest.roundAndLegacyQuirks` | `_envelope.py` · `test_v1.test_rejected_parameter_is_an_error` | `Envelope.php` · `V1Test::testEmptyResultsAndErrors` | `envelope.ts` · `v1.test.ts` "treats empty results…" |
| 4 | Lenient values: `""` → missing, numbers from strings **or** JSON numbers, case-insensitive flags, `locked`/`unlocked`, id/year `0` → missing, malformed → missing | Everything is text; v2 search sends numbers | `internal/Fields.kt` · `ParsingTest` | `_fields.py` · `test_parsing` | `Internal/Rec.php` · `ParsingTest` | `fields.ts` · `parsing.test.ts` |
| 5 | Timestamps are UTC, with a `T` or a space (`strTimeStamp` on TV) | Measured | `Fields.instant` · `ParsingTest.timestampsAreUtc` | `Rec.ts` (aware `datetime`) · `test_parsing.test_timestamps_are_utc` | `Rec::instant` (UTC `DateTimeImmutable`) · `ParsingTest::testTimestampsAreUtc` | `Rec.instant` (`Date`) · `parsing.test.ts` "reads timestamps as UTC" |
| 6 | Free keys are `123` **and** `3`; v2 with a free key fails before any request | Key `3` still works (found in the Python review) | `SportsDbConfig.FREE_API_KEYS` · `V2Test.freeKeysNeverCallV2` | `config.FREE_API_KEYS` · `test_v2.test_free_keys_never_call_v2` | `Config::FREE_API_KEYS` · `V2Test::testFreeKeysNeverCallV2` | `FREE_API_KEYS` · `v2.test.ts` "never calls v2 with free key…" |
| 7 | Wrong key → `InvalidApiKey…` | HTTP 400 with a "premium" message | `Requester.kt` · `V2Test.invalidKeyIsReported` | `_requester.py` · `test_v2.test_invalid_key_and_premium_detection` | `Internal/Requester.php` · `V2Test::testInvalidKeyAndPremiumDetection` | `requester.ts` · `v2.test.ts` "reports invalid keys…" |
| 8 | The v1 key never appears in errors, logs or cache keys; v2's key only in the `X-API-KEY` header | v1 puts the key in the URL | `RequesterTest.v1KeyNeverAppearsInErrors`, `cacheServesRepeatsAndKeepsKeysOut` | `test_requester.test_v1_key_never_appears_in_errors`, `test_cache_serves_repeats_and_keeps_keys_out` | `RequesterTest::testV1KeyNeverAppearsInErrors`, `testCacheServesRepeatsAndKeepsKeysOut` | `requester.test.ts` "never puts the v1 key in errors", "serves repeats from the cache…" |
| 9 | Client-side rate limit by tier (30 free / 100 paid per minute) | Documented limits | `http/RateLimiter.kt` · `RateLimiterTest` | `AsyncRateLimiter` · `test_requester.test_client_side_rate_limit` | `Internal/RateLimiter.php` · `RequesterTest::testClientSideRateLimit` | `RateLimiter` · `requester.test.ts` "limits requests client-side" |
| 10 | 429: wait for `Retry-After` (or 60 s), retry once; 5xx and network errors: retry with backoff | Documented 429 behaviour; neither older client had retries | `Requester.send` · `RequesterTest` | `_requester._send` · `test_requester` | `Requester::send` · `RequesterTest` | `Requester.send` · `requester.test.ts` |
| 11 | A request timeout (30 s by default) | Upstream Python had none and could hang forever | `SportsDbConfig.timeout` · `RequesterTest.aSilentServerTimesOut` | `timeout=` · `test_timeout` | `timeout:` · `RequesterTest::testASilentServerTimesOut` | `timeoutMs` · `requester.test.ts` "gives up on a silent server" |
| 12 | Optional cache, with TTLs by data freshness; empty bodies and live scores aren't cached | Data changes at very different rates | `cache/ResponseCache.kt` · `RequesterTest` | `cache.py` · `test_requester` | `Cache/` (in-memory or PSR-16) · `RequesterTest` | `cache.ts` (sync or async caches) · `requester.test.ts` |
| 13 | Identical calls in flight share one request; if the first caller is cancelled, a waiter takes over | Avoids bursts against the rate limit | `Requester.shared` · `ConcurrencyTest` | `_requester._shared` · `test_async` | not applicable: PHP calls are synchronous | `Requester.shared` · `concurrency.test.ts` (no per-call cancellation, so no handover case) |
| 14 | A per-call event (redacted URL, status, attempts, cache/shared flags, duration, error); a failing listener can't break calls | Observability | `RequestEvent.kt` · `ConcurrencyTest.listener…` | `events.py` · `test_requester.test_listener…` | `RequestEvent.php` · `RequesterTest::testListenerSeesRetriesCacheHitsAndErrors` | `events.ts` · `requester.test.ts` "reports retries, cache hits and errors…" |
| 15 | Models are read-only, equal when built from the same fields, short `toString`/`repr`, original fields in `raw` | Fields can be added without breaking callers | `ApiRecord` · `ModelTest` | `ApiRecord` (frozen, kw-only) · `test_models` | `ApiRecord` (readonly; `==` compares) · `ModelTest` | frozen objects with `kind`; `sameRecord()` · `models.test.ts` |
| 16 | Endpoints and parameters as the API actually answers them: `lookuplineup.php` (not `…lineups`), `searchvenues.php?v=`, `eventsround.php?id=`, `search_all_seasons.php?id=`, league teams by **name** via `search_all_teams.php` (not `lookup_all_teams.php`, which returns another league) | Each one was broken in upstream Python | `V1Api.kt` · `V1FixtureTest` | `_async/v1.py` · `test_v1.test_endpoint` | `V1/*.php` · `V1Test::testEndpoint` | `v1.ts` · `v1.test.ts` |
| 17 | `eventstv.php` with a country needs a sport; `?id=` is a **channel** id | Empty body otherwise; the original reference had `id=` wrong | `V1Api.Tv` · `V1FixtureTest.tvCountryNeedsSport` | `AsyncV1Tv` · `test_v1.test_argument_rules…` | `V1/Tv.php` · `V1Test::testArgumentRulesAreCheckedBeforeAnyRequest` | `V1Tv` · `v1.test.ts` "checks argument rules…" |
| 18 | Undocumented endpoints exposed with warnings: `livescore.php` (free keys get the full feed), `eventsround.php` (free keys only; 404 for premium) | Useful but unstable | `V1Api.Live`, `V1Api.Schedule.round` | `AsyncV1Live`, `AsyncV1Schedule.round` | `V1/Live.php`, `V1/Schedule::round` | `V1Live`, `V1Schedule.round` |
| 19 | Helpers route by tier: v2 for premium, v1 for free | One code path for any key | `Helpers.kt` · `HelpersTest` | `_async/helpers.py` · `test_helpers` | `Helpers.php` · `HelpersTest` | `helpers.ts` · `helpers.test.ts` |
| 20 | `eventsOnLocalDate` / `events_on_local_date` fetches every UTC day a local day overlaps | Events are filed under their UTC date | `HelpersTest.eventsOnLocalDate…` | `test_helpers.test_events_on_local_date…` | `HelpersTest::testEventsOnLocalDate…` | `helpers.test.ts` "events on a local date…" (offsets via `Intl`) |
| 21 | `roundEvents` / `round_events`: the v2 season filtered by round for premium keys, `eventsround.php` for free keys | `eventsround.php` returns 404 to premium keys | `HelpersTest.roundEvents` | `test_helpers.test_round_events_take_different_routes` | `HelpersTest::testRoundEventsTakeDifferentRoutes` | `helpers.test.ts` "round events take different routes" |
| 22 | Image sizes only for `r2.thesportsdb.com` and `www…/images/media/` URLs | `/tiny` returns 404 elsewhere | `model/Common.kt sized` · `ParsingTest.imageSizes` | `models.sized` · `test_parsing.test_image_sizes` | `ImageSize::of` · `ParsingTest::testImageSizes` | `sized()` · `parsing.test.ts` "sizes only TheSportsDB media images" |
| 23 | `EventStatus` covers every code in TheSportsDB's data documentation (including `POST`, `S1`–`S5`, `PT`, `AW`), with `INTERRUPTED` for `SUSP`/`INT`/`INTR`; older events often have no code | docs_api_data; the first version misread `INT` as in play | `EventStatus.of` · `ParsingTest.eventStatus` | `EventStatus.of` · `test_parsing.test_event_status` | `EventStatus::of` · `ParsingTest::testEventStatus` | `eventStatus()` · `parsing.test.ts` |
| 24 | Two APIs from one source, with a test that fails if the generated one is stale | Avoids hand-copying | Java `SportsDbFutures` from `kotlin/tools/gen-futures.py` · `FuturesCoverageTest` | blocking `SportsDB` from `tools/unasync.py` · `test_generated` | models generated from the Kotlin ones by `tools/gen_models.py` · `GeneratedTest` | `src/models.ts` generated from the Kotlin models by `tools/gen-models.mjs` · `generated.test.ts` |
| 25 | `intRound` stage codes (125 quarter-final … 200 final … 500 pre-season) are read into `stage`; other values are round numbers | docs_api_data; confirmed in the recordings (2017 FA Cup final = 200) | `RoundStage` · `ParsingTest.roundStages`, `V1FixtureTest.lookup` | `RoundStage` · `test_parsing.test_round_stages` | `RoundStage::of`, `Event::stage()` · `ParsingTest::testRoundStages` | `roundStage()`, `event.stage` · `parsing.test.ts` "reads round stage codes" |

## Deliberate differences

| | Kotlin | Python | PHP | JavaScript |
|---|---|---|---|---|
| Second API | `CompletableFuture` for Java | Blocking `SportsDB` alongside `AsyncSportsDB` | — (synchronous) | Promises only |
| De-duplication of identical calls in flight | yes | yes | not applicable (synchronous) | yes (no per-call cancellation) |
| Overloads | `leagues()` / `leagues(country, sport)`, `channel(name)` / `channel(id)` | `leagues()` / `leagues_in_country(...)`, `channel(name)` / `channel_id(id)` | `leagues()` / `leaguesInCountry(...)`, `channel(name)` / `channelId(id)` | `leagues()` / `leaguesInCountry(...)`, `channel(name)` / `channelId(id)`; optional parameters as an options object |
| Image sizes | `url.sized(ImageSize.TINY)` extension | `sized(url, ImageSize.TINY)` function (accepts `None`) | `ImageSize::Tiny->of($url)` (accepts `null`) | `sized(url, "tiny")` (accepts `null`) |
| Durations | `kotlin.time.Duration` (Java: `java.time.Duration` setters) | seconds as `float` | seconds as `float` | **milliseconds** (`timeoutMs`, `retryBackoffMs`) |
| Dates and times of day | `LocalDate`, `LocalTime` | `date`, `time` | `DateTimeImmutable` at midnight UTC; `HH:MM:SS` strings | `"YYYY-MM-DD"` and `"HH:MM:SS"` strings; instants are `Date`s |
| Naive times (`dateUpdated`, `updated`) | `LocalDateTime` | naive `datetime` | `DateTimeImmutable` in the default zone, as written | `"YYYY-MM-DDTHH:MM:SS"` strings |
| Models generated from | (source) | hand-written, same fields | the Kotlin models (`php/tools/gen_models.py`) | the Kotlin models (`javascript/tools/gen-models.mjs`) |
| Records | `ApiRecord` classes (internal constructors) | frozen dataclasses | readonly classes (`==`) | frozen plain objects with `kind` (`sameRecord()`), JSON-serialisable |

## The GraphQL schema

`graphql/` isn't a client, but it encodes the same lessons for anyone building a GraphQL gateway:
- Field names come from the shared models.
- `DateTime` is UTC.
- Ids are `ID`, because some exceed 32 bits.
- Lists are never null.
- Image sizes apply only where TheSportsDB supports them.
- `eventsOnLocalDate` describes the UTC-day rule.
- `EventStatus` (with `INTERRUPTED`) and `RoundStage` match the libraries.
- `@source` notes record the tier rules: `eventsround.php` is for free keys only, `eventstv.php` needs a sport with a country, `livescore.php` ignores leagues, and league teams come by name, not via `lookup_all_teams.php`.

Its tests check every `@source` against the OpenAPI specs in `docs-site/openapi/`.
