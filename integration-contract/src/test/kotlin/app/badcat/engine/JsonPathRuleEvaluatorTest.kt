package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JsonPathRuleEvaluatorTest {

    private val document = SourceDocument(
        url = "https://example.test/books.json",
        body = """{"books":[{"name":"Alpha","author":"A"},{"name":"Beta","author":"B"}],"meta":{"count":2}}""",
        contentType = "application/json",
    )

    @Test
    fun extractsJsonPathList() {
        val result = JsonPathRuleEvaluator.evaluate(document, "$.books[*].name")
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun combinesJsonPathFallbackRules() {
        val result = JsonPathRuleEvaluator.evaluate(
            document,
            "$.missing[*] || $.books[*].name",
        )
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun reportsMalformedJsonPath() {
        val result = JsonPathRuleEvaluator.evaluate(document, "$.books[")
        assertIs<EngineResult.Failure>(result)
    }
}
