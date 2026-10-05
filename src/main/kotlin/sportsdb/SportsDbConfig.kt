package sportsdb

import sportsdb.cache.CachePolicy
import sportsdb.cache.ResponseCache
import sportsdb.http.HttpTransport
import sportsdb.http.OkHttpTransport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/**
 * Client settings. Build with [SportsDbClient]'s DSL:
 *
 * ```
 * val client = SportsDbClient { apiKey = System.getenv("THESPORTSDB_API_KEY") }
 * ```
 */
public class SportsDbConfig {
    /** Your API key. Defaults to the public free key `123` (v1 only, small result limits). */
    public var apiKey: String = FREE_API_KEY

    /**
     * Client-side limit, in requests per minute, shared by v1 and v2. `null` picks the
     * documented limit for the key: 30 for the free key, 100 for a premium key.
     * Set it to 120 on a business plan, or `0` to turn limiting off.
     */
    public var requestsPerMinute: Int? = null

    /** Retries for network errors and HTTP 5xx, with exponential backoff. */
    public var maxRetries: Int = 2

    /** First backoff delay; doubles on every retry. */
    public var retryBackoff: Duration = Duration.parse("500ms")

    /** On HTTP 429, wait (the `Retry-After` time, or [rateLimitWait]) and try once more. */
    public var retryOnRateLimit: Boolean = true

    /** How long to wait after a 429 when the server does not say. The docs say one minute. */
    public var rateLimitWait: Duration = 1.minutes

    /** Optional response cache; `null` (the default) disables caching. */
    public var cache: ResponseCache? = null

    /** Time-to-live per kind of data, used when [cache] is set. */
    public var cachePolicy: CachePolicy = CachePolicy.DEFAULT

    /** The HTTP stack. Pass `OkHttpTransport(yourClient)` to share an OkHttp client. */
    public var transport: HttpTransport = OkHttpTransport()

    /** Change only for testing or a proxy. */
    public var baseUrl: String = "https://www.thesportsdb.com"

    public var userAgent: String = "sportsdb-kotlin"

    // Java callers can't use kotlin.time.Duration setters; these take java.time.Duration.

    /** Java: sets [retryBackoff]. */
    public fun setRetryBackoff(value: java.time.Duration) { retryBackoff = value.toKotlinDuration() }

    /** Java: sets [rateLimitWait]. */
    public fun setRateLimitWait(value: java.time.Duration) { rateLimitWait = value.toKotlinDuration() }

    /** True when [apiKey] is the public free key. Paid keys are not checked offline; see [SportsDbClient.isPremiumKey]. */
    public val isFreeKey: Boolean get() = apiKey == FREE_API_KEY

    public companion object {
        /** The public key for development and testing. */
        public const val FREE_API_KEY: String = "123"
    }
}
