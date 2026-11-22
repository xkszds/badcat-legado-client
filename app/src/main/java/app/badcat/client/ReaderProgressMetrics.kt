package app.badcat.client

import kotlin.math.ceil

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
        val validScale = scale.toDouble().let { if (it.isFinite() && it > 0.0) it else 1.0 }
        val scaledContent = contentHeight.coerceAtLeast(0).toDouble() * validScale
        val maxScrollLong = (scaledContent - viewport.toDouble())
            .coerceAtLeast(0.0)
            .coerceAtMost(Int.MAX_VALUE.toDouble())
            .toLong()
        val maxScroll = maxScrollLong.toInt()
        val boundedScroll = scrollY.coerceIn(0, maxScroll)
        val percent = if (maxScrollLong == 0L) 0 else
            ((boundedScroll.toLong() * 100L) / maxScrollLong).toInt().coerceIn(0, 100)
        val totalPages = ceil((maxScrollLong.toDouble() + viewport.toDouble()) / viewport.toDouble())
            .coerceAtMost(Int.MAX_VALUE.toDouble())
            .toInt()
            .coerceAtLeast(1)
        val currentPage = (boundedScroll.toLong() / viewport.toLong() + 1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
            .coerceIn(1, totalPages)
        return ReaderProgressSnapshot(maxScroll, percent, currentPage, totalPages)
    }
}
