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
    fun supportsFlexLayoutAndOverflowDeclarationsForReaderChildren() {
        val css = ReaderCssDocument.renderRules(
            listOf(
                ".reader-content" to
                    "display: flex; flex-direction: column; gap: 8px; overflow-y: auto; box-sizing: border-box;",
                ".reader-paragraph" to "text-overflow: ellipsis; hyphens: auto; text-shadow: 1px 1px 2px #000000;",
            ),
        )

        assertTrue(css.contains(".reader-root .reader-content{display:flex;flex-direction:column;gap:8px;overflow-y:auto;box-sizing:border-box}"))
        assertTrue(css.contains(".reader-root .reader-paragraph{text-overflow:ellipsis;hyphens:auto;text-shadow:1px 1px 2px #000000}"))
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

    @Test
    fun rejectsScriptLikeValuesAndAtRulesWithoutDroppingSafeDeclarations() {
        val css = ReaderCssDocument.renderRules(
            listOf(
                ".reader-content" to "color: #123456; background-image: url(javascript:alert(1));",
                ".reader-paragraph" to "font-size: 18px; behavior: expression(alert(1));",
                "@import" to "color: #ffffff;",
            ),
        )
        assertTrue(css.contains(".reader-root .reader-content{color:#123456}"))
        assertTrue(css.contains(".reader-root .reader-paragraph{font-size:18px}"))
        assertFalse(css.contains("javascript", ignoreCase = true))
        assertFalse(css.contains("expression", ignoreCase = true))
        assertFalse(css.contains("@import", ignoreCase = true))
    }

}