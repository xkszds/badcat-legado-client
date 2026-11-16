package app.badcat.engine

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SelectorLegadoRuleRuntimeTest {

    @Test
    fun evaluatesConfiguredFieldsFromNestedSourceRuleObject(): Unit = runBlocking {
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
    fun executesSelectedStageDirectlyFromImportedBookSource(): Unit = runBlocking {
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
    fun defaultsUnprefixedLegadoSelectorsToCss(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div.book@text"}""",
            source,
            SourceDocument("https://reader.example/", "<div class='book'>Alpha</div>"),
        )
        assertEquals(
            listOf(mapOf("bookList" to "Alpha")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

    @Test
    fun appliesLegadoFallbackAndUnionOperatorsToSelectorFields(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val document = SourceDocument(
            "https://reader.example/search",
            """<div class="first">Alpha</div><div class="second">Beta</div>""",
        )

        val fallback = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div.missing||div.second"}""",
            source,
            document,
        )
        assertEquals(
            listOf(mapOf("bookList" to "Beta")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(fallback).value,
        )

        val union = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div.first&&div.second"}""",
            source,
            document,
        )
        assertEquals(
            listOf(mapOf("bookList" to "Alpha"), mapOf("bookList" to "Beta")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(union).value,
        )
    }

    @Test
    fun failedSelectorBranchFallsBackButUnionFailureIsPreserved(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val document = SourceDocument(
            "https://reader.example/search",
            """<div class="second">Beta</div>""",
        )

        val fallback = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div:unsupported-pseudo||div.second"}""",
            source,
            document,
        )
        assertEquals(
            listOf(mapOf("bookList" to "Beta")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(fallback).value,
        )

        val union = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"div:unsupported-pseudo&&div.second"}""",
            source,
            document,
        )
        assertIs<EngineResult.Failure>(union)
    }

    @Test
    fun dispatchesExplicitXPathAndJsonPathFields(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val html = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@xpath://div[@class='book']/text()"}""",
            source,
            SourceDocument("https://reader.example/", "<div class='book'>XPath book</div>"),
        )
        assertEquals(
            listOf(mapOf("bookList" to "XPath book")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(html).value,
        )

        val json = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@jsonpath:$.books[*].name"}""",
            source,
            SourceDocument("https://reader.example/", """{"books":[{"name":"One"},{"name":"Two"}]}"""),
        )
        assertEquals(
            listOf(mapOf("bookList" to "One"), mapOf("bookList" to "Two")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(json).value,
        )
    }

    @Test
    fun rejectsScriptAndRegexWithoutTheirDedicatedRuntime(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        for (expression in listOf("@js:java.lang.System.exit(0)", "@regex:chapter.*")) {
            val result = runtime.evaluate(
                SourceRuleStage.SEARCH,
                """{"bookList":"$expression"}""",
                source,
                SourceDocument("https://reader.example/", "<p>ignored</p>"),
            )
            assertEquals(EngineErrorCode.UNSUPPORTED_RULE, assertIs<EngineResult.Failure>(result).code)
        }
    }

    @Test
    fun rejectsNonObjectRuleJson(): Unit = runBlocking {
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
