# sportsdb-kotlin

A Kotlin/JVM client for [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2.

- **Every documented endpoint**, v1 and v2, as one method each: [docs/ENDPOINTS.md](docs/ENDPOINTS.md).
- **Typed models** with readable names (`event.homeScore: Int?`, `event.timestamp: Instant?`), parsed leniently: the API sends everything as strings, and a bad value becomes `null` instead of failing the call. The original fields stay available in `raw`.
- **The API's quirks are handled for you**: one-off record keys, the four ways of saying "no results", UTC timestamps in two formats, `yes`/`No`/`NO` flags.
- **Safe by default**: a client-side rate limiter matched to your key, retries for 429s and server errors, and no API key in any exception message.
- **Optional caching**, with TTLs based on how fast each kind of data changes.
- Coroutines (`suspend`) on OkHttp 5. JVM 11+ and Android (API 26+).

> Not affiliated with TheSportsDB. Read their [terms](https://www.thesportsdb.com/docs_terms_of_use.php) before you publish an app. In particular, artwork that isn't Creative Commons may not be used in published apps (see `Player.creativeCommons`).

## Quick start

```kotlin
val client = SportsDbClient()                     // the free key, 123: v1 only, small results

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
- **Status:** `event.statusCode` is the raw code (`NS`, `2H`, `Q3`, `P2`, `IN7`, `FT`, `PST`...). `event.status` reduces it to `NOT_STARTED`, `IN_PLAY`, `FINISHED`, `POSTPONED`, `CANCELLED`, `ABANDONED` or `UNKNOWN`. Older events often have no status code at all.
- **Descriptions** come as a map keyed by language code, `descriptions["DE"]`. `description` is the English one.
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
    transport = OkHttpTransport(mySharedOkHttpClient)
}
```

Create one client per key and share it: its rate limiter and cache cover every call made through it. Every endpoint has a freshness class: `STATIC` (sports, countries), `SLOW` (teams, players), `MEDIUM` (schedules, TV, tables) or `LIVE`. The cache policy maps each class to a TTL. Implement `ResponseCache` to cache in Redis, a database or on disk. Cache keys never contain your API key.

## Security

v1 puts the key in the URL, so a v1 URL is a secret. The library never puts a URL containing the key into an exception or a cache key; it shows `/api/v1/json/***/...` instead. If you add an OkHttp logging interceptor, redact the path yourself. With a premium key, prefer v2: there, the key travels only in a header.

## Java

The API uses `suspend` functions. From Java, call it through `kotlinx-coroutines` (`BuildersKt.runBlocking`, or `future { }` from `kotlinx-coroutines-jdk8`). A Java-friendly `CompletableFuture` facade is planned.

## Building

Requires JDK 17 to build (the output targets Java 11).

```sh
./gradlew test        # offline: parser and HTTP tests against recorded responses
./gradlew liveTest    # calls the real API with the free key (set THESPORTSDB_API_KEY for v2)
tools/record-fixtures.sh   # re-record test fixtures (THESPORTSDB_PREMIUM_KEY=... adds v2)
```

See [docs/THESPORTSDB-API-REFERENCE.md](docs/THESPORTSDB-API-REFERENCE.md) for the measured API behaviour this library is built on.
