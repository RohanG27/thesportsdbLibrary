import { InvalidApiKeyError } from "./errors.js";
import { Helpers } from "./helpers.js";
import { resolveOptions, systemClock, type ResolvedOptions, type SportsDbOptions } from "./options.js";
import { Requester } from "./requester.js";
import { fetchTransport } from "./transport.js";
import { V1Api } from "./v1.js";
import { V2Api } from "./v2.js";

/**
 * The TheSportsDB client.
 *
 * ```ts
 * const db = new SportsDb();                                     // free key 123: v1 only
 * const arsenal = await db.v1.lookup.team(133604);
 * const premium = new SportsDb({ apiKey: process.env.THESPORTSDB_API_KEY, cache: new InMemoryResponseCache() });
 * const tv = await premium.v2.tv.country("Canada");
 * ```
 *
 * Reuse one client per key: its rate limiter, cache and de-duplication cover every call made through it.
 */
export class SportsDb {
  readonly options: ResolvedOptions;
  /** The v1 API (free and premium keys). */
  readonly v1: V1Api;
  /** The v2 API (premium keys only). */
  readonly v2: V2Api;
  /** Common tasks in one call, using v2 or v1 depending on the key. */
  readonly helpers: Helpers;

  constructor(options: SportsDbOptions = {}) {
    this.options = resolveOptions(options);
    const requester = new Requester(this.options, options.transport ?? fetchTransport(this.options.timeoutMs), options.clock ?? systemClock);
    this.v1 = new V1Api(requester);
    this.v2 = new V2Api(requester);
    this.helpers = new Helpers(this);
  }

  /** Whether v2 accepts the key. One call (v2 lookup/league/4328) unless it's a free key. */
  async isPremiumKey(): Promise<boolean> {
    if (this.options.isFreeKey) return false;
    try {
      await this.v2.lookup.league(4328);
      return true;
    } catch (e) {
      if (e instanceof InvalidApiKeyError) return false;
      throw e;
    }
  }
}
