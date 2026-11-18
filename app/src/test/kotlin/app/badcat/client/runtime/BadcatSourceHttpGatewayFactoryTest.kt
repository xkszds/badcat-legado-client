package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpRequest
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.SourceRequestClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadcatSourceHttpGatewayFactoryTest {
    private class RecordingClient : SourceRequestClient {
        val requests = mutableListOf<SourceHttpRequest>()

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            requests += request
            val body = if (request.url.endsWith("/form")) {
                """<form action="/search?q=cat">"""
            } else {
                "<html>search result</html>"
            }
            return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
        }
    }

    @Test
    fun javaAjaxInsideSearchUrlScriptUsesTheSharedLegadoUrlResolver() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/source.json",
            name = "Fixture source",
            baseUrl = "https://reader.example/",
        )
        val client = RecordingClient()
        val gateway = BadcatSourceHttpGatewayFactory(client = client).forSource(source)

        val result = gateway.execute(
            source = source,
            ruleUrl = """https://reader.example/search?q=@js:return java.ajax('/form').match(/action="(.+?)"/)[1]""",
            key = "cat",
        )

        assertTrue(result is EngineResult.Success)
        assertEquals(
            listOf(
                "https://reader.example/form",
                "https://reader.example/search?q=cat",
            ),
            client.requests.map { it.url },
        )
    }
}
