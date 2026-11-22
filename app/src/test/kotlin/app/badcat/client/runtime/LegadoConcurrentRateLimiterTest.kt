package app.badcat.client.runtime

import org.junit.Assert.assertTrue
import org.junit.Test

class LegadoConcurrentRateLimiterTest {
    @Test
    fun limitsRequestsByConfiguredWindow() {
        val sourceId = "rate-limit-" + System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "1/80")
        val started = System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "1/80")
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000L
        assertTrue("expected limiter to wait, actual " + elapsedMillis + "ms", elapsedMillis >= 40)
    }

    @Test
    fun skipRateLimitDoesNotConsumeOrWaitForQuota() {
        val sourceId = "rate-limit-skip-" + System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "1/300")
        val started = System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "1/300", skipRateLimit = true)
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000L
        assertTrue("skipRateLimit should bypass the wait, actual " + elapsedMillis + "ms", elapsedMillis < 100)
    }

    @Test
    fun ignoresInvalidAndZeroValues() {
        val sourceId = "rate-limit-invalid-" + System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "invalid")
        LegadoConcurrentRateLimiter.acquire(sourceId, "0")
        LegadoConcurrentRateLimiter.acquire(sourceId, "0/10")
    }
}
