package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SelectorLegadoRuleRuntimeTest {

    @Test
    fun evaluatesConfiguredFieldsFromNestedSourceRuleObject() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleSearchJson = """{"bookList":"div.book","ignored":"javascript:noop()"}""",
        )
        val runtime = SelectorLegadoRuleRuntime(
            mapOf("search.bookList" to SelectorRuleType.CSS),
        )
        val result = runtime.evaluate(
            stage = SourceRuleStage.SEARCH,
            ruleJson = source.ruleSearchJson!!,
            source = source,
            document = SourceDocument(
                url = "https://reader.example/search",
                body = """<div class="book">Alpha</div><div class="book">Beta</div>""",
            ),
        )
        assertEquals(
            listOf(mapOf("bookList" to "Alpha"), mapOf("bookList" to "Beta")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

    @Test
    fun executesSelectedStageDirectlyFromImportedBookSource() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleSearchJson = """{"bookList":"div.book"}""",
        )
        val runtime = SelectorLegadoRuleRuntime(
            mapOf("search.bookList" to SelectorRuleType.CSS),
        )
        val result = SourceRuleResolver.evaluateStage(
            source = source,
            stage = SourceRuleStage.SEARCH,
            document = SourceDocument(
                "https://reader.example/search",
                """<div class="book">Gamma</div>""",
            ),
            runtime = runtime,
        )
        assertEquals(
            listOf(mapOf("bookList" to "Gamma")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

    @Test
    fun failsWhenNoFieldsHaveExplicitSelectorModes() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
        )
        val runtime = SelectorLegadoRuleRuntime(emptyMap())
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div.book"}""",
            source,
            SourceDocument("https://reader.example/", "<div class='book'>Alpha</div>"),
        )
        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun rejectsNonObjectRuleJson() = runBlocking {
        val source = BookSource("id", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime(mapOf("search.bookList" to SelectorRuleType.CSS))
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """["invalid"]""",
            source,
            SourceDocument("https://reader.example/", "<p/>"),
        )
        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }
}
