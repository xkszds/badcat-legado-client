package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoEmbeddedScriptEvaluator
import app.badcat.engine.SourceScriptRuntime

/**
 * Narrow bridge from URL interpolation to the bounded source-script runtime.
 * Only the search key is exposed; no Java, network, cookie, cache or WebView
 * bindings are supplied by this adapter.
 */
class RhinoEmbeddedUrlScriptEvaluator(
    private val source: BookSource,
    private val runtime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
) : LegadoEmbeddedScriptEvaluator {
    override suspend fun evaluate(script: String, key: String): EngineResult<Any?> =
        when (val result = runtime.evaluate(source, script, mapOf("key" to key))) {
            is EngineResult.Success -> EngineResult.Success(result.value)
            is EngineResult.Failure -> result
        }
}
