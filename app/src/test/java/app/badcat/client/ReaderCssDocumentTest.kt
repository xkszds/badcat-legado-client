package app.badcat.client

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ReaderCssDocumentTest {
    @Test
    fun rendersStylesForExpandedReaderRegions() {
        val css = ReaderCssDocument.renderRules(
            listOf(
                ".reader-header" to "padding: 8px;",
                ".reader-book-title" to "font-size: 14px; color: #123456;",
                ".reader-chapter-title" to "line-height: 1.4; text-align: center;",
                ".reader-footer" to "opacity: 0.5;",
            ),
        )

        assertTrue(css.contains(".reader-root .reader-header{padding:8px}"))
        assertTrue(css.contains(".reader-root .reader-book-title{font-size:14px;color:#123456}"))
        assertTrue(css.contains(".reader-root .reader-chapter-title{line-height:1.4;text-align:center}"))
        assertTrue(css.contains(".reader-root .reader-footer{opacity:0.5}"))
    }

    @Test
    fun supportsCommonLayoutAndPercentageDeclarationsForChildSelectors() {
        val css = ReaderCssDocument.renderRules(
            listOf(
                ".reader-root .reader-paragraph:nth-child(2)" to
                    "max-width: 92%; text-decoration: underline; word-spacing: 2px;",
                ".reader-content" to "display: block; visibility: visible;",
            ),
        )

        assertTrue(css.contains(".reader-root .reader-paragraph:nth-child(2){max-width:92%;text-decoration:underline;word-spacing:2px}"))
        assertTrue(css.contains(".reader-root .reader-content{display:block;visibility:visible}"))
    }

    @Test
    fun rejectsUnsafeDeclarationsAndKeepsValidRules() {
        val css = ReaderCssDocument.renderRules(
            listOf(
                ".reader-content" to "background-image: url(https://example.invalid/a);",
                ".reader-paragraph" to "font-size: 19px;",
            ),
        )

        assertFalse(css.contains("url("))
        assertTrue(css.contains(".reader-root .reader-paragraph{font-size:19px}"))
    }
}
