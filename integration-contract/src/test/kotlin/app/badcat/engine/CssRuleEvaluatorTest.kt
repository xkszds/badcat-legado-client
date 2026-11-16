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
        kotlin.test.assertTrue(markup.startsWith("<a class=\"title\""))
        kotlin.test.assertTrue(markup.contains("<b>Novel</b>"))
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
    fun rejectsMixedNestedCombinationOperatorsInsteadOfTreatingThemAsCss() {
        val leftNested = CssRuleEvaluator.evaluate(
            document,
            "a.title@text && p.intro@text || a.missing@text",
        )
        val rightNested = CssRuleEvaluator.evaluate(
            document,
            "a.missing@text || p.intro@text && a.title@text",
        )

        assertIs<EngineResult.Failure>(leftNested)
        assertIs<EngineResult.Failure>(rightNested)
    }

    @Test
    fun rejectsUnsupportedSuffixAndMalformedSelector() {
        val unsupported = CssRuleEvaluator.evaluate(document, "a.title@attr()")
        assertIs<EngineResult.Failure>(unsupported)

        val malformed = CssRuleEvaluator.evaluate(document, "a[")
        assertIs<EngineResult.Failure>(malformed)
    }
}
