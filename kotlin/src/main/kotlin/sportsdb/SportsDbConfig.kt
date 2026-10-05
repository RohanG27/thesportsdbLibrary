package sportsdb

import sportsdb.cache.CachePolicy
import sportsdb.cache.ResponseCache
import sportsdb.http.HttpTransport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
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

    /**
     * Share one HTTP request between identical calls made at the same time (same URL and key),
     * so ten coroutines asking for the same table cause one request, not ten.
     */
    public var deduplicateRequests: Boolean = true

    /** Called once per call, after it finishes: for logging and metrics. See [RequestEvent]. */
    public var requestListener: RequestListener? = null

    /** Optional response cache; `null` (the default) disables caching. */
    public var cache: ResponseCache? = null

    /** Time-to-live per kind of data, used when [cache] is set. */
    public var cachePolicy: CachePolicy = CachePolicy.DEFAULT

    /** Time allowed for one HTTP request (connect, send and read). Applies to the default transport. */
    public var timeout: Duration = 30.seconds

    /**
     * The HTTP stack. `null` (the default) uses OkHttp with [timeout]. Pass
     * `OkHttpTransport(yourClient)` to share an OkHttp client (its own timeouts then apply).
     */
    public var transport: HttpTransport? = null

    /** Change only for testing or a proxy. */
    public var baseUrl: String = "https://www.thesportsdb.com"

    public var userAgent: String = "sportsdb-kotlin"

    // Java callers can't use kotlin.time.Duration setters; these take java.time.Duration.

    /** Java: sets [retryBackoff]. */
    public fun setRetryBackoff(value: java.time.Duration) { retryBackoff = value.toKotlinDuration() }

    /** Java: sets [timeout]. */
    public fun setTimeout(value: java.time.Duration) { timeout = value.toKotlinDuration() }

    /** Java: sets [rateLimitWait]. */
    public fun setRateLimitWait(value: java.time.Duration) { rateLimitWait = value.toKotlinDuration() }

    /**
     * True when [apiKey] is one of the public free keys ([FREE_API_KEYS]). Any other key the
     * API accepts is a paid key; see [SportsDbClient.isPremiumKey] to check one.
     */
    public val isFreeKey: Boolean get() = apiKey in FREE_API_KEYS

    public companion object {
        /** The public key for development and testing. */
        public const val FREE_API_KEY: String = "123"

        /**
         * Keys the API accepts without payment: `123` (documented) and `3` (an older key that
         * still works, with the same limits; measured 5 Oct 2026).
         */
        @JvmField
        public val FREE_API_KEYS: Set<String> = setOf(FREE_API_KEY, "3")
    }
}
