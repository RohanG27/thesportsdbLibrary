// Runs the BUILT package (dist/) with plain Node, no test framework: CI uses it to check the
// published code on Node 18, the oldest supported runtime (Vitest itself needs Node 22+).
import { readFileSync } from "node:fs";
import assert from "node:assert/strict";
import { SportsDb, PremiumRequiredError } from "../dist/index.js";

const fixture = (p) => readFileSync(new URL(`./fixtures/${p}`, import.meta.url), "utf8");
const transport = { async get(url) { return { status: 200, body: url.includes("lookupteam") ? fixture("v1-free/lookup_team.json") : "{}", headers: {} }; } };
const db = new SportsDb({ transport, requestsPerMinute: 0 });

const team = await db.v1.lookup.team(133604);
assert.equal(team.name, "Arsenal");
assert.equal(team.leagues[0].id, 4328);
await assert.rejects(db.v2.all.sports(), PremiumRequiredError);
assert.equal(typeof AbortSignal.timeout, "function");
assert.equal(typeof fetch, "function");
console.log(`smoke ok on Node ${process.version}`);
