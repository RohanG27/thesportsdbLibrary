# Known issues

Places where the API behaves differently from its older documentation, or in surprising ways. All were measured on 5 Oct 2026.

## Limits

- **v2 "Limit" values aren't the real limits.**
  - Every v2 lookup is documented with "Limit: 1", but list-like lookups return many records: `lookup/player_stats` returned 315, `lookup/event_lineup` 22.
  - `filter/tv/day` (287) and `filter/tv/sport` (189) returned more than their documented 100.
  - `search/team` returned 12 against a documented 10.
  - `schedule/next/league` and `schedule/previous/league` returned 20 against 10.
- **Some free-key results are lower than documented:** `all_leagues.php` returned 5 (documented 10), `search_all_leagues.php` 5 (10), `eventsseason.php` 5 (15).
- **The free keys get fewer fields from `all_leagues.php`:** `strLeagueAlternate` is missing.

## Keys

- **A second free key, `3`, still works**, with the same limits as `123`.
- **Wrong keys get HTTP 400**, with a "premium" message even on v1. The old example keys `1` and `2` no longer work.

## Responses

- **Record keys don't always match their content:** `eventslast.php` returns events under `results`; `search_all_leagues.php` returns leagues under `countries`.
- **"No results" has three forms** (`null`, an empty body, `{"Message":"No data found"}`), all with HTTP 200.
- **Rejected parameters come back with HTTP 200** and the error text where the records belong: `{"seasons":"Invalid League ID passed"}`.
- **v2 search sends ids as numbers.** Every other endpoint sends text.

## Endpoints

- **`eventstv.php` with `a=` (country) but no `s=` (sport) returns an empty body.** `eventstv.php?id=` takes a channel id, not an event id.
- **v2 `search/event` needs the exact stored event name.** v1 `searchevents.php` matches looser input such as `Arsenal_vs_Chelsea`.
- **`livescore.php` (v1) is undocumented** but works. The free keys get the full live feed (51 games, the same as premium), and `l=` is ignored.

## Legacy endpoints

These aren't in the current documentation. They answer the free keys but **return HTTP 404 (an HTML page) to premium keys**, so an app built on the free key can break when it upgrades.

| Endpoint | Free keys | Premium | Use instead |
|---|---|---|---|
| `eventsround.php?id=&r=&s=` | the whole round | 404 | v2 `schedule/league/{id}/{season}`, filtered by `intRound` |
| `lookup_all_teams.php?id=` | **wrong league**: id 4328 (Premier League) returned 24 League One clubs | 404 | `search_all_teams.php?l={league name}`, or v2 `list/teams/{id}` |
| `searchloves.php?u=` | works | 404 | — |

Removed endpoints that return 404 to every key: `lookuplineups.php` (use `lookuplineup.php`) and `eventsvs.php`. These searches no longer answer: `searchteams.php?sname=` (short code) and `searchplayers.php?t=` (by team) both return an empty body. Use `lookup_all_players.php?id={teamId}` for a squad.
