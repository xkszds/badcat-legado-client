package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PurificationPipelineTest {
    @Test
    fun chapterReplacementOnlyReceivesChapterText() {
        val pipeline = PurificationPipeline(
            bookTextReplacement = BookTextReplacement { it.replace("ad", "") },
            pageAdFilter = PageAdFilter { "page:$it" },
            catalogCleaner = CatalogCleaner { it },
            videoRuleProcessor = VideoRuleProcessor { "video:$it" },
        )

        assertEquals("content", pipeline.cleanChapterText("adcontent"))
        assertEquals("page:<html/>", pipeline.cleanPageHtml("<html/>"))
        assertEquals("video:episode", pipeline.cleanVideoPayload("episode"))
    }

    @Test
    fun catalogCleanerReceivesChapterMetadata() {
        val input = listOf(
            BookChapter(index = 0, title = "ad chapter", url = "/0"),
            BookChapter(index = 1, title = "real chapter", url = "/1"),
        )
        val pipeline = PurificationPipeline(
            bookTextReplacement = BookTextReplacement { it },
            pageAdFilter = PageAdFilter { it },
            catalogCleaner = CatalogCleaner { chapters ->
                chapters.filterNot { it.title.startsWith("ad ") }
            },
            videoRuleProcessor = VideoRuleProcessor { it },
        )

        assertEquals(listOf(input[1]), pipeline.cleanCatalog(input))
    }
}
