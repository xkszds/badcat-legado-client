package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CssRuleEvaluatorTest {

    private val document = SourceDocument(
        url = "https://reader.example/search",
        body = """
            <html><body>
              <div class="book"><a class="title" href="/book/1">  Alpha <b>Novel</b> </a>
                <img src="/covers/1.jpg" data-id="one" data-label="@cover">
                <p class="intro">First intro</p>
              </div>
              <div class="book"><a class="title" href="/book/2">Beta Novel</a>
                <img src="/covers/2.jpg" data-id="two">
                <p class="intro">Second intro</p>
              </div>
            </body></html>
        """.trimIndent(),
    )

    @Test
    fun forcedDefaultCssPrefixUsesJsoupAndKeepsCollectionIndexSuffix() {
        val scalar = CssRuleEvaluator.evaluate(document, "@@a.title@text")
        assertEquals(
            listOf("Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(scalar).value,
        )

        val indexed = CssRuleEvaluator.evaluateElements(document, "@@div.book.-1")
        val fragments = assertIs<EngineResult.Success<List<String>>>(indexed).value
        assertEquals(1, fragments.size)
        kotlin.test.assertTrue(fragments.single().contains("Beta Novel"))
        kotlin.test.assertFalse(fragments.single().contains("Alpha"))
    }

    @Test
    fun extractsTextFromAllCssMatches() {
        val result = CssRuleEvaluator.evaluate(document, "a.title@text")
        val prefixed = CssRuleEvaluator.evaluate(document, "@CSS: a.title@text")
        assertEquals(
            listOf("Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(prefixed).value,
        )
        assertEquals(
            listOf("Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun extractsAttributesAndResolvesRelativeUrls() {
        val href = CssRuleEvaluator.evaluate(document, "a.title@href")
        val src = CssRuleEvaluator.evaluate(document, "img@src")
        assertEquals(
            listOf("https://reader.example/book/1", "https://reader.example/book/2"),
            assertIs<EngineResult.Success<List<String>>>(href).value,
        )
        assertEquals(
            listOf("https://reader.example/covers/1.jpg", "https://reader.example/covers/2.jpg"),
            assertIs<EngineResult.Success<List<String>>>(src).value,
        )
    }

    @Test
    fun extractsHtmlAndNamedAttribute() {
        val html = CssRuleEvaluator.evaluate(document, "a.title@html")
        val ids = CssRuleEvaluator.evaluate(document, "img@attr(data-id)")
        val directIds = CssRuleEvaluator.evaluate(document, "img@data-id")
        val htmlValues = assertIs<EngineResult.Success<List<String>>>(html).value
        assertEquals(1, htmlValues.size)
        kotlin.test.assertTrue(htmlValues.single().contains("<a class=\"title\""))
        kotlin.test.assertTrue(htmlValues.single().contains("<b>Novel</b>"))
        kotlin.test.assertTrue(htmlValues.single().contains("Beta Novel"))
        assertEquals(
            listOf("one", "two"),
            assertIs<EngineResult.Success<List<String>>>(ids).value,
        )
        assertEquals(
            listOf("one", "two"),
            assertIs<EngineResult.Success<List<String>>>(directIds).value,
        )
    }

    @Test
    fun evaluatesUpstreamDefaultModeChainedSelectorsAndIndexes() {
        val chainedText = CssRuleEvaluator.evaluate(
            document,
            "div.book@a.title@text",
        )
        assertEquals(
            listOf("Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(chainedText).value,
        )

        val chainedIndexedHref = CssRuleEvaluator.evaluate(
            document,
            "div.book@a.title.0@href",
        )
        assertEquals(
            listOf("https://reader.example/book/1", "https://reader.example/book/2"),
            assertIs<EngineResult.Success<List<String>>>(chainedIndexedHref).value,
        )
    }

    @Test
    fun chainedSelectorTokenizerDoesNotSplitAtSignsInsideCssAttributes() {
        val result = CssRuleEvaluator.evaluate(
            document,
            """div.book@img[data-label="@cover"]@data-id""",
        )
        assertEquals(
            listOf("one"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun ignoresAtSignsInsideQuotedCssAttributeValues() {
        val result = CssRuleEvaluator.evaluate(
            document,
            """img[data-label="@cover"]@data-id""",
        )
        assertEquals(
            listOf("one"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun extractsOwnTextAndTagName() {
        val ownText = CssRuleEvaluator.evaluate(document, "a.title@ownText")
        val tag = CssRuleEvaluator.evaluate(document, "a.title@tag")
        assertEquals(
            listOf("Alpha", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(ownText).value,
        )
        assertEquals(
            listOf("a", "a"),
            assertIs<EngineResult.Success<List<String>>>(tag).value,
        )
    }


    @Test
    fun extractsLegadoTextNodesAndAllModes() {
        val textNodes = CssRuleEvaluator.evaluate(document, "a.title@textNodes")
        assertEquals(
            listOf("Alpha", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(textNodes).value,
        )

        val all = CssRuleEvaluator.evaluate(document, "a.title:first-child@all")
        val markup = assertIs<EngineResult.Success<List<String>>>(all).value.single()
        kotlin.test.assertTrue(markup.contains("<a class=\"title\""))
        kotlin.test.assertTrue(markup.contains("<b>Novel</b>"))
        kotlin.test.assertTrue(markup.contains("Beta Novel"))
    }

    @Test
    fun combinesCssRulesWithLegadoOperators() {
        val fallback = CssRuleEvaluator.evaluate(
            document,
            "a.missing@text || p.intro@text",
        )
        assertEquals(
            listOf("First intro", "Second intro"),
            assertIs<EngineResult.Success<List<String>>>(fallback).value,
        )

        val concatenated = CssRuleEvaluator.evaluate(
            document,
            "a.title@text && p.intro@text",
        )
        assertEquals(
            listOf("Alpha Novel", "Beta Novel", "First intro", "Second intro"),
            assertIs<EngineResult.Success<List<String>>>(concatenated).value,
        )
    }

    @Test
    fun evaluatesMixedNestedCombinationOperatorsRecursively() {
        val leftNested = CssRuleEvaluator.evaluate(
            document,
            "a.title@text && p.intro@text || a.missing@text",
        )
        val rightNested = CssRuleEvaluator.evaluate(
            document,
            "a.missing@text || p.intro@text && a.title@text",
        )

        assertEquals(
            listOf("Alpha Novel", "Beta Novel", "First intro", "Second intro"),
            assertIs<EngineResult.Success<List<String>>>(leftNested).value,
        )
        assertEquals(
            listOf("First intro", "Second intro", "Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(rightNested).value,
        )
    }

    @Test
    fun rejectsUnsupportedSuffixAndMalformedSelector() {
        val unsupported = CssRuleEvaluator.evaluate(document, "a.title@attr()")
        assertIs<EngineResult.Failure>(unsupported)

        val malformed = CssRuleEvaluator.evaluate(document, "a[")
        assertIs<EngineResult.Failure>(malformed)
    }

    @Test
    fun selectsCollectionElementsAsIndependentHtmlFragments() {
        val items = CssRuleEvaluator.evaluateElements(document, "div.book")
        val fragments = assertIs<EngineResult.Success<List<String>>>(items).value

        assertEquals(2, fragments.size)
        kotlin.test.assertTrue(fragments[0].contains("Alpha"))
        kotlin.test.assertTrue(fragments[0].contains("First intro"))
        kotlin.test.assertFalse(fragments[0].contains("Beta Novel"))
        kotlin.test.assertTrue(fragments[1].contains("Beta Novel"))
        kotlin.test.assertTrue(fragments[1].contains("Second intro"))
        kotlin.test.assertFalse(fragments[1].contains("Alpha"))
    }

    @Test
    fun collectionExtractionKeepsTopLevelCombinationBranchesSeparate() {
        val items = CssRuleEvaluator.evaluateElements(document, "div.book && div.book")
        val fragments = assertIs<EngineResult.Success<List<String>>>(items).value

        assertEquals(4, fragments.size)
        assertEquals(2, fragments.count { it.contains("Alpha") })
        assertEquals(2, fragments.count { it.contains("Beta Novel") })
    }


    @Test
    fun appliesLegadoIndexSuffixesOnlyToCollectionExtraction() {
        val last = CssRuleEvaluator.evaluateElements(document, "div.book.-1")
        val selected = assertIs<EngineResult.Success<List<String>>>(last).value
        assertEquals(1, selected.size)
        kotlin.test.assertTrue(selected.single().contains("Beta Novel"))

        val bracket = CssRuleEvaluator.evaluateElements(document, "div.book[0]")
        val first = assertIs<EngineResult.Success<List<String>>>(bracket).value
        assertEquals(1, first.size)
        kotlin.test.assertTrue(first.single().contains("Alpha"))

        // Upstream @CSS: rules bypass ElementsSingle index processing.
        val explicitCss = CssRuleEvaluator.evaluateElements(document, "@CSS: div.book.0")
        val explicitCssValues = (explicitCss as? EngineResult.Success)?.value.orEmpty()
        kotlin.test.assertFalse(explicitCssValues.any { it.contains("Alpha") })

        // Scalar CSS evaluation is intentionally not given ElementsSingle suffix parsing.
        val scalar = CssRuleEvaluator.evaluate(document, "div.book.-1@text")
        val scalarValues = (scalar as? EngineResult.Success)?.value.orEmpty()
        kotlin.test.assertFalse(scalarValues.contains("Beta Novel"))
    }

}
