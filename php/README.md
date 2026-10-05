# sportsdb-php

A PHP client for [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2. It's the PHP sibling of the Kotlin library (`../`) and the Python library (`../python/`): same behaviour, same lessons, tested against the same recorded responses.

- **Every documented endpoint**, plus the useful undocumented ones, one method each, for v1 and v2.
- **Typed, read-only models**, generated from the Kotlin models, so all three libraries have the same fields:
  - values are parsed leniently, so a bad value becomes `null` instead of failing the call;
  - timestamps are UTC `DateTimeImmutable`s;
  - the original fields stay available in `$record->raw`.
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
  - API keys never appear in exceptions or cache keys.
- **Helpers for common tasks** that use v2 with a premium key and v1 with a free key.
- Optional caching (in memory, or any PSR-16 cache), and a per-call event hook for logging.
- **No runtime dependencies.** PHP 8.2+. The default HTTP transport uses `ext-curl`; implement `Transport` to use a PSR-18 client or anything else. PHPStan level 8 clean.

> Not affiliated with TheSportsDB. Read their [terms](https://www.thesportsdb.com/docs_terms_of_use.php) before publishing an app; artwork that isn't Creative Commons may not be used in published apps (see `Player::$creativeCommons`).

## Quick start

```php
use SportsDb\SportsDb;
use SportsDb\Model\ImageSize;

$db = new SportsDb();                                   // the free key "123" ("3" also works): v1 only
$arsenal = $db->v1->lookup->team(133604);
echo $arsenal->name, ' ', ImageSize::Tiny->of($arsenal->badge);
$table = $db->v1->lookup->table(4328);                  // Premier League standings
```

```php
use SportsDb\Cache\InMemoryResponseCache;

$db = new SportsDb(apiKey: getenv('THESPORTSDB_API_KEY'), cache: new InMemoryResponseCache());
$league = $db->v2->lookup->league(4328);
$season = $db->v2->schedule->leagueSeason(4328, $league->currentSeason);  // ~380 games, one call
$tv = $db->v2->tv->country('Canada');                                     // about a week of listings
```

The method groups follow the API: `$db->v1->search`, `->lookup`, `->list`, `->schedule`, `->tv`, `->video`, `->live`, and `$db->v2->search`, `->lookup`, `->list`, `->all`, `->schedule`, `->tv`, `->live`. Each method's docblock names the endpoint it calls and the free key's limit. Names match the Kotlin library; the full list, the record keys and the free/premium counts are in [`../docs/ENDPOINTS.md`](../docs/ENDPOINTS.md). PHP has no overloading, so Kotlin's `leagues(country)` and `channel(id)` are `leaguesInCountry()` and `channelId()` here.

## Helpers

`$db->helpers` picks v2 with a premium key and v1 with a free key, so the same code works with either.

```php
$h = $db->helpers;
$h->currentSeason(4328);                         // "2026-2027"
$h->seasonEvents(4328);                          // the current season's fixtures
$h->roundEvents(4328, round: 7);                 // one matchday
$h->upcomingLeagueEvents(4328, days: 7);
$h->recentLeagueResults(4328);
$h->teamSchedule(133604);                        // past and future, all competitions
$h->eventsOnLocalDate('2026-10-04', new DateTimeZone('America/Toronto'), sport: 'Ice Hockey');
$h->liveScores(leagueId: 4328);
$h->leagueTeams(4328);
$h->eventChannels(2494052);
$h->tvListings('Canada', sport: 'Ice Hockey', days: 3);
```

- `eventsOnLocalDate` exists because the API files events under their UTC date, so evening games in the Americas land on the next UTC day. It fetches every UTC day the local day overlaps and keeps the right events.
- `roundEvents` has two routes because `eventsround.php` is undocumented: it returns a whole round to the free keys but HTTP 404 to premium keys. With a premium key, the helper filters the season instead.

## Models

- Records are `readonly` classes extending `ApiRecord`:
  - `$a == $b` (or `$a->equals($b)`) is true when the API sent the same fields;
  - `(string) $team` is a short summary, `Team(id=133604, name=Arsenal)`;
  - `$team->raw` holds every original field.
- **Missing values are `null`** (or empty arrays). Ids and years of `0` mean unknown, and become `null` too.
- **Times:**
  - `timestamp` properties are UTC `DateTimeImmutable`s;
  - date-only fields (`date`, `born`, …) are `DateTimeImmutable`s at midnight UTC;
  - times of day (`time`, `localTime`) are `HH:MM:SS` strings.
- **Status:** `$event->statusCode` is the raw code; `$event->status()` is an `EventStatus` (`NotStarted`, `InPlay`, `Finished`, `Interrupted`, …), covering every sport's documented codes. Older events often have no code.
- **Stages:** `$event->stage()` reads stage codes in `round` (200 = final, 500 = pre-season…) as a `RoundStage`, or `null` for an ordinary round.
- To test your own code, fake the HTTP layer with a `Transport` rather than building records.

## Errors

All exceptions extend `SportsDb\Exception\SportsDbException`:

| Exception | When |
|---|---|
| `InvalidApiKeyException` | HTTP 400 for the key. Only `123`, `3` and paid keys work. |
| `PremiumRequiredException` | A v2 call with a free key, before any request. |
| `RateLimitException` | HTTP 429 after the retry; `$retryAfter` in seconds if given. |
| `HttpStatusException` | Any other HTTP error, after retries for 5xx. |
| `NetworkException` | No response at all, after retries (including timeouts). |
| `ResponseParseException` | The body wasn't a TheSportsDB envelope (e.g. an HTML page). |
| `ApiMessageException` | The API answered with a message instead of data, e.g. a rejected parameter. |

"No results" is not an error. You get an empty array, or `null` for a single lookup. Invalid argument combinations throw `InvalidArgumentException` before any request.

## Settings

```php
new SportsDb(
    apiKey: '...',
    requestsPerMinute: null,     // default: 30 for free keys, 100 otherwise; 0 = off
    maxRetries: 2, retryBackoff: 0.5,
    retryOnRateLimit: true, rateLimitWait: 60.0,
    cache: new InMemoryResponseCache(),                  // or new Psr16ResponseCache($anyPsr16Cache)
    cachePolicy: new CachePolicy(medium: 600),
    timeout: 30.0,
    requestListener: fn (RequestEvent $e) => $logger->debug("$e->url $e->status {$e->duration}s"),
    transport: null,             // your own Transport (e.g. wrapping a PSR-18 client)
);
```

PHP calls are synchronous, so there's nothing in flight to de-duplicate: unlike the Kotlin and Python clients, this one has no `deduplicateRequests`. The rate limiter and in-memory cache last as long as the client object. To share them across web requests, use a PSR-16 cache (e.g. Redis or APCu) with `Psr16ResponseCache`.

## Development

Run from `php/`:

```sh
composer install
vendor/bin/phpunit                          # offline tests against recorded responses
vendor/bin/phpunit --group live             # the real API (free key; set THESPORTSDB_API_KEY for v2)
vendor/bin/phpstan analyse
python3 tools/gen_models.py                 # regenerate src/Model/ after changing the Kotlin models
php tools/sync_fixtures.php                 # copy re-recorded fixtures from the Kotlin project
```

`src/Model/` is generated from `../src/main/kotlin/sportsdb/model/` (except the hand-written `ApiRecord`, `Socials`, `LeagueRef`, `PlayerExternalIds`, `EventStatus` and `ImageSize`), and a test fails if it's stale. Behaviour shared with the other libraries is tracked in [`../docs/LIBRARY-PARITY.md`](../docs/LIBRARY-PARITY.md).

## License

MIT. See [LICENSE](LICENSE). TheSportsDB's data and artwork are covered by [its own terms](https://www.thesportsdb.com/docs_terms_of_use.php).
