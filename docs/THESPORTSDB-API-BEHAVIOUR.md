# TheSportsDB API: measured behaviour (v1 and v2)

How TheSportsDB's API really behaves, measured with real calls on **5 Oct 2026**, using the public free key `123` for v1 and a premium key for v1 and v2. It is written for anyone building a client library or documentation, and is not tied to any app or language.

- Official documentation: https://www.thesportsdb.com/documentation
- Terms of use: https://www.thesportsdb.com/docs_terms_of_use.php
- **Evidence:** every count and field list here comes from the recorded responses in `kotlin/src/test/resources/fixtures/`: `v1-free/`, `v1-premium/` and `v2/`, one JSON file per call. Re-record them with `tools/record-fixtures.sh`.
- **Counts change.** Schedules, TV listings and live scores change from hour to hour, so treat counts as "about this many on that day". Documented limits are quoted from the official page on the same date.
- No API key appears in this document or in the recordings.

## 1. Basics

| | v1 | v2 |
|---|---|---|
| Base URL | `https://www.thesportsdb.com/api/v1/json/{key}/{endpoint}.php?…` | `https://www.thesportsdb.com/api/v2/json/{group}/{name}/{param}` |
| Key | **in the URL path**, so a v1 URL is a secret and must never be logged | `X-API-KEY` header |
| Free key | `123` (public, for development and testing). The older key **`3` also works**, with the same limits. | none: v2 is premium only |
| Premium key | works here too, with much larger results (section 3) | the only way in |
| Wrong key | HTTP **400**, `{"Message":"Invalid Premium API key: Signup here: …"}`. Only `123`, `3` and paid keys work; `1`, `2`, `4`, `50` and `1234` were rejected. | the same, for a free, wrong or missing key |
| Rate limit (documented) | free 30/min, premium 100/min, business 120/min; over the limit: HTTP 429, wait a minute | the same |
| Parameters | query string; spaces as `%20` or `_` (`English_Premier_League`) | path segments; spaces as `%20` |

**Telling a premium key from a free one:** call v2 `lookup/league/4328`. A premium key gets HTTP 200 with a `lookup` array; any other key gets HTTP 400.

## 2. Conventions (both versions)

- **The envelope.** Every response is a JSON object with **one top-level key**, whose value is an array of records. The key name depends on the endpoint (sections 3 and 4), and some are surprising: `eventslast.php` uses `results` for events, and `search_all_leagues.php` uses `countries` for leagues. v2 always uses the endpoint group as the key: `search`, `lookup`, `list`, `filter`, `all`, `schedule`, `livescore`.
- **"No results" takes several forms. Treat all of them as an empty result, not an error:**
  - v1: the key with `null`, e.g. `{"events":null}`.
  - v1: an **empty body**, with HTTP 200. Seen from `eventstv.php?d=…&a=Canada` without `s=`.
  - v2: `{"Message":"No data found"}`, with HTTP 200.
- **A rejected parameter is not "no results".** v1 answers HTTP 200 with the error **as text in place of the records**: `{"seasons":"Invalid League ID passed"}` (`search_all_seasons.php?l=` instead of `id=`), or `{"events":"Invalid League ID or no round passed"}`. Treat a string under the record key as an error.
- **Values are strings.** Numbers, ids, dates and flags arrive as strings (`"idEvent":"2494052"`, `"intHomeScore":"2"`), or as `null`. Exception: **v2 `search/…` sends ids as JSON numbers** (`"idTeam":133604`). Parse both.
- **Blank strings mean "no value".** Many fields are `""` rather than `null` (`strTwitter`, `strTimeLocal`, `strLeague2`…). Treat `""` like `null`.
- **Zero sometimes means unknown:** `intFormedYear` `"0"`, `idCup` `"0"` (meaning "not a cup").
- **Flags are inconsistent text:**
  - Yes/no fields: `strPostponed` `no`/`yes`; `strHome` and `strSubstitute` `Yes`/`No`; `strComplete` `yes`/`No`; `strCreativeCommons` `Yes`/`No`/`NO`.
  - `strLocked` is `locked`/`unlocked`.
  - Compare without regard to case.
- **Lists are comma-separated text:** `strTeamAlternate` (`Arsenal Football Club, AFC, Arsenal FC`), `strKeywords`, `strLeagueAlternate`.
- **Numbered fields** stand in for arrays: `idLeague`, `idLeague2` … `idLeague7` (with `strLeague…`), `strFanart1`–`strFanart4`, `strColour1`–`strColour3`, and `strDescriptionEN`, `strDescriptionDE`, … (15 languages for teams, leagues and players).
- **Times:**
  - `strTimestamp` (events, schedules, live scores) is **UTC**, in ISO format without a zone: `2026-10-10T11:30:00`.
  - `dateEvent` and `strTime` are UTC as well. `dateEventLocal` and `strTimeLocal` are the venue's local time, and are often `null` or `""` for future events.
  - **TV listings use `strTimeStamp`** (capital S) with a **space**: `2026-10-10 11:30:00`, also UTC.
  - `strEventTime` (live scores) is `HH:mm`.
  - `dateUpdated` (tables), `updated` (live scores) and `date` (equipment) are `yyyy-MM-dd HH:mm:ss` with no stated zone.
- **Status codes** (`strStatus`) are listed per sport in TheSportsDB's [data documentation](https://www.thesportsdb.com/docs_api_data):
  - Not started: `NS`, `TBD`.
  - In play: `1H`, `HT`, `2H`, `ET`, `BT`, `P`, `PT`, `Q1`–`Q4`, `OT`, `P1`–`P3`, `SO`, `IN1`–`IN9`, `S1`–`S5`.
  - Finished: `FT`, `AET`, `PEN`, `AOT`, `AP`. Awarded: `AWD`, `AW`, `WO`.
  - Postponed: `PST` and `POST`. Suspended or interrupted: `SUSP`, `INT`, `INTR`. Cancelled: `CANC`. Abandoned: `ABD`.
  - The same state has different codes in different sports (postponed is `PST` in soccer and `POST` elsewhere).
  - Seen in the recordings: `NS`, `1H`, `HT`, `2H`, `BT`, `OT`, `Q2`–`Q4`, `P3`, `FT`, `AOT`, `AP`, `PEN`, `AWD`, `PST`, `CANC`.
  - `strProgress` (live scores) is the minute, or `Final`.
  - **Older events often have no `strStatus` at all**, even with a final score (e.g. event 441613).
- **Stage codes in `intRound`:** 125 quarter-final, 150 semi-final, 160 playoff, 170 playoff semi-final, 180 playoff final, 200 final, 400 qualifier, 500 pre-season (from the data documentation). Confirmed in the recordings: the 2017 FA Cup final has `200`, the May 2026 EFL play-off finals `180`, the EFL Cup semi-final `150`, and NBA pre-season games `500`. A value of `226` (a Russian lower-league game) is not a listed code, so treat unlisted values as round numbers.
- **Images** are served from `r2.thesportsdb.com`. Append `/medium`, `/small` or `/tiny` for a smaller copy. Measured for one badge: original 128 KB, `/medium` 92 KB, `/small` 46 KB, `/tiny` 14 KB.
- **Image rights.** Under the terms, artwork whose `strCreativeCommons` is not "Yes" must not be used in published apps.
- **Live scores can be stale.** The live feed still listed a game from 4 Oct, in its third period, when it was recorded on 5 Oct. Check `updated` before treating a score as live.

## 3. v1 endpoints

"Free" and "Premium" are the records returned in one recorded call. "Documented" is the official page's free/premium limit. Every row has a recorded response in the fixtures.

### Search

| Endpoint | Record key | Free | Premium | Documented | Notes |
|---|---|---|---|---|---|
| `searchteams.php?t=Arsenal` | `teams` | 1 | 15 | 1 / 100 | the free key finds only "Arsenal" itself |
| `searchevents.php?e=Arsenal_vs_Chelsea` | `event` | 1 | 10 | 1 / 10 | |
| `searchevents.php?e=…&s=2016-2017` | `event` | 1 | 2 | 1 / 10 | narrows by season |
| `searchevents.php?e=Arsenal_vs_Chelsea&d=2015-04-26` | `event` | 1 | 1 | 1 / 10 | narrows by date |
| `searchevents.php?f=English_Premier_League_2015-04-26_Arsenal_vs_Chelsea` | `event` | 1 | 1 | 1 / 10 | the same result as `searchfilename.php` |
| `searchfilename.php?e=English_Premier_League_2015-04-26_Arsenal_vs_Chelsea` | `event` | 1 | 1 | 1 / 10 | `strFilename` is `{league} {date} {home} vs {away}` |
| `searchfilename.php?e=…&s=2014-2015` | `event` | 1 | 1 | 1 / 10 | |
| `searchplayers.php?p=Danny_Welbeck` | `player` | 1 | 1 | 1 / 10 | summary fields plus `relevance` |
| `searchvenues.php?v=Wembley` | `venues` | 1 | 2 | 1 / 10 | |

### Lookup

| Endpoint | Record key | Free | Premium | Documented | Notes |
|---|---|---|---|---|---|
| `lookupleague.php?id=4328` | `leagues` | 1 | 1 | 1 / 1 | |
| `lookuptable.php?l=4328` | `table` | 5 | 20 | 5 / 100 | standings; only some leagues (mostly featured soccer) |
| `lookuptable.php?l=4328&s=2024-2025` | `table` | 5 | 20 | 5 / 100 | a past season's table |
| `lookupteam.php?id=133604` | `teams` | 1 | 1 | 1 / 1 | |
| `lookupequipment.php?id=133597` | `equipment` | 2 | 18 | 2 / 100 | kits by season |
| `lookupplayer.php?id=34145937` | `players` | 1 | 1 | 1 / 1 | |
| `lookuphonours.php?id=34147178` | `honours` | 5 | 5 | 5 / 500 | |
| `lookupformerteams.php?id=34147178` | `formerteams` | 5 | 6 | 5 / 100 | |
| `lookupmilestones.php?id=34161397` | `milestones` | 3 | 3 | 5 / 100 | |
| `lookupcontracts.php?id=34147178` | `contracts` | 1 | 1 | 1 / 100 | |
| `playerresults.php?id=34160573` | `results` | 5 | 24 | 5 / 500 | individual sports (races, golf…) |
| `lookupplayerstats.php?id=34146304` | `playerstats` | 10 | 315 | 10 / 10000 | one record per statistic per season |
| `lookupevent.php?id=441613` | `events` | 1 | 1 | 1 / 1 | |
| `eventresults.php?id=652890` | `results` | 5 | 22 | 5 / 100 | every competitor in an individual-sport event |
| `lookuplineup.php?id=1032723` | `lineup` | 5 | 22 | 5 / 100 | |
| `lookuptimeline.php?id=1032718` | `timeline` | 5 | 10 | 5 / 100 | goals, cards, substitutions |
| `lookupeventstats.php?id=1032723` | `eventstats` | 5 | 16 | 5 / 100 | home vs away team stats |
| `lookuptv.php?id=2494052` | `tvevent` | 2 | 13 | 2 / 100 | the channels showing an event |
| `lookupvenue.php?id=16163` | `venues` | 1 | 1 | 1 / 1 | |

### Lists

| Endpoint | Record key | Free | Premium | Documented | Notes |
|---|---|---|---|---|---|
| `all_sports.php` | `sports` | 2 | 37 | 2 / 50 | |
| `all_countries.php` | `countries` | 50 | 256 | 50 / 500 | `name_en` and `flag_url_32` only |
| `all_leagues.php` | `leagues` | 5 | 1547 | 10 / 3000 | id, name, sport; `strLeagueAlternate` **only with a premium key** |
| `search_all_leagues.php?c=England&s=Soccer` | **`countries`** | 5 | 25 | 10 / 100 | full league records; `s=` is optional |
| `search_all_seasons.php?id=4328` | `seasons` | 5 | 35 | 5 / 500 | `strSeason` only |
| `search_all_seasons.php?id=…&poster=1` | `seasons` | 5 | 35 | 5 / 500 | adds `strPoster` |
| `search_all_seasons.php?id=…&badge=1` | `seasons` | 5 | 35 | 5 / 500 | adds `strBadge` |
| `search_all_seasons.php?id=…&description=1` | `seasons` | 5 | 35 | 5 / 500 | adds `strDescriptionEN` |
| `search_all_teams.php?l=English_Premier_League` | `teams` | 10 | 20 | 10 / 3000 | by league **name** |
| `search_all_teams.php?s=Soccer&c=Spain` | `teams` | 10 | 729 | 10 / 3000 | |
| `lookup_all_players.php?id=133604` | `player` | 10 | 27 | 10 / 3000 | a team's squad |

### Schedules

| Endpoint | Record key | Free | Premium | Documented | Notes |
|---|---|---|---|---|---|
| `eventsnext.php?id=133602` | `events` | 1 | 10 | 1 / 10 | team; free key: home games only (documented) |
| `eventslast.php?id=133602` | **`results`** | 1 | 5 | 1 / 10 | team; the records are events |
| `eventsnextleague.php?id=4328` | `events` | 1 | 20 | 1 / 20 | |
| `eventspastleague.php?id=4328` | `events` | 1 | 15 | 1 / 20 | |
| `eventsday.php?d=2026-10-04` | `events` | 3 | 901 | 3 / 1500 | every event that (UTC) day |
| `eventsday.php?d=2026-10-04&s=Ice_Hockey` | `events` | 3 | 97 | 3 / 1500 | |
| `eventsday.php?d=2026-10-04&l=4328` | `events` | null | null | 3 / 1500 | no games that day; `l=` takes an id or a name |
| `eventsseason.php?id=4328&s=2026-2027` | `events` | 5 | 380 | 15 / 3000 | **a whole season in one call**; fewer fields than a lookup |

### TV, video and live

| Endpoint | Record key | Free | Premium | Documented | Notes |
|---|---|---|---|---|---|
| `eventstv.php?d=2026-10-05` | `tvevents` | 1 | 287 | 1 / 1500 | every listing that day |
| `eventstv.php?d=2026-10-05&s=Ice_Hockey` | `tvevents` | 1 | 23 | 1 / 1500 | |
| `eventstv.php?d=2026-10-05&a=Canada&s=Ice_Hockey` | `tvevents` | 1 | 4 | 1 / 1500 | country **and** sport |
| `eventstv.php?d=2026-10-05&a=Canada` | — | empty body | empty body | — | **a country without a sport returns an empty body** |
| `eventstv.php?c=TSN_1` | `tvevents` | 1 | 3 | 1 / 1500 | by channel name |
| `eventstv.php?id=8631` | `tvevents` | 1 | 4 | 1 / 1500 | by **channel** id (`idChannel`), not event id |
| `eventshighlights.php?d=2026-10-04` | `tvhighlights` | 2 | 50 | 2 / 50 | the premium result hit the documented cap |
| `eventshighlights.php?d=2026-10-04&l=4328` | `tvhighlights` | null | null | 2 / 50 | no Premier League games that day |
| `eventshighlights.php?d=2026-10-04&s=Soccer` | `tvhighlights` | 2 | 27 | 2 / 50 | |
| `livescore.php?s=Soccer` | `livescore` | 19 | 19 | *undocumented* | **the free key gets the full live feed** |
| `livescore.php?l=4328` | `livescore` | 51 | 51 | *undocumented* | **ignores `l=`**: 51 games from 20 leagues, none of them league 4328 |

### Undocumented legacy endpoints

These aren't in the current documentation. They **answer the free keys but return HTTP 404 (an HTML page) to premium keys.** Older client libraries still call them.

| Endpoint | Record key | Free | Premium | Notes |
|---|---|---|---|---|
| `eventsround.php?id=4328&r=1&s=2026-2027` | `events` | 10 | 404 | a whole round; `l=` instead of `id=` gives `{"events":"Invalid League ID or no round passed"}` |
| `lookup_all_teams.php?id=4328` | `teams` | 24 | 404 | **wrong data:** the Premier League's id returned 24 English League One clubs. Use `search_all_teams.php?l={name}` |
| `searchloves.php?u={user}` | | works | 404 | a user's loved teams and players |
| `lookuplineups.php`, `eventsvs.php` | | 404 | 404 | removed; use `lookuplineup.php` |
| `searchteams.php?sname=ARS` | | empty body | | short-code search no longer answers |
| `searchplayers.php?t={team}` | | empty body | | players by team name no longer answers; use `lookup_all_players.php?id=` |

## 4. v2 endpoints (premium key)

| Endpoint | Record key | Records | Documented limit | Notes |
|---|---|---|---|---|
| `search/league/English%20Premier%20League` | `search` | 2 | 10 | summary fields; ids are JSON numbers |
| `search/team/Arsenal` | `search` | 12 | 10 | |
| `search/player/Danny%20Welbeck` | `search` | 1 | 10 | |
| `search/event/Arsenal%20vs%20Chelsea` | `Message` | No data found | 10 | needs the exact event name |
| `search/venue/Wembley` | `search` | 3 | 10 | |
| `lookup/league/4328` | `lookup` | 1 | 1 | |
| `lookup/team/133604` | `lookup` | 1 | 1 | |
| `lookup/team_equipment/133597` | `lookup` | 18 | 1 | |
| `lookup/player/34145937` | `lookup` | 1 | 1 | |
| `lookup/player_contracts/34147178` | `lookup` | 1 | 1 | |
| `lookup/player_results/34160573` | `lookup` | 24 | 1 | |
| `lookup/player_honours/34147178` | `lookup` | 5 | 1 | |
| `lookup/player_milestones/34161397` | `lookup` | 3 | 1 | |
| `lookup/player_teams/34147178` | `lookup` | 6 | 1 | v1's `formerteams` |
| `lookup/player_stats/34146304` | `lookup` | 315 | 1 | |
| `lookup/event/441613` | `lookup` | 1 | 1 | |
| `lookup/event_lineup/1032723` | `lookup` | 22 | 1 | adds `strFormation`, `strPositionShort` (both null in this event) |
| `lookup/event_results/652890` | `lookup` | 22 | 1 | |
| `lookup/event_stats/1032723` | `lookup` | 16 | 1 | |
| `lookup/event_timeline/1032718` | `lookup` | 10 | 1 | |
| `lookup/event_tv/2494052` | `lookup` | 13 | 1 | |
| `lookup/event_highlights/441613` | `lookup` | 1 | 1 | event fields; `strVideo` empty for this event |
| `lookup/venue/16163` | `lookup` | 1 | 1 | |
| `list/teams/4328` | `list` | 20 | 100 | badges and colours, **no alternate names** |
| `list/seasons/4328` | `list` | 35 | 100 | with badge, poster, description |
| `list/players/133604` | `list` | 27 | 100 | |
| `list/seasonposters/4328` | `list` | 9 | — | every season poster/badge with `idArt` and the uploader; **only in the official OpenAPI file**, not the HTML docs |
| `filter/tv/day/2026-10-05` | `filter` | 287 | 100 | every listing worldwide that day |
| `filter/tv/country/Canada` | `filter` | 90 | 100 | about a week of a country's listings |
| `filter/tv/sport/Ice%20Hockey` | `filter` | 189 | 100 | |
| `filter/tv/channel/TSN%201` | `filter` | 3 | 100 | the API's spelling (`TSN 1`, not `TSN1`); partial names match |
| `filter/tv/channelid/8631` | `filter` | 4 | 100 | |
| `all/countries` | `all` | 256 | 500 | adds `code`, `name_fr`, 16/32/64 px flags |
| `all/sports` | `all` | 37 | 500 | |
| `all/leagues` | `all` | 1547 | 3000 | |
| `schedule/next/league/4328` | `schedule` | 20 | 10 | |
| `schedule/previous/league/4328` | `schedule` | 20 | 10 | |
| `schedule/next/team/133604` | `schedule` | 10 | 10 | |
| `schedule/previous/team/133604` | `schedule` | 10 | 10 | |
| `schedule/next/venue/16163` | `schedule` | 3 | 10 | |
| `schedule/previous/venue/16163` | `schedule` | 10 | 10 | |
| `schedule/full/team/133604` | `schedule` | 48 | 250 | past and future, all competitions |
| `schedule/league/4328/2026-2027` | `schedule` | 380 | 3000 | **the whole season**; the season name comes from `strCurrentSeason` |
| `livescore/soccer` | `livescore` | 17 | 100 | |
| `livescore/4328` | `Message` | No data found | 100 | the league had no game in play |
| `livescore/all` | `livescore` | 54 | 500 | every sport |

v2 has **no league tables**; use v1 `lookuptable.php` with a premium key.

## 5. Where the official documentation and the API disagree

Useful for client authors and for anyone improving the official docs.


1. **v2 "Limit" values are not the actual limits.**
   - Every v2 lookup is documented as "Limit: 1", but list-like lookups return many records: 315 player stats, 22 lineup entries, 18 kits.
   - `filter/tv/day` (287) and `filter/tv/sport` (189) exceed their documented 100.
   - `search/team` returned 12 against a documented 10.
   - `schedule/next/league` and `schedule/previous/league` returned 20 against 10.
2. **Some free-key results are lower than documented:** `all_leagues.php` (5 vs 10), `search_all_leagues.php` (5 vs 10), `eventsseason.php` (5 vs 15).
3. **`livescore.php` (v1) is undocumented** but works. The free key receives the full live feed (51 games, the same as premium), and `l=` is ignored.
4. **`eventstv.php`:**
   - With `a=` (country) and no `s=` (sport), it returns an empty body rather than an error or results.
   - `id=` is a channel id, not an event id. For an event's channels, use `lookuptv.php`.
5. **Record keys don't always match their content.** `eventslast.php` returns events under `results`; `search_all_leagues.php` returns leagues under `countries`. The docs don't list record keys at all.
6. **Type inconsistency:** v2 `search/…` sends ids as JSON numbers, while every other endpoint sends strings.
7. **v2 `search/event`** needs the exact stored event name. v1 `searchevents.php` matches looser input such as `Arsenal_vs_Chelsea`.
8. **Empty results** take three forms (section 2), and none of them is documented.
9. **The free key gets fewer fields from `all_leagues.php`:** `strLeagueAlternate` is missing. Elsewhere, the free key returns the same fields as premium, just fewer records. The docs mention only the record limits.
10. **Undocumented legacy endpoints still answer free keys but 404 for premium keys** (`eventsround.php`, `lookup_all_teams.php`, `searchloves.php`), and `lookup_all_teams.php` returns the wrong league. An app that works on the free key can break when it upgrades.
11. **Rejected parameters come back as HTTP 200** with the error text where the records belong (section 2), not as an error status or a `Message`.
12. **A second free key, `3`, still works.** The documentation mentions only `123`.
13. **Wrong keys get HTTP 400**, with a "premium" message even on v1. Old example keys such as `1` no longer work.

## 5b. TheSportsDB's own OpenAPI files, and its roadmap

TheSportsDB publishes OpenAPI descriptions at `https://www.thesportsdb.com/api/spec/v1/openapi.yaml` and `/api/spec/v2/openapi.yaml`, plus Postman collections linked from the documentation page. Compared with the API's actual behaviour (5 Oct 2026):
- **They miss endpoints that work and are documented in HTML:** v1 `searchfilename.php`, `playerresults.php` and `lookupplayerstats.php`; v2 `lookup/player_results`, `lookup/player_stats` and `filter/tv/channelid`. They also miss the `eventstv.php` parameters `c=` and `id=`, and the `search_all_seasons.php` option `description=1`.
- **They have one endpoint the HTML docs don't:** v2 `list/seasonposters/{idLeague}` (recorded: 9 posters for the Premier League).
- **They list fields the API no longer sends:** `idSoccerXML`, `intStadiumCapacity`, `strTweet2`, `strTweet3`, `intEventScore`, `intEventScoreTotal`. None appear in any recorded response.
- **The v1 file is titled "Search API"** and describes only team search in its summary.

Data quality, from the recordings and the project's bug list:
- **An event's `strLeague` can be an old name for its `idLeague`:** 16 events carry `Colombia Categoría Primera A` under id 4497, which the league list calls `Colombian Liga DIMAYOR`. Match leagues by id, not by name. A related open bug: Champions League events whose league fields show the home team's league.
- **`strWeather` is rarely filled:** 1 of 901 events on 4 Oct 2026.
- **An open bug: NFL live scores break at 00:00 UTC.** Events crossing midnight UTC are where date handling goes wrong.

Planned changes, from the public roadmap (trello.com/b/PMumZYQg, v2 "todo" list). Clients should tolerate them:
- **Format changes:** standardised event times and dates; normalised null versus empty strings; removal of `strFilename`; merged lookup and search methods.
- **New data:** a round endpoint; head-to-head; team honours; collections; latest transfers; all players of a sport (with paging); one call grouping lineups, stats and timelines.

## 6. Fields of each record type

<!-- FIELDS:BEGIN -->
All fields returned, alphabetical, from the recorded responses. Endpoints that return the same shape
are grouped. `strDescription{XX}` stands for one field per language code.

### v1 (premium key; the free key returns the same fields, except that `all_leagues.php` omits `strLeagueAlternate`)

- `searchteams.php`, `lookupteam.php`, `search_all_teams.php`, `lookup_all_teams.php` (63 fields): `idAPIfootball`, `idESPN`, `idLeague`, `idLeague2`, `idLeague3`, `idLeague4`, `idLeague5`, `idLeague6`, `idLeague7`, `idTeam`, `idVenue`, `intFormedYear`, `intLoved`, `strBadge`, `strBanner`, `strColour1`, `strColour2`, `strColour3`, `strCountry`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strDivision`, `strEquipment`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strInstagram`, `strKeywords`, `strLeague`, `strLeague2`, `strLeague3`, `strLeague4`, `strLeague5`, `strLeague6`, `strLeague7`, `strLocation`, `strLocked`, `strLogo`, `strRSS`, `strSport`, `strStadium`, `strTeam`, `strTeamAlternate`, `strTeamShort`, `strTwitter`, `strWebsite`, `strYoutube`
- `searchevents.php`, `searchfilename.php`, `lookupevent.php`, `eventsnext.php`, `eventslast.php`, `eventsnextleague.php`, `eventspastleague.php`, `eventsday.php`, `eventsround.php` (49 fields): `dateEvent`, `dateEventLocal`, `idAPIfootball`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idVenue`, `intAwayScore`, `intAwayScoreExtra`, `intHomeScore`, `intHomeScoreExtra`, `intRound`, `intScore`, `intScoreVotes`, `intSpectators`, `strAwayTeam`, `strAwayTeamBadge`, `strBanner`, `strCity`, `strCountry`, `strDescriptionEN`, `strEvent`, `strEventAlternate`, `strFanart`, `strFilename`, `strGroup`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strLeagueBadge`, `strLocked`, `strMap`, `strOfficial`, `strPoster`, `strPostponed`, `strResult`, `strSeason`, `strSport`, `strSquare`, `strStatus`, `strThumb`, `strTime`, `strTimeLocal`, `strTimestamp`, `strTweet1`, `strVenue`, `strVideo`, `strWeather`
- `searchplayers.php` (13 fields): `dateBorn`, `idPlayer`, `idTeam`, `relevance`, `strCutout`, `strGender`, `strNationality`, `strPlayer`, `strPosition`, `strSport`, `strStatus`, `strTeam`, `strThumb`
- `searchvenues.php`, `lookupvenue.php` (29 fields): `idDupe`, `idVenue`, `intCapacity`, `intFormedYear`, `intLoved`, `strArchitect`, `strCost`, `strCountry`, `strCreativeCommons`, `strDescriptionEN`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strInstagram`, `strLocation`, `strLocked`, `strLogo`, `strMap`, `strSport`, `strThumb`, `strTimezone`, `strTwitter`, `strVenue`, `strVenueAlternate`, `strVenueSponsor`, `strWebsite`, `strYoutube`
- `lookupleague.php`, `search_all_leagues.php` (47 fields): `dateFirstEvent`, `idAPIfootball`, `idAPIfootballv3`, `idCup`, `idLeague`, `intDivision`, `intFormedYear`, `strBadge`, `strBanner`, `strComplete`, `strCountry`, `strCurrentSeason`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strInstagram`, `strLeague`, `strLeagueAlternate`, `strLocked`, `strLogo`, `strNaming`, `strPoster`, `strRSS`, `strSport`, `strTrophy`, `strTvRights`, `strTwitter`, `strWebsite`, `strYoutube`
- `lookuptable.php` (20 fields): `dateUpdated`, `idLeague`, `idStanding`, `idTeam`, `intDraw`, `intGoalDifference`, `intGoalsAgainst`, `intGoalsFor`, `intLoss`, `intPlayed`, `intPoints`, `intRank`, `intWin`, `strBadge`, `strDescription`, `strForm`, `strGroup`, `strLeague`, `strSeason`, `strTeam`
- `lookupequipment.php` (7 fields): `date`, `idEquipment`, `idTeam`, `strEquipment`, `strSeason`, `strType`, `strUsername`
- `lookupplayer.php`, `lookup_all_players.php` (71 fields): `dateBorn`, `dateDied`, `dateSigned`, `idAPIfootball`, `idESPN`, `idGoogle`, `idPlayer`, `idPlayerManager`, `idTeam`, `idTeam2`, `idTeamNational`, `idTransferMkt`, `idWikidata`, `intLoved`, `intSoccerXMLTeamID`, `strAgent`, `strBanner`, `strBirthLocation`, `strCartoon`, `strCollege`, `strCreativeCommons`, `strCreativeCommonsAttribution`, `strCutout`, `strDeathLocation`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strEthnicity`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strHeight`, `strInstagram`, `strKit`, `strLastName`, `strLocked`, `strNationality`, `strNumber`, `strOutfitter`, `strPlayer`, `strPlayerAlternate`, `strPosition`, `strPoster`, `strRender`, `strSide`, `strSigning`, `strSport`, `strStatus`, `strTeam`, `strTeam2`, `strThumb`, `strTwitter`, `strWage`, `strWebsite`, `strWeight`, `strYoutube`
- `lookuphonours.php` (13 fields): `id`, `idHonour`, `idLeague`, `idPlayer`, `idTeam`, `strHonour`, `strHonourLogo`, `strHonourTrophy`, `strPlayer`, `strSeason`, `strSport`, `strTeam`, `strTeamBadge`
- `lookupformerteams.php` (12 fields): `id`, `idFormerTeam`, `idPlayer`, `intAppearances`, `intGoals`, `strBadge`, `strDeparted`, `strFormerTeam`, `strJoined`, `strMoveType`, `strPlayer`, `strSport`
- `lookupmilestones.php` (10 fields): `dateMilestone`, `id`, `idMilestone`, `idPlayer`, `idTeam`, `strMilestone`, `strMilestoneLogo`, `strPlayer`, `strSport`, `strTeam`
- `lookupcontracts.php` (10 fields): `id`, `idPlayer`, `idTeam`, `strBadge`, `strPlayer`, `strSport`, `strTeam`, `strWage`, `strYearEnd`, `strYearStart`
- `playerresults.php`, `eventresults.php` (14 fields): `dateEvent`, `idEvent`, `idPlayer`, `idResult`, `idTeam`, `intPoints`, `intPosition`, `strCountry`, `strDetail`, `strEvent`, `strPlayer`, `strResult`, `strSeason`, `strSport`
- `lookupplayerstats.php` (13 fields): `id`, `idLeague`, `idPlayer`, `idTeam`, `strLeague`, `strLeagueBadge`, `strPlayer`, `strSeason`, `strSport`, `strStatistic`, `strTeam`, `strTeamBadge`, `strValue`
- `lookuplineup.php` (13 fields): `idEvent`, `idLineup`, `idPlayer`, `idTeam`, `intSquadNumber`, `strCutout`, `strHome`, `strPlayer`, `strPosition`, `strRender`, `strSubstitute`, `strTeam`, `strThumb`
- `lookuptimeline.php` (19 fields): `dateEvent`, `idAPIfootball`, `idAssist`, `idEvent`, `idPlayer`, `idTeam`, `idTimeline`, `intTime`, `strAssist`, `strComment`, `strCutout`, `strEvent`, `strHome`, `strPeriod`, `strPlayer`, `strSeason`, `strTeam`, `strTimeline`, `strTimelineDetail`
- `lookupeventstats.php` (7 fields): `idApiFootball`, `idEvent`, `idStatistic`, `intAway`, `intHome`, `strEvent`, `strStat`
- `lookuptv.php`, `eventstv.php` (18 fields): `dateEvent`, `id`, `idChannel`, `idEvent`, `intDivision`, `strChannel`, `strCountry`, `strEvent`, `strEventBanner`, `strEventCountry`, `strEventPoster`, `strEventSquare`, `strEventThumb`, `strLogo`, `strSeason`, `strSport`, `strTime`, `strTimeStamp`
- `all_sports.php` (7 fields): `idSport`, `strFormat`, `strSport`, `strSportDescription`, `strSportIconGreen`, `strSportThumb`, `strSportThumbBW`
- `all_countries.php` (2 fields): `flag_url_32`, `name_en`
- `all_leagues.php` (4 fields): `idLeague`, `strLeague`, `strLeagueAlternate`, `strSport`
- `search_all_seasons.php` (1 field): `strSeason`
- `search_all_seasons.php?…&poster=1` (2 fields): `strPoster`, `strSeason`
- `eventsseason.php` (30 fields): `dateEvent`, `dateEventLocal`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `intAwayScore`, `intHomeScore`, `intRound`, `strAwayTeam`, `strAwayTeamBadge`, `strCountry`, `strEvent`, `strEventAlternate`, `strFilename`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strLeagueBadge`, `strPoster`, `strPostponed`, `strSeason`, `strSport`, `strStatus`, `strThumb`, `strTime`, `strTimeLocal`, `strTimestamp`, `strVenue`, `strVideo`
- `eventshighlights.php` (10 fields): `idEvent`, `idLeague`, `strEvent`, `strFanart`, `strLeague`, `strPoster`, `strSeason`, `strSport`, `strThumb`, `strVideo`
- `livescore.php` (20 fields): `dateEvent`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idLiveScore`, `intAwayScore`, `intDivision`, `intHomeScore`, `strAwayTeam`, `strAwayTeamBadge`, `strEventTime`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strProgress`, `strSport`, `strStatus`, `strTimestamp`, `updated`
- `search_all_seasons.php?…&badge=1` (2 fields): `strBadge`, `strSeason`
- `search_all_seasons.php?…&description=1` (2 fields): `strDescriptionEN`, `strSeason`

### v2

- `search/league/English%20Premier%20League` (7 fields): `idLeague`, `strBadge`, `strCountry`, `strCurrentSeason`, `strGender`, `strLeague`, `strSport`
- `search/team/Arsenal` (9 fields): `idLeague`, `idTeam`, `strBadge`, `strCountry`, `strGender`, `strLeague`, `strLocation`, `strSport`, `strTeam`
- `search/player/Danny%20Welbeck` (7 fields): `dateBorn`, `idPlayer`, `idTeam`, `strPlayer`, `strSport`, `strTeam`, `strThumb`
- `search/venue/Wembley` (6 fields): `idVenue`, `strCountry`, `strLocation`, `strSport`, `strThumb`, `strVenue`
- `lookup/league/4328` (47 fields): `dateFirstEvent`, `idAPIfootball`, `idAPIfootballv3`, `idCup`, `idLeague`, `intDivision`, `intFormedYear`, `strBadge`, `strBanner`, `strComplete`, `strCountry`, `strCurrentSeason`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strInstagram`, `strLeague`, `strLeagueAlternate`, `strLocked`, `strLogo`, `strNaming`, `strPoster`, `strRSS`, `strSport`, `strTrophy`, `strTvRights`, `strTwitter`, `strWebsite`, `strYoutube`
- `lookup/team/133604` (63 fields): `idAPIfootball`, `idESPN`, `idLeague`, `idLeague2`, `idLeague3`, `idLeague4`, `idLeague5`, `idLeague6`, `idLeague7`, `idTeam`, `idVenue`, `intFormedYear`, `intLoved`, `strBadge`, `strBanner`, `strColour1`, `strColour2`, `strColour3`, `strCountry`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strDivision`, `strEquipment`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strInstagram`, `strKeywords`, `strLeague`, `strLeague2`, `strLeague3`, `strLeague4`, `strLeague5`, `strLeague6`, `strLeague7`, `strLocation`, `strLocked`, `strLogo`, `strRSS`, `strSport`, `strStadium`, `strTeam`, `strTeamAlternate`, `strTeamShort`, `strTwitter`, `strWebsite`, `strYoutube`
- `lookup/team_equipment/133597` (7 fields): `date`, `idEquipment`, `idTeam`, `strEquipment`, `strSeason`, `strType`, `strUsername`
- `lookup/player/34145937` (71 fields): `dateBorn`, `dateDied`, `dateSigned`, `idAPIfootball`, `idESPN`, `idGoogle`, `idPlayer`, `idPlayerManager`, `idTeam`, `idTeam2`, `idTeamNational`, `idTransferMkt`, `idWikidata`, `intLoved`, `intSoccerXMLTeamID`, `strAgent`, `strBanner`, `strBirthLocation`, `strCartoon`, `strCollege`, `strCreativeCommons`, `strCreativeCommonsAttribution`, `strCutout`, `strDeathLocation`, `strDescription{XX}` (CN, DE, EN, ES, FR, HU, IL, IT, JP, NL, NO, PL, PT, RU, SE), `strEthnicity`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strGender`, `strHeight`, `strInstagram`, `strKit`, `strLastName`, `strLocked`, `strNationality`, `strNumber`, `strOutfitter`, `strPlayer`, `strPlayerAlternate`, `strPosition`, `strPoster`, `strRender`, `strSide`, `strSigning`, `strSport`, `strStatus`, `strTeam`, `strTeam2`, `strThumb`, `strTwitter`, `strWage`, `strWebsite`, `strWeight`, `strYoutube`
- `lookup/player_contracts/34147178` (10 fields): `id`, `idPlayer`, `idTeam`, `strBadge`, `strPlayer`, `strSport`, `strTeam`, `strWage`, `strYearEnd`, `strYearStart`
- `lookup/player_results/34160573`, `lookup/event_results/652890` (14 fields): `dateEvent`, `idEvent`, `idPlayer`, `idResult`, `idTeam`, `intPoints`, `intPosition`, `strCountry`, `strDetail`, `strEvent`, `strPlayer`, `strResult`, `strSeason`, `strSport`
- `lookup/player_honours/34147178` (13 fields): `id`, `idHonour`, `idLeague`, `idPlayer`, `idTeam`, `strHonour`, `strHonourLogo`, `strHonourTrophy`, `strPlayer`, `strSeason`, `strSport`, `strTeam`, `strTeamBadge`
- `lookup/player_milestones/34161397` (10 fields): `dateMilestone`, `id`, `idMilestone`, `idPlayer`, `idTeam`, `strMilestone`, `strMilestoneLogo`, `strPlayer`, `strSport`, `strTeam`
- `lookup/player_teams/34147178` (12 fields): `id`, `idFormerTeam`, `idPlayer`, `intAppearances`, `intGoals`, `strBadge`, `strDeparted`, `strFormerTeam`, `strJoined`, `strMoveType`, `strPlayer`, `strSport`
- `lookup/player_stats/34146304` (13 fields): `id`, `idLeague`, `idPlayer`, `idTeam`, `strLeague`, `strLeagueBadge`, `strPlayer`, `strSeason`, `strSport`, `strStatistic`, `strTeam`, `strTeamBadge`, `strValue`
- `lookup/event/441613`, `lookup/event_highlights/441613` (49 fields): `dateEvent`, `dateEventLocal`, `idAPIfootball`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idVenue`, `intAwayScore`, `intAwayScoreExtra`, `intHomeScore`, `intHomeScoreExtra`, `intRound`, `intScore`, `intScoreVotes`, `intSpectators`, `strAwayTeam`, `strAwayTeamBadge`, `strBanner`, `strCity`, `strCountry`, `strDescriptionEN`, `strEvent`, `strEventAlternate`, `strFanart`, `strFilename`, `strGroup`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strLeagueBadge`, `strLocked`, `strMap`, `strOfficial`, `strPoster`, `strPostponed`, `strResult`, `strSeason`, `strSport`, `strSquare`, `strStatus`, `strThumb`, `strTime`, `strTimeLocal`, `strTimestamp`, `strTweet1`, `strVenue`, `strVideo`, `strWeather`
- `lookup/event_lineup/1032723` (17 fields): `idAPIfootball`, `idEvent`, `idLineup`, `idPlayer`, `idTeam`, `intSquadNumber`, `strCountry`, `strCutout`, `strEvent`, `strFormation`, `strHome`, `strPlayer`, `strPosition`, `strPositionShort`, `strSeason`, `strSubstitute`, `strTeam`
- `lookup/event_stats/1032723` (7 fields): `idApiFootball`, `idEvent`, `idStatistic`, `intAway`, `intHome`, `strEvent`, `strStat`
- `lookup/event_timeline/1032718` (19 fields): `dateEvent`, `idAPIfootball`, `idAssist`, `idEvent`, `idPlayer`, `idTeam`, `idTimeline`, `intTime`, `strAssist`, `strComment`, `strCutout`, `strEvent`, `strHome`, `strPeriod`, `strPlayer`, `strSeason`, `strTeam`, `strTimeline`, `strTimelineDetail`
- `lookup/event_tv/2494052`, `filter/tv/day/2026-10-05`, `filter/tv/country/Canada`, `filter/tv/channel/TSN%201`, `filter/tv/sport/Ice%20Hockey`, `filter/tv/channelid/8631` (18 fields): `dateEvent`, `id`, `idChannel`, `idEvent`, `intDivision`, `strChannel`, `strCountry`, `strEvent`, `strEventBanner`, `strEventCountry`, `strEventPoster`, `strEventSquare`, `strEventThumb`, `strLogo`, `strSeason`, `strSport`, `strTime`, `strTimeStamp`
- `lookup/venue/16163` (29 fields): `idDupe`, `idVenue`, `intCapacity`, `intFormedYear`, `intLoved`, `strArchitect`, `strCost`, `strCountry`, `strCreativeCommons`, `strDescriptionEN`, `strFacebook`, `strFanart1`, `strFanart2`, `strFanart3`, `strFanart4`, `strInstagram`, `strLocation`, `strLocked`, `strLogo`, `strMap`, `strSport`, `strThumb`, `strTimezone`, `strTwitter`, `strVenue`, `strVenueAlternate`, `strVenueSponsor`, `strWebsite`, `strYoutube`
- `list/teams/4328` (14 fields): `idLeague`, `idTeam`, `strBadge`, `strBanner`, `strColour1`, `strColour2`, `strColour3`, `strCountry`, `strEquipment`, `strFanart1`, `strLeague`, `strLogo`, `strTeam`, `strTeamShort`
- `list/seasons/4328` (4 fields): `strBadge`, `strDescriptionEN`, `strPoster`, `strSeason`
- `list/seasonposters/4328` (7 fields): `idArt`, `idLeague`, `strBadge`, `strDescriptionEN`, `strPoster`, `strSeason`, `strUsername`
- `list/players/133604` (9 fields): `dateBorn`, `idPlayer`, `idTeam`, `strCutout`, `strPlayer`, `strPosition`, `strRender`, `strTeam`, `strThumb`
- `all/countries` (7 fields): `code`, `flag_url_16`, `flag_url_32`, `flag_url_64`, `idAPIfootball`, `name_en`, `name_fr`
- `all/sports` (7 fields): `idSport`, `strFormat`, `strSport`, `strSportDescription`, `strSportIconGreen`, `strSportThumb`, `strSportThumbBW`
- `schedule/next/league/4328`, `schedule/league/4328/2026-2027`, `schedule/previous/league/4328`, `schedule/next/team/133604`, `schedule/previous/team/133604`, `schedule/next/venue/16163`, `schedule/previous/venue/16163` (31 fields): `dateEvent`, `dateEventLocal`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idVenue`, `intAwayScore`, `intHomeScore`, `intRound`, `strAwayTeam`, `strAwayTeamBadge`, `strCountry`, `strEvent`, `strEventAlternate`, `strFilename`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strLeagueBadge`, `strPoster`, `strPostponed`, `strSeason`, `strSport`, `strStatus`, `strThumb`, `strTime`, `strTimeLocal`, `strTimestamp`, `strVenue`, `strVideo`
- `schedule/full/team/133604` (27 fields): `dateEvent`, `dateEventLocal`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idVenue`, `intAwayScore`, `intHomeScore`, `intRound`, `strAwayTeam`, `strAwayTeamBadge`, `strCountry`, `strEvent`, `strFilename`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strPoster`, `strPostponed`, `strSport`, `strStatus`, `strThumb`, `strTime`, `strTimeLocal`, `strTimestamp`, `strVenue`
- `livescore/all`, `livescore/soccer` (20 fields): `dateEvent`, `idAwayTeam`, `idEvent`, `idHomeTeam`, `idLeague`, `idLiveScore`, `intAwayScore`, `intDivision`, `intHomeScore`, `strAwayTeam`, `strAwayTeamBadge`, `strEventTime`, `strHomeTeam`, `strHomeTeamBadge`, `strLeague`, `strProgress`, `strSport`, `strStatus`, `strTimestamp`, `updated`
- `all/leagues` (4 fields): `idLeague`, `strLeague`, `strLeagueAlternate`, `strSport`
<!-- FIELDS:END -->

## 7. Choosing an endpoint

| Task | Free key (v1) | Premium key |
|---|---|---|
| A league's current season name | `lookupleague.php?id=` → `strCurrentSeason` | `lookup/league/{id}` → `strCurrentSeason` |
| A whole season's fixtures | `eventsseason.php` (5 for free) | `schedule/league/{id}/{season}`: one call |
| The next N days of a league | `eventsday.php?d=…&l={id}`, once per day (3 per day for free) | the season call, filtered by date |
| Recent results | `eventspastleague.php?id=` (1 for free) | `schedule/previous/league/{id}` |
| A team's full calendar | `eventsnext.php` + `eventslast.php` (home games only for free) | `schedule/full/team/{id}` |
| Teams with badges | `search_all_teams.php?l={name}` (10 for free) | `list/teams/{idLeague}` |
| Teams' alternate names, for matching | `search_all_teams.php` (`strTeamAlternate`) | `lookup/team/{id}`, one call per team |
| Live scores | `livescore.php?s={Sport}` (undocumented; ignores `l=`) | `livescore/{sport}`, `livescore/{idLeague}`, `livescore/all` |
| Which channels show a game | `lookuptv.php?id={idEvent}` (2 for free) | `lookup/event_tv/{idEvent}` |
| A country's TV listings | `eventstv.php?d=…&a={Country}&s={Sport}`, once per day and sport | `filter/tv/country/{Country}`: about a week in one call |
| Everything on one channel | `eventstv.php?c={Name}` or `?id={idChannel}` | `filter/tv/channel/{name}` or `channelid/{id}` |
| League standings | `lookuptable.php?l=` (5 for free) | v1 `lookuptable.php` with the premium key; v2 has none |
| Smaller images | append `/tiny` or `/small` to the URL | the same |

## 8. Notes for client libraries

- **Route by endpoint, not by key name.** Model the envelope as "one key → array", "a `Message`", "`null`" or "an empty body". Try the endpoint's known record key first, then any array-valued key, in case a key is renamed.
- **Parse every value leniently:**
  - Numbers from strings or JSON numbers.
  - `""` as missing.
  - Flags without regard to case.
  - Timestamps with `T` or a space, assumed UTC.
  - A malformed value should become "missing", not fail the whole response.
- **Never log a v1 URL** (it holds the key). Redact the key in error messages, and keep it out of cache keys. With a premium key, prefer v2: the key is only in a header.
- **Respect the rate limit client-side** (30, 100 or 120 per minute by plan), back off on 429, and retry 5xx and network errors a few times.
- **Cache by how fast the data changes:** catalogues for days, teams and players for a day or more, schedules, tables and TV for about an hour, and live scores barely or not at all. Don't cache empty v1 bodies.
- **Normalise TV channel names** before matching them against your own list (`TSN 1` vs `TSN1`, `SportsNet West` vs `Sportsnet West`).
- **Show Creative Commons attribution** (`strCreativeCommonsAttribution`), and check `strCreativeCommons` before using artwork in a published app.
