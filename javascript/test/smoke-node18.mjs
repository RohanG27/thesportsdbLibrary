// Runs the BUILT package (dist/) with plain Node and no test framework, so it can check the oldest
// supported runtime, Node 18 (Vitest itself needs Node 22+). CI runs it on Node 18.
//
//   node test/smoke-node18.mjs            offline: recorded responses, fake transport
//   LIVE=1 node test/smoke-node18.mjs     also calls the real API through this Node's own fetch
import { readFileSync } from "node:fs";
import { createServer } from "node:net";
import assert from "node:assert/strict";
import { SportsDb, NetworkError, PremiumRequiredError, sameRecord, sized } from "../dist/index.js";

const fixture = (p) => readFileSync(new URL(`./fixtures/${p}`, import.meta.url), "utf8");
const routes = (map) => {
  const calls = [];
  return {
    calls,
    async get(url) {
      const call = url.split("/json/")[1];
      calls.push(call);
      const hit = Object.entries(map).find(([k]) => call.includes(k));
      return { status: 200, body: hit ? (hit[1].startsWith("{") ? hit[1] : fixture(hit[1])) : "{}", headers: {} };
    },
  };
};
const db = (transport, apiKey = "123") => new SportsDb({ apiKey, transport, requestsPerMinute: 0 });
let checks = 0;
const check = async (name, fn) => {
  await fn();
  checks++;
  console.log(`  ok  ${name}`);
};

console.log(`Node ${process.version}`);

await check("v1 lookup parses a team", async () => {
  const team = await db(routes({ lookupteam: "v1-free/lookup_team.json" })).v1.lookup.team(133604);
  assert.equal(team.kind, "Team");
  assert.equal(team.name, "Arsenal");
  assert.equal(team.leagues[0].id, 4328);
  assert.ok(Object.isFrozen(team));
  assert.equal(sized(team.badge, "tiny"), `${team.badge}/tiny`);
});

await check("v2 parses a season, timestamps are UTC Dates", async () => {
  const events = await db(routes({ "schedule/league": "v2/schedule_league_season.json" }), "9999999999").v2.schedule.leagueSeason(4328, "2026-2027");
  assert.equal(events.length, 380);
  assert.ok(events[0].timestamp instanceof Date);
});

await check("free keys never call v2", async () => {
  const t = routes({});
  await assert.rejects(db(t).v2.all.sports(), PremiumRequiredError);
  assert.deepEqual(t.calls, []);
});

await check("time zones via Intl: Toronto spans two UTC days", async () => {
  const t = routes({ "d=2026-10-04": "v1-premium/events_day.json", "d=2026-10-05": '{"events":null}' });
  const events = await db(t, "9999999999").helpers.eventsOnLocalDate("2026-10-04", "America/Toronto");
  assert.deepEqual(t.calls.map((c) => c.split("/").pop()).sort(), ["eventsday.php?d=2026-10-04", "eventsday.php?d=2026-10-05"]);
  const from = Date.parse("2026-10-04T04:00:00Z");
  const to = Date.parse("2026-10-05T04:00:00Z");
  assert.ok(events.length > 0 && events.every((e) => e.timestamp >= from && e.timestamp < to));
});

await check("time zones via Intl: Tokyo is ahead of UTC", async () => {
  const t = routes({});
  await db(t).helpers.eventsOnLocalDate("2026-10-04", "Asia/Tokyo", { sport: "Soccer" });
  assert.deepEqual(t.calls.map((c) => c.split("/").pop()).sort(), ["eventsday.php?d=2026-10-03&s=Soccer", "eventsday.php?d=2026-10-04&s=Soccer"]);
});

await check("identical calls in flight share one request", async () => {
  let release;
  const gate = new Promise((r) => (release = r));
  const urls = [];
  const transport = { async get(url) { urls.push(url); await gate; return { status: 200, body: fixture("v1-free/all_sports.json"), headers: {} }; } };
  const c = db(transport);
  const calls = [c.v1.list.sports(), c.v1.list.sports(), c.v1.list.sports()];
  await new Promise((r) => setTimeout(r, 0));
  assert.equal(urls.length, 1);
  release();
  const [a, b] = await Promise.all(calls);
  assert.ok(sameRecord(a[0], b[0]));
});

await check("a silent server times out (AbortSignal.timeout)", async () => {
  const server = createServer(() => undefined);
  await new Promise((r) => server.listen(0, "127.0.0.1", r));
  const c = new SportsDb({ baseUrl: `http://127.0.0.1:${server.address().port}`, timeoutMs: 300, maxRetries: 0, requestsPerMinute: 0 });
  const started = Date.now();
  await assert.rejects(c.v1.list.sports(), NetworkError);
  assert.ok(Date.now() - started < 5000);
  server.close();
});

if (process.env.LIVE) {
  await check("live: the real API through this Node's fetch", async () => {
    const c = new SportsDb();
    assert.equal((await c.v1.lookup.team(133604)).name, "Arsenal");
    assert.equal((await c.v1.search.teams("Toronto Maple Leafs"))[0].name, "Toronto Maple Leafs");
    const season = await c.helpers.currentSeason(4328);
    assert.ok((await c.helpers.seasonEvents(4328, season)).length > 0);
  });
}

console.log(`smoke ok: ${checks} checks on Node ${process.version}`);
