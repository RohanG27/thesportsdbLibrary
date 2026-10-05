import { describe, expect, it } from "vitest";
import { SportsDb } from "../src/index.js";
import { FakeClock, PREMIUM, Routes, fixtureRecords, tails } from "./support.js";

const helpers = (t: Routes, premium: boolean) =>
  new SportsDb({ apiKey: premium ? PREMIUM : "123", transport: t, requestsPerMinute: 0, clock: new FakeClock() }).helpers;

describe("helpers route by key tier", () => {
  it("season events", async () => {
    const p = new Routes({ "lookup/league/4328": "v2/lookup_league.json", "schedule/league/4328/2026-2027": "v2/schedule_league_season.json" });
    const events = await helpers(p, true).seasonEvents(4328);
    expect(p.calls).toEqual(["lookup/league/4328", "schedule/league/4328/2026-2027"]);
    expect(events).toHaveLength(380);
    const times = events.map((e) => e.timestamp?.getTime() ?? 0);
    expect(times).toEqual([...times].sort((a, b) => a - b));
    const f = new Routes({ "lookupleague.php": "v1-free/lookup_league.json", "eventsseason.php": "v1-free/events_season.json" });
    expect(await helpers(f, false).seasonEvents(4328)).toHaveLength(5);
  });

  it("round events take different routes", async () => {
    const f = new Routes({ "eventsround.php": "v1-free/events_round.json" });
    expect(await helpers(f, false).roundEvents(4328, 1, "2026-2027")).toHaveLength(10);
    expect(f.calls).toEqual(["123/eventsround.php?id=4328&r=1&s=2026-2027"]);
    const p = new Routes({ "schedule/league/4328/2026-2027": "v2/schedule_league_season.json" });
    const expected = fixtureRecords("v2/schedule_league_season.json", "schedule").filter((e) => e["intRound"] === "1").length;
    expect(await helpers(p, true).roundEvents(4328, 1, "2026-2027")).toHaveLength(expected);
    expect(p.calls).toEqual(["schedule/league/4328/2026-2027"]);
  });

  it("treats key 3 as free", async () => {
    const t = new Routes({ "eventsround.php": "v1-free/events_round.json" });
    await new SportsDb({ apiKey: "3", transport: t, requestsPerMinute: 0 }).helpers.roundEvents(4328, 1, "2026-2027");
    expect(t.calls).toEqual(["3/eventsround.php?id=4328&r=1&s=2026-2027"]);
  });

  it("upcoming league events", async () => {
    const p = new Routes({ "lookup/league/4328": "v2/lookup_league.json", "schedule/league/": "v2/schedule_league_season.json" });
    const upcoming = await helpers(p, true).upcomingLeagueEvents(4328, 7, "2026-10-17");
    const expected = fixtureRecords("v2/schedule_league_season.json", "schedule")
      .filter((e) => (e["dateEvent"] ?? "") >= "2026-10-17" && (e["dateEvent"] ?? "") < "2026-10-24").length;
    expect(expected).toBeGreaterThan(0);
    expect(upcoming).toHaveLength(expected);
    const f = new Routes({ "eventsday.php": "v1-free/events_day.json" });
    await helpers(f, false).upcomingLeagueEvents(4328, 3, "2026-10-04");
    expect(tails(f.calls)).toEqual(["eventsday.php?d=2026-10-04&l=4328", "eventsday.php?d=2026-10-05&l=4328", "eventsday.php?d=2026-10-06&l=4328"]);
  });

  it("team schedule", async () => {
    const f = new Routes({ "eventsnext.php": "v1-free/events_next.json", "eventslast.php": "v1-free/events_last.json" });
    const s = await helpers(f, false).teamSchedule(133602);
    expect(f.calls).toHaveLength(2);
    expect(new Set(s.map((e) => e.id)).size).toBe(s.length);
    expect(await helpers(new Routes({ "schedule/full/team/133604": "v2/schedule_full_team.json" }), true).teamSchedule(133604)).toHaveLength(48);
  });

  it("events on a local date span two UTC days (Toronto)", async () => {
    const t = new Routes({ "d=2026-10-04": "v1-premium/events_day.json", "d=2026-10-05": '{"events":null}' });
    const events = await helpers(t, true).eventsOnLocalDate("2026-10-04", "America/Toronto");
    expect(tails(t.calls)).toEqual(["eventsday.php?d=2026-10-04", "eventsday.php?d=2026-10-05"]);
    const from = Date.parse("2026-10-04T04:00:00Z"); // Toronto is UTC-4 in October
    const to = Date.parse("2026-10-05T04:00:00Z");
    const expected = fixtureRecords("v1-premium/events_day.json", "events").filter((e) => {
      const ts = Date.parse(`${e["strTimestamp"]}Z`);
      return ts >= from && ts < to;
    }).length;
    expect(expected).toBeGreaterThan(0);
    expect(expected).toBeLessThan(901);
    expect(events).toHaveLength(expected);
  });

  it("events on a local date ahead of UTC (Tokyo)", async () => {
    const t = new Routes();
    await helpers(t, false).eventsOnLocalDate("2026-10-04", "Asia/Tokyo", { sport: "Soccer" });
    expect(tails(t.calls)).toEqual(["eventsday.php?d=2026-10-03&s=Soccer", "eventsday.php?d=2026-10-04&s=Soccer"]);
  });

  it("live scores", async () => {
    const league = fixtureRecords("v1-free/livescore_soccer.json", "livescore")[0]?.["idLeague"];
    const f = new Routes({ "lookupleague.php": `{"leagues":[{"idLeague":"${league}","strSport":"Soccer"}]}`, "livescore.php?s=Soccer": "v1-free/livescore_soccer.json" });
    const scores = await helpers(f, false).liveScores({ leagueId: Number(league) });
    expect(scores.length).toBeGreaterThan(0);
    expect(scores.every((s) => s.leagueId === Number(league))).toBe(true);
    expect(await helpers(new Routes({ "livescore/all": "v2/livescore_all.json" }), true).liveScores()).toHaveLength(54);
    await expect(helpers(new Routes(), false).liveScores()).rejects.toBeInstanceOf(RangeError);
  });

  it("league teams and event channels", async () => {
    const f = new Routes({ "lookupleague.php": "v1-free/lookup_league.json", "search_all_teams.php": "v1-free/search_all_teams_league.json" });
    expect(await helpers(f, false).leagueTeams(4328)).toHaveLength(10);
    expect(f.calls[1]?.endsWith("search_all_teams.php?l=English%20Premier%20League")).toBe(true);
    expect(await helpers(new Routes({ "list/teams/4328": "v2/list_teams.json" }), true).leagueTeams(4328)).toHaveLength(20);
    expect(await helpers(new Routes({ "lookup/event_tv/2494052": "v2/lookup_event_tv.json" }), true).eventChannels(2494052)).toHaveLength(13);
    expect(await helpers(new Routes({ "lookuptv.php": "v1-free/lookup_tv.json" }), false).eventChannels(2494052)).toHaveLength(2);
  });

  it("TV listings", async () => {
    const p = new Routes({ "filter/tv/country/Canada": "v2/filter_tv_country.json" });
    const hockey = await helpers(p, true).tvListings("Canada", { sport: "ice hockey", days: 2, start: "2026-10-05" });
    expect(p.calls).toHaveLength(1);
    expect(hockey.length).toBeGreaterThan(0);
    expect(hockey.every((l) => l.sport === "Ice Hockey" && ["2026-10-05", "2026-10-06"].includes(l.date ?? ""))).toBe(true);
    const f = new Routes({ "eventstv.php": "v1-free/events_tv_country.json" });
    await helpers(f, false).tvListings("Canada", { sport: "Ice Hockey", days: 2, start: "2026-10-05" });
    expect(f.calls).toHaveLength(2);
    await expect(helpers(new Routes(), false).tvListings("Canada")).rejects.toBeInstanceOf(RangeError);
  });
});
