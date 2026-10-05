package sportsdb.http

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sliding-window limiter: at most [permits] calls in any [windowMillis] span.
 * Callers suspend (they are not rejected) until a slot frees up.
 */
internal class RateLimiter(
    private val permits: Int,
    private val windowMillis: Long = 60_000,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val starts = ArrayDeque<Long>()

    init {
        require(permits > 0) { "permits must be positive" }
    }

    suspend fun acquire() {
        mutex.withLock {
            while (true) {
                val t = now()
                while (starts.isNotEmpty() && t - starts.first() >= windowMillis) starts.removeFirst()
                if (starts.size < permits) {
                    starts.addLast(t)
                    return
                }
                delay(windowMillis - (t - starts.first()))
            }
        }
    }
}
