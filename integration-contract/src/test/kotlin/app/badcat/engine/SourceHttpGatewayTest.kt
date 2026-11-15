package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SourceHttpGatewayTest {

    private class RecordingClient : SourceRequestClient {
        var captured: SourceHttpRequest? = null

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            captured = request
            return EngineResult.Success(
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = request.url,
                    body = "<html>ok</html>",
                ),
            )
        }
    }

    private fun source(enabled: Boolean = true) = BookSource(
        id = "fixture-source",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        enabled = enabled,
        header = """{"User-Agent":"BadcatFixture"}""",
        searchUrl = "/search?q={{key}}",
    )

    @Test
    fun resolvesPageAndOptionsThenExecutesTransportRequest() {
        val client = RecordingClient()
        val gateway = SourceHttpGateway(client)

        val result = kotlinx.coroutines.runBlocking {
            gateway.execute(
                source(),
                "/search?page=<1,2,3>,{\"method\":\"POST\",\"headers\":{\"X-Test\":\"yes\"},\"body\":\"q=cat\"}",
                page = 2,
            )
        }

        val response = assertIs<EngineResult.Success<SourceHttpResponse>>(result).value
        val request = requireNotNull(client.captured)
        assertEquals(200, response.statusCode)
        assertEquals("https://reader.example/search?page=2", request.url)
        assertEquals("POST", request.method)
        assertEquals("BadcatFixture", request.headers["User-Agent"])
        assertEquals("yes", request.headers["X-Test"])
        assertEquals("q=cat", request.body)
    }

    @Test
    fun disabledSourceDoesNotReachTransport() {
        val client = RecordingClient()
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client).execute(source(enabled = false), "/search")
        }

        assertIs<EngineResult.Failure>(result)
        assertTrue(client.captured == null)
    }

    @Test
    fun unsupportedScriptRuleDoesNotReachTransport() {
        val client = RecordingClient()
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client).execute(source(), "/search?key={{java.getKey()}}")
        }

        assertIs<EngineResult.Failure>(result)
        assertTrue(client.captured == null)
    }
}
