package app.badcat.client.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 * Port of Legado's ConcurrentRateLimiter.fetchStart/getConcurrentRecordBlocking.
 * Records are shared across runtime instances and keyed by the imported source ID.
 */
internal object LegadoConcurrentRateLimiter {
    private data class Record(
        var timeMillis: Long,
        var accessLimit: Int,
        var intervalMillis: Int,
        var frequency: Int,
    )

    private val records = ConcurrentHashMap<String, Record>()

    fun acquire(sourceId: String, setting: String?, skipRateLimit: Boolean = false) {
        if (skipRateLimit) return
        val parsed = parseInitial(setting) ?: return
        val (accessLimit, interval) = parsed
        val record = records.computeIfAbsent(sourceId) {
            Record(System.currentTimeMillis(), accessLimit, interval, 1)
        }
        synchronized(record) {
            // Legado's updateConcurrentRate preserves the current window/frequency
            // while applying newly imported source settings.
            record.accessLimit = accessLimit
            record.intervalMillis = interval
        }
        while (true) {
            val waitMillis = synchronized(record) {
                val now = System.currentTimeMillis()
                val nextWindow = record.timeMillis + record.intervalMillis
                when {
                    now >= nextWindow -> {
                        record.timeMillis = now
                        record.frequency = 1
                        0L
                    }
                    record.frequency < record.accessLimit -> {
                        record.frequency++
                        0L
                    }
                    else -> nextWindow - now
                }
            }
            if (waitMillis <= 0L) return
            try {
                Thread.sleep(waitMillis)
            } catch (interrupted: InterruptedException) {
                Thread.currentThread().interrupt()
                throw java.util.concurrent.CancellationException("Interrupted while waiting for source rate limit").also {
                    it.initCause(interrupted)
                }
            }
        }
    }

    /**
     * Matches the pinned BaseSource initial fetchStart parser. Unlike
     * updateConcurrentRate, malformed imported settings still create a record
     * with the upstream fallback values; only null, empty, and exact "0" skip.
     */
    internal fun parseInitial(value: String?): Pair<Int, Int>? {
        if (value.isNullOrEmpty() || value == "0") return null
        val slash = value.indexOf('/')
        return if (slash > 0) {
            val accessLimit = value.substring(0, slash).toIntOrNull() ?: 1
            val interval = value.substring(slash + 1).toIntOrNull() ?: 0
            accessLimit to interval
        } else {
            1 to (value.toIntOrNull() ?: 0)
        }
    }
}
