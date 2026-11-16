package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceScriptRuntime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RhinoEmbeddedUrlScriptEvaluatorTest {
    private class CapturingRuntime : SourceScriptRuntime {
        var capturedSource: BookSource? = null
        var capturedScript: String? = null
        var capturedBindings: Map<String, String> = emptyMap()

        override suspend fun evaluate(
            source: BookSource,
            script: String,
            bindings: Map<String, String>,
        ): EngineResult<String> {
            capturedSource = source
            capturedScript = script
            capturedBindings = bindings
            return EngineResult.Success("https://reader.example/search?q=cat")
        }
    }

    @Test
    fun passesLegadoRequestValuesAndSafeSourceMetadata() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/source.json",
            name = "Fixture source",
            baseUrl = "https://reader.example/",
        )
        val runtime = CapturingRuntime()
        val evaluator = RhinoEmbeddedUrlScriptEvaluator(source, runtime)

        val result = evaluator.evaluateWithResult(
            script = "baseUrl + key + page + result",
            key = "cat",
            page = 3,
            result = "previous-result",
        )

        assertEquals("https://reader.example/search?q=cat", (result as EngineResult.Success).value)
        assertEquals(source, runtime.capturedSource)
        assertEquals("baseUrl + key + page + result", runtime.capturedScript)
        assertEquals(
            mapOf(
                "key" to "cat",
                "page" to "3",
                "result" to "previous-result",
                "baseUrl" to "https://reader.example/",
                "sourceUrl" to "https://reader.example/source.json",
                "sourceId" to "https://reader.example/source.json",
                "sourceName" to "Fixture source",
            ),
            runtime.capturedBindings,
        )
        assertTrue(runtime.capturedBindings.values.none { it.contains("android.content.Context") })
    }
}
