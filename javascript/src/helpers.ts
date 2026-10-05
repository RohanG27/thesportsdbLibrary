import type { SportsDb } from "./client.js";
import type { Event, LiveScore, Team, TvListing } from "./models.js";
import type { Day } from "./v1.js";

const ymd = (d: Day): string => (typeof d === "string" ? d.slice(0, 10) : d.toISOString().slice(0, 10));

/**
 * Common tasks in one call. Each helper uses v2 with a premium key and v1 with a free key (then
 * the free keys' small limits apply). Any key other than the free keys that the API accepts is a
 * paid key, so no extra call is made to find out. Events come back sorted by start time.
 */
export class Helpers {
  /** @internal */
  constructor(private readonly db: SportsDb) {}

  private get premium(): boolean {
    return !this.db.options.isFreeKey;
  }

  /** A league's current season name, e.g. "2026-2027"; null if the league is unknown. */
  async currentSeason(leagueId: number): Promise<string | null> {
    const league = this.premium ? await this.db.v2.lookup.league(leagueId) : await this.db.v1.lookup.league(leagueId);
    return league?.currentSeason ?? null;
  }

  /** Every event of a season (default: current). Premium: one call. Free keys: the first 5. */
  async seasonEvents(leagueId: number, season?: string): Promise<Event[]> {
    const name = season ?? (await this.currentSeason(leagueId));
    if (name == null) return [];
    return byStart(this.premium ? await this.db.v2.schedule.leagueSeason(leagueId, name) : await this.db.v1.schedule.season(leagueId, name));
  }

  /**
   * One round (matchday). Premium: filtered from the season. Free keys: eventsround.php
   * (undocumented; the whole round). Premium keys get 404 from that endpoint, hence two routes.
   */
  async roundEvents(leagueId: number, round: number, season?: string): Promise<Event[]> {
    if (this.premium) return (await this.seasonEvents(leagueId, season)).filter((e) => e.round === round);
    const name = season ?? (await this.currentSeason(leagueId));
    return name == null ? [] : byStart(await this.db.v1.schedule.round(leagueId, round, name));
  }

  /**
   * Events in the next `days` UTC days from `start` (default: today, UTC). Premium: filtered from
   * the season. Free keys: one eventsday.php call per day (3 events a day).
   */
  async upcomingLeagueEvents(leagueId: number, days = 7, start?: Day): Promise<Event[]> {
    const dates = dayRange(days, start);
    if (!this.premium) {
      const perDay = await Promise.all(dates.map((d) => this.db.v1.schedule.day(d, { leagueId })));
      return byStart(distinct(perDay.flat()));
    }
    const wanted = new Set(dates);
    return (await this.seasonEvents(leagueId)).filter((e) => e.date != null && wanted.has(e.date));
  }

  /** A league's latest results. Premium: about 20. Free keys: 1. */
  async recentLeagueResults(leagueId: number): Promise<Event[]> {
    return byStart(this.premium ? await this.db.v2.schedule.leaguePrevious(leagueId) : await this.db.v1.schedule.leaguePast(leagueId));
  }

  /** A team's schedule. Premium: past and future, all competitions. Free keys: next + last, home games only. */
  async teamSchedule(teamId: number): Promise<Event[]> {
    if (this.premium) return byStart(await this.db.v2.schedule.teamFull(teamId));
    const [last, next] = await Promise.all([this.db.v1.schedule.teamLast(teamId), this.db.v1.schedule.teamNext(teamId)]);
    return byStart(distinct([...last, ...next]));
  }

  /**
   * Events starting on `day` in IANA time zone `timeZone` (e.g. "America/Toronto"). The API files
   * events under their UTC date, so a local day can span two API days; this fetches each UTC day it
   * overlaps and keeps the events that start on the local day. Free keys: at most 3 per UTC day.
   */
  async eventsOnLocalDate(day: Day, timeZone: string, options: { sport?: string; leagueId?: number } = {}): Promise<Event[]> {
    const date = ymd(day);
    const start = zonedMidnight(date, timeZone);
    const end = zonedMidnight(nextDay(date), timeZone);
    const utcDays: string[] = [];
    for (let d = start.toISOString().slice(0, 10); Date.parse(`${d}T00:00:00Z`) < end.getTime(); d = nextDay(d)) utcDays.push(d);
    const perDay = await Promise.all(utcDays.map((d) => this.db.v1.schedule.day(d, options)));
    return byStart(
      distinct(perDay.flat()).filter((e) => (e.timestamp ? e.timestamp >= start && e.timestamp < end : e.date === date)),
    );
  }

  /**
   * Games in progress. Premium: v2. Free keys: v1's undocumented feed, which needs a sport; for a
   * league, its sport is looked up and the feed filtered. Entries can be stale: check `updated`.
   */
  async liveScores(options: { sport?: string; leagueId?: number } = {}): Promise<LiveScore[]> {
    const { leagueId } = options;
    if (this.premium) {
      if (leagueId != null) return this.db.v2.live.league(leagueId);
      if (options.sport != null) return this.db.v2.live.sport(options.sport);
      return this.db.v2.live.all();
    }
    const sport = options.sport ?? (leagueId != null ? (await this.db.v1.lookup.league(leagueId))?.sport : null);
    if (sport == null) throw new RangeError("With a free key, live scores need a sport or a leagueId");
    const scores = await this.db.v1.live.sport(sport);
    return leagueId == null ? scores : scores.filter((s) => s.leagueId === leagueId);
  }

  /** A league's teams with badges. Free keys: looks up the league name, then up to 10 teams. */
  async leagueTeams(leagueId: number): Promise<Team[]> {
    if (this.premium) return this.db.v2.list.teams(leagueId);
    const name = (await this.db.v1.lookup.league(leagueId))?.name;
    return name == null ? [] : this.db.v1.list.teamsInLeague(name);
  }

  /** The channels showing an event. Free keys: at most 2. */
  eventChannels(eventId: number): Promise<TvListing[]> {
    return this.premium ? this.db.v2.lookup.eventTv(eventId) : this.db.v1.lookup.eventTv(eventId);
  }

  /**
   * A country's TV listings for `days` days. Premium: the country's week in one call, filtered.
   * Free keys: one call per day, which needs a sport.
   */
  async tvListings(country: string, options: { sport?: string; days?: number; start?: Day } = {}): Promise<TvListing[]> {
    const dates = dayRange(options.days ?? 7, options.start);
    const sport = options.sport;
    let listings: TvListing[];
    if (this.premium) {
      const wanted = new Set(dates);
      listings = (await this.db.v2.tv.country(country)).filter(
        (t) => t.date != null && wanted.has(t.date) && (sport == null || (t.sport ?? "").toLowerCase() === sport.toLowerCase()),
      );
    } else {
      if (sport == null) throw new RangeError("With a free key, TV listings for a country need a sport");
      listings = (await Promise.all(dates.map((d) => this.db.v1.tv.day(d, { sport, country })))).flat();
    }
    return listings.sort((a, b) => compareTimes(a.timestamp, b.timestamp));
  }
}

function dayRange(days: number, start?: Day): string[] {
  if (!Number.isInteger(days) || days < 1 || days > 31) throw new RangeError("days must be between 1 and 31");
  const out = [ymd(start ?? new Date())];
  while (out.length < days) out.push(nextDay(out[out.length - 1] as string));
  return out;
}

function nextDay(date: string): string {
  return new Date(Date.parse(`${date}T00:00:00Z`) + 86_400_000).toISOString().slice(0, 10);
}

/** The UTC instant of local midnight on `date` in `timeZone`. */
function zonedMidnight(date: string, timeZone: string): Date {
  const guess = Date.parse(`${date}T00:00:00Z`);
  let t = guess - offsetMs(new Date(guess), timeZone);
  t = guess - offsetMs(new Date(t), timeZone); // re-check across a DST change
  return new Date(t);
}

/** How far `timeZone` is ahead of UTC at `at`, in ms. */
function offsetMs(at: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone, hourCycle: "h23", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit",
  }).formatToParts(at);
  const get = (type: string): number => Number(parts.find((p) => p.type === type)?.value);
  return Date.UTC(get("year"), get("month") - 1, get("day"), get("hour"), get("minute"), get("second")) - (at.getTime() - at.getMilliseconds());
}

function compareTimes(a: Date | null, b: Date | null): number {
  if (a == null) return b == null ? 0 : 1;
  if (b == null) return -1;
  return a.getTime() - b.getTime();
}

function byStart(events: Event[]): Event[] {
  return [...events].sort((a, b) => compareTimes(a.timestamp, b.timestamp));
}

function distinct(events: Event[]): Event[] {
  const seen = new Set<unknown>();
  return events.filter((e) => {
    const key = e.id ?? e;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
