# TheSportsDB API

TheSportsDB is an open, crowd-sourced database of sports data and artwork: leagues, teams, players, fixtures and results, TV listings, live scores, badges, kits and photos. Its JSON API comes in two versions:

| | v1 | v2 |
|---|---|---|
| Keys | the free keys and premium keys | premium keys only |
| Request | `/api/v1/json/{key}/{endpoint}.php?{parameters}` | `/api/v2/json/{group}/{name}/{parameter}` with an `X-API-KEY` header |
| Best for | trying the API; league tables; anything by day | full schedules, whole seasons, TV by country, live scores |

Both return the same kinds of records, with the same field names.

## Your first request

No sign-up is needed: `123` is a public key for development and testing.

```sh
curl "https://www.thesportsdb.com/api/v1/json/123/lookupteam.php?id=133604"
```

```json
{"teams": [{
  "idTeam": "133604",
  "strTeam": "Arsenal",
  "strTeamShort": "ARS",
  "strLeague": "English Premier League",
  "intFormedYear": "1892",
  "strBadge": "https://r2.thesportsdb.com/images/media/team/badge/uyhbfe1612467038.png",
  "…": "…"
}]}
```

Every response is one object with **one key** (`teams` here) holding a **list of records**. Every value is text, even numbers.

With a premium key, v2:

```sh
curl -H "X-API-KEY: $YOUR_KEY" "https://www.thesportsdb.com/api/v2/json/schedule/next/league/4328"
```

## Ids to try

| | Id |
|---|---|
| English Premier League | league `4328` |
| Arsenal | team `133604` |
| Wembley Stadium | venue `16163` |
| Arsenal vs Chelsea, 26 Apr 2015 | event `441613` |

Find others with the search endpoints, e.g. `searchteams.php?t=Toronto_Maple_Leafs`.

## Read next

1. [Keys and limits](keys-and-limits.html): which key gets how many results, and the rate limit.
2. [Responses and errors](responses.html): the response format, the four forms of "no results", and errors.
3. [Dates and times](dates-and-times.html): everything is UTC; how to get "Saturday's games" in your time zone.
4. [Recipes](recipes.html): the right endpoint for common tasks.
5. The reference: [v1](reference/v1.html) and [v2](reference/v2.html), every endpoint with real examples.

Everything here was measured with real requests on 5 Oct 2026. Where the API differs from its older documentation, these pages describe what the API actually does; [Known issues](known-issues.html) lists the differences.
