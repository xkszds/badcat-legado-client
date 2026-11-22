package app.badcat.client

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BadcatReadingStateRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun readsLegacyLastChapterAndScrollKeysAndSurvivesRepositoryRecreation() {
        val source = "source-A"
        val book = "https://book/a"
        val chapter = "https://book/a/2"
        val encodedBook = Base64.encodeToString("$source\n$book".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        val encodedChapter = Base64.encodeToString("$source\n$book\n$chapter".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
        context.getSharedPreferences("badcat_reading_progress", 0).edit()
            .putString("last_chapter_$encodedBook", chapter)
            .putInt("chapter_scroll_$encodedChapter", 321)
            .commit()

        val first = BadcatReadingStateRepository(context)
        assertEquals(chapter, first.lastChapterUrl(source, book))
        assertEquals(321, first.chapterScrollY(source, book, chapter))

        first.saveLastChapterUrl(source, book, "https://book/a/3")
        first.saveChapterScrollY(source, book, chapter, 654)
        val reopened = BadcatReadingStateRepository(context)
        assertEquals("https://book/a/3", reopened.lastChapterUrl(source, book))
        assertEquals(654, reopened.chapterScrollY(source, book, chapter))
    }

    @Test
    fun bookmarkMutationKeepsLegacyKeyShapeAndRemovesCompanionMetadataTogether() {
        val store = BadcatReadingStateRepository(context)
        store.saveBookmark("source", "book", "chapter", "书名", "章节", 77, 1234L)

        val bookmarks = store.bookmarks("source", "book")
        assertEquals(1, bookmarks.size)
        assertEquals("章节", bookmarks.single().chapterTitle)
        assertEquals("书名", bookmarks.single().bookName)
        assertEquals(77, bookmarks.single().scrollY)
        assertEquals(1234L, bookmarks.single().savedAt)
        assertEquals(77, store.bookmarkScrollY("source", "book", "chapter"))

        store.removeBookmark("source", "book", "chapter")
        assertTrue(store.bookmarks().isEmpty())
        assertNull(store.bookmarkScrollY("source", "book", "chapter"))
        val keys = context.getSharedPreferences("badcat_reading_progress", 0).all.keys
        assertFalse(keys.any { it.startsWith("chapter_bookmark_") })
    }

    @Test
    fun bookmarkIdentityIncludesSourceAndBook() {
        val store = BadcatReadingStateRepository(context)
        store.saveBookmark("source-A", "same-book-url", "chapter", "A", "A", 1, 1)
        store.saveBookmark("source-B", "same-book-url", "chapter", "B", "B", 2, 2)
        store.saveBookmark("source-A", "other-book-url", "chapter", "C", "C", 3, 3)

        assertEquals(2, store.bookmarks("source-A").size)
        assertEquals(1, store.bookmarks("source-B", "same-book-url").size)
        assertEquals(3, store.bookmarks().size)
    }

    @Test
    fun historyUpsertAndClearDoNotDeleteProgressOrBookmarks() {
        val store = BadcatReadingStateRepository(context)
        store.saveLastChapterUrl("source", "book", "chapter-1")
        store.saveBookmark("source", "book", "chapter-1", "书", "章", 5, 10)
        store.saveHistory("source", "book", "书", "chapter-1", "第一章", 20)
        store.saveHistory("source", "book", "书", "chapter-2", "第二章", 30)

        assertEquals(1, store.historyRecords().size)
        assertEquals("chapter-2", store.historyRecords().single().chapterUrl)
        assertEquals(30L, store.latestReadTimes()["source|book"])

        store.clearHistory()
        assertTrue(store.historyRecords().isEmpty())
        assertEquals("chapter-1", store.lastChapterUrl("source", "book"))
        assertEquals(1, store.bookmarks().size)
    }

    @Test
    fun latestReadTimesIgnoresLegacyRowsWithoutPositiveTimestamp() {
        context.getSharedPreferences(ReadingHistoryActivity.PREFS, 0).edit()
            .putString(ReadingHistoryActivity.KEY,
                """[{"sourceId":"s","bookUrl":"b","chapterUrl":"c","updatedAt":0}]""")
            .commit()

        assertTrue(BadcatReadingStateRepository(context).latestReadTimes().isEmpty())
    }

    @Test
    fun malformedHistoryJsonFailsClosedWithoutThrowing() {
        context.getSharedPreferences(ReadingHistoryActivity.PREFS, 0).edit()
            .putString(ReadingHistoryActivity.KEY, "{not-json")
            .commit()

        assertTrue(BadcatReadingStateRepository(context).historyRecords().isEmpty())
    }
}
