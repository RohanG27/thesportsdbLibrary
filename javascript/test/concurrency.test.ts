import { describe, expect, it } from "vitest";
import { HttpStatusError, type HttpResponse, type RequestEvent, type Transport } from "../src/index.js";
import { client } from "./support.js";

const SPORTS = '{"sports":[{"idSport":"102","strSport":"Soccer"}]}';

/** Holds every request until release() is called. */
class Gated implements Transport {
  readonly urls: string[] = [];
  private open!: () => void;
  private readonly gate = new Promise<void>((resolve) => (this.open = resolve));
  constructor(private readonly response: HttpResponse = { status: 200, body: SPORTS, headers: {} }) {}
  release(): void {
    this.open();
  }
  async get(url: string): Promise<HttpResponse> {
    this.urls.push(url);
    await this.gate;
    return this.response;
  }
}

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe("de-duplication of identical calls in flight", () => {
  it("shares one request between identical calls", async () => {
    const t = new Gated();
    const events: RequestEvent[] = [];
    const c = client(t, "123", { requestListener: (e) => events.push(e) });
    const calls = Array.from({ length: 10 }, () => c.v1.list.sports());
    await tick();
    expect(t.urls).toHaveLength(1);
    t.release();
    const results = await Promise.all(calls);
    expect(results.every((r) => r === results[0] || JSON.stringify(r) === JSON.stringify(results[0]))).toBe(true);
    expect(events.filter((e) => !e.shared && e.attempts === 1)).toHaveLength(1);
    expect(events.filter((e) => e.shared && e.attempts === 0 && e.status === 200)).toHaveLength(9);
  });

  it("doesn't merge different calls, and can be turned off", async () => {
    const t = new Gated();
    const c = client(t);
    const calls = [c.v1.list.sports(), c.v1.lookup.team(1)];
    await tick();
    expect(t.urls).toHaveLength(2);
    t.release();
    await Promise.all(calls);
    const t2 = new Gated();
    const c2 = client(t2, "123", { deduplicateRequests: false });
    const calls2 = [c2.v1.list.sports(), c2.v1.list.sports(), c2.v1.list.sports()];
    await tick();
    expect(t2.urls).toHaveLength(3);
    t2.release();
    await Promise.all(calls2);
  });

  it("gives every waiter the error", async () => {
    const t = new Gated({ status: 404, body: "nope", headers: {} });
    const c = client(t);
    const calls = [1, 2, 3].map(() => c.v1.list.sports().catch((e: unknown) => e));
    await tick();
    t.release();
    const results = await Promise.all(calls);
    expect(t.urls).toHaveLength(1);
    expect(results.every((r) => r instanceof HttpStatusError)).toBe(true);
  });
});
