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
