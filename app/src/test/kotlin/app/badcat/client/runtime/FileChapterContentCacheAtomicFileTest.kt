package app.badcat.client.runtime

import android.util.AtomicFile
import app.badcat.engine.BookChapter
import app.badcat.engine.CachedChapterContent
import app.badcat.engine.ChapterContent
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileChapterContentCacheAtomicFileTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val cacheDirectory get() = File(context.filesDir, "legado_chapter_cache")

    @Before
    fun clearCacheDirectory() {
        cacheDirectory.deleteRecursively()
    }

    @After
    fun removeCacheDirectory() {
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun legacyUrlEntryRoundTripsThroughAndroidAtomicFile() = runBlocking {
        val cache = FileChapterContentCache(context)
        val sourceId = "https://atomic-legacy.example/"
        val bookUrl = "https://atomic-legacy.example/book"
        val chapterUrl = "https://atomic-legacy.example/chapter/1"
        val expected = CachedChapterContent(
            content = ChapterContent(
                title = "Legacy chapter",
                text = "committed legacy body",
                nextPageUrl = "https://atomic-legacy.example/chapter/1?page=2",
                previousChapterUrl = null,
            ),
            savedAtEpochMillis = 1_700_000_001L,
        )

        cache.put(sourceId, bookUrl, chapterUrl, expected)

        assertEquals(expected, cache.get(sourceId, bookUrl, chapterUrl))
        assertTrue(File(cacheDirectory, "${chapterCacheDigest(sourceId, bookUrl, chapterUrl)}.json").isFile)
    }

    @Test
    fun indexedEntryRoundTripsAndRejectsMismatchedChapterUrl() = runBlocking {
        val cache = FileChapterContentCache(context)
        val sourceId = "https://atomic-index.example/"
        val bookUrl = "https://atomic-index.example/book"
        val chapter = BookChapter(
            index = 7,
            title = "Indexed chapter",
            url = "https://atomic-index.example/chapter/7",
        )
        val expected = CachedChapterContent(
            content = ChapterContent("Indexed chapter", "committed indexed body"),
            savedAtEpochMillis = 1_700_000_007L,
        )
        val token = cache.beginSave(sourceId, bookUrl, chapter)

        assertTrue(cache.put(sourceId, bookUrl, chapter, token, expected))
        assertEquals(expected, cache.get(sourceId, bookUrl, chapter))
        assertNull(cache.get(sourceId, bookUrl, chapter.copy(url = "https://atomic-index.example/replaced")))
    }

    @Test
    fun indexInvalidationDeletesOnlyIndexNamespaceAndPreservesLegacyUrlEntry() = runBlocking {
        val cache = FileChapterContentCache(context)
        val sourceId = "https://atomic-invalidate.example/"
        val bookUrl = "https://atomic-invalidate.example/book"
        val chapterUrl = "https://atomic-invalidate.example/chapter/3"
        val chapter = BookChapter(index = 3, title = "Chapter 3", url = chapterUrl)
        val value = CachedChapterContent(
            content = ChapterContent("Chapter 3", "same URL, separate namespaces"),
            savedAtEpochMillis = 1_700_000_003L,
        )
        val token = cache.beginSave(sourceId, bookUrl, chapter)
        val legacyFile = File(cacheDirectory, "${chapterCacheDigest(sourceId, bookUrl, chapterUrl)}.json")
        val indexFile = File(cacheDirectory, "${chapterCacheDigest(sourceId, bookUrl, chapter.index)}.json")

        cache.put(sourceId, bookUrl, chapterUrl, value)
        assertTrue(cache.put(sourceId, bookUrl, chapter, token, value))
        assertTrue(legacyFile.isFile)
        assertTrue(indexFile.isFile)

        cache.invalidate(sourceId, bookUrl, chapter.index)

        assertNull(cache.get(sourceId, bookUrl, chapter))
        assertNotNull(cache.get(sourceId, bookUrl, chapterUrl))
        assertFalse(indexFile.exists())
        assertTrue(legacyFile.isFile)
    }

    @Test
    fun openReadRestoresLastCommittedEntryAfterInterruptedAtomicWrite() = runBlocking {
        val cache = FileChapterContentCache(context)
        val sourceId = "https://atomic-recovery.example/"
        val bookUrl = "https://atomic-recovery.example/book"
        val chapterUrl = "https://atomic-recovery.example/chapter/1"
        val committed = CachedChapterContent(
            content = ChapterContent("Committed", "old durable body"),
            savedAtEpochMillis = 1_700_000_101L,
        )
        cache.put(sourceId, bookUrl, chapterUrl, committed)

        val entry = File(cacheDirectory, "${chapterCacheDigest(sourceId, bookUrl, chapterUrl)}.json")
        val interruptedWrite = AtomicFile(entry).startWrite()
        interruptedWrite.write("{\"text\":\"partial replacement\"}".toByteArray(Charsets.UTF_8))
        interruptedWrite.flush()
        // Deliberately do not call finishWrite/failWrite: this simulates process death mid-commit.

        assertEquals(committed, cache.get(sourceId, bookUrl, chapterUrl))
        assertFalse(File(entry.path + ".bak").exists())
    }
}
