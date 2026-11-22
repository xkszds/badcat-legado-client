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

    @Test
    fun processWideFenceRejectsTokenCapturedByAnotherEngineInstance() {
        val firstEngineFence = ChapterContentSaveFence.processWide
        val secondEngineFence = ChapterContentSaveFence.processWide
        val sharedKey = ChapterContentCacheKey("shared-fence-fixture", "book", 901)
        val oldRequest = firstEngineFence.token(sharedKey)

        secondEngineFence.invalidate(sharedKey)

        assertFalse(firstEngineFence.isCurrent(oldRequest))
        assertEquals(oldRequest.version + 1L, secondEngineFence.token(sharedKey).version)
    }

    @Test
    fun concurrentTokenAndInvalidateOperationsKeepEveryGeneration() {
        val fence = ChapterContentSaveFence()
        val sharedKey = ChapterContentCacheKey("concurrent-fence-fixture", "book", 902)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(8)
        val start = java.util.concurrent.CountDownLatch(1)
        try {
            val tasks = (1..100).map { index ->
                executor.submit {
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    if (index % 2 == 0) fence.invalidate(sharedKey)
                    else fence.token(sharedKey)
                }
            }
            start.countDown()
            tasks.forEach { it.get(5, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(50L, fence.token(sharedKey).version)
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun invalidateWaitsForAnInProgressAtomicPublication() {
        val fence = ChapterContentSaveFence()
        val sharedKey = ChapterContentCacheKey("linear-fence-fixture", "book", 903)
        val token = fence.token(sharedKey)
        val enteredPublish = java.util.concurrent.CountDownLatch(1)
        val releasePublish = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val publish = executor.submit<Boolean> {
                fence.publishIfCurrent(token) {
                    enteredPublish.countDown()
                    check(releasePublish.await(5, java.util.concurrent.TimeUnit.SECONDS))
                }
            }
            assertTrue(enteredPublish.await(5, java.util.concurrent.TimeUnit.SECONDS))
            val invalidatingStarted = java.util.concurrent.CountDownLatch(1)
            val invalidated = executor.submit {
                invalidatingStarted.countDown()
                fence.invalidate(sharedKey)
            }
            assertTrue(invalidatingStarted.await(5, java.util.concurrent.TimeUnit.SECONDS))
            assertFalse(invalidated.isDone)
            releasePublish.countDown()
            assertTrue(publish.get(5, java.util.concurrent.TimeUnit.SECONDS))
            invalidated.get(5, java.util.concurrent.TimeUnit.SECONDS)
            assertFalse(fence.isCurrent(token))
        } finally {
            releasePublish.countDown()
            executor.shutdownNow()
        }
    }

}
