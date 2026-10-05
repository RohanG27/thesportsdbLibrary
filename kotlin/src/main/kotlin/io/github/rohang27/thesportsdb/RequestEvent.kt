package io.github.rohang27.thesportsdb

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Receives one [RequestEvent] per API call, after it finishes. Set it with
 * [SportsDbConfig.requestListener]. It runs on the calling coroutine, so keep it quick.
 * Exceptions it throws are ignored, so a broken listener can't break API calls.
 *
 * ```
 * SportsDbClient {
 *     requestListener = RequestListener { e -> log.debug("${e.url} ${e.status} ${e.duration}") }
 * }
 * ```
 */
public fun interface RequestListener {
    public fun onRequest(event: RequestEvent)
}

/** One finished API call. */
public class RequestEvent internal constructor(
    /** The URL, with any v1 key replaced by `***`. Safe to log. */
    public val url: String,
    /** The last HTTP status, or null if no response arrived or the result came from the cache. */
    public val status: Int?,
    /** HTTP requests made for this call, counting retries. 0 when served from the cache or by another caller's request. */
    public val attempts: Int,
    /** True when the result came from [SportsDbConfig.cache]. */
    public val fromCache: Boolean,
    /** True when an identical call already in flight supplied the result ([SportsDbConfig.deduplicateRequests]). */
    public val shared: Boolean,
    /** Wall-clock time for the whole call, including waits for the rate limiter and retries. */
    public val durationMillis: Long,
    /** The exception the call failed with, or null on success. */
    public val error: Throwable?,
) {
    /** [durationMillis] as a [Duration]. */
    public val duration: Duration get() = durationMillis.milliseconds

    override fun toString(): String =
        "RequestEvent(url=$url, status=$status, attempts=$attempts, fromCache=$fromCache, shared=$shared, " +
            "durationMillis=$durationMillis, error=${error?.javaClass?.simpleName})"
}
