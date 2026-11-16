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
