package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class SourceSearchServiceTest {
    private class FixtureClient : SourceRequestClient {
        var requestCount = 0

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            requestCount++
            return EngineResult.Success(
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = request.url,
                    body = """<div class="book"><h3>Search Novel</h3><a href="/book/1">Read</a></div>""",
                ),
            )
        }
    }

    private fun source(enabled: Boolean = true) = BookSource(
        id = "https://reader.example/",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        enabled = enabled,
        searchUrl = "https://reader.example/search?q={{key}}",
        ruleSearchJson = """{"bookList":"div.book","name":"h3@text","bookUrl":"a@href"}""",
    )

    @Test
    fun rejectsDisabledSourceBeforeNetworkRequest() = runBlocking {
        val client = FixtureClient()
        val result = SourceSearchService(SourceHttpGateway(client)).search(
            source(enabled = false),
            "https://reader.example/search?q=cat",
        )
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
        assertEquals(0, client.requestCount)
    }

    @Test
    fun executesJavaScriptFieldRulesOnEachSearchCardThroughProductionService() = runBlocking {
        val client = FixtureClient()
        val calls = mutableListOf<Pair<String, String>>()
        val scriptRuntime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                calls += script to bindings["result"].orEmpty()
                return EngineResult.Success("Script title")
            }
        }
        val scriptedSource = source().copy(
            ruleSearchJson = """{"bookList":"div.book","name":"@js:extractTitle(result)","bookUrl":"a@href"}""",
        )
        val result = assertIs<EngineResult.Success<List<BookCandidate>>>(
            SourceSearchService(SourceHttpGateway(client), scriptRuntime = scriptRuntime).search(
                scriptedSource,
                "https://reader.example/search?q=cat",
            ),
        )
        assertEquals(listOf("Script title"), result.value.map { it.name })
        assertEquals(1, calls.size)
        assertEquals("extractTitle(result)", calls.single().first)
        assertEquals(true, calls.single().second.contains("Search Novel"))
    }

    @Test
    fun acceptsMainJsSourceWhenBoundedRuntimeIsConfigured() = runBlocking {
        val client = FixtureClient()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success("MainJs title")
        }
        val scriptedSource = source().copy(
            mainJs = "function sharedHelper() { return 'ready'; }",
            ruleSearchJson = """{"bookList":"div.book","name":"@js:sharedHelper()","bookUrl":"a@href"}""",
        )

        val result = assertIs<EngineResult.Success<List<BookCandidate>>>(
            SourceSearchService(SourceHttpGateway(client), scriptRuntime = runtime).search(
                scriptedSource,
                "https://reader.example/search?q=cat",
            ),
        )

        assertEquals(listOf("MainJs title"), result.value.map { it.name })
        assertEquals(1, client.requestCount)
    }

    @Test
    fun rejectsMainJsSourceWhenNoRuntimeIsConfigured() = runBlocking {
        val client = FixtureClient()
        val result = assertIs<EngineResult.Failure>(
            SourceSearchService(SourceHttpGateway(client)).search(
                source().copy(mainJs = "function sharedHelper() { return 'ready'; }"),
                "https://reader.example/search?q=cat",
            ),
        )

        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, result.code)
        assertEquals(0, client.requestCount)
    }

    @Test
    fun evaluatesDollarExpressionsInSearchUrlAsJavaScript() = runBlocking {
        val client = FixtureClient()
        val seenScripts = mutableListOf<String>()
        val evaluator = LegadoEmbeddedScriptEvaluator { script, _, _ ->
            seenScripts += script
            EngineResult.Success("javascript-filter")
        }

        val result = SourceSearchService(
            SourceHttpGateway(client, embeddedScriptEvaluator = evaluator),
        ).search(
            source(),
            "/search?filter={{$.filter}}",
        )

        assertIs<EngineResult.Success<List<BookCandidate>>>(result)
        assertEquals(listOf("$.filter"), seenScripts)
        assertEquals(1, client.requestCount)
    }

    @Test
    fun parsesEnabledSourceSearchResults() = runBlocking {
        val client = FixtureClient()
        val result = assertIs<EngineResult.Success<List<BookCandidate>>>(
            SourceSearchService(SourceHttpGateway(client)).search(
                source(),
                "https://reader.example/search?q=cat",
            ),
        )
        assertEquals(1, client.requestCount)
        assertEquals(listOf("Search Novel"), result.value.map { it.name })
        assertEquals(listOf("https://reader.example/book/1"), result.value.map { it.bookUrl })
    }
}
