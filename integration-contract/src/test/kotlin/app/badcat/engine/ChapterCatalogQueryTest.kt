package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterCatalogQueryTest {
    private val chapters = listOf(
        BookChapter(0, "第一章 开始", "https://example.test/1"),
        BookChapter(1, "第二章 旅程", "https://example.test/2"),
        BookChapter(2, "第三章 结束", "https://example.test/3"),
    )

    @Test
    fun filtersTitlesCaseInsensitivelyAndKeepsChapterIdentity() {
        val result = ChapterCatalogQuery.apply(chapters, "旅程")
        assertEquals(listOf(chapters[1]), result)
        assertEquals(1, result.single().index)
        assertEquals("https://example.test/2", result.single().url)
    }

    @Test
    fun blankQueryKeepsAllChaptersAndReverseChangesDisplayOnly() {
        assertEquals(chapters, ChapterCatalogQuery.apply(chapters, "  "))
        assertEquals(chapters.reversed(), ChapterCatalogQuery.apply(chapters, "", reverse = true))
        assertEquals(0, chapters.first().index)
    }

    @Test
    fun savedChapterUrlResolvesOnlyWhenItExistsInTheCurrentCatalog() {
        assertEquals(chapters[1], ChapterCatalogQuery.findByUrl(chapters, chapters[1].url))
        assertEquals(null, ChapterCatalogQuery.findByUrl(chapters, "https://example.test/deleted"))
        assertEquals(null, ChapterCatalogQuery.findByUrl(chapters, "  "))
        assertEquals(null, ChapterCatalogQuery.findByUrl(chapters, null))
    }


    @Test
    fun rejectsAmbiguousUrlInsteadOfGuessingFirstChapter() {
        val duplicateUrl = "https://example.test/shared"
        val duplicates = listOf(BookChapter(4, "First", duplicateUrl), BookChapter(9, "Second", duplicateUrl))
        assertEquals(null, ChapterCatalogQuery.findByUrl(duplicates, duplicateUrl))
    }

    @Test
    fun resolvesTypedIdentityByIndexAndChecksUrlAlias() {
        val duplicateUrl = "https://example.test/shared"
        val duplicates = listOf(BookChapter(4, "First", duplicateUrl), BookChapter(9, "Second", duplicateUrl))
        assertEquals(duplicates[1], ChapterCatalogQuery.findByIdentity(duplicates, 9, duplicateUrl))
        assertEquals(null, ChapterCatalogQuery.findByIdentity(duplicates, 9, "https://example.test/stale"))
        assertEquals(null, ChapterCatalogQuery.findByIdentity(duplicates, 7, duplicateUrl))
    }

    @Test
    fun filteringAndReverseCanBeCombined() {
        assertEquals(
            listOf(chapters[2], chapters[0]),
            ChapterCatalogQuery.apply(chapters, "章", reverse = true).filter { it.index != 1 },
        )
    }
}
