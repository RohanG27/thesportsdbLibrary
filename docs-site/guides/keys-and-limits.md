# Keys and limits

## Keys

| Key | v1 | v2 | Results |
|---|---|---|---|
| `123` | yes | no | small (see below) |
| `3` | yes | no | the same as `123` (an older free key that still works) |
| a premium key | yes | yes | full |
| anything else | HTTP 400 | HTTP 400 | — |

A key that isn't accepted gets **HTTP 400** with:

```json
{"Message": "Invalid Premium API key: Signup here: https://www.thesportsdb.com/pricing"}
```

**Is a key premium?** Call v2 `lookup/league/4328` with it. A premium key gets HTTP 200; any other key gets HTTP 400.

### Where the key goes

- **v1** puts it in the URL path: `/api/v1/json/{key}/…`. A v1 URL is therefore a secret: don't log it, put it in error messages, or use it as a cache key.
- **v2** takes it in the `X-API-KEY` header. With a premium key, prefer v2 where both versions offer what you need.

## How many results

The free keys return a few records per call. Premium keys return everything, up to each endpoint's documented limit.

| Request | Free keys | Premium |
|---|---|---|
| `eventsday.php` (all events on a day) | 3 | 901 |
| `eventsseason.php` (a season) | 5 | 380 |
| `eventsround.php` (a round) | 10 | HTTP 404 (see [Known issues](known-issues.html)) |
| `lookuptable.php` (league table) | 5 | 20 |
| `searchteams.php?t=Arsenal` | 1 | 15 |
| `all_leagues.php` | 5 | 1547 |
| `eventsnext.php` / `eventslast.php` (a team) | 1, home games only | 10 / 5 |
| `eventstv.php?d=…` (TV on a day) | 1 | 287 |
| `livescore.php?s=Soccer` | **the full feed** | the full feed |

Every endpoint in the reference lists both counts, measured on 5 Oct 2026, next to its documented limit. One endpoint returns **fewer fields** to the free keys, not just fewer records: `all_leagues.php` omits `strLeagueAlternate`.

## Rate limit

| Plan | Requests per minute |
|---|---|
| Free | 30 |
| Premium | 100 |
| Business | 120 |

Over the limit, the API answers **HTTP 429**. Wait a minute before retrying, or less if the response has a `Retry-After` header.

The best approach is to stay under the limit on your side: keep a sliding window of request times and wait when it's full. Cache what doesn't change often (see [Recipes](recipes.html#caching)).
