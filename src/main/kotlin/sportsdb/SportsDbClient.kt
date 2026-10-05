package sportsdb

import sportsdb.internal.Requester

/**
 * The TheSportsDB client.
 *
 * ```
 * val client = SportsDbClient()                         // free key 123, v1 only
 * val premium = SportsDbClient { apiKey = "..." }       // v1 and v2
 *
 * val arsenal = client.v1.lookup.team(133604)
 * val today = premium.v2.tv.country("Canada")
 * ```
 *
 * One client is safe to share across coroutines and threads; its rate limiter and cache
 * are shared by all calls made through it. Create one per API key and reuse it.
 */
public class SportsDbClient(public val config: SportsDbConfig = SportsDbConfig()) {
    private val requester = Requester(config)

    /** The v1 API (free and premium keys). */
    public val v1: V1Api = V1Api(requester)

    /** The v2 API (premium keys only). */
    public val v2: V2Api = V2Api(requester)

    /**
     * Whether [SportsDbConfig.apiKey] is a premium key: v2 accepts it. Makes one call
     * (v2 `lookup/league/4328`) unless the key is the free key.
     */
    public suspend fun isPremiumKey(): Boolean {
        if (config.isFreeKey) return false
        return try {
            v2.lookup.league(4328)
            true
        } catch (_: InvalidApiKeyException) {
            false
        }
    }
}

/** Builds a client: `SportsDbClient { apiKey = "..."; cache = InMemoryResponseCache() }`. */
public fun SportsDbClient(configure: SportsDbConfig.() -> Unit): SportsDbClient =
    SportsDbClient(SportsDbConfig().apply(configure))
