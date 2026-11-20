package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ChapterContentSaveFenceTest {
    private fun key(index: Int) = ChapterContentCacheKey("source", "book", index)

    @Test
    fun currentTokenCanPublishAndRepeatedTokenCannotOverwriteAfterReplacement() {
        val fence = ChapterContentSaveFence()
        val request = fence.token(key(3))
        var published = ""

        assertTrue(fence.publishIfCurrent(request) { published = "first" })
        fence.replace(key(3)) { published = "replacement" }

        assertFalse(fence.publishIfCurrent(request) { published = "stale" })
        assertEquals("replacement", published)
        assertEquals(1L, fence.token(key(3)).version)
    }

    @Test
    fun failedReplacementStillAdvancesVersionAndFencesOlderRequest() {
        val fence = ChapterContentSaveFence()
        val old = fence.token(key(3))

        assertFailsWith<IllegalStateException> {
            fence.replace(key(3)) { error("disk write failed") }
        }

        assertFalse(fence.isCurrent(old))
        assertEquals(1L, fence.token(key(3)).version)
    }

    @Test
    fun sameUrlChaptersHaveIndependentIndexScopedKeys() {
        val fence = ChapterContentSaveFence()
        val first = key(3)
        val second = key(8)
        val tokenFirst = fence.token(first)
        val tokenSecond = fence.token(second)

        fence.invalidate(first)

        assertFalse(fence.isCurrent(tokenFirst))
        assertTrue(fence.isCurrent(tokenSecond))
        assertEquals(0L, fence.token(second).version)
    }

    @Test
    fun invalidateAdvancesOnlyTheSelectedChapterGeneration() {
        val fence = ChapterContentSaveFence()
        val before = fence.token(key(2))
        fence.invalidate(key(2))
        val after = fence.token(key(2))

        assertEquals(before.version + 1L, after.version)
        assertFalse(fence.publishIfCurrent(before) { error("stale publish must not run") })
    }
}
