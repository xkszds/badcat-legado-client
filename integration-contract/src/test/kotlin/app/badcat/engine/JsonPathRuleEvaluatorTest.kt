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
    fun evaluatesMixedNestedCombinationOperators() {
        val result = JsonPathRuleEvaluator.evaluate(
            document,
            "$.books[*].name && $.books[*].author || $.missing",
        )
        assertEquals(
            listOf("Alpha", "Beta", "A", "B"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun extractsScalarFieldsFromStandaloneJsonObjects() {
        val item = SourceDocument(
            url = "https://example.test/book.json",
            body = """{"title":"Single item","id":"42"}""",
            contentType = "application/json",
        )
        assertEquals(
            listOf("Single item"),
            assertIs<EngineResult.Success<List<String>>>(
                JsonPathRuleEvaluator.evaluate(item, "$.title"),
            ).value,
        )
        assertEquals(
            listOf("42"),
            assertIs<EngineResult.Success<List<String>>>(
                JsonPathRuleEvaluator.evaluate(item, "$.id"),
            ).value,
        )
    }

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
    fun selectsJsonCollectionEntriesAsIndependentFragments() {
        val result = JsonPathRuleEvaluator.evaluateElements(document, "$.books[*]")
        assertEquals(
            listOf(
                """{"name":"Alpha","author":"A"}""",
                """{"name":"Beta","author":"B"}""",
            ),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun appliesFallbackToJsonPathCollectionRules() {
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "@jsonpath:$.missing[*]||@jsonpath:$.books[*]",
        )
        assertEquals(
            listOf(
                """{"name":"Alpha","author":"A"}""",
                """{"name":"Beta","author":"B"}""",
            ),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }


    @Test
    fun doesNotStripModeMarkersInsideQuotedJsonPathLiterals() {
        val item = SourceDocument(
            url = "https://example.test/markers.json",
            body = """{"books":[{"name":"@json:foo"},{"name":"bar"}]}""",
            contentType = "application/json",
        )
        val result = JsonPathRuleEvaluator.evaluate(
            item,
            "$.books[?(@.name == '@json:foo')].name",
        )
        assertEquals(
            listOf("@json:foo"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun reportsMalformedJsonDocument() {
        val malformed = document.copy(body = """{"books":[""")
        val result = JsonPathRuleEvaluator.evaluate(malformed, "$.books[*].name")
        assertIs<EngineResult.Failure>(result)
    }
}
