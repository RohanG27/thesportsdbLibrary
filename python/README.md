# sportsdb-python

A Python client for [TheSportsDB](https://www.thesportsdb.com) API, v1 and v2. It's the Python twin of the Kotlin library in this repository (`../`): same behaviour, same lessons, tested against the same recorded responses.

- **Every documented endpoint**, plus the useful undocumented ones, one method each, for v1 and v2.
- **Blocking and async:** `SportsDB` and `AsyncSportsDB` (asyncio or trio) share one implementation. The async code is the source, and the blocking client is generated from it.
- **Typed, read-only models.** The API sends everything as strings; values here are parsed leniently, so a bad value becomes `None` instead of failing the call. Timestamps are timezone-aware UTC `datetime`s. The original fields stay available in `raw`.
- **The API's quirks are handled:**
  - per-endpoint record keys;
  - the forms "no results" takes;
  - errors sent as text where records belong (`{"seasons":"Invalid League ID passed"}`);
  - two timestamp formats;
  - `yes`/`No`/`NO` flags;
  - v2 search sending numbers where everything else sends strings.
- **Safe by default:**
  - a rate limiter matched to your key, with retries for 429s, 5xx and network errors;
  - a 30-second timeout;
  - API keys never appear in errors or cache keys.
- **Helpers for common tasks** that use v2 with a premium key and v1 with a free key.
- Optional caching, de-duplication of identical calls in flight, and a per-call event hook for logging.
- Only dependency: `httpx`. Python 3.10+. Fully typed: `py.typed`, strict mypy.

> Not affiliated with TheSportsDB. Read their [terms](https://www.thesportsdb.com/docs_terms_of_use.php) before publishing an app; artwork that isn't Creative Commons may not be used in published apps (see `Player.creative_commons`).

## Quick start

```python
from sportsdb import SportsDB, ImageSize, sized

db = SportsDB()                                    # the free key "123" ("3" also works): v1 only
arsenal = db.v1.lookup.team(133604)
print(arsenal.name, sized(arsenal.badge, ImageSize.TINY))
table = db.v1.lookup.table(4328)                   # Premier League standings
```

```python
import os
from sportsdb import AsyncSportsDB, InMemoryResponseCache

async with AsyncSportsDB(os.environ["THESPORTSDB_API_KEY"], cache=InMemoryResponseCache()) as db:
    league = await db.v2.lookup.league(4328)
    season = await db.v2.schedule.league_season(4328, league.current_season)   # ~380 games, one call
    tv = await db.v2.tv.country("Canada")                                      # about a week of listings
```

The method names follow the API's groups: `db.v1.search`, `.lookup`, `.list`, `.schedule`, `.tv`, `.video`, `.live`, and `db.v2.search`, `.lookup`, `.list`, `.all`, `.schedule`, `.tv`, `.live`. Each method's docstring names the endpoint it calls and the free key's limit. The full list, the record keys and the free/premium counts are in [`../docs/ENDPOINTS.md`](../docs/ENDPOINTS.md); the Python names are the snake_case versions of the Kotlin ones. Python has no overloading, so two Kotlin overloads get their own names here: `leagues_in_country` and `channel_id`.

## Helpers

`db.helpers` picks v2 with a premium key and v1 with a free key, so the same code works with either.

```python
from datetime import date
from zoneinfo import ZoneInfo

h = db.helpers
h.current_season(4328)                         # "2026-2027"
h.season_events(4328)                          # the current season's fixtures
h.round_events(4328, round=7)                  # one matchday
h.upcoming_league_events(4328, days=7)
h.recent_league_results(4328)
h.team_schedule(133604)                        # past and future, all competitions
h.events_on_local_date(date.today(), ZoneInfo("America/Toronto"), sport="Ice Hockey")
h.live_scores(league_id=4328)
h.league_teams(4328)
h.event_channels(2494052)
h.tv_listings("Canada", sport="Ice Hockey", days=3)
```

**Why `events_on_local_date`:** the API files events under their UTC date, so evening games in North America land on the next UTC day. The helper fetches every UTC day the local day overlaps and keeps the right events.

**Why `round_events` has two routes:** `eventsround.php` is undocumented. It returns a whole round to the free keys but HTTP 404 to premium keys, so with a premium key the helper filters the season schedule instead.

## Models

- Every model is a frozen, keyword-only dataclass extending `ApiRecord`:
  - two records are equal when the API sent the same fields;
  - `repr` is short (`Team(id=133604, name='Arsenal')`);
  - `raw` holds every original field.
- Each field's comment names the API field it comes from, e.g. `short_name: str | None  # strTeamShort`.
- **Missing values are `None`** (or empty tuples/dicts). Ids and years of `0` mean unknown, and become `None` too.
- **Times:**
  - `Event.timestamp`, `date` and `time` are UTC; `local_date` and `local_time` are the venue's.
  - `TvListing.timestamp` is UTC too (the API writes it as `strTimeStamp`, with a space).
- **Status:** `event.status_code` is the raw code; `event.status` is an `EventStatus` (`NOT_STARTED`, `IN_PLAY`, `FINISHED`, …). Older events often have no code.
- To test your own code, fake the HTTP layer with a transport rather than building models.

## Errors

All errors extend `SportsDBError`:

| Error | When |
|---|---|
| `InvalidApiKeyError` | HTTP 400 for the key. Only `123`, `3` and paid keys work. |
| `PremiumRequiredError` | A v2 call with a free key, before any request. |
| `RateLimitError` | HTTP 429 after the retry; `retry_after` in seconds if given. |
| `HTTPStatusError` | Any other HTTP error, after retries for 5xx. |
| `NetworkError` | No response at all, after retries (including timeouts). |
| `ResponseParseError` | The body wasn't a TheSportsDB envelope (e.g. an HTML page). |
| `ApiMessageError` | The API answered with a message instead of data, e.g. a rejected parameter. |

"No results" is not an error. You get an empty list, or `None` for a single lookup. Invalid argument combinations raise `ValueError` before any request.

## Settings

```python
SportsDB(
    api_key,
    requests_per_minute=None,     # default: 30 for free keys, 100 otherwise; 0 = off
    max_retries=2, retry_backoff=0.5,
    retry_on_rate_limit=True, rate_limit_wait=60.0,
    cache=InMemoryResponseCache(), cache_policy=CachePolicy(medium=600),
    timeout=30.0,
    deduplicate_requests=True,    # identical calls in flight share one request
    request_listener=lambda e: log.debug("%s %s %.2fs", e.url, e.status, e.duration),
    transport=None,               # your own Transport / AsyncTransport (e.g. a shared httpx client)
)
```

Create one client per key and share it. Its rate limiter, cache and de-duplication cover every call made through it. Close it when done (`with` / `async with`, or `close()` / `aclose()`).

## Development

Run everything from `python/`. If your Python lacks `ensurepip`, create the venv with `python3 -m venv --without-pip .venv` and install with `pip --python .venv/bin/python …`.

```sh
pip --python .venv/bin/python install -e '.[dev]'
.venv/bin/pytest                     # offline tests against recorded responses
.venv/bin/pytest -m live             # the real API (free key; set THESPORTSDB_API_KEY for v2)
.venv/bin/mypy && .venv/bin/ruff check src tests tools
python tools/unasync.py              # regenerate src/sportsdb/_sync/ after editing src/sportsdb/_async/
python tools/sync_fixtures.py        # copy re-recorded fixtures from the Kotlin project
```

`src/sportsdb/_async/` is the source of truth. `_sync/` is generated, except the hand-written `_compat.py` on each side, and a test fails if it's out of date. Behaviour shared with the Kotlin library is tracked in [`../docs/LIBRARY-PARITY.md`](../docs/LIBRARY-PARITY.md).
