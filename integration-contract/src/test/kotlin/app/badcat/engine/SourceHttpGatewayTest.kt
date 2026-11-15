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
            return EngineResult.Success(SourceHttpResponse(200, request.url, body = "<html>ok</html>"))
        }
    }

    private fun source(enabled: Boolean = true) = BookSource(
        id = "fixture-source", name = "Fixture", baseUrl = "https://reader.example/",
        enabled = enabled, header = """{"User-Agent":"BadcatFixture"}""",
        searchUrl = "/search?q={{key}}",
    )

    @Test
    fun resolvesPageAndOptionsThenExecutesTransportRequest() {
        val client = RecordingClient()
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client).execute(
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
    fun unsupportedScriptRuleDoesNotReachTransportWithoutOptInEvaluator() {
        val client = RecordingClient()
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client).execute(source(), "/search?key={{key}}", key = "cat")
        }
        assertIs<EngineResult.Failure>(result)
        assertTrue(client.captured == null)
    }

    @Test
    fun expandsKeyBeforeUriEncodingAndRequestExecution() {
        val client = RecordingClient()
        val evaluator = LegadoEmbeddedScriptEvaluator { script, key, _ ->
            assertEquals("key", script)
            EngineResult.Success(key)
        }
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client, evaluator).execute(source(), "/search?q={{key}}", key = "雪 猫")
        }
        assertIs<EngineResult.Success<SourceHttpResponse>>(result)
        assertEquals("https://reader.example/search?q=%E9%9B%AA%20%E7%8C%AB", requireNotNull(client.captured).url)
    }

    @Test
    fun evaluatorProviderUsesTheCurrentSourceForEveryRequest() {
        val client = RecordingClient()
        val seenContexts = mutableListOf<String>()
        val gateway = SourceHttpGateway(
            client = client,
            embeddedScriptEvaluatorProvider = { currentSource ->
                LegadoEmbeddedScriptEvaluator { script, key, page ->
                    assertEquals("key", script)
                    seenContexts += "${currentSource.id}:${key}:${page}"
                    EngineResult.Success("${currentSource.id}-$key-$page")
                }
            },
        )
        kotlinx.coroutines.runBlocking {
            assertIs<EngineResult.Success<SourceHttpResponse>>(
                gateway.execute(source().copy(id = "source-a"), "/s?q={{key}}", key = "x"),
            )
            assertIs<EngineResult.Success<SourceHttpResponse>>(
                gateway.execute(source().copy(id = "source-b"), "/s?q={{key}}", key = "y"),
            )
        }
        assertEquals(listOf("source-a:x:1", "source-b:y:1"), seenContexts)
        assertTrue(requireNotNull(client.captured).url.contains("source-b-y"))
    }

    @Test
    fun templateEvaluationFailureNeverReachesTransport() {
        val client = RecordingClient()
        val evaluator = LegadoEmbeddedScriptEvaluator { _, _, _ ->
            EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "blocked")
        }
        val result = kotlinx.coroutines.runBlocking {
            SourceHttpGateway(client, evaluator).execute(source(), "/search?q={{key}}", key = "cat")
        }
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, assertIs<EngineResult.Failure>(result).code)
        assertTrue(client.captured == null)
    }
}
