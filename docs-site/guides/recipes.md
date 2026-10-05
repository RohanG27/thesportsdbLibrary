# Recipes

The best endpoint for common tasks, with the free keys and with a premium key.

## A league's current season

```
v1  lookupleague.php?id=4328          → strCurrentSeason
v2  lookup/league/4328                → strCurrentSeason
```

The value is `2026-2027` for most leagues and `2026` for single-year ones. Pass it as-is to the season endpoints.

## A whole season's fixtures

```
v2  schedule/league/4328/2026-2027               all 380 events, one call
v1  eventsseason.php?id=4328&s=2026-2027         premium: all; free: 5
```

## One round (matchday)

```
v1  eventsround.php?id=4328&r=1&s=2026-2027      free keys only: the whole round
v2  schedule/league/4328/2026-2027               premium: filter by intRound
```

`eventsround.php` is undocumented and returns **HTTP 404 to premium keys**, so premium apps should filter the season instead.

## The next week of a league

- Premium: fetch the season once and filter by `dateEvent`.
- Free keys: `eventsday.php?d={date}&l=4328` once per day (3 events per day).

## Recent results

```
v2  schedule/previous/league/4328     about 20
v1  eventspastleague.php?id=4328      premium 15; free 1
```

## A team's calendar

```
v2  schedule/full/team/133604         past and future, all competitions (48)
v1  eventsnext.php?id=… + eventslast.php?id=…     free: 1 each, home games only
```

## Everything on a day, or a day in your time zone

```
v1  eventsday.php?d=2026-10-04                    all sports; premium ~900, free 3
v1  eventsday.php?d=2026-10-04&s=Ice_Hockey       one sport
v1  eventsday.php?d=2026-10-04&l=4328             one league (id or name)
```

The day is a UTC date. For a calendar day in your own time zone, see [Dates and times](dates-and-times.html#saturdays-games-in-your-time-zone).

## A league table

```
v1  lookuptable.php?l=4328            free: top 5; premium: all 20
v1  lookuptable.php?l=4328&s=2024-2025    a past season
```

v2 has no tables. Premium users use this v1 endpoint with their key.

## A league's teams, with badges

```
v2  list/teams/4328                              20 teams, badges and colours
v1  search_all_teams.php?l=English_Premier_League    by league **name**; free: 10
```

Don't use `lookup_all_teams.php?id=`, which returns another league's teams (see [Known issues](known-issues.html)). For alternate names (useful for matching names from other sources), look each team up by id. They change rarely, so cache them for days.

## Who's showing a game

```
v2  lookup/event_tv/2494052           13 channels for one Premier League match
v1  lookuptv.php?id=2494052           free: 2
```

## A country's TV listings

```
v2  filter/tv/country/Canada                          about a week, one call
v1  eventstv.php?d=2026-10-05&a=Canada&s=Ice_Hockey   one day and one sport per call
```

With v1, `a=` (country) **needs** `s=` (sport): without it, the response is an empty body.

## Everything on one channel

```
v2  filter/tv/channel/TSN%201       by name, in TheSportsDB's spelling; partial names match
v2  filter/tv/channelid/8631        by channel id
v1  eventstv.php?c=TSN_1   or   eventstv.php?id=8631
```

`eventstv.php?id=` takes a **channel** id. For an event's channels, use `lookuptv.php?id={eventId}`.

## Live scores

```
v2  livescore/soccer      livescore/4328      livescore/all
v1  livescore.php?s=Soccer       undocumented; the free keys get the full feed
```

`livescore.php` ignores `l=` and returns every live game. Filter by `idLeague` yourself. Entries can be stale, so check `updated`.

## Caching

The data changes at very different rates. Suggested lifetimes:

| Data | Cache for |
|---|---|
| Sports, countries, the league list | a week |
| Teams, players, venues, seasons, honours | a day or more |
| Schedules, tables, TV listings, highlights | about an hour |
| Live scores | not at all, or under a minute |

Don't cache an empty v1 body: it can be a temporary problem rather than "no results".
