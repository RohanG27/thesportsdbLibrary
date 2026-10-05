import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { SportsDb, type Clock, type HttpResponse, type SportsDbOptions, type Transport } from "../src/index.js";

const fixtures = join(dirname(fileURLToPath(import.meta.url)), "fixtures");

export const PREMIUM = "9999999999"; // a stand-in; fixtures never contain a real key

export function fixture(path: string): string {
  return readFileSync(join(fixtures, path), "utf8");
}

export function fixtureRecords(path: string, key: string): Array<Record<string, string | null>> {
  return JSON.parse(fixture(path))[key];
}

/** Records requests and answers from a queue, then with `fallback`. */
export class FakeTransport implements Transport {
  readonly queue: Array<() => HttpResponse> = [];
  readonly requests: Array<[string, Readonly<Record<string, string>>]> = [];

  constructor(public fallback: () => HttpResponse = () => ({ status: 200, body: "{}", headers: {} })) {}

  respond(body: string, status = 200, headers: Record<string, string> = {}): this {
    this.queue.push(() => ({ status, body, headers }));
    return this;
  }

  fail(message = "connection reset"): this {
    this.queue.push(() => {
      throw new TypeError(message);
    });
    return this;
  }

  get lastUrl(): string {
    return this.requests.at(-1)?.[0] ?? "";
  }

  async get(url: string, headers: Readonly<Record<string, string>>): Promise<HttpResponse> {
    this.requests.push([url, headers]);
    return (this.queue.shift() ?? this.fallback)();
  }
}

/** Answers by URL: the first route whose key is in the call (after /json/) wins; a fixture path or inline JSON. */
export class Routes implements Transport {
  readonly calls: string[] = [];

  constructor(private readonly routes: Record<string, string> = {}) {}

  async get(url: string): Promise<HttpResponse> {
    const call = url.split("/json/")[1] ?? url;
    this.calls.push(call);
    const body = Object.entries(this.routes).find(([k]) => call.includes(k))?.[1] ?? "{}";
    return { status: 200, body: body.startsWith("{") ? body : fixture(body), headers: {} };
  }
}

/** A virtual clock: sleep() advances time instead of waiting. */
export class FakeClock implements Clock {
  readonly sleeps: number[] = [];
  private t = 1_000_000;
  now(): number {
    return this.t;
  }
  async sleep(ms: number): Promise<void> {
    this.sleeps.push(ms);
    this.t += ms;
  }
}

export function client(transport: Transport, apiKey = "123", options: SportsDbOptions = {}): SportsDb {
  return new SportsDb({ requestsPerMinute: 0, clock: new FakeClock(), ...options, apiKey, transport });
}

export function tails(calls: string[]): string[] {
  return calls.map((c) => c.slice(c.lastIndexOf("/") + 1)).sort();
}
