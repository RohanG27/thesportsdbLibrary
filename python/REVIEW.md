# Review: TralahM/thesportsdb (Python), 5 Oct 2026

Repository: https://github.com/TralahM/thesportsdb, reviewed at `master` (v0.3.1, last commit 6 Dec 2025).
GPL-3.0 licensed; 43 stars, 12 forks, 1 open issue. Every finding below was checked with a real call using the free key, against `../docs/THESPORTSDB-API-BEHAVIOUR.md`.

## Summary

A thin wrapper around v1: about 45 functions, each building one URL and returning `response.json()`. It works for the simple cases. But **7 of those functions are broken today** (wrong parameter names, removed endpoints, or an endpoint that returns another league's data), there is no v2 support at all, and errors are never reported. Its 13 tests mock the HTTP call and only check the URL built, so none of them can catch these bugs.

## Broken functions (verified with live calls)

| Function | Calls | Problem | Fix |
|---|---|---|---|
| `events.eventLineups(id)` | `lookuplineups.php` | **HTTP 404**, an HTML page | `lookuplineup.php` (no `s`) |
| `search.searchVenue(name)` | `searchvenues.php?t=` | empty body: wrong parameter | `v=` |
| `events.leagueSeasonRoundEvents(...)` | `eventsround.php?l=&s=&r=` | `{"events":"Invalid League ID or no round passed"}` | `id=` instead of `l=` |
| `leagues.listSeasons(id)` | `search_all_seasons.php?l=` | `{"seasons":"Invalid League ID passed"}` | `id=` |
| `teams.leagueTeams(id)`, `leagues.listTeams(id)` | `lookup_all_teams.php?id=` (undocumented) | **another league's teams**: `id=4328` (Premier League) returns 24 English League One clubs | `search_all_teams.php?l={league name}` |
| `search.searchPlayersByTeam(name)` | `searchplayers.php?t=` | empty body | `lookup_all_players.php?id={idTeam}` |
| `search.searchTeamByCode(code)` | `searchteams.php?sname=` | empty body | none; the API dropped short-code search |

`settings.py` also defines `EVENTS_TVT = "/eventsvs.php"`, which returns 404, but no function uses it.

## Other problems

**Keys and requests**
- **The default key is `"3"`.** It still works: it's a second free key, with the same limits as `123`. But the docs only mention `123`, so the default could stop working without notice. Keys `1`, `2`, `4` and `50` are rejected with HTTP 400.
- **The key is read once, at import** (`API_KEY = os.getenv(...)` in `settings.py`). Changing it later means mutating module state; there's no client object and no per-call key.
- **No error handling.** HTTP status is never checked. A 400 (bad key), 404 (HTML page), 429 (rate limit) or 5xx comes back as a dict, or as a raw HTML/text string when the body isn't JSON (`make_request` falls back to `response.content.decode()`). Callers can't tell results from errors.
- **No timeout** on `requests.get`, so a stalled connection hangs forever.
- **No rate limiting, retries or caching**, although the API documents 30 requests a minute for free keys.

**Coverage**
- **No v2.** Premium users can't send the `X-API-KEY` header, and get none of v2's schedules, TV filters or live scores.
- **Missing v1 endpoints:** TV listings (`eventstv.php`), highlights (`eventshighlights.php`), player stats (`lookupplayerstats.php`), player results (`playerresults.php`), team equipment (`lookupequipment.php`), and live scores (`livescore.php`). Some of these have a URL constant in `settings.py` but no function.

**Results**
- **Raw results only.** Every function returns the raw JSON envelope, so callers must know each endpoint's record key. That includes `results` for `eventslast.php` and `countries` for `search_all_leagues.php`.
- Callers also need to know the three forms of "no results" (`null`, empty body, `{"Message":...}`) and the new **error-as-a-string form** (`{"seasons":"Invalid League ID passed"}`). Values stay strings.
- **3,400 lines of embedded data** in `settings.py` (sports, leagues, and the maps between them) were captured in 2020 and are stale. `sports.sportInfo` and `leagues.sportLeagues` answer from this data, not from the API.

**Naming, packaging and docs**
- **Mixed naming:** camelCase functions (`leagueSeasonEvents`) and a function shadowing a builtin parameter name (`round`). Ids are typed `str`, and there are no return types.
- **Packaging contradicts itself:**
  - The `LICENSE` file is GPL-3.0, but the setup.cfg classifier says MIT, and its `licence =` key is misspelled, so the license field is never set.
  - It claims Python 3.6+, but no CI runs the tests on any version.
  - It uses `setup.py`/`setup.cfg` rather than `pyproject.toml`.
- **Docs:** the module docstring for `events` promises "next 15 events"; the free key returns 1.

## What's worth keeping

- The module split (`events`, `leagues`, `players`, `search`, `teams`, `countries`, `sports`) is easy to navigate.
- Recent commits are tidy (a changelog, conventional commits), and the maintainer merged an outside fix in Nov 2025, so contributions are accepted.
- It has users: anyone importing `thesportsdb.events.leagueSeasonEvents` should keep working after an update.

## Licensing note

The project is GPL-3.0. Fixes contributed upstream would be GPL-3.0. Code copied from it into another project would make that project GPL-3.0 too. The Kotlin library (`../`) contains none of its code, and nothing here has been copied.
