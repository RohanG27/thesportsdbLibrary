/** One finished API call, passed to the requestListener option. */
export interface RequestEvent {
  /** The URL with any v1 key replaced by ***. Safe to log. */
  readonly url: string;
  /** The last HTTP status; null if no response arrived or the result came from the cache. */
  readonly status: number | null;
  /** HTTP requests made, counting retries; 0 when served by the cache or another caller's request. */
  readonly attempts: number;
  readonly fromCache: boolean;
  /** True when an identical call already in flight supplied the result. */
  readonly shared: boolean;
  /** Milliseconds for the whole call, including rate-limit waits and retries. */
  readonly durationMs: number;
  readonly error: unknown;
}
