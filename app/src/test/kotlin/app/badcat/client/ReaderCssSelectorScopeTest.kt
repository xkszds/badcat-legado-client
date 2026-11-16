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
    fun keepsAllowedMultilineReaderCssDeclarations() {
        assertEquals(
            "font-size:18px;line-height:1.8;color:#555555",
            ReaderCssDocument.sanitizeDeclarations(
                "font-size: 18px;\nline-height: 1.8;\ncolor: #555555;",
            ),
        )
    }

    @Test
    fun dropsUnsupportedAndActiveCssDeclarationValues() {
        assertEquals(
            "color:#333333",
            ReaderCssDocument.sanitizeDeclarations(
                "color: #333333; position: fixed; background-color: url(https://bad.invalid/x); " +
                    "font-size: expression(alert(1)); line-height: 1.5 !important;",
            ),
        )
    }

    @Test
    fun rejectsOversizedCssDeclarationBlock() {
        assertEquals("", ReaderCssDocument.sanitizeDeclarations("color:" + "a".repeat(4001)))
    }

    @Test
    fun reportsOnlyExactDuplicateSelectors() {
        assertEquals(
            mapOf(".reader-paragraph" to listOf("profile A / body", "profile B / paragraph")),
            ReaderCssDocument.duplicateSelectorGroups(
                listOf(
                    ".reader-paragraph" to "profile A / body",
                    ".reader-content" to "profile A / content",
                    ".reader-paragraph" to "profile B / paragraph",
                ),
            ),
        )
    }

    @Test
    fun ignoresInvalidSelectorsWhenCheckingDuplicates() {
        assertEquals(
            emptyMap<String, List<String>>(),
            ReaderCssDocument.duplicateSelectorGroups(
                listOf(
                    ".reader-paragraph" to "valid",
                    ".reader-paragraph)" to "invalid",
                ),
            ),
        )
    }

    @Test
    fun preservesAllOwnersForThreeWayDuplicateSelector() {
        assertEquals(
            listOf("first", "second", "third"),
            ReaderCssDocument.duplicateSelectorGroups(
                listOf(
                    ".reader-content" to "first",
                    ".reader-content" to "second",
                    ".reader-content" to "third",
                ),
            )[".reader-content"],
        )
    }

    @Test
    fun keepsCommaInsideQuotedAttributeValueValid() {
        assertEquals(
            "[data-label='first,second']",
            ReaderCssDocument.sanitizeSelector("[data-label='first,second']"),
        )
    }

}
