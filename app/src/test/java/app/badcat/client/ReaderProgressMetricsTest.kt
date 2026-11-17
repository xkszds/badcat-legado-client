package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressMetricsTest {
    @Test
    fun calculatesPercentAndPageCountsAtDocumentBoundaries() {
        assertEquals(
            ReaderProgressSnapshot(0, 0, 1, 1),
            ReaderProgressMetrics.calculate(0, 500, 1f, 600),
        )
        assertEquals(
            ReaderProgressSnapshot(1_400, 50, 3, 4),
            ReaderProgressMetrics.calculate(700, 2_000, 1f, 600),
        )
        assertEquals(
            ReaderProgressSnapshot(1_400, 100, 4, 4),
            ReaderProgressMetrics.calculate(9_000, 2_000, 1f, 600),
        )
    }

    @Test
    fun clampsInvalidScaleAndProtectsLargeScrollArithmetic() {
        assertEquals(
            ReaderProgressSnapshot(1_400, 0, 1, 4),
            ReaderProgressMetrics.calculate(0, 2_000, Float.NaN, 600),
        )
        val huge = ReaderProgressMetrics.calculate(
            scrollY = Int.MAX_VALUE,
            contentHeight = Int.MAX_VALUE,
            scale = 4f,
            viewportHeight = 1,
        )
        assertEquals(Int.MAX_VALUE, huge.maxScroll)
        assertEquals(100, huge.percent)
        assertEquals(Int.MAX_VALUE, huge.currentPage)
        assertEquals(Int.MAX_VALUE, huge.totalPages)
    }

    @Test
    fun clampsNegativeDimensionsAndScrollPositions() {
        assertEquals(
            ReaderProgressSnapshot(0, 0, 1, 1),
            ReaderProgressMetrics.calculate(-10, -100, 1f, 0),
        )
    }
}
