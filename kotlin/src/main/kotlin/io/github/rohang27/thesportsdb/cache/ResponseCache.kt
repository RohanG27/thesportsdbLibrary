package io.github.rohang27.thesportsdb.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/**
 * How quickly an endpoint's data changes. Every endpoint is tagged with one of these,
 * and a [CachePolicy] turns it into a time-to-live.
 */
public enum class Freshness {
    /** Sports, countries, the league list: changes a few times a year. */
    STATIC,

    /** Teams, players, venues, seasons, honours, a finished event's details. */
    SLOW,

    /** Schedules, standings, TV listings, highlights. */
    MEDIUM,

    /** Live scores. */
    LIVE,
}

/** Time-to-live for each [Freshness]. A zero or negative TTL means "don't cache". */
public data class CachePolicy(
    val static: Duration = 7.days,
    val slow: Duration = 1.days,
    val medium: Duration = 1.hours,
    val live: Duration = Duration.ZERO,
) {
    public fun ttl(freshness: Freshness): Duration = when (freshness) {
        Freshness.STATIC -> static
        Freshness.SLOW -> slow
        Freshness.MEDIUM -> medium
        Freshness.LIVE -> live
    }

    public companion object {
        @JvmField
        public val DEFAULT: CachePolicy = CachePolicy()

        /** Useful defaults with a short cache for live scores too, to soften polling. */
        @JvmField
        public val AGGRESSIVE: CachePolicy = CachePolicy(live = 1.minutes)

        /** Java: builds a policy from `java.time.Duration`s. */
        @JvmStatic
        public fun of(
            static: java.time.Duration,
            slow: java.time.Duration,
            medium: java.time.Duration,
            live: java.time.Duration,
        ): CachePolicy = CachePolicy(
            static.toKotlinDuration(), slow.toKotlinDuration(), medium.toKotlinDuration(), live.toKotlinDuration(),
        )
    }
}

/**
 * Stores raw response bodies. Keys never contain an API key.
 * Implement this to back the cache with Redis, a database, disk, etc.
 */
public interface ResponseCache {
    public suspend fun get(key: String): String?
    public suspend fun put(key: String, body: String, ttl: Duration)
}

/** A bounded, thread-safe, in-memory LRU cache with per-entry expiry. */
public class InMemoryResponseCache(
    private val maxEntries: Int = 1_000,
    private val now: () -> Long = System::currentTimeMillis,
) : ResponseCache {
    private class Entry(val body: String, val expiresAt: Long)

    private val map = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean = size > maxEntries
    }

    override suspend fun get(key: String): String? = synchronized(map) {
        val e = map[key] ?: return null
        if (e.expiresAt <= now()) {
            map.remove(key)
            null
        } else {
            e.body
        }
    }

    override suspend fun put(key: String, body: String, ttl: Duration) {
        if (!ttl.isPositive()) return
        synchronized(map) { map[key] = Entry(body, now() + ttl.inWholeMilliseconds) }
    }

    public fun clear(): Unit = synchronized(map) { map.clear() }

    public val size: Int get() = synchronized(map) { map.size }
}
