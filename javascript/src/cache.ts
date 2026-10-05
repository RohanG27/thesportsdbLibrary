/** How quickly an endpoint's data changes. Every endpoint is tagged with one. */
export type Freshness = "static" | "slow" | "medium" | "live";

/** Time-to-live in milliseconds for each Freshness. Zero or less means don't cache. */
export interface CachePolicy {
  readonly static: number;
  readonly slow: number;
  readonly medium: number;
  readonly live: number;
}

/** Catalogues for a week, teams and players for a day, schedules for an hour, live scores never. */
export const DEFAULT_CACHE_POLICY: CachePolicy = Object.freeze({
  static: 7 * 24 * 3600_000, slow: 24 * 3600_000, medium: 3600_000, live: 0,
});

/**
 * Stores raw response bodies. Keys never contain an API key. Implement it for Redis, KV stores,
 * localStorage, etc.; methods may return promises.
 */
export interface ResponseCache {
  get(key: string): string | null | undefined | Promise<string | null | undefined>;
  set(key: string, body: string, ttlMs: number): void | Promise<void>;
}

/** A bounded in-memory LRU cache with per-entry expiry. */
export class InMemoryResponseCache implements ResponseCache {
  private readonly entries = new Map<string, { body: string; expires: number }>();

  constructor(private readonly maxEntries = 1000, private readonly now: () => number = Date.now) {}

  get(key: string): string | null {
    const e = this.entries.get(key);
    if (!e) return null;
    this.entries.delete(key);
    if (e.expires <= this.now()) return null;
    this.entries.set(key, e); // most recently used last
    return e.body;
  }

  set(key: string, body: string, ttlMs: number): void {
    if (ttlMs <= 0) return;
    this.entries.delete(key);
    this.entries.set(key, { body, expires: this.now() + ttlMs });
    while (this.entries.size > this.maxEntries) {
      const oldest = this.entries.keys().next();
      if (oldest.done) break;
      this.entries.delete(oldest.value);
    }
  }

  keys(): string[] {
    return [...this.entries.keys()];
  }

  clear(): void {
    this.entries.clear();
  }

  get size(): number {
    return this.entries.size;
  }
}
