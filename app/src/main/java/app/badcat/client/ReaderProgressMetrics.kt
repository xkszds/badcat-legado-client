package app.badcat.client

import kotlin.math.ceil
import kotlin.math.floor

/** Overflow-safe reader progress math shared by the UI and its regression tests. */
data class ReaderProgressSnapshot(
    val maxScroll: Int,
    val percent: Int,
    val currentPage: Int,
    val totalPages: Int,
)

object ReaderProgressMetrics {
    fun calculate(
        scrollY: Int,
        contentHeight: Int,
        scale: Float,
        viewportHeight: Int,
    ): ReaderProgressSnapshot {
        val viewport = viewportHeight.coerceAtLeast(1)
        val scaledContent = (contentHeight.coerceAtLeast(0).toDouble() *
            scale.toDouble().takeIf { it.isFinite() && it > 0.0 } .let { it ?: 1.0 })
        val maxScrollLong = (scaledContent - viewport.toDouble())
            .coerceAtLeast(0.0)
            .coerceAtMost(Int.MAX_VALUE.toDouble())
            .toLong()
        val maxScroll = maxScrollLong.toInt()
        val boundedScroll = scrollY.coerceIn(0, maxScroll)
        val percent = if (maxScrollLong == 0L) 0 else
            ((boundedScroll.toLong() * 100L) / maxScrollLong).toInt().coerceIn(0, 100)
        val totalPages = ceil((maxScrollLong.toDouble() + viewport.toDouble()) / viewport.toDouble())
            .toInt().coerceAtLeast(1)
        val currentPage = (floor(boundedScroll.toDouble() / viewport.toDouble()).toInt() + 1)
            .coerceIn(1, totalPages)
        return ReaderProgressSnapshot(maxScroll, percent, currentPage, totalPages)
    }
}
