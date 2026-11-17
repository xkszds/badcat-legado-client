package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesFolderQueryTest {
    private val books = listOf(
        FavoriteBook("source-a", "https://example.test/a", "A"),
        FavoriteBook("source-b", "https://example.test/b", "B", folderId = "folder-1"),
        FavoriteBook("source-c", "https://example.test/c", "C", folderId = "folder-2")
    )

    @Test
    fun allNovelsIncludesDefaultAndCustomFolders() {
        assertEquals(books, favoriteBooksInFolder(books, FavoriteBooks.DEFAULT_FOLDER_ID))
        assertEquals(3, favoriteBookCountInFolder(books, FavoriteBooks.DEFAULT_FOLDER_ID))
    }

    @Test
    fun customFolderOnlyIncludesItsOwnNovels() {
        assertEquals(listOf(books[1]), favoriteBooksInFolder(books, "folder-1"))
        assertEquals(1, favoriteBookCountInFolder(books, "folder-1"))
    }

    @Test
    fun sortUsesLastReadTimestampAndFallsBackToFavoriteTime() {
        val withTimes = listOf(
            FavoriteBook("source-a", "https://example.test/a", "A", addedAt = 100L),
            FavoriteBook("source-b", "https://example.test/b", "B", addedAt = 300L),
            FavoriteBook("source-c", "https://example.test/c", "C", addedAt = 200L),
        )
        val lastRead = mapOf("source-a|https://example.test/a" to 500L)
        assertEquals(
            listOf("A", "B", "C"),
            sortFavoriteBooksByLastRead(withTimes, lastRead, newestFirst = false).map { it.name },
        )
        assertEquals(
            listOf("A", "B", "C"),
            sortFavoriteBooksByLastRead(withTimes, lastRead, newestFirst = true).map { it.name }.reversed(),
        )
    }

    @Test
    fun newestFirstSortUsesMostRecentlyReadBookFirst() {
        val withTimes = listOf(
            FavoriteBook("source-a", "https://example.test/a", "A", addedAt = 100L),
            FavoriteBook("source-b", "https://example.test/b", "B", addedAt = 300L),
            FavoriteBook("source-c", "https://example.test/c", "C", addedAt = 200L),
        )
        val lastRead = mapOf(
            "source-a|https://example.test/a" to 500L,
            "source-b|https://example.test/b" to 150L,
        )
        assertEquals(
            listOf("A", "C", "B"),
            sortFavoriteBooksByLastRead(withTimes, lastRead, newestFirst = true).map { it.name },
        )
    }

    @Test
    fun emptyCustomFolderHasZeroCount() {
        assertEquals(emptyList<FavoriteBook>(), favoriteBooksInFolder(books, "missing"))
        assertEquals(0, favoriteBookCountInFolder(books, "missing"))
    }
}
