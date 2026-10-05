import { describe, expect, it } from "vitest";
import { FREE_API_KEY, SportsDb } from "../src/index.js";

// Calls the real API: npm run test:live (set THESPORTSDB_API_KEY for v2).
const key = process.env["THESPORTSDB_API_KEY"]?.trim() || FREE_API_KEY;
const db = new SportsDb({ apiKey: key });

describe("the real API", () => {
  it("looks up and searches", async () => {
    expect((await db.v1.lookup.team(133604))?.name).toBe("Arsenal");
    expect((await db.v1.search.teams("Toronto Maple Leafs"))[0]?.name).toBe("Toronto Maple Leafs");
    expect(await db.v1.schedule.day("2026-10-04", { leagueId: 4328 })).toEqual([]);
    expect(await db.isPremiumKey()).toBe(key !== FREE_API_KEY);
  });

  it("runs helpers", async () => {
    const season = await db.helpers.currentSeason(4328);
    expect(season).toBeTruthy();
    expect((await db.helpers.seasonEvents(4328, season ?? undefined)).length).toBeGreaterThan(0);
    const local = await db.helpers.eventsOnLocalDate("2026-10-04", "America/Toronto", { sport: "Soccer" });
    const fmt = new Intl.DateTimeFormat("en-CA", { timeZone: "America/Toronto" });
    expect(local.every((e) => e.timestamp == null || fmt.format(e.timestamp) === "2026-10-04")).toBe(true);
  });

  it.skipIf(key === FREE_API_KEY)("calls v2 with a premium key", async () => {
    const season = (await db.v2.lookup.league(4328))?.currentSeason;
    expect(season).toBeTruthy();
    expect((await db.v2.schedule.leagueSeason(4328, season ?? "")).length).toBeGreaterThan(100);
  });
});
