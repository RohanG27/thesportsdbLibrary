package sportsdb.internal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import sportsdb.HttpStatusException
import sportsdb.InvalidApiKeyException
import sportsdb.NetworkException
import sportsdb.PremiumRequiredException
import sportsdb.RateLimitException
import sportsdb.SportsDbConfig
import sportsdb.cache.Freshness
import sportsdb.http.HttpResponse
import sportsdb.http.RateLimiter
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/** Builds URLs and runs every call through the rate limiter, retries, cache and parser. */
internal class Requester(private val config: SportsDbConfig, now: () -> Long = System::currentTimeMillis) {
    private val base = config.baseUrl.toHttpUrl()
    private val limiter = run {
        val rpm = config.requestsPerMinute ?: if (config.isFreeKey) 30 else 100
        if (rpm > 0) RateLimiter(rpm, now = now) else null
    }
    private val tier = if (config.isFreeKey) "free" else "paid"

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
                "v2 endpoints need a premium key; the free key 123 only works with v1 (client.v1).",
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
        val cache = config.cache
        val ttl = config.cachePolicy.ttl(freshness)
        if (cache != null && ttl.isPositive()) cache.get(cacheKey)?.let { return it }

        val body = send(url, headers + ("User-Agent" to config.userAgent), display)

        // Don't cache an empty body: for v1 it can mean a transient problem, not "no results".
        if (cache != null && ttl.isPositive() && body.isNotBlank()) cache.put(cacheKey, body, ttl)
        return body
    }

    private suspend fun send(url: HttpUrl, headers: Map<String, String>, display: String): String {
        var attempt = 0
        var rateLimitRetried = false
        var backoff = config.retryBackoff
        while (true) {
            limiter?.acquire()
            val response: HttpResponse = try {
                config.transport.get(url, headers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (attempt++ < config.maxRetries) {
                    delay(backoff); backoff *= 2; continue
                }
                throw NetworkException("Request to $display failed: ${e.message}", e)
            }

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
