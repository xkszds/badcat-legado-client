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
    @Test
    fun acceptsValidCommaSeparatedGroupsWithFunctionalCommas() {
        assertEquals(
            ":not(.first, .second), .reader-content",
            ReaderCssDocument.sanitizeSelector(":not(.first, .second), .reader-content"),
        )
    }

    @Test
    fun rejectsUnbalancedParenthesesAndBrackets() {
        assertEquals(null, ReaderCssDocument.sanitizeSelector(":not(.first, .second"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector("[data-mode='night'"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector(".reader-content)"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector(".reader-content]"))
    }

    @Test
    fun rejectsUnclosedQuotesAndEmptySelectorGroups() {
        assertEquals(null, ReaderCssDocument.sanitizeSelector("[data-label='unfinished]"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector(".reader-content,"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector(", .reader-content"))
        assertEquals(null, ReaderCssDocument.sanitizeSelector(".reader-content,,.reader-paragraph"))
    }

    @Test
    fun keepsCommaInsideQuotedAttributeValueValid() {
        assertEquals(
            "[data-label='first,second']",
            ReaderCssDocument.sanitizeSelector("[data-label='first,second']"),
        )
    }

}
