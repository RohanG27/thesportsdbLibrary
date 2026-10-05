package io.github.rohang27.thesportsdb.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import io.github.rohang27.thesportsdb.HttpStatusException
import io.github.rohang27.thesportsdb.InvalidApiKeyException
import io.github.rohang27.thesportsdb.NetworkException
import io.github.rohang27.thesportsdb.PremiumRequiredException
import io.github.rohang27.thesportsdb.RateLimitException
import io.github.rohang27.thesportsdb.RequestEvent
import io.github.rohang27.thesportsdb.SportsDbConfig
import io.github.rohang27.thesportsdb.cache.Freshness
import okhttp3.OkHttpClient
import io.github.rohang27.thesportsdb.http.HttpResponse
import io.github.rohang27.thesportsdb.http.OkHttpTransport
import io.github.rohang27.thesportsdb.http.RateLimiter
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/** Builds URLs and runs every call through the rate limiter, retries, cache and parser. */
internal class Requester(
    private val config: SportsDbConfig,
    now: () -> Long = System::currentTimeMillis,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val base = config.baseUrl.toHttpUrl()
    private val limiter = run {
        val rpm = config.requestsPerMinute ?: if (config.isFreeKey) 30 else 100
        if (rpm > 0) RateLimiter(rpm, now = now) else null
    }
    private val tier = if (config.isFreeKey) "free" else "paid"
    private val transport = config.transport ?: OkHttpTransport(
        OkHttpClient.Builder().callTimeout(config.timeout.toJavaDuration()).build(),
    )

    /** `GET /api/v1/json/{key}/{endpoint}?params`. Null params are left out. */
    suspend fun v1(
        endpoint: String,
        recordKey: String,
        freshness: Freshness,
        vararg params: Pair<String, Any?>,
    ): List<JsonObject> {
        val query = params.filter { it.second != null }.map { it.first to it.second.toString() }
        fun build(key: String) = base.newBuilder()
            .addPathSegments("api/v1/json").addPathSegment(key).addPathSegment(endpoint)
            .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()

        val url = build(config.apiKey)
        val display = build("***").redacted()
        val body = fetch(url, emptyMap(), display, "v1:$tier:${display.substringAfter("***/")}", freshness)
        return parseRecords(body, recordKey, display)
    }

    /** `GET /api/v2/json/{segments...}` with the key in the `X-API-KEY` header. */
    suspend fun v2(recordKey: String, freshness: Freshness, vararg segments: Any): List<JsonObject> {
        if (config.isFreeKey) {
            throw PremiumRequiredException(
                "v2 endpoints need a premium key; free keys (123, 3) only work with v1 (client.v1).",
            )
        }
        val url = base.newBuilder().addPathSegments("api/v2/json")
            .apply { segments.forEach { addPathSegment(it.toString()) } }
            .build()
        val display = url.redacted()
        val body = fetch(url, mapOf("X-API-KEY" to config.apiKey), display, "v2:${url.encodedPath}", freshness)
        return parseRecords(body, recordKey, display)
    }

    private suspend fun fetch(
        url: HttpUrl,
        headers: Map<String, String>,
        display: String,
        cacheKey: String,
        freshness: Freshness,
    ): String {
        val started = clock()
        val trace = Trace()
        var outcome: Throwable? = null
        try {
            val cache = config.cache
            val ttl = config.cachePolicy.ttl(freshness)
            if (cache != null && ttl.isPositive()) {
                cache.get(cacheKey)?.let { trace.fromCache = true; return it }
            }

            val allHeaders = headers + ("User-Agent" to config.userAgent)
            val body = if (config.deduplicateRequests) {
                shared(cacheKey, trace) { send(url, allHeaders, display, it) }
            } else {
                send(url, allHeaders, display, trace)
            }

            // Don't cache an empty body: for v1 it can mean a transient problem, not "no results".
            if (cache != null && ttl.isPositive() && body.isNotBlank()) cache.put(cacheKey, body, ttl)
            return body
        } catch (e: Throwable) {
            outcome = e
            throw e
        } finally {
            notify(display, trace, clock() - started, outcome)
        }
    }

    /** What happened during one call, for [RequestListener]. */
    private class Trace(
        var status: Int? = null,
        var attempts: Int = 0,
        var fromCache: Boolean = false,
        var shared: Boolean = false,
    )

    private class Outcome(val body: String, val status: Int?, val attempts: Int)

    /** Calls in flight, by cache key. A null result means the caller making the request was cancelled. */
    private val inFlight = HashMap<String, CompletableDeferred<Result<Outcome>?>>()

    /**
     * Single-flight: the first caller for [key] runs [block]; identical calls that arrive
     * meanwhile wait for its result instead of making their own request. If that first caller
     * is cancelled, a waiter takes over rather than being cancelled too.
     */
    private suspend fun shared(key: String, trace: Trace, block: suspend (Trace) -> String): String {
        while (true) {
            var leader = false
            val deferred = synchronized(inFlight) {
                inFlight[key] ?: CompletableDeferred<Result<Outcome>?>().also { inFlight[key] = it; leader = true }
            }
            if (!leader) {
                val result = deferred.await() ?: continue
                trace.shared = true
                result.getOrNull()?.let { trace.status = it.status }
                return result.getOrThrow().body
            }
            var result: Result<Outcome>? = null
            try {
                result = try {
                    Result.success(Outcome(block(trace), trace.status, trace.attempts))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Result.failure(e)
                }
                return result.getOrThrow().body
            } finally {
                synchronized(inFlight) { inFlight.remove(key) }
                deferred.complete(result)
            }
        }
    }

    private fun notify(display: String, trace: Trace, elapsedMillis: Long, error: Throwable?) {
        val listener = config.requestListener ?: return
        val status = trace.status ?: when (error) {
            is HttpStatusException -> error.status
            is RateLimitException -> 429
            else -> null
        }
        try {
            listener.onRequest(
                RequestEvent(display, status, trace.attempts, trace.fromCache, trace.shared, elapsedMillis, error),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A failing listener must not break the call it is observing.
        }
    }

    private suspend fun send(url: HttpUrl, headers: Map<String, String>, display: String, trace: Trace): String {
        var attempt = 0
        var rateLimitRetried = false
        var backoff = config.retryBackoff
        while (true) {
            limiter?.acquire()
            trace.attempts++
            val response: HttpResponse = try {
                transport.get(url, headers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (attempt++ < config.maxRetries) {
                    delay(backoff); backoff *= 2; continue
                }
                throw NetworkException("Request to $display failed: ${e.message}", e)
            }

            trace.status = response.status
            when (val status = response.status) {
                in 200..299 -> return response.body
                429 -> {
                    val wait = response.header("Retry-After")?.trim()?.toLongOrNull()?.seconds
                    if (config.retryOnRateLimit && !rateLimitRetried) {
                        rateLimitRetried = true
                        delay(wait ?: config.rateLimitWait)
                        continue
                    }
                    throw RateLimitException("Rate limit exceeded calling $display", wait)
                }
                in 500..599 -> if (attempt++ < config.maxRetries) {
                    delay(backoff); backoff *= 2
                } else {
                    throw httpError(status, response.body, display)
                }
                else -> {
                    if (response.body.contains("Invalid Premium API key", ignoreCase = true)) {
                        throw InvalidApiKeyException(
                            "TheSportsDB rejected the API key (HTTP $status, $display). " +
                                "Only the free key 123 and paid keys work; v2 needs a paid key.",
                        )
                    }
                    throw httpError(status, response.body, display)
                }
            }
        }
    }

    private fun httpError(status: Int, body: String, display: String) =
        HttpStatusException("HTTP $status from $display", status, body.take(300))
}

/** The URL as display text. Callers pass v1 URLs built with `***` in place of the key; v2 keys are only in headers. */
private fun HttpUrl.redacted(): String = "$scheme://$host$encodedPath" + (encodedQuery?.let { "?$it" } ?: "")
