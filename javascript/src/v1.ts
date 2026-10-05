import * as m from "./models.js";
import type { Requester } from "./requester.js";

/** A day: a Date (its UTC date is used) or "YYYY-MM-DD". */
export type Day = Date | string;

const day = (d: Day): string => (typeof d === "string" ? d.slice(0, 10) : d.toISOString().slice(0, 10));

/** Search endpoints. Free keys: one result each. */
export class V1Search {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** searchteams.php?t=: teams by name. */
  teams(name: string): Promise<m.Team[]> {
    return this.r.v1("searchteams.php", "teams", "slow", m.parseTeam, { t: name });
  }

  /** searchevents.php?e=: events by name, "Home vs Away". Narrow with a season or a date, not both. */
  events(name: string, options: { season?: string; date?: Day } = {}): Promise<m.Event[]> {
    if (options.season != null && options.date != null) return Promise.reject(new RangeError("Pass season or date, not both"));
    return this.r.v1("searchevents.php", "event", "medium", m.parseEvent, {
      e: name, s: options.season, d: options.date == null ? null : day(options.date),
    });
  }

  /** searchfilename.php?e=: events by Event.filename, e.g. "English Premier League 2015-04-26 Arsenal vs Chelsea". */
  eventsByFilename(filename: string, season?: string): Promise<m.Event[]> {
    return this.r.v1("searchfilename.php", "event", "medium", m.parseEvent, { e: filename, s: season });
  }

  /** searchplayers.php?p=: players by name (summary fields). */
  players(name: string): Promise<m.Player[]> {
    return this.r.v1("searchplayers.php", "player", "slow", m.parsePlayer, { p: name });
  }

  /** searchvenues.php?v=: venues by name. */
  venues(name: string): Promise<m.Venue[]> {
    return this.r.v1("searchvenues.php", "venues", "slow", m.parseVenue, { v: name });
  }
}

const first = <T>(items: T[]): T | null => items[0] ?? null;

/** Lookups by id. */
export class V1Lookup {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** lookupleague.php?id= */
  async league(id: number): Promise<m.League | null> {
    return first(await this.r.v1("lookupleague.php", "leagues", "slow", m.parseLeague, { id }));
  }

  /** lookuptable.php?l=: the league table. Free keys: top 5. Only some leagues have tables. */
  table(leagueId: number, season?: string): Promise<m.Standing[]> {
    return this.r.v1("lookuptable.php", "table", "medium", m.parseStanding, { l: leagueId, s: season });
  }

  /** lookupteam.php?id= */
  async team(id: number): Promise<m.Team | null> {
    return first(await this.r.v1("lookupteam.php", "teams", "slow", m.parseTeam, { id }));
  }

  /** lookupequipment.php?id=: a team's kits. Free keys: 2. */
  equipment(teamId: number): Promise<m.Equipment[]> {
    return this.r.v1("lookupequipment.php", "equipment", "slow", m.parseEquipment, { id: teamId });
  }

  /** lookupplayer.php?id= */
  async player(id: number): Promise<m.Player | null> {
    return first(await this.r.v1("lookupplayer.php", "players", "slow", m.parsePlayer, { id }));
  }

  /** lookuphonours.php?id=. Free keys: 5. */
  honours(playerId: number): Promise<m.Honour[]> {
    return this.r.v1("lookuphonours.php", "honours", "slow", m.parseHonour, { id: playerId });
  }

  /** lookupformerteams.php?id=. Free keys: 5. */
  formerTeams(playerId: number): Promise<m.FormerTeam[]> {
    return this.r.v1("lookupformerteams.php", "formerteams", "slow", m.parseFormerTeam, { id: playerId });
  }

  /** lookupmilestones.php?id=. Free keys: 5. */
  milestones(playerId: number): Promise<m.Milestone[]> {
    return this.r.v1("lookupmilestones.php", "milestones", "slow", m.parseMilestone, { id: playerId });
  }

  /** lookupcontracts.php?id=. Free keys: 1. */
  contracts(playerId: number): Promise<m.Contract[]> {
    return this.r.v1("lookupcontracts.php", "contracts", "slow", m.parseContract, { id: playerId });
  }

  /** playerresults.php?id=: individual-sport results. Free keys: 5. */
  playerResults(playerId: number): Promise<m.EventResult[]> {
    return this.r.v1("playerresults.php", "results", "medium", m.parseEventResult, { id: playerId });
  }

  /** lookupplayerstats.php?id=. Free keys: 10. */
  playerStats(playerId: number): Promise<m.PlayerStat[]> {
    return this.r.v1("lookupplayerstats.php", "playerstats", "medium", m.parsePlayerStat, { id: playerId });
  }

  /** lookupevent.php?id= */
  async event(id: number): Promise<m.Event | null> {
    return first(await this.r.v1("lookupevent.php", "events", "medium", m.parseEvent, { id }));
  }

  /** eventresults.php?id=. Free keys: 5. */
  eventResults(eventId: number): Promise<m.EventResult[]> {
    return this.r.v1("eventresults.php", "results", "medium", m.parseEventResult, { id: eventId });
  }

  /** lookuplineup.php?id= (not lookuplineups.php, which is gone). Free keys: 5. */
  lineup(eventId: number): Promise<m.LineupEntry[]> {
    return this.r.v1("lookuplineup.php", "lineup", "medium", m.parseLineupEntry, { id: eventId });
  }

  /** lookuptimeline.php?id=. Free keys: 5. */
  timeline(eventId: number): Promise<m.TimelineEntry[]> {
    return this.r.v1("lookuptimeline.php", "timeline", "medium", m.parseTimelineEntry, { id: eventId });
  }

  /** lookupeventstats.php?id=. Free keys: 5. */
  eventStats(eventId: number): Promise<m.EventStat[]> {
    return this.r.v1("lookupeventstats.php", "eventstats", "medium", m.parseEventStat, { id: eventId });
  }

  /** lookuptv.php?id=: the channels showing an event. Free keys: 2. */
  eventTv(eventId: number): Promise<m.TvListing[]> {
    return this.r.v1("lookuptv.php", "tvevent", "medium", m.parseTvListing, { id: eventId });
  }

  /** lookupvenue.php?id= */
  async venue(id: number): Promise<m.Venue | null> {
    return first(await this.r.v1("lookupvenue.php", "venues", "slow", m.parseVenue, { id }));
  }
}

/** Lists. */
export class V1Lists {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** all_sports.php. Free keys: 2. */
  sports(): Promise<m.Sport[]> {
    return this.r.v1("all_sports.php", "sports", "static", m.parseSport);
  }

  /** all_countries.php: names and flags. Free keys: 50. */
  countries(): Promise<m.Country[]> {
    return this.r.v1("all_countries.php", "countries", "static", m.parseCountry);
  }

  /** all_leagues.php: id, name, sport (alternate names with a premium key only). Free keys: 5. */
  leagues(): Promise<m.League[]> {
    return this.r.v1("all_leagues.php", "leagues", "static", m.parseLeague);
  }

  /** search_all_leagues.php?c=&s=: full records (the record key really is "countries"). */
  leaguesInCountry(country: string, sport?: string): Promise<m.League[]> {
    return this.r.v1("search_all_leagues.php", "countries", "slow", m.parseLeague, { c: country, s: sport });
  }

  /** search_all_seasons.php?id=: names, plus one of badges, posters or descriptions. */
  seasons(leagueId: number, extra: { badges?: boolean; posters?: boolean; descriptions?: boolean } = {}): Promise<m.Season[]> {
    if ([extra.badges, extra.posters, extra.descriptions].filter(Boolean).length > 1) {
      return Promise.reject(new RangeError("Ask for one of badges, posters or descriptions per request"));
    }
    return this.r.v1("search_all_seasons.php", "seasons", "slow", m.parseSeason, {
      id: leagueId, badge: extra.badges ? 1 : null, poster: extra.posters ? 1 : null, description: extra.descriptions ? 1 : null,
    });
  }

  /** search_all_teams.php?l=: a league's teams by league **name** (not lookup_all_teams.php, which returns another league). Free keys: 10. */
  teamsInLeague(leagueName: string): Promise<m.Team[]> {
    return this.r.v1("search_all_teams.php", "teams", "slow", m.parseTeam, { l: leagueName });
  }

  /** search_all_teams.php?s=&c=. Free keys: 10. */
  teamsInCountry(sport: string, country: string): Promise<m.Team[]> {
    return this.r.v1("search_all_teams.php", "teams", "slow", m.parseTeam, { s: sport, c: country });
  }

  /** lookup_all_players.php?id=: a team's squad. Free keys: 10. */
  players(teamId: number): Promise<m.Player[]> {
    return this.r.v1("lookup_all_players.php", "player", "slow", m.parsePlayer, { id: teamId });
  }
}

/** Schedules and results. Event times are UTC. */
export class V1Schedule {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** eventsnext.php?id=. Free keys: 1, home games only. */
  teamNext(teamId: number): Promise<m.Event[]> {
    return this.r.v1("eventsnext.php", "events", "medium", m.parseEvent, { id: teamId });
  }

  /** eventslast.php?id= (record key "results"). Free keys: 1, home games only. */
  teamLast(teamId: number): Promise<m.Event[]> {
    return this.r.v1("eventslast.php", "results", "medium", m.parseEvent, { id: teamId });
  }

  /** eventsnextleague.php?id=. Free keys: 1. */
  leagueNext(leagueId: number): Promise<m.Event[]> {
    return this.r.v1("eventsnextleague.php", "events", "medium", m.parseEvent, { id: leagueId });
  }

  /** eventspastleague.php?id=. Free keys: 1. */
  leaguePast(leagueId: number): Promise<m.Event[]> {
    return this.r.v1("eventspastleague.php", "events", "medium", m.parseEvent, { id: leagueId });
  }

  /** eventsday.php?d=: every event on a UTC day, optionally one sport or league. Free keys: 3. */
  day(date: Day, options: { sport?: string; leagueId?: number; leagueName?: string } = {}): Promise<m.Event[]> {
    if (options.leagueId != null && options.leagueName != null) return Promise.reject(new RangeError("Pass leagueId or leagueName, not both"));
    return this.r.v1("eventsday.php", "events", "medium", m.parseEvent, {
      d: day(date), s: options.sport, l: options.leagueId ?? options.leagueName,
    });
  }

  /** eventsseason.php?id=&s=: a whole season (premium). Free keys: 5. */
  season(leagueId: number, season: string): Promise<m.Event[]> {
    return this.r.v1("eventsseason.php", "events", "medium", m.parseEvent, { id: leagueId, s: season });
  }

  /**
   * eventsround.php?id=&r=&s=: one round. **Undocumented and free keys only**: premium keys get
   * HTTP 404. helpers.roundEvents() picks the right route for any key.
   */
  round(leagueId: number, round: number, season: string): Promise<m.Event[]> {
    return this.r.v1("eventsround.php", "events", "medium", m.parseEvent, { id: leagueId, r: round, s: season });
  }
}

/** TV listings. Free keys: 1 per call. */
export class V1Tv {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /**
   * eventstv.php?d=: listings for a day, optionally a sport, or a country **and** sport. The API
   * returns an empty body for a country without a sport, so that is rejected here.
   */
  day(date: Day, options: { sport?: string; country?: string } = {}): Promise<m.TvListing[]> {
    if (options.country != null && options.sport == null) {
      return Promise.reject(new RangeError("eventstv.php needs a sport when filtering by country"));
    }
    return this.r.v1("eventstv.php", "tvevents", "medium", m.parseTvListing, { d: day(date), a: options.country, s: options.sport });
  }

  /** eventstv.php?c=: a channel by name (e.g. "TSN 1"). */
  channel(name: string): Promise<m.TvListing[]> {
    return this.r.v1("eventstv.php", "tvevents", "medium", m.parseTvListing, { c: name });
  }

  /** eventstv.php?id=: by **channel** id (TvListing.channelId), not event id. */
  channelId(channelId: number): Promise<m.TvListing[]> {
    return this.r.v1("eventstv.php", "tvevents", "medium", m.parseTvListing, { id: channelId });
  }
}

export class V1Video {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** eventshighlights.php?d=: events with highlight videos (Event.video). Free keys: 2. */
  highlights(date: Day, options: { leagueId?: number; sport?: string } = {}): Promise<m.Event[]> {
    return this.r.v1("eventshighlights.php", "tvhighlights", "medium", m.parseEvent, { d: day(date), l: options.leagueId, s: options.sport });
  }
}

/** Live scores on v1: **undocumented** but answering, including the full feed for free keys. */
export class V1Live {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** livescore.php?s= (l= is ignored by the API, so it isn't offered). */
  sport(sport: string): Promise<m.LiveScore[]> {
    return this.r.v1("livescore.php", "livescore", "live", m.parseLiveScore, { s: sport });
  }
}

/** The v1 API: https://www.thesportsdb.com/api/v1/json/{key}/{endpoint}.php. Free and premium keys. */
export class V1Api {
  readonly search: V1Search;
  readonly lookup: V1Lookup;
  readonly list: V1Lists;
  readonly schedule: V1Schedule;
  readonly tv: V1Tv;
  readonly video: V1Video;
  readonly live: V1Live;

  /** @internal */
  constructor(r: Requester) {
    this.search = new V1Search(r);
    this.lookup = new V1Lookup(r);
    this.list = new V1Lists(r);
    this.schedule = new V1Schedule(r);
    this.tv = new V1Tv(r);
    this.video = new V1Video(r);
    this.live = new V1Live(r);
  }
}
