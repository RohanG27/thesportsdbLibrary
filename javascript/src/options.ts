import { DEFAULT_CACHE_POLICY, type CachePolicy, type ResponseCache } from "./cache.js";
import type { RequestEvent } from "./events.js";
import type { Transport } from "./transport.js";

/** The documented public key for development and testing. */
export const FREE_API_KEY = "123";

/** Keys the API accepts without payment: 123 and the older 3 (same limits; measured 5 Oct 2026). */
export const FREE_API_KEYS: readonly string[] = Object.freeze([FREE_API_KEY, "3"]);

/** Time source for rate limiting and retries. Replace it in tests to avoid real waits. */
export interface Clock {
  now(): number;
  sleep(ms: number): Promise<void>;
}

export const systemClock: Clock = {
  now: () => Date.now(),
  sleep: (ms) => new Promise((resolve) => setTimeout(resolve, Math.max(0, ms))),
};

export interface SportsDbOptions {
  /** Your API key. Default: the free key "123". */
  apiKey?: string;
  /** Client-side limit; default 30 for a free key, 100 otherwise; 0 turns it off. */
  requestsPerMinute?: number;
  /** Retries for network errors and HTTP 5xx, with exponential backoff. Default 2. */
  maxRetries?: number;
  /** First backoff in ms; doubles on every retry. Default 500. */
  retryBackoffMs?: number;
  /** On HTTP 429, wait (Retry-After, or rateLimitWaitMs) and retry once. Default true. */
  retryOnRateLimit?: boolean;
  /** Default 60000. */
  rateLimitWaitMs?: number;
  /** Optional response cache. */
  cache?: ResponseCache;
  /** Time-to-live per kind of data, in ms. */
  cachePolicy?: Partial<CachePolicy>;
  /** Per request, in ms; applies to the default transport. Default 30000. */
  timeoutMs?: number;
  /** Identical calls made at the same time share one HTTP request. Default true. */
  deduplicateRequests?: boolean;
  /** Called once per call, after it finishes. Errors it throws are ignored. */
  requestListener?: (event: RequestEvent) => void;
  /** Default the global fetch with timeoutMs. */
  transport?: Transport;
  /** For tests. */
  clock?: Clock;
  baseUrl?: string;
  /** Sent as User-Agent (ignored by browsers). */
  userAgent?: string;
}

/** The resolved settings. */
export interface ResolvedOptions {
  readonly apiKey: string;
  readonly requestsPerMinute: number;
  readonly maxRetries: number;
  readonly retryBackoffMs: number;
  readonly retryOnRateLimit: boolean;
  readonly rateLimitWaitMs: number;
  readonly cache: ResponseCache | null;
  readonly cachePolicy: CachePolicy;
  readonly timeoutMs: number;
  readonly deduplicateRequests: boolean;
  readonly requestListener: ((event: RequestEvent) => void) | null;
  readonly baseUrl: string;
  readonly userAgent: string;
  readonly isFreeKey: boolean;
}

export function resolveOptions(o: SportsDbOptions): ResolvedOptions {
  const apiKey = o.apiKey ?? FREE_API_KEY;
  const isFreeKey = FREE_API_KEYS.includes(apiKey);
  return Object.freeze({
    apiKey,
    requestsPerMinute: o.requestsPerMinute ?? (isFreeKey ? 30 : 100),
    maxRetries: o.maxRetries ?? 2,
    retryBackoffMs: o.retryBackoffMs ?? 500,
    retryOnRateLimit: o.retryOnRateLimit ?? true,
    rateLimitWaitMs: o.rateLimitWaitMs ?? 60_000,
    cache: o.cache ?? null,
    cachePolicy: Object.freeze({ ...DEFAULT_CACHE_POLICY, ...o.cachePolicy }),
    timeoutMs: o.timeoutMs ?? 30_000,
    deduplicateRequests: o.deduplicateRequests ?? true,
    requestListener: o.requestListener ?? null,
    baseUrl: (o.baseUrl ?? "https://www.thesportsdb.com").replace(/\/+$/, ""),
    userAgent: o.userAgent ?? "sportsdb-js",
    isFreeKey,
  });
}
