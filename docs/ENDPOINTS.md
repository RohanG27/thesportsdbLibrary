# Endpoint map

Every TheSportsDB endpoint, the library method that calls it, and what it returns.
"Free" is the number of records the free key `123` gets (measured 5 Oct 2026, or from the
official docs where marked †). Premium keys get the full result.

The **record key** is the top-level JSON key that holds the records. It varies, and the library handles it, but you need it if you call the API yourself.

## v1 — `https://www.thesportsdb.com/api/v1/json/{key}/…`

### Search

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `searchteams.php?t={name}` | `v1.search.teams(name)` | `List<Team>` | `teams` | 1 |
| `searchevents.php?e={name}` | `v1.search.events(name)` | `List<Event>` | `event` | 1 |
| `searchevents.php?e={name}&s={season}` | `v1.search.events(name, season = …)` | `List<Event>` | `event` | 1 |
| `searchevents.php?e={name}&d={date}` | `v1.search.events(name, date = …)` | `List<Event>` | `event` | 1 † |
| `searchfilename.php?e={filename}` | `v1.search.eventsByFilename(filename)` | `List<Event>` | `event` | 1 |
| `searchfilename.php?e={filename}&s={season}` | `v1.search.eventsByFilename(filename, season)` | `List<Event>` | `event` | 1 † |
| `searchplayers.php?p={name}` | `v1.search.players(name)` | `List<Player>` (summary) | `player` | 1 |
| `searchvenues.php?v={name}` | `v1.search.venues(name)` | `List<Venue>` | `venues` | 1 |

### Lookup

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `lookupleague.php?id=` | `v1.lookup.league(id)` | `League?` | `leagues` | 1 |
| `lookuptable.php?l={idLeague}[&s={season}]` | `v1.lookup.table(leagueId, season?)` | `List<Standing>` | `table` | 5 |
| `lookupteam.php?id=` | `v1.lookup.team(id)` | `Team?` | `teams` | 1 |
| `lookupequipment.php?id={idTeam}` | `v1.lookup.equipment(teamId)` | `List<Equipment>` | `equipment` | 2 |
| `lookupplayer.php?id=` | `v1.lookup.player(id)` | `Player?` | `players` | 1 |
| `lookuphonours.php?id={idPlayer}` | `v1.lookup.honours(playerId)` | `List<Honour>` | `honours` | 5 |
| `lookupformerteams.php?id={idPlayer}` | `v1.lookup.formerTeams(playerId)` | `List<FormerTeam>` | `formerteams` | 5 |
| `lookupmilestones.php?id={idPlayer}` | `v1.lookup.milestones(playerId)` | `List<Milestone>` | `milestones` | 3–5 |
| `lookupcontracts.php?id={idPlayer}` | `v1.lookup.contracts(playerId)` | `List<Contract>` | `contracts` | 1 |
| `playerresults.php?id={idPlayer}` | `v1.lookup.playerResults(playerId)` | `List<EventResult>` | `results` | 5 |
| `lookupplayerstats.php?id={idPlayer}` | `v1.lookup.playerStats(playerId)` | `List<PlayerStat>` | `playerstats` | 10 |
| `lookupevent.php?id=` | `v1.lookup.event(id)` | `Event?` | `events` | 1 |
| `eventresults.php?id={idEvent}` | `v1.lookup.eventResults(eventId)` | `List<EventResult>` | `results` | 5 |
| `lookuplineup.php?id={idEvent}` | `v1.lookup.lineup(eventId)` | `List<LineupEntry>` | `lineup` | 5 |
| `lookuptimeline.php?id={idEvent}` | `v1.lookup.timeline(eventId)` | `List<TimelineEntry>` | `timeline` | 5 |
| `lookupeventstats.php?id={idEvent}` | `v1.lookup.eventStats(eventId)` | `List<EventStat>` | `eventstats` | 5 |
| `lookuptv.php?id={idEvent}` | `v1.lookup.eventTv(eventId)` | `List<TvListing>` | `tvevent` | 2 |
| `lookupvenue.php?id=` | `v1.lookup.venue(id)` | `Venue?` | `venues` | 1 |

### Lists

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `all_sports.php` | `v1.list.sports()` | `List<Sport>` | `sports` | 2 |
| `all_countries.php` | `v1.list.countries()` | `List<Country>` | `countries` | 50 |
| `all_leagues.php` | `v1.list.leagues()` | `List<League>` (id, name, sport) | `leagues` | 5 |
| `search_all_leagues.php?c={country}[&s={sport}]` | `v1.list.leagues(country, sport?)` | `List<League>` (full) | **`countries`** | 5 |
| `search_all_seasons.php?id={idLeague}` | `v1.list.seasons(leagueId)` | `List<Season>` | `seasons` | 5 |
| `…&badge=1` / `&poster=1` / `&description=1` | `v1.list.seasons(id, badges/posters/descriptions = true)` | `List<Season>` | `seasons` | 5 |
| `search_all_teams.php?l={leagueName}` | `v1.list.teamsInLeague(leagueName)` | `List<Team>` | `teams` | 10 |
| `search_all_teams.php?s={sport}&c={country}` | `v1.list.teamsInCountry(sport, country)` | `List<Team>` | `teams` | 10 |
| `lookup_all_players.php?id={idTeam}` | `v1.list.players(teamId)` | `List<Player>` | `player` | 10 |

### Schedules

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `eventsnext.php?id={idTeam}` | `v1.schedule.teamNext(teamId)` | `List<Event>` | `events` | 1, home games only |
| `eventslast.php?id={idTeam}` | `v1.schedule.teamLast(teamId)` | `List<Event>` | **`results`** | 1, home games only |
| `eventsnextleague.php?id={idLeague}` | `v1.schedule.leagueNext(leagueId)` | `List<Event>` | `events` | 1 |
| `eventspastleague.php?id={idLeague}` | `v1.schedule.leaguePast(leagueId)` | `List<Event>` | `events` | 1 |
| `eventsday.php?d={date}[&s={sport}][&l={idLeague or name}]` | `v1.schedule.day(date, sport?, leagueId?/leagueName?)` | `List<Event>` | `events` | 3 |
| `eventsseason.php?id={idLeague}&s={season}` | `v1.schedule.season(leagueId, season)` | `List<Event>` | `events` | 5 |

### TV

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `eventstv.php?d={date}[&s={sport}]` | `v1.tv.day(date, sport?)` | `List<TvListing>` | `tvevents` | 1 |
| `eventstv.php?d={date}&a={country}&s={sport}` | `v1.tv.day(date, sport, country)` | `List<TvListing>` | `tvevents` | 1 |
| `eventstv.php?c={channelName}` | `v1.tv.channel(name)` | `List<TvListing>` | `tvevents` | 1 |
| `eventstv.php?id={idChannel}` | `v1.tv.channel(channelId)` | `List<TvListing>` | `tvevents` | 1 |

`eventstv.php` with `a=` but no `s=` returns an **empty body**, so the library rejects that combination. `id=` takes a **channel** id (`TvListing.channelId`), not an event id; use `lookuptv.php` for an event's channels.

### Video and live

| Endpoint | Method | Returns | Record key | Free |
|---|---|---|---|---|
| `eventshighlights.php?d={date}[&l={idLeague}][&s={sport}]` | `v1.video.highlights(date, leagueId?, sport?)` | `List<Event>` (`video` set) | `tvhighlights` | 2 |
| `livescore.php?s={sport}` *(not in the official docs)* | `v1.live.sport(sport)` | `List<LiveScore>` | `livescore` | all |

`livescore.php?l=` ignores the league and returns every live game, so the library doesn't offer it. Use v2 `live.league(id)`.

## v2 — `https://www.thesportsdb.com/api/v2/json/…`, header `X-API-KEY`

Premium keys only. The record key is always the first path segment. "No results" comes back as `{"Message":"No data found"}`, which the library turns into an empty list.

| Endpoint | Method | Returns |
|---|---|---|
| `search/league/{name}` | `v2.search.leagues(name)` | `List<League>` (summary) |
| `search/team/{name}` | `v2.search.teams(name)` | `List<Team>` (summary) |
| `search/player/{name}` | `v2.search.players(name)` | `List<Player>` (summary) |
| `search/event/{name}` | `v2.search.events(name)` | `List<Event>`, exact names only |
| `search/venue/{name}` | `v2.search.venues(name)` | `List<Venue>` (summary) |
| `lookup/league/{id}` | `v2.lookup.league(id)` | `League?` |
| `lookup/team/{id}` | `v2.lookup.team(id)` | `Team?` |
| `lookup/team_equipment/{idTeam}` | `v2.lookup.teamEquipment(teamId)` | `List<Equipment>` |
| `lookup/player/{id}` | `v2.lookup.player(id)` | `Player?` |
| `lookup/player_contracts/{idPlayer}` | `v2.lookup.playerContracts(playerId)` | `List<Contract>` |
| `lookup/player_results/{idPlayer}` | `v2.lookup.playerResults(playerId)` | `List<EventResult>` |
| `lookup/player_honours/{idPlayer}` | `v2.lookup.playerHonours(playerId)` | `List<Honour>` |
| `lookup/player_milestones/{idPlayer}` | `v2.lookup.playerMilestones(playerId)` | `List<Milestone>` |
| `lookup/player_teams/{idPlayer}` | `v2.lookup.playerTeams(playerId)` | `List<FormerTeam>` |
| `lookup/player_stats/{idPlayer}` | `v2.lookup.playerStats(playerId)` | `List<PlayerStat>` |
| `lookup/event/{id}` | `v2.lookup.event(id)` | `Event?` |
| `lookup/event_lineup/{idEvent}` | `v2.lookup.eventLineup(eventId)` | `List<LineupEntry>` (adds `formation`, `positionShort`) |
| `lookup/event_results/{idEvent}` | `v2.lookup.eventResults(eventId)` | `List<EventResult>` |
| `lookup/event_stats/{idEvent}` | `v2.lookup.eventStats(eventId)` | `List<EventStat>` |
| `lookup/event_timeline/{idEvent}` | `v2.lookup.eventTimeline(eventId)` | `List<TimelineEntry>` |
| `lookup/event_tv/{idEvent}` | `v2.lookup.eventTv(eventId)` | `List<TvListing>` |
| `lookup/event_highlights/{idEvent}` | `v2.lookup.eventHighlights(eventId)` | `List<Event>` |
| `lookup/venue/{id}` | `v2.lookup.venue(id)` | `Venue?` |
| `list/teams/{idLeague}` | `v2.list.teams(leagueId)` | `List<Team>` (no alternate names) |
| `list/seasons/{idLeague}` | `v2.list.seasons(leagueId)` | `List<Season>` |
| `list/players/{idTeam}` | `v2.list.players(teamId)` | `List<Player>` (summary) |
| `filter/tv/day/{date}` | `v2.tv.day(date)` | `List<TvListing>` |
| `filter/tv/country/{country}` | `v2.tv.country(country)` | `List<TvListing>`, about a week |
| `filter/tv/sport/{sport}` | `v2.tv.sport(sport)` | `List<TvListing>` |
| `filter/tv/channel/{name}` | `v2.tv.channel(name)` | `List<TvListing>`; partial names match |
| `filter/tv/channelid/{idChannel}` | `v2.tv.channel(channelId)` | `List<TvListing>` |
| `all/countries` | `v2.all.countries()` | `List<Country>` (adds codes, more flag sizes) |
| `all/sports` | `v2.all.sports()` | `List<Sport>` |
| `all/leagues` | `v2.all.leagues()` | `List<League>` (id, name, sport) |
| `schedule/next/league/{id}` | `v2.schedule.leagueNext(leagueId)` | `List<Event>` |
| `schedule/previous/league/{id}` | `v2.schedule.leaguePrevious(leagueId)` | `List<Event>` |
| `schedule/next/team/{id}` | `v2.schedule.teamNext(teamId)` | `List<Event>` |
| `schedule/previous/team/{id}` | `v2.schedule.teamPrevious(teamId)` | `List<Event>` |
| `schedule/next/venue/{id}` | `v2.schedule.venueNext(venueId)` | `List<Event>` |
| `schedule/previous/venue/{id}` | `v2.schedule.venuePrevious(venueId)` | `List<Event>` |
| `schedule/full/team/{id}` | `v2.schedule.teamFull(teamId)` | `List<Event>` |
| `schedule/league/{id}/{season}` | `v2.schedule.leagueSeason(leagueId, season)` | `List<Event>`, the whole season |
| `livescore/{sport}` | `v2.live.sport(sport)` | `List<LiveScore>` |
| `livescore/{idLeague}` | `v2.live.league(leagueId)` | `List<LiveScore>` |
| `livescore/all` | `v2.live.all()` | `List<LiveScore>` |

v2 has no league tables. Use `v1.lookup.table` with your premium key.
