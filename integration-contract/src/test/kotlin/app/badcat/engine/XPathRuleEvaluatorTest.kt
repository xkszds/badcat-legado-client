package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class XPathRuleEvaluatorTest {

    private val document = SourceDocument(
        url = "https://example.test/books",
        body = """<html><body><p class="name">Alpha</p><p class="name">Beta</p></body></html>""",
        contentType = "text/html",
    )

    @Test
    fun evaluatesMixedNestedCombinationOperators() {
        val result = XPathRuleEvaluator.evaluate(
            document,
            "//p[@class='name']/text() && //p[@class='name']/text() || //p[@class='missing']/text()",
        )
        assertEquals(
            listOf("Alpha", "Beta", "Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun extractsXPathTextNodes() {
        val result = XPathRuleEvaluator.evaluate(document, "//p[@class='name']/text()")
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun extractsXPathElementsForBookAndChapterCollections() {
        val source = SourceDocument(
            url = "https://example.test/books",
            body = """<div class="book">Alpha</div><div class="book">Beta</div>""",
        )
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            source,
            "@xpath://div[@class='missing']||@xpath://div[@class='book']",
        )
        assertEquals(
            listOf("<div class=\"book\">Alpha</div>", "<div class=\"book\">Beta</div>"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun combinesXPathFallbackRules() {
        val result = XPathRuleEvaluator.evaluate(
            document,
            "//p[@class='missing']/text() || //p[@class='name']/text()",
        )
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }
}
