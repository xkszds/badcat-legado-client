package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SelectorRuleEvaluatorTest {

    @Test
    fun dispatchesCssRules() {
        val document = SourceDocument("https://example.test/", "<p>Alpha</p>")
        val result = SelectorRuleEvaluator.evaluate(document, SelectorRuleType.CSS, "p@text")
        assertEquals(listOf("Alpha"), assertIs<EngineResult.Success<List<String>>>(result).value)
    }

    @Test
    fun cssDispatchUsesPinnedLegadoCssSemantics() {
        val document = SourceDocument(
            "https://example.test/",
            """<ul class="books"><li>Alpha</li><li>Beta</li></ul>""",
        )
        val result = SelectorRuleEvaluator.evaluate(
            document,
            SelectorRuleType.CSS,
            "ul.books@tag.li@text",
        )
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun dispatchesXPathRules() {
        val document = SourceDocument("https://example.test/", "<p>Alpha</p>")
        val result = SelectorRuleEvaluator.evaluate(document, SelectorRuleType.XPATH, "//p/text()")
        assertEquals(listOf("Alpha"), assertIs<EngineResult.Success<List<String>>>(result).value)
    }

    @Test
    fun dispatchesJsonPathRules() {
        val document = SourceDocument("https://example.test/", """{"name":"Alpha"}""")
        val result = SelectorRuleEvaluator.evaluate(document, SelectorRuleType.JSON_PATH, "$.name")
        assertEquals(listOf("Alpha"), assertIs<EngineResult.Success<List<String>>>(result).value)
    }
}
