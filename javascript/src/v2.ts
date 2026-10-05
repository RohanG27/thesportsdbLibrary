import * as m from "./models.js";
import type { Requester } from "./requester.js";
import type { Day } from "./v1.js";

const first = <T>(items: T[]): T | null => items[0] ?? null;

/** search/...: up to about 10 results, summary fields only. Ids arrive as JSON numbers here. */
export class V2Search {
  /** @internal */
  constructor(private readonly r: Requester) {}

  leagues(name: string): Promise<m.League[]> {
    return this.r.v2("search", "slow", m.parseLeague, ["search", "league", name]);
  }

  teams(name: string): Promise<m.Team[]> {
    return this.r.v2("search", "slow", m.parseTeam, ["search", "team", name]);
  }

  players(name: string): Promise<m.Player[]> {
    return this.r.v2("search", "slow", m.parsePlayer, ["search", "player", name]);
  }

  /** Needs the exact stored event name; a loose "Arsenal vs Chelsea" finds nothing. */
  events(name: string): Promise<m.Event[]> {
    return this.r.v2("search", "medium", m.parseEvent, ["search", "event", name]);
  }

  venues(name: string): Promise<m.Venue[]> {
    return this.r.v2("search", "slow", m.parseVenue, ["search", "venue", name]);
  }
}

/** lookup/...: full records by id. */
export class V2Lookup {
  /** @internal */
  constructor(private readonly r: Requester) {}

  async league(id: number): Promise<m.League | null> {
    return first(await this.r.v2("lookup", "slow", m.parseLeague, ["lookup", "league", id]));
  }

  async team(id: number): Promise<m.Team | null> {
    return first(await this.r.v2("lookup", "slow", m.parseTeam, ["lookup", "team", id]));
  }

  teamEquipment(teamId: number): Promise<m.Equipment[]> {
    return this.r.v2("lookup", "slow", m.parseEquipment, ["lookup", "team_equipment", teamId]);
  }

  async player(id: number): Promise<m.Player | null> {
    return first(await this.r.v2("lookup", "slow", m.parsePlayer, ["lookup", "player", id]));
  }

  playerContracts(playerId: number): Promise<m.Contract[]> {
    return this.r.v2("lookup", "slow", m.parseContract, ["lookup", "player_contracts", playerId]);
  }

  playerResults(playerId: number): Promise<m.EventResult[]> {
    return this.r.v2("lookup", "medium", m.parseEventResult, ["lookup", "player_results", playerId]);
  }

  playerHonours(playerId: number): Promise<m.Honour[]> {
    return this.r.v2("lookup", "slow", m.parseHonour, ["lookup", "player_honours", playerId]);
  }

  playerMilestones(playerId: number): Promise<m.Milestone[]> {
    return this.r.v2("lookup", "slow", m.parseMilestone, ["lookup", "player_milestones", playerId]);
  }

  /** A player's former teams (v1 calls these formerteams). */
  playerTeams(playerId: number): Promise<m.FormerTeam[]> {
    return this.r.v2("lookup", "slow", m.parseFormerTeam, ["lookup", "player_teams", playerId]);
  }

  playerStats(playerId: number): Promise<m.PlayerStat[]> {
    return this.r.v2("lookup", "medium", m.parsePlayerStat, ["lookup", "player_stats", playerId]);
  }

  async event(id: number): Promise<m.Event | null> {
    return first(await this.r.v2("lookup", "medium", m.parseEvent, ["lookup", "event", id]));
  }

  eventLineup(eventId: number): Promise<m.LineupEntry[]> {
    return this.r.v2("lookup", "medium", m.parseLineupEntry, ["lookup", "event_lineup", eventId]);
  }

  eventResults(eventId: number): Promise<m.EventResult[]> {
    return this.r.v2("lookup", "medium", m.parseEventResult, ["lookup", "event_results", eventId]);
  }

  eventStats(eventId: number): Promise<m.EventStat[]> {
    return this.r.v2("lookup", "medium", m.parseEventStat, ["lookup", "event_stats", eventId]);
  }

  eventTimeline(eventId: number): Promise<m.TimelineEntry[]> {
    return this.r.v2("lookup", "medium", m.parseTimelineEntry, ["lookup", "event_timeline", eventId]);
  }

  /** The channels showing an event. */
  eventTv(eventId: number): Promise<m.TvListing[]> {
    return this.r.v2("lookup", "medium", m.parseTvListing, ["lookup", "event_tv", eventId]);
  }

  /** The event with its highlight video in Event.video. */
  eventHighlights(eventId: number): Promise<m.Event[]> {
    return this.r.v2("lookup", "medium", m.parseEvent, ["lookup", "event_highlights", eventId]);
  }

  async venue(id: number): Promise<m.Venue | null> {
    return first(await this.r.v2("lookup", "slow", m.parseVenue, ["lookup", "venue", id]));
  }
}

/** list/... */
export class V2Lists {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** A league's teams with badges and colours, but no alternate names. */
  teams(leagueId: number): Promise<m.Team[]> {
    return this.r.v2("list", "slow", m.parseTeam, ["list", "teams", leagueId]);
  }

  seasons(leagueId: number): Promise<m.Season[]> {
    return this.r.v2("list", "slow", m.parseSeason, ["list", "seasons", leagueId]);
  }

  /** Every season poster and badge uploaded for a league, with the uploader (in the official OpenAPI description). */
  seasonPosters(leagueId: number): Promise<m.SeasonPoster[]> {
    return this.r.v2("list", "slow", m.parseSeasonPoster, ["list", "seasonposters", leagueId]);
  }

  players(teamId: number): Promise<m.Player[]> {
    return this.r.v2("list", "slow", m.parsePlayer, ["list", "players", teamId]);
  }
}

/** all/...: complete catalogues. */
export class V2All {
  /** @internal */
  constructor(private readonly r: Requester) {}

  countries(): Promise<m.Country[]> {
    return this.r.v2("all", "static", m.parseCountry, ["all", "countries"]);
  }

  sports(): Promise<m.Sport[]> {
    return this.r.v2("all", "static", m.parseSport, ["all", "sports"]);
  }

  leagues(): Promise<m.League[]> {
    return this.r.v2("all", "static", m.parseLeague, ["all", "leagues"]);
  }
}

/** schedule/.... Event times are UTC. */
export class V2Schedule {
  /** @internal */
  constructor(private readonly r: Requester) {}

  leagueNext(leagueId: number): Promise<m.Event[]> {
    return this.get("next", "league", leagueId);
  }

  leaguePrevious(leagueId: number): Promise<m.Event[]> {
    return this.get("previous", "league", leagueId);
  }

  teamNext(teamId: number): Promise<m.Event[]> {
    return this.get("next", "team", teamId);
  }

  teamPrevious(teamId: number): Promise<m.Event[]> {
    return this.get("previous", "team", teamId);
  }

  venueNext(venueId: number): Promise<m.Event[]> {
    return this.get("next", "venue", venueId);
  }

  venuePrevious(venueId: number): Promise<m.Event[]> {
    return this.get("previous", "venue", venueId);
  }

  /** A team's whole schedule, past and future, across all competitions. */
  teamFull(teamId: number): Promise<m.Event[]> {
    return this.get("full", "team", teamId);
  }

  /** A league's whole season in one call; the current season is League.currentSeason. */
  leagueSeason(leagueId: number, season: string): Promise<m.Event[]> {
    return this.get("league", leagueId, season);
  }

  private get(...path: Array<string | number>): Promise<m.Event[]> {
    return this.r.v2("schedule", "medium", m.parseEvent, ["schedule", ...path]);
  }
}

/** filter/tv/... */
export class V2Tv {
  /** @internal */
  constructor(private readonly r: Requester) {}

  /** Every listing worldwide on a day. */
  day(date: Day): Promise<m.TvListing[]> {
    return this.get("day", typeof date === "string" ? date.slice(0, 10) : date.toISOString().slice(0, 10));
  }

  /** About a week of listings for a country, e.g. "Canada". */
  country(country: string): Promise<m.TvListing[]> {
    return this.get("country", country);
  }

  sport(sport: string): Promise<m.TvListing[]> {
    return this.get("sport", sport);
  }

  /** By channel name in the API's spelling ("TSN 1", not "TSN1"); partial names match. */
  channel(name: string): Promise<m.TvListing[]> {
    return this.get("channel", name);
  }

  channelId(channelId: number): Promise<m.TvListing[]> {
    return this.get("channelid", channelId);
  }

  private get(kind: string, value: string | number): Promise<m.TvListing[]> {
    return this.r.v2("filter", "medium", m.parseTvListing, ["filter", "tv", kind, value]);
  }
}

/** livescore/.... Not cached unless the cache policy says so. Entries can be stale: check `updated`. */
export class V2Live {
  /** @internal */
  constructor(private readonly r: Requester) {}

  sport(sport: string): Promise<m.LiveScore[]> {
    return this.r.v2("livescore", "live", m.parseLiveScore, ["livescore", sport]);
  }

  league(leagueId: number): Promise<m.LiveScore[]> {
    return this.r.v2("livescore", "live", m.parseLiveScore, ["livescore", leagueId]);
  }

  all(): Promise<m.LiveScore[]> {
    return this.r.v2("livescore", "live", m.parseLiveScore, ["livescore", "all"]);
  }
}

/**
 * The v2 API: https://www.thesportsdb.com/api/v2/json/{group}/{name}/{param}, key in the
 * X-API-KEY header. **Premium keys only**: with a free key every call rejects with
 * PremiumRequiredError without touching the network.
 */
export class V2Api {
  readonly search: V2Search;
  readonly lookup: V2Lookup;
  readonly list: V2Lists;
  readonly all: V2All;
  readonly schedule: V2Schedule;
  readonly tv: V2Tv;
  readonly live: V2Live;

  /** @internal */
  constructor(r: Requester) {
    this.search = new V2Search(r);
    this.lookup = new V2Lookup(r);
    this.list = new V2Lists(r);
    this.all = new V2All(r);
    this.schedule = new V2Schedule(r);
    this.tv = new V2Tv(r);
    this.live = new V2Live(r);
  }
}
