# sportsdb-kotlin

**API reference:** https://rohang27.github.io/thesportsdbLibrary/api/kotlin/index.html (Dokka)

> Part of [thesportsdbLibrary](../README.md). Run the commands below from `kotlin/`.

A Kotlin/JVM client for [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2.

> Python, PHP and JavaScript siblings with the same behaviour live in [`python/`](../python/README.md), [`php/`](../php/README.md) and [`javascript/`](../javascript/README.md). [docs/LIBRARY-PARITY.md](../docs/LIBRARY-PARITY.md) tracks every lesson all four libraries implement. [`graphql/`](../graphql/README.md) is a GraphQL schema for the same data, and [`docs-site/`](../docs-site/README.md) holds proposed API documentation (guides and OpenAPI reference) for TheSportsDB, published at **https://rohang27.github.io/thesportsdbLibrary/**.

- **Every documented endpoint**, v1 and v2, as one method each: [../docs/ENDPOINTS.md](../docs/ENDPOINTS.md).
- **Typed models** with readable names (`event.homeScore: Int?`, `event.timestamp: Instant?`), parsed leniently: the API sends everything as strings, and a bad value becomes `null` instead of failing the call. The original fields stay available in `raw`.
- **The API's quirks are handled for you**: one-off record keys, the four ways of saying "no results", UTC timestamps in two formats, `yes`/`No`/`NO` flags.
- **Safe by default**: a client-side rate limiter matched to your key, retries for 429s and server errors, and no API key in any exception message.
- **Optional caching**, with TTLs based on how fast each kind of data changes.
- Coroutines (`suspend`) on OkHttp 5, plus a `CompletableFuture` API for Java. JVM 11+ and Android (API 26+).

> Not affiliated with TheSportsDB. Read their [terms](https://www.thesportsdb.com/docs_terms_of_use.php) before you publish an app. In particular, artwork that isn't Creative Commons may not be used in published apps (see `Player.creativeCommons`).

## Quick start

```kotlin
val client = SportsDbClient()                     // the free key, 123 (3 also works): v1 only, small results

val arsenal = client.v1.lookup.team(133604)
println(arsenal?.name)                            // Arsenal
println(arsenal?.badge?.sized(ImageSize.TINY))    // a ~14 KB version of the badge

val table = client.v1.lookup.table(4328)          // Premier League standings
val today = client.v1.schedule.day(LocalDate.now(ZoneOffset.UTC), sport = "Soccer")
```

With a premium key:

```kotlin
val client = SportsDbClient {
    apiKey = System.getenv("THESPORTSDB_API_KEY")
    cache = InMemoryResponseCache()
}

val league = client.v2.lookup.league(4328)!!
val season = client.v2.schedule.leagueSeason(4328, league.currentSeason!!)   // ~380 games, one call
val tv = client.v2.tv.country("Canada")                                       // about a week of listings
val live = client.v2.live.all()
```

## Helpers: common tasks in one call

`client.helpers` picks v2 with a premium key and v1 with the free key, so the same code works with either key. With the free key you get the free key's small results.

```kotlin
val h = client.helpers
h.currentSeason(4328)                                   // "2026-2027"
h.seasonEvents(4328)                                    // the current season's fixtures
h.roundEvents(4328, round = 7)                          // one matchday
h.upcomingLeagueEvents(4328, days = 7)                  // the next week of a league
h.recentLeagueResults(4328)
h.teamSchedule(133604)                                  // past and future, all competitions
h.eventsOnLocalDate(LocalDate.now(), ZoneId.of("America/Toronto"), sport = "Ice Hockey")
h.liveScores(leagueId = 4328)
h.leagueTeams(4328)                                     // with badges
h.eventChannels(2494052)                                // who's showing it
h.tvListings("Canada", sport = "Ice Hockey", days = 3)
```

| Helper | Premium key | Free key |
|---|---|---|
| `currentSeason` | v2 `lookup/league` | `lookupleague.php` |
| `seasonEvents` | v2 `schedule/league/{id}/{season}`: the whole season | `eventsseason.php`: first 5 events |
| `roundEvents` | the season schedule, filtered by round | `eventsround.php` (undocumented; the whole round) |
| `upcomingLeagueEvents` | the season schedule, filtered by date | `eventsday.php` once per day (3 events a day) |
| `recentLeagueResults` | v2 `schedule/previous/league` | `eventspastleague.php` (1) |
| `teamSchedule` | v2 `schedule/full/team` | `eventsnext.php` + `eventslast.php` (home games only) |
| `eventsOnLocalDate` | `eventsday.php` for each UTC day the local day overlaps, filtered to the local day | the same (3 events per UTC day) |
| `liveScores` | v2 `livescore/…` | `livescore.php` (undocumented), filtered by league here; needs a sport or a league |
| `leagueTeams` | v2 `list/teams` | league name, then `search_all_teams.php` (10) |
| `eventChannels` | v2 `lookup/event_tv` | `lookuptv.php` (2) |
| `tvListings` | v2 `filter/tv/country` (about a week), filtered | `eventstv.php` once per day; needs a sport |

**Why `eventsOnLocalDate`:** the API files events under their UTC date, so evening games in North America appear on the next UTC day. "Saturday's games in Toronto" spans two API days; this helper fetches both and keeps the right ones.

From Java: `db.helpers().seasonEvents(4328).join()`.

## v1 or v2?

| | v1 | v2 |
|---|---|---|
| Keys | free `123` **and** premium | premium only |
| How the key is sent | in the URL path | `X-API-KEY` header |
| Free-key results | 1–10 per call (noted on each method) | — |
| Unique to it | league tables, `eventsday`, highlights by day, TV by day + country + sport | full team schedule, venue schedules, TV for a whole country, documented live scores |

Both return the same model types. A premium key gives full results on v1 too. With the free key, v2 methods throw `PremiumRequiredException` without making a call. `client.isPremiumKey()` checks a key with one v2 call.

## Models

Each model documents the API field it comes from, e.g. `/** `strTeamShort`, e.g. `ARS` */ val shortName`.

| Model | From |
|---|---|
| `Sport`, `Country`, `League`, `Season`, `Team`, `Venue` | catalogues, searches, lookups |
| `Player`, `Honour`, `FormerTeam`, `Milestone`, `Contract`, `PlayerStat`, `Equipment` | players and teams |
| `Event` | events, schedules, results, highlights |
| `EventResult` | per-competitor results in individual sports (races, golf) |
| `Standing`, `LineupEntry`, `TimelineEntry`, `EventStat` | tables and match details |
| `TvListing`, `LiveScore` | TV and live scores |

How values are read:

- **Missing values are `null`.** The API sends `""`, `null`, or leaves the field out; all three become `null`. Lists (`alternateNames`, `fanart`, `leagues`) are empty instead.
- **Numbers** are parsed from strings. Ids are `Long`. Years and ids of `0` mean unknown and become `null`.
- **Times:** `Event.timestamp`, `date` and `time` are **UTC**. `localDate` and `localTime` are the venue's local time, and are often missing for future games. `TvListing.timestamp` is UTC too: the API writes it differently (`strTimeStamp`, with a space), and the library reads both forms.
- **Status:** `event.statusCode` is the raw code (`NS`, `2H`, `Q3`, `P2`, `IN7`, `FT`, `PST`...). `event.status` reduces it to `NOT_STARTED`, `IN_PLAY`, `FINISHED`, `POSTPONED`, `INTERRUPTED`, `CANCELLED`, `ABANDONED` or `UNKNOWN`, covering every sport's codes in TheSportsDB's data documentation. Older events often have no status code at all.
- **Stages:** `intRound` sometimes holds a stage code instead of a round number (200 = final, 500 = pre-season...). `event.stage` reads it as a `RoundStage` (`FINAL`, `SEMI_FINAL`, `PLAYOFF_FINAL`...), or null for an ordinary round.
- **Descriptions** come as a map keyed by language code, `descriptions["DE"]`. `description` is the English one.
- **Records are read-only.** Every model extends `ApiRecord`: two records are equal when the API sent the same fields, and `toString()` is a short summary (`Team(id=133604, name=Arsenal)`). Constructors are internal, so later versions can add fields without breaking compiled code. To test your own code, fake the HTTP layer with an `HttpTransport` rather than building models.
- **Anything not modelled** is in `raw`, e.g. `team.raw["strKeywords"]`. Checked against the recorded responses: every field that v1 and v2 currently return is mapped to a model property.
- **v2 search** sends ids as JSON numbers rather than strings; the models read both.
- **Live scores can be stale.** The live feed can still list a game hours after its last update (a 4 Oct game was still listed on 5 Oct). Check `LiveScore.updated` before showing a score as live.

## Errors

All errors extend `SportsDbException`:

| Exception | When |
|---|---|
| `InvalidApiKeyException` | The key was rejected (HTTP 400). Only `123` and paid keys work. |
| `PremiumRequiredException` | A v2 call with the free key. Thrown before any request is made. |
| `RateLimitException` | HTTP 429 after the retry. `retryAfter` is set if the server said how long to wait. |
| `HttpStatusException` | Any other HTTP error, after retries for 5xx. |
| `NetworkException` | No HTTP response at all, after retries. |
| `ResponseParseException` | The body wasn't a TheSportsDB envelope (e.g. an HTML error page). |
| `ApiMessageException` | A `{"Message": ...}` body the library doesn't recognise. |

"No results" is **not** an error: you get an empty list, or `null` for single lookups.

Invalid argument combinations throw `IllegalArgumentException` before any request is made. For example, `v1.tv.day(country = ...)` without a `sport`, which the API would answer with an empty body.

## Rate limits, retries, caching

```kotlin
SportsDbClient {
    apiKey = "..."
    requestsPerMinute = 120          // default: 30 for the free key, 100 otherwise; 0 = off
    maxRetries = 2                   // network errors and 5xx, with exponential backoff
    retryOnRateLimit = true          // on 429: wait (Retry-After, or 1 minute) and retry once
    cache = InMemoryResponseCache(maxEntries = 5_000)
    cachePolicy = CachePolicy(static = 7.days, slow = 1.days, medium = 1.hours, live = Duration.ZERO)
    timeout = 30.seconds             // per request; applies to the default transport
    transport = OkHttpTransport(mySharedOkHttpClient)   // optional: share an OkHttp client
}
```

Create one client per key and share it: its rate limiter, cache and de-duplication cover every call made through it. **Identical calls in flight are de-duplicated:** if ten coroutines ask for the same table at once, one request is made and all ten get its result (`deduplicateRequests = false` turns this off).

**Observability:** `requestListener` receives a `RequestEvent` for every call: the redacted URL, status, attempts, whether it came from the cache or from another caller's request, the duration and any error.

```kotlin
SportsDbClient {
    requestListener = RequestListener { e ->
        log.debug("${e.url} -> ${e.status} in ${e.duration} (attempts=${e.attempts}, cache=${e.fromCache}, shared=${e.shared})")
    }
}
```

 Every endpoint has a freshness class: `STATIC` (sports, countries), `SLOW` (teams, players), `MEDIUM` (schedules, TV, tables) or `LIVE`. The cache policy maps each class to a TTL. Implement `ResponseCache` to cache in Redis, a database or on disk. Cache keys never contain your API key.

## Security

v1 puts the key in the URL, so a v1 URL is a secret. The library never puts a URL containing the key into an exception or a cache key; it shows `/api/v1/json/***/...` instead. If you add an OkHttp logging interceptor, redact the path yourself. With a premium key, prefer v2: there, the key travels only in a header.

## Java

`SportsDbFutures` wraps every method in a `CompletableFuture`:

```java
SportsDbConfig config = new SportsDbConfig();
config.setApiKey(System.getenv("THESPORTSDB_API_KEY"));
config.setCache(new InMemoryResponseCache());

try (SportsDbFutures db = new SportsDbFutures(config)) {
    Team arsenal = db.v1().lookup().team(133604).join();             // null if not found
    db.v2().schedule().leagueNext(4328)
        .thenAccept(events -> events.forEach(e -> System.out.println(e.getName())));
}
```

Optional Kotlin arguments become Java overloads (`db.v1().schedule().day(date)`, `day(date, "Soccer")`, ...). Failures complete the future exceptionally with a `SportsDbException` (`join()` wraps it in a `CompletionException`). `close()` cancels calls that are still running. For `Duration` settings, use `config.setRetryBackoff(java.time.Duration)`, `CachePolicy.of(...)` and `RateLimitException.retryAfterDuration()`.

`SportsDbFutures` is generated from the Kotlin API by `tools/gen-futures.py`. A test fails if it falls out of date.

## Building

Requires JDK 17 to build (the output targets Java 11).

```sh
./gradlew test        # offline: parser, HTTP and Java-interop tests against recorded responses
./gradlew liveTest    # calls the real API with the free key (set THESPORTSDB_API_KEY for v2)
./gradlew dokkaGenerate    # API reference: build/dokka/html/index.html
../tools/record-fixtures.sh   # re-record test fixtures (shared by every library) (THESPORTSDB_PREMIUM_KEY=... adds v2)
python3 tools/gen-futures.py   # regenerate SportsDbFutures after changing V1Api/V2Api
./gradlew updateKotlinAbi      # after an intended public-API change (checkKotlinAbi runs in `check`)
./gradlew publishToMavenLocal  # use it from another project: mavenLocal(), local.sportsdb:sportsdb-kotlin:0.1.0-SNAPSHOT
```

CI (`../.github/workflows/ci.yml`) runs the offline build, the ABI check and the docs build on every push and pull request. Weekly, it also runs the live tests, to catch changes in TheSportsDB's API. Add a `THESPORTSDB_API_KEY` repository secret to include v2.

See [docs/THESPORTSDB-API-BEHAVIOUR.md](../docs/THESPORTSDB-API-BEHAVIOUR.md) for the measured API behaviour this library is built on, including where it differs from the official documentation.

## License

MIT. See [LICENSE](LICENSE). TheSportsDB's data and artwork are covered by [its own terms](https://www.thesportsdb.com/docs_terms_of_use.php), not by this license.
