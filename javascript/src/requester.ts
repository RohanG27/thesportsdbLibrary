import type { Freshness } from "./cache.js";
import { parseRecords } from "./envelope.js";
import { HttpStatusError, InvalidApiKeyError, NetworkError, PremiumRequiredError, RateLimitError } from "./errors.js";
import type { RawRecord } from "./fields.js";
import type { Clock, ResolvedOptions } from "./options.js";
import type { HttpResponse, Transport } from "./transport.js";

/** Sliding window: at most `permits` calls in any `windowMs`; callers wait for a slot. @internal */
export class RateLimiter {
  private readonly starts: number[] = [];
  private queue: Promise<void> = Promise.resolve();

  constructor(private readonly permits: number, private readonly clock: Clock, private readonly windowMs = 60_000) {}

  acquire(): Promise<void> {
    // Serialise callers so the window is checked one at a time.
    const next = this.queue.then(() => this.take());
    this.queue = next.catch(() => undefined);
    return next;
  }

  private async take(): Promise<void> {
    for (;;) {
      const t = this.clock.now();
      while (this.starts.length > 0 && t - (this.starts[0] ?? t) >= this.windowMs) this.starts.shift();
      if (this.starts.length < this.permits) {
        this.starts.push(t);
        return;
      }
      await this.clock.sleep(this.windowMs - (t - (this.starts[0] ?? t)));
    }
  }
}

type Param = string | number | boolean | Date | null | undefined;

interface Trace {
  status: number | null;
  attempts: number;
  fromCache: boolean;
  shared: boolean;
}

interface Outcome {
  body: string;
  status: number | null;
}

/** Builds URLs and runs every call through the rate limiter, retries, cache, de-duplication and parser. @internal */
export class Requester {
  private readonly limiter: RateLimiter | null;
  private readonly tier: string;
  private readonly inFlight = new Map<string, Promise<Outcome>>();

  constructor(readonly options: ResolvedOptions, private readonly transport: Transport, private readonly clock: Clock) {
    this.limiter = options.requestsPerMinute > 0 ? new RateLimiter(options.requestsPerMinute, clock) : null;
    this.tier = options.isFreeKey ? "free" : "paid";
  }

  /** GET /api/v1/json/{key}/{endpoint}?params. Null/undefined params are left out. */
  async v1<T>(endpoint: string, recordKey: string, freshness: Freshness, parse: (raw: RawRecord) => T, params: Record<string, Param> = {}): Promise<T[]> {
    const query = Object.entries(params)
      .filter(([, v]) => v != null)
      .map(([k, v]) => `${k}=${encodeURIComponent(text(v as Exclude<Param, null | undefined>))}`)
      .join("&");
    const suffix = query ? `${endpoint}?${query}` : endpoint;
    const base = `${this.options.baseUrl}/api/v1/json/`;
    const url = `${base}${encodeURIComponent(this.options.apiKey)}/${suffix}`;
    const display = `${base}***/${suffix}`;
    const body = await this.fetch(url, {}, display, `v1:${this.tier}:${suffix}`, freshness);
    return parseRecords(body, recordKey, display).map(parse);
  }

  /** GET /api/v2/json/{segments...} with the key in the X-API-KEY header. */
  async v2<T>(recordKey: string, freshness: Freshness, parse: (raw: RawRecord) => T, segments: ReadonlyArray<string | number | Date>): Promise<T[]> {
    if (this.options.isFreeKey) {
      throw new PremiumRequiredError("v2 endpoints need a premium key; free keys (123, 3) only work with v1 (client.v1).");
    }
    const path = segments.map((s) => encodeURIComponent(text(s))).join("/");
    const url = `${this.options.baseUrl}/api/v2/json/${path}`;
    const body = await this.fetch(url, { "X-API-KEY": this.options.apiKey }, url, `v2:${path}`, freshness);
    return parseRecords(body, recordKey, url).map(parse);
  }

  private async fetch(url: string, headers: Record<string, string>, display: string, cacheKey: string, freshness: Freshness): Promise<string> {
    const started = this.clock.now();
    const trace: Trace = { status: null, attempts: 0, fromCache: false, shared: false };
    let error: unknown = null;
    try {
      const { cache, cachePolicy } = this.options;
      const ttl = cachePolicy[freshness];
      if (cache && ttl > 0) {
        const cached = await cache.get(cacheKey);
        if (cached != null) {
          trace.fromCache = true;
          return cached;
        }
      }
      const allHeaders = { ...headers, "User-Agent": this.options.userAgent };
      const body = this.options.deduplicateRequests
        ? await this.shared(cacheKey, trace, () => this.send(url, allHeaders, display, trace))
        : await this.send(url, allHeaders, display, trace);
      // Don't cache an empty body: for v1 it can mean a transient problem, not "no results".
      if (cache && ttl > 0 && body.trim() !== "") await cache.set(cacheKey, body, ttl);
      return body;
    } catch (e) {
      error = e;
      throw e;
    } finally {
      this.notify(display, trace, this.clock.now() - started, error);
    }
  }

  /** Single flight: identical calls made while one is in flight share its result (or its error). */
  private async shared(key: string, trace: Trace, block: () => Promise<string>): Promise<string> {
    const existing = this.inFlight.get(key);
    if (existing) {
      const outcome = await existing;
      trace.shared = true;
      trace.status = outcome.status;
      return outcome.body;
    }
    const flight = block().then((body) => ({ body, status: trace.status }));
    this.inFlight.set(key, flight);
    try {
      return (await flight).body;
    } finally {
      this.inFlight.delete(key);
    }
  }

  private async send(url: string, headers: Record<string, string>, display: string, trace: Trace): Promise<string> {
    let attempt = 0;
    let rateLimitRetried = false;
    let backoff = this.options.retryBackoffMs;
    for (;;) {
      await this.limiter?.acquire();
      trace.attempts++;
      let response: HttpResponse;
      try {
        response = await this.transport.get(url, headers);
      } catch (e) {
        if (attempt++ < this.options.maxRetries) {
          await this.clock.sleep(backoff);
          backoff *= 2;
          continue;
        }
        const reason = e instanceof Error ? e.message : String(e);
        throw new NetworkError(`Request to ${display} failed: ${reason}`, { cause: e });
      }
      const status = (trace.status = response.status);
      if (status >= 200 && status < 300) return response.body;
      if (status === 429) {
        const retryAfter = header(response, "retry-after");
        const waitMs = retryAfter != null && /^\d+(\.\d+)?$/.test(retryAfter.trim()) ? Number(retryAfter) * 1000 : null;
        if (this.options.retryOnRateLimit && !rateLimitRetried) {
          rateLimitRetried = true;
          await this.clock.sleep(waitMs ?? this.options.rateLimitWaitMs);
          continue;
        }
        throw new RateLimitError(`Rate limit exceeded calling ${display}`, waitMs);
      }
      if (status >= 500 && status < 600 && attempt++ < this.options.maxRetries) {
        await this.clock.sleep(backoff);
        backoff *= 2;
        continue;
      }
      if (response.body.toLowerCase().includes("invalid premium api key")) {
        throw new InvalidApiKeyError(
          `TheSportsDB rejected the API key (HTTP ${status}, ${display}). Only the free keys (123, 3) and paid keys work; v2 needs a paid key.`,
        );
      }
      throw new HttpStatusError(`HTTP ${status} from ${display}`, status, response.body.slice(0, 300));
    }
  }

  private notify(display: string, trace: Trace, durationMs: number, error: unknown): void {
    const listener = this.options.requestListener;
    if (!listener) return;
    const status = trace.status ?? (error instanceof HttpStatusError ? error.status : error instanceof RateLimitError ? 429 : null);
    try {
      listener(Object.freeze({ url: display, status, attempts: trace.attempts, fromCache: trace.fromCache, shared: trace.shared, durationMs, error }));
    } catch {
      // A failing listener must not break the call it is observing.
    }
  }
}

function header(response: HttpResponse, name: string): string | null {
  for (const [k, v] of Object.entries(response.headers)) if (k.toLowerCase() === name) return v;
  return null;
}

function text(value: string | number | boolean | Date): string {
  if (value instanceof Date) return value.toISOString().slice(0, 10);
  if (typeof value === "boolean") return value ? "1" : "0";
  return String(value);
}
