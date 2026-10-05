import { createServer, type Server } from "node:net";
import { describe, expect, it } from "vitest";
import {
  HttpStatusError, InMemoryResponseCache, InvalidApiKeyError, NetworkError, RateLimitError, SportsDb, type RequestEvent,
} from "../src/index.js";
import { FakeClock, FakeTransport, client, fixture } from "./support.js";

const PAID = "5550001234";
const OK = '{"sports":[{"idSport":"102","strSport":"Soccer"}]}';

describe("retries, rate limits, errors", () => {
  it("retries server errors then succeeds", async () => {
    const clock = new FakeClock();
    const t = new FakeTransport().respond("oops", 503).respond("oops", 502).respond(OK);
    expect(await client(t, "123", { clock }).v1.list.sports()).toHaveLength(1);
    expect(t.requests).toHaveLength(3);
    expect(clock.sleeps).toEqual([500, 1000]);
  });

  it("gives up after max retries", async () => {
    const t = new FakeTransport(() => ({ status: 500, body: "down", headers: {} }));
    await expect(client(t).v1.list.sports()).rejects.toMatchObject({ name: "HttpStatusError", status: 500 });
    expect(t.requests).toHaveLength(3);
  });

  it("retries and wraps network errors", async () => {
    expect(await client(new FakeTransport().fail().respond(OK)).v1.list.sports()).toHaveLength(1);
    await expect(client(new FakeTransport().fail().fail().fail()).v1.list.sports()).rejects.toBeInstanceOf(NetworkError);
  });

  it("waits for Retry-After then retries once", async () => {
    const clock = new FakeClock();
    const t = new FakeTransport().respond("", 429, { "Retry-After": "7" }).respond(OK);
    await client(t, "123", { clock }).v1.list.sports();
    expect(clock.sleeps).toEqual([7000]);
    const always = new FakeTransport(() => ({ status: 429, body: "", headers: {} }));
    await expect(client(always).v1.list.sports()).rejects.toMatchObject({ name: "RateLimitError", retryAfterMs: null });
    expect(always.requests).toHaveLength(2);
  });

  it("can skip the rate-limit retry", async () => {
    const t = new FakeTransport().respond("", 429, { "retry-after": "30" });
    const error = await client(t, "123", { retryOnRateLimit: false }).v1.list.sports().catch((e: unknown) => e);
    expect(error).toBeInstanceOf(RateLimitError);
    expect((error as RateLimitError).retryAfterMs).toBe(30_000);
    expect(t.requests).toHaveLength(1);
  });

  it("limits requests client-side", async () => {
    const clock = new FakeClock();
    const c = new SportsDb({ transport: new FakeTransport(() => ({ status: 200, body: OK, headers: {} })), requestsPerMinute: 3, clock });
    for (let i = 0; i < 4; i++) await c.v1.list.sports();
    expect(clock.sleeps).toEqual([60_000]);
  });

  it("defaults the rate limit by key", () => {
    expect(new SportsDb({ apiKey: "123" }).options.requestsPerMinute).toBe(30);
    expect(new SportsDb({ apiKey: "3" }).options.requestsPerMinute).toBe(30);
    expect(new SportsDb({ apiKey: PAID }).options.requestsPerMinute).toBe(100);
  });

  it("never puts the v1 key in errors", async () => {
    for (const [transport, type] of [
      [new FakeTransport(() => ({ status: 404, body: "nope", headers: {} })), HttpStatusError],
      [new FakeTransport().respond(fixture("v1-free/invalid_key.json"), 400), InvalidApiKeyError],
      [new FakeTransport().fail("boom").fail("boom").fail("boom"), NetworkError],
    ] as const) {
      const error = await client(transport, PAID).v1.lookup.team(1).catch((e: unknown) => e);
      expect(error).toBeInstanceOf(type);
      expect(String((error as Error).message)).not.toContain(PAID);
    }
    const error = await client(new FakeTransport(() => ({ status: 404, body: "", headers: {} })), PAID).v1.lookup.team(1).catch((e: Error) => e);
    expect((error as Error).message).toContain("/api/v1/json/***/lookupteam.php?id=1");
  });

  it("sends a User-Agent", async () => {
    const t = new FakeTransport().respond(OK);
    await client(t).v1.list.sports();
    expect(t.requests[0]?.[1]["User-Agent"]).toBe("thesportsdb-client-js");
  });
});

describe("cache and listener", () => {
  it("serves repeats from the cache and keeps keys out of it", async () => {
    const cache = new InMemoryResponseCache();
    const t = new FakeTransport().respond(OK);
    const c = client(t, PAID, { cache });
    await c.v1.list.sports();
    await c.v1.list.sports();
    expect(t.requests).toHaveLength(1);
    expect(cache.size).toBe(1);
    expect(cache.keys().some((k) => k.includes(PAID))).toBe(false);
  });

  it("doesn't cache live scores or empty bodies", async () => {
    const body = '{"livescore":[{"idLiveScore":"1"}]}';
    const t = new FakeTransport().respond(body).respond(body).respond("").respond(OK);
    const c = client(t, "123", { cache: new InMemoryResponseCache() });
    await c.v1.live.sport("Soccer");
    await c.v1.live.sport("Soccer");
    expect(t.requests).toHaveLength(2);
    expect(await c.v1.list.sports()).toEqual([]);
    expect(await c.v1.list.sports()).toHaveLength(1);
  });

  it("reports retries, cache hits and errors to the listener", async () => {
    const events: RequestEvent[] = [];
    const t = new FakeTransport().respond("busy", 503).respond(OK).respond("gone", 404);
    const c = client(t, "123", { cache: new InMemoryResponseCache(), requestListener: (e) => events.push(e) });
    await c.v1.list.sports();
    await c.v1.list.sports();
    await c.v1.lookup.team(1).catch(() => undefined);
    const [retried, cached, failed] = events;
    expect([retried?.attempts, retried?.status, retried?.error]).toEqual([2, 200, null]);
    expect([cached?.fromCache, cached?.attempts]).toEqual([true, 0]);
    expect(failed?.status).toBe(404);
    expect(failed?.error).toBeInstanceOf(HttpStatusError);
    expect(failed?.url.endsWith("/api/v1/json/***/lookupteam.php?id=1")).toBe(true);
  });

  it("survives a failing listener", async () => {
    const c = client(new FakeTransport().respond(OK), "123", { requestListener: () => { throw new Error("listener bug"); } });
    expect(await c.v1.list.sports()).toHaveLength(1);
  });
});

describe("timeout", () => {
  it("gives up on a silent server", async () => {
    // Accepts the connection but never answers: the call must give up, not hang.
    const server: Server = createServer(() => undefined);
    await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
    const address = server.address();
    const port = typeof address === "object" && address ? address.port : 0;
    const db = new SportsDb({ baseUrl: `http://127.0.0.1:${port}`, timeoutMs: 300, maxRetries: 0, requestsPerMinute: 0 });
    const started = Date.now();
    await expect(db.v1.list.sports()).rejects.toBeInstanceOf(NetworkError);
    expect(Date.now() - started).toBeLessThan(5000);
    server.close();
  });
});
