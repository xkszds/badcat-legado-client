package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun initialSettingParserMatchesPinnedFallbackAndSkipRules() {
        assertNull(LegadoConcurrentRateLimiter.parseInitial(null))
        assertNull(LegadoConcurrentRateLimiter.parseInitial(""))
        assertNull(LegadoConcurrentRateLimiter.parseInitial("0"))

        assertEquals(1 to 100, LegadoConcurrentRateLimiter.parseInitial("100"))
        assertEquals(2 to 100, LegadoConcurrentRateLimiter.parseInitial("2/100"))
        assertEquals(0 to 10, LegadoConcurrentRateLimiter.parseInitial("0/10"))
        assertEquals(-1 to 100, LegadoConcurrentRateLimiter.parseInitial("-1/100"))
        assertEquals(1 to 100, LegadoConcurrentRateLimiter.parseInitial("x/100"))
        assertEquals(2 to 0, LegadoConcurrentRateLimiter.parseInitial("2/x"))
        assertEquals(1 to 0, LegadoConcurrentRateLimiter.parseInitial("invalid"))
        assertEquals(1 to 0, LegadoConcurrentRateLimiter.parseInitial(" "))
        assertEquals(1 to -5, LegadoConcurrentRateLimiter.parseInitial("-5"))
        assertEquals(1 to 0, LegadoConcurrentRateLimiter.parseInitial("/10"))
    }

    @Test
    fun malformedInitialSettingStillCreatesAndCanAcquireTheFirstRecord() {
        val sourceId = "rate-limit-invalid-" + System.nanoTime()
        LegadoConcurrentRateLimiter.acquire(sourceId, "invalid")
        LegadoConcurrentRateLimiter.acquire(sourceId, "0")
        LegadoConcurrentRateLimiter.acquire(sourceId, "2/0")
    }
}
