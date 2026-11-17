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
    fun evaluatesLegacyAtRootMarkerWithIndexedTextNodesSuffix() {
        val page = SourceDocument(
            url = "https://reader.example/book/7",
            body = """
                <ul class="w_txt">
                  <li>zero</li><li>one</li><li>two</li><li>three</li>
                  <li>four <b>nested</b> ignored-direct-node</li>
                </ul>
            """.trimIndent(),
        )
        val result = LegadoRuleExpressionDispatcher.evaluate(
            page,
            "@.w_txt li.4@textNodes",
        )
        assertEquals(
            listOf("four\nignored-direct-node"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

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
    fun scalarDefaultModeAppliesLegacyTagClassAndIndexSyntax() {
        val firstAnchor = CssRuleEvaluator.evaluate(
            document,
            "@@tag.a.0@href",
        )
        assertEquals(
            listOf("https://reader.example/book/1"),
            assertIs<EngineResult.Success<List<String>>>(firstAnchor).value,
        )

        val nestedAnchor = CssRuleEvaluator.evaluate(
            document,
            "@@class.book@tag.a.0@href",
        )
        assertEquals(
            listOf("https://reader.example/book/1", "https://reader.example/book/2"),
            assertIs<EngineResult.Success<List<String>>>(nestedAnchor).value,
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
    fun acceptsLeadingLegadoCssMarkerWithIndexedTextNodes() {
        val source = SourceDocument(
            url = "https://reader.example/book/1",
            body = """
                <ul class="w_txt">
                  <li>Chapter 0</li>
                  <li>Chapter 1</li>
                  <li>Chapter 2</li>
                  <li>Chapter 3</li>
                  <li>Chapter 4</li>
                </ul>
            """.trimIndent(),
        )

        val scalar = CssRuleEvaluator.evaluate(source, "@.w_txt li.4@textNodes")
        assertEquals(
            listOf("Chapter 4"),
            assertIs<EngineResult.Success<List<String>>>(scalar).value,
        )

        val collection = CssRuleEvaluator.evaluateElements(source, "@.w_txt li.4")
        val fragments = assertIs<EngineResult.Success<List<String>>>(collection).value
        assertEquals(1, fragments.size)
        kotlin.test.assertTrue(fragments.single().contains("Chapter 4"))
        kotlin.test.assertFalse(fragments.single().contains("Chapter 3"))
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
    fun collectionRootsRetainElementsWhenTheyHaveScalarExtractionSuffixes() {
        val textSuffix = CssRuleEvaluator.evaluateElements(document, "div.book@text")
        val hrefSuffix = CssRuleEvaluator.evaluateElements(document, "a.title@href")
        val attributeSuffix = CssRuleEvaluator.evaluateElements(document, "img@attr(data-id)")

        val textNodes = assertIs<EngineResult.Success<List<String>>>(textSuffix).value
        val hrefNodes = assertIs<EngineResult.Success<List<String>>>(hrefSuffix).value
        val attributeNodes = assertIs<EngineResult.Success<List<String>>>(attributeSuffix).value

        assertEquals(2, textNodes.size)
        kotlin.test.assertTrue(textNodes[0].contains("Alpha"))
        kotlin.test.assertTrue(textNodes[1].contains("Beta"))
        assertEquals(2, hrefNodes.size)
        kotlin.test.assertTrue(hrefNodes[0].contains("/book/1"))
        kotlin.test.assertTrue(hrefNodes[1].contains("/book/2"))
        assertEquals(2, attributeNodes.size)
        kotlin.test.assertTrue(attributeNodes[0].contains("data-id"))
        kotlin.test.assertTrue(attributeNodes[1].contains("data-id"))
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

        // Default-mode scalar rules use ElementsSingle index suffixes.
        val scalar = CssRuleEvaluator.evaluate(document, "div.book.-1@a.title@text")
        assertEquals(
            listOf("Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(scalar).value,
        )

        // Explicit @CSS: remains a direct CSS selector and bypasses legacy indexes.
        val explicitCssScalar = CssRuleEvaluator.evaluate(document, "@CSS: div.book.-1@text")
        val explicitCssScalarValues = (explicitCssScalar as? EngineResult.Success)?.value.orEmpty()
        kotlin.test.assertFalse(explicitCssScalarValues.contains("Beta Novel"))
    }
    @Test
    fun scalarDefaultModeSupportsLegacyHeadsWithoutExplicitValueSuffix() {
        val classes = CssRuleEvaluator.evaluate(document, "class.book")
        val tags = CssRuleEvaluator.evaluate(document, "tag.a")
        assertEquals(
            listOf("Alpha Novel First intro", "Beta Novel Second intro"),
            assertIs<EngineResult.Success<List<String>>>(classes).value,
        )
        assertEquals(
            listOf("Alpha Novel", "Beta Novel"),
            assertIs<EngineResult.Success<List<String>>>(tags).value,
        )
    }


}
