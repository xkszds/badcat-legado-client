package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderCssSelectorScopeTest {
    @Test
    fun scopesEachCommaSeparatedSelectorIndependently() {
        assertEquals(
            ".reader-root .reader-paragraph, .reader-root .reader-content",
            ReaderCssDocument.scopeSelector(".reader-paragraph, .reader-content"),
        )
    }

    @Test
    fun keepsExistingRootSelectorsAndScopesOtherGroups() {
        assertEquals(
            ".reader-root:hover, .reader-root .reader-paragraph",
            ReaderCssDocument.scopeSelector(".reader-root:hover, .reader-paragraph"),
        )
    }

    @Test
    fun doesNotSplitCommasInsideFunctionalSelectors() {
        assertEquals(
            ".reader-root :not(.first, .second), .reader-root .reader-content",
            ReaderCssDocument.scopeSelector(":not(.first, .second), .reader-content"),
        )
    }

    @Test
    fun doesNotSplitCommasInsideQuotedAttributeValues() {
        assertEquals(
            ".reader-root [data-label='first,second'], .reader-root .reader-content",
            ReaderCssDocument.scopeSelector("[data-label='first,second'], .reader-content"),
        )
    }

    @Test
    fun preservesMultipleAlreadyScopedRootGroups() {
        assertEquals(
            ".reader-root.dark, .reader-root[data-mode='night,quiet']",
            ReaderCssDocument.scopeSelector(".reader-root.dark, .reader-root[data-mode='night,quiet']"),
        )
    }
}
