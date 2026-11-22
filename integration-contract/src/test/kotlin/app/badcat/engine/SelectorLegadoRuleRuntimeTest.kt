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
    fun rejectsJavaScriptWithoutAnInjectedScriptRuntime(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@js:java.lang.System.exit(0)"}""",
            source,
            SourceDocument("https://reader.example/", "<p>ignored</p>"),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, assertIs<EngineResult.Failure>(result).code)
    }

    @Test
    fun executesJavaScriptRuleFieldThroughInjectedBoundedRuntime(): Unit = runBlocking {
        val source = BookSource(
            id = "https://reader.example/source.json",
            name = "Fixture source",
            baseUrl = "https://reader.example/",
        )
        var capturedScript = ""
        var capturedBindings: Map<String, String> = emptyMap()
        val scriptRuntime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                capturedScript = script
                capturedBindings = bindings
                return EngineResult.Success("JavaScript result")
            }
        }
        val runtime = SelectorLegadoRuleRuntime(scriptRuntime = scriptRuntime)
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@js:result.toUpperCase()"}""",
            source,
            SourceDocument("https://reader.example/search", "<p>source body</p>"),
        )

        assertEquals("result.toUpperCase()", capturedScript)
        assertEquals("<p>source body</p>", capturedBindings["result"])
        assertEquals("https://reader.example/search", capturedBindings["baseUrl"])
        assertEquals("https://reader.example/source.json", capturedBindings["sourceUrl"])
        assertEquals(
            listOf(mapOf("bookList" to "JavaScript result")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

    @Test
    fun evaluatesExplicitRegexCollectionFields(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val runtime = SelectorLegadoRuleRuntime()
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@regex:<a>(.*?)</a>"}""",
            source,
            SourceDocument(
                "https://reader.example/search",
                "<a>Alpha</a><a>Beta</a>",
            ),
        )
        assertEquals(
            listOf(mapOf("bookList" to "<a>Alpha</a>"), mapOf("bookList" to "<a>Beta</a>")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
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
    @Test
    fun expandsPersistentGetAndPutVariablesBeforeSelectorDispatch(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        val values = mutableMapOf<String, String>()
        val scriptRuntime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success(script)

            override suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
                EngineResult.Success(values[key])

            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                values[key] = value
                return EngineResult.Success(Unit)
            }
        }
        val runtime = SelectorLegadoRuleRuntime(scriptRuntime = scriptRuntime)
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"@put:{\"selector\":\".book\"}@get:{selector}"}""",
            source,
            SourceDocument(
                url = "https://reader.example/search",
                body = """<div class="book">Stored selector</div>""",
                key = "猫",
                page = 3,
            ),
        )
        assertEquals(".book", values["selector"])
        assertEquals(
            listOf(mapOf("bookList" to "Stored selector")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

    @Test
    fun evaluatesEmbeddedRuleScriptsWithCurrentResultKeyAndPage(): Unit = runBlocking {
        val source = BookSource("https://reader.example/", "Fixture", "https://reader.example/")
        var capturedScript = ""
        var capturedBindings: Map<String, String> = emptyMap()
        val scriptRuntime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                capturedScript = script
                capturedBindings = bindings
                return EngineResult.Success(".book")
            }
        }
        val runtime = SelectorLegadoRuleRuntime(scriptRuntime = scriptRuntime)
        val result = runtime.evaluate(
            SourceRuleStage.SEARCH,
            """{"bookList":"{{chooseSelector(result, key, page)}}" }""",
            source,
            SourceDocument(
                url = "https://reader.example/search",
                body = """<div class="book">Embedded selector</div>""",
                key = "雪猫",
                page = 4,
            ),
        )
        assertEquals("chooseSelector(result, key, page)", capturedScript)
        assertEquals("雪猫", capturedBindings["key"])
        assertEquals("4", capturedBindings["page"])
        assertEquals("https://reader.example/search", capturedBindings["baseUrl"])
        assertEquals(
            listOf(mapOf("bookList" to "Embedded selector")),
            assertIs<EngineResult.Success<List<Map<String, String>>>>(result).value,
        )
    }

}
