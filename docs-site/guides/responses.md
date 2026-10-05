# Responses and errors

## The response format

Every response is a JSON object with **one key** whose value is a **list of records**:

```json
{"events": [{"idEvent": "441613", "strEvent": "Arsenal vs Chelsea", "intHomeScore": "4", "…": "…"}]}
```

The key depends on the endpoint, so read it from the reference rather than assuming. Some keys are surprising:

| Endpoint | Key | Records |
|---|---|---|
| `eventslast.php` | `results` | events |
| `search_all_leagues.php` | `countries` | leagues |
| `searchevents.php`, `searchfilename.php` | `event` | events |
| `searchplayers.php`, `lookup_all_players.php` | `player` | players |
| `lookupplayer.php` | `players` | players |
| v2, every endpoint | the group: `search`, `lookup`, `list`, `filter`, `all`, `schedule`, `livescore` | |

A robust client looks for the endpoint's known key first, then for any key holding a list.

## "No results"

The API says "nothing found" in several ways, **all with HTTP 200**:

| Form | Where |
|---|---|
| `{"events": null}` | v1 |
| an empty body | v1, e.g. `eventstv.php` with a country but no sport |
| `{"Message": "No data found"}` | v2 |

Treat all of these as an empty result, not as an error.

## Errors

| Response | Meaning |
|---|---|
| HTTP 400, `{"Message": "Invalid Premium API key: …"}` | The key isn't accepted, or a free key was used with v2. |
| HTTP 200, **text instead of records**: `{"seasons": "Invalid League ID passed"}`, `{"events": "Invalid League ID or no round passed"}` | A parameter was rejected (usually the wrong parameter name). |
| HTTP 404, an HTML page | The endpoint doesn't exist, or a premium key called a legacy endpoint (see [Known issues](known-issues.html)). |
| HTTP 429 | Over the rate limit. |
| HTTP 5xx | A server problem; retrying after a short wait usually works. |

The text-instead-of-records form is easy to miss: the status is 200 and the key is the usual one, but its value is a string rather than a list.

## Values

- **Everything is text.** Numbers, ids, dates and flags all arrive as strings: `"idEvent": "2494052"`, `"intHomeScore": "2"`. The one exception is v2 `search/…`, which sends ids as JSON numbers (`"idTeam": 133604`). Parse both.
- **Missing values** are `null` **or** `""`. Treat them the same.
- **Zero can mean unknown:** `intFormedYear: "0"`.
- **Yes/no fields aren't consistent:** `strPostponed` is `yes`/`no`, `strHome` and `strSubstitute` are `Yes`/`No`, and `strCreativeCommons` can be `Yes`, `No` or `NO`. Compare without regard to case. `strLocked` is `locked` or `unlocked`.
- **Lists are comma-separated text:** `strTeamAlternate`, `strKeywords`, `strLeagueAlternate`.
- **Numbered fields stand in for lists:**
  - `idLeague` … `idLeague7`, with `strLeague` … `strLeague7` (a team's competitions);
  - `strFanart1`–`4`;
  - `strColour1`–`3`;
  - `strDescriptionEN`, `strDescriptionDE`, … (up to 15 languages).
- **Some fields are formatted for people**, not parsing: `strHeight` (`1.82 m (6 ft 0 in)` or `186 cm`), `strWage` (`£5,200,000`), `strResult` (may contain `<br>`).

Every field is described in [Fields](fields.html).

## Search and summary records

Search and list endpoints return **summary** records with only some fields. For example, v2 `search/team` has 9 fields where a team lookup has 63, and v2 `list/teams` has no alternate names. Look a record up by id when you need all of it.
