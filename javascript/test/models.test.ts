import { describe, expect, it } from "vitest";
import { sameRecord } from "../src/index.js";
import { FakeTransport, client, fixture } from "./support.js";

describe("models", () => {
  it("are the same record when the API sent the same data", async () => {
    const t = new FakeTransport().respond(fixture("v1-free/lookup_team.json")).respond(fixture("v1-free/lookup_team.json"))
      .respond(fixture("v1-free/search_all_teams_league.json"));
    const c = client(t);
    const a = await c.v1.lookup.team(133604);
    const b = await c.v1.lookup.team(133604);
    expect(a).not.toBe(b);
    expect(sameRecord(a, b)).toBe(true);
    expect(a).toEqual(b);
    const other = (await c.v1.list.teamsInLeague("English Premier League")).find((x) => x.id !== 133604);
    expect(sameRecord(a, other)).toBe(false);
  });

  it("differ across kinds built from the same fields", async () => {
    const body = '{"tvhighlights":[{"idEvent":"1"}],"tvevents":[{"idEvent":"1"}]}';
    const c = client(new FakeTransport().respond(body).respond(body));
    const [event] = await c.v1.video.highlights("2026-01-01");
    const [listing] = await c.v1.tv.channel("x");
    expect(event?.raw).toEqual(listing?.raw);
    expect(sameRecord(event, listing)).toBe(false);
    expect([event?.kind, listing?.kind]).toEqual(["Event", "TvListing"]);
  });

  it("are frozen and serialise to JSON", async () => {
    const team = await client(new FakeTransport().respond(fixture("v1-free/lookup_team.json"))).v1.lookup.team(133604);
    expect(Object.isFrozen(team)).toBe(true);
    expect(() => {
      (team as { name: string | null }).name = "x";
    }).toThrow(TypeError);
    expect(JSON.parse(JSON.stringify(team)).name).toBe("Arsenal");
  });
});
