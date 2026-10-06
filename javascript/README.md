# thesportsdb-client

**API reference:** https://rohang27.github.io/thesportsdbLibrary/api/javascript/index.html (TypeDoc)

A JavaScript and TypeScript client for [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2. It's the JS sibling of the Kotlin ([`kotlin/`](https://github.com/RohanG27/thesportsdbLibrary/tree/main/kotlin)), Python ([`python/`](https://github.com/RohanG27/thesportsdbLibrary/tree/main/python)) and PHP ([`php/`](https://github.com/RohanG27/thesportsdbLibrary/tree/main/php)) libraries: same behaviour, same lessons, tested against the same recorded responses.

- **Every documented endpoint**, plus the useful undocumented ones, one method each, for v1 and v2.
- **Typed, frozen, plain-object models**, generated from the Kotlin models, so every library has the same fields:
  - values are parsed leniently, so a bad value becomes `null` instead of failing the call;
  - timestamps are `Date`s;
  - every record has a `kind` and the original fields in `raw`;
  - records serialise to JSON as they are.
- **The API's quirks are handled:**
  - per-endpoint record keys;
  - the forms "no results" takes;
  - errors sent as text where records belong;
  - two timestamp formats;
  - `yes`/`No`/`NO` flags;
  - v2 search sending numbers where everything else sends strings.
- **Safe by default:**
  - a rate limiter matched to your key, with retries for 429s, 5xx and network errors;
  - a 30-second timeout;
  - API keys never appear in errors or cache keys.
- **Identical calls in flight share one request**, and a per-call event hook is available for logging.
- **Helpers for common tasks** that use v2 with a premium key and v1 with a free key.
- **No dependencies.** It uses the built-in `fetch`, so it runs on Node 18+, Deno, Bun and browsers. Written in TypeScript and shipped as ES modules with type declarations.

> Not affiliated with TheSportsDB. Read their [terms](https://www.thesportsdb.com/docs_terms_of_use.php) before publishing an app; artwork that isn't Creative Commons may not be used in published apps (see `Player.creativeCommons`).
>
> **In a browser, any key you use is visible to your users**, and v1 puts it in the URL. Call the API from a server if your key is a paid one.

## Install

Not published yet: until the first release, build it from this folder (see [Development](#development)).

```sh
npm install thesportsdb-client
```

## Quick start

```js
import { SportsDb, sized } from "thesportsdb-client";

const db = new SportsDb();                                 // the free key "123" ("3" also works): v1 only
const arsenal = await db.v1.lookup.team(133604);
console.log(arsenal.name, sized(arsenal.badge, "tiny"));
const table = await db.v1.lookup.table(4328);              // Premier League standings
```

```ts
import { SportsDb, InMemoryResponseCache } from "thesportsdb-client";

const db = new SportsDb({ apiKey: process.env.THESPORTSDB_API_KEY, cache: new InMemoryResponseCache() });
const league = await db.v2.lookup.league(4328);
const season = await db.v2.schedule.leagueSeason(4328, league!.currentSeason!);   // ~380 games, one call
const tv = await db.v2.tv.country("Canada");                                       // about a week of listings
```

The method groups follow the API: `db.v1.search`, `.lookup`, `.list`, `.schedule`, `.tv`, `.video`, `.live`, and `db.v2.search`, `.lookup`, `.list`, `.all`, `.schedule`, `.tv`, `.live`. Each method's doc comment names the endpoint it calls and the free key's limit. Names match the Kotlin library; the full list, the record keys and the free/premium counts are in [`../docs/ENDPOINTS.md`](https://github.com/RohanG27/thesportsdbLibrary/blob/main/docs/ENDPOINTS.md). JS has no overloading, so Kotlin's `leagues(country)` and `channel(id)` are `leaguesInCountry()` and `channelId()` here, and optional parameters are passed as an options object: `db.v1.schedule.day("2026-10-04", { sport: "Soccer" })`. Days can be a `Date` (its UTC date is used) or a `"YYYY-MM-DD"` string.

## Helpers

`db.helpers` picks v2 with a premium key and v1 with a free key, so the same code works with either.

```js
const h = db.helpers;
await h.currentSeason(4328);                      // "2026-2027"
await h.seasonEvents(4328);                       // the current season's fixtures
await h.roundEvents(4328, 7);                     // one matchday
await h.upcomingLeagueEvents(4328, 7);
await h.recentLeagueResults(4328);
await h.teamSchedule(133604);                     // past and future, all competitions
await h.eventsOnLocalDate("2026-10-04", "America/Toronto", { sport: "Ice Hockey" });
await h.liveScores({ leagueId: 4328 });
await h.leagueTeams(4328);
await h.eventChannels(2494052);
await h.tvListings("Canada", { sport: "Ice Hockey", days: 3 });
```

- `eventsOnLocalDate` exists because the API files events under their UTC date, so evening games in the Americas land on the next UTC day. It fetches every UTC day the local day overlaps and keeps the right events. Time zones are IANA names, and the offsets come from the built-in `Intl` API.
- `roundEvents` has two routes because `eventsround.php` is undocumented: it returns a whole round to the free keys but HTTP 404 to premium keys. With a premium key, the helper filters the season instead.

## Models

- Records are frozen plain objects with a `kind` (`"Team"`, `"Event"`, …) and `raw`. `sameRecord(a, b)` is true when both are the same kind of record built from the same API fields.
- **Missing values are `null`** (or empty arrays and objects). Ids and years of `0` mean unknown, and become `null` too.
- **Times:**
  - `timestamp` fields are `Date`s (UTC instants);
  - date-only fields (`date`, `born`, …) are `"YYYY-MM-DD"` strings and times of day (`time`, `localTime`) are `"HH:MM:SS"`, because JS `Date` handles date-only values badly;
  - `Event.date` and `time` are UTC; `localDate` and `localTime` are the venue's.
- **Status:** `event.statusCode` is the raw code; `event.status` is `"NOT_STARTED"`, `"IN_PLAY"`, `"FINISHED"`, `"INTERRUPTED"`, …, covering every sport's documented codes. Older events often have no code.
- **Stages:** `event.stage` reads stage codes in `round` (200 = final, 500 = pre-season…) as `"FINAL"`, `"PRE_SEASON"`, …, or `null` for an ordinary round.
- To test your own code, fake the HTTP layer with a `Transport` rather than building records.

## Errors

All errors extend `SportsDbError`:

| Error | When |
|---|---|
| `InvalidApiKeyError` | HTTP 400 for the key. Only `123`, `3` and paid keys work. |
| `PremiumRequiredError` | A v2 call with a free key, before any request. |
| `RateLimitError` | HTTP 429 after the retry; `retryAfterMs` if the server said. |
| `HttpStatusError` | Any other HTTP error, after retries for 5xx. |
| `NetworkError` | No response at all, after retries (including timeouts). |
| `ResponseParseError` | The body wasn't a TheSportsDB envelope (e.g. an HTML page). |
| `ApiMessageError` | The API answered with a message instead of data, e.g. a rejected parameter. |

"No results" is not an error. You get an empty array, or `null` for a single lookup. Invalid argument combinations reject with a `RangeError` before any request.

## Options

```ts
new SportsDb({
  apiKey: "...",
  requestsPerMinute: 30,          // default: 30 for free keys, 100 otherwise; 0 = off
  maxRetries: 2, retryBackoffMs: 500,
  retryOnRateLimit: true, rateLimitWaitMs: 60_000,
  cache: new InMemoryResponseCache(),          // or your own ResponseCache (methods may be async: Redis, KV...)
  cachePolicy: { medium: 10 * 60_000 },        // ms per kind of data
  timeoutMs: 30_000,
  deduplicateRequests: true,                   // identical calls in flight share one request
  requestListener: (e) => console.debug(e.url, e.status, `${e.durationMs}ms`, e.shared ? "shared" : ""),
  transport: undefined,                        // your own Transport (default: fetch)
});
```

Create one client per key and share it. Its rate limiter, cache and de-duplication cover every call made through it. Durations here are in milliseconds, as is usual in JavaScript; the other libraries use seconds.

## Development

Run from `javascript/`:

```sh
npm install
npm test                    # offline tests against recorded responses
npm run test:live           # the real API (free key; set THESPORTSDB_API_KEY for v2)
npm run typecheck && npm run build
node test/smoke-node18.mjs  # the built package with plain Node (LIVE=1 adds real API calls); CI runs it on Node 18
npm run generate            # regenerate src/models.ts after changing the Kotlin models
npm run sync-fixtures       # copy re-recorded fixtures from the Kotlin project
```

Vitest needs Node 22.12+ (it fails to start on Node 18), while the library supports Node 18+. So `test/smoke-node18.mjs` checks the built package on Node 18 without a test framework: parsing, time zones via `Intl`, de-duplication, the timeout and, with `LIVE=1`, real calls. Verified on Node 18.20.8.

`src/models.ts` is generated from `../kotlin/src/main/kotlin/io/github/rohang27/thesportsdb/model/`, and a test fails if it's stale. Behaviour shared with the other libraries is tracked in [`../docs/LIBRARY-PARITY.md`](https://github.com/RohanG27/thesportsdbLibrary/blob/main/docs/LIBRARY-PARITY.md).

## License

MIT. See [LICENSE](LICENSE). TheSportsDB's data and artwork are covered by [its own terms](https://www.thesportsdb.com/docs_terms_of_use.php).
