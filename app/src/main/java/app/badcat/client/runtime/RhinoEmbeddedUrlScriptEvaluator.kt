package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoEmbeddedScriptEvaluator
import app.badcat.engine.SourceScriptRuntime

/**
 * Narrow bridge from URL interpolation to the bounded source-script runtime.
 * Only the search key and current page number are exposed; no Java, network, cookie, cache or WebView
 * bindings are supplied by this adapter.
 */
class RhinoEmbeddedUrlScriptEvaluator(
    private val source: BookSource,
    private val runtime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
) : LegadoEmbeddedScriptEvaluator {
    override suspend fun evaluate(script: String, key: String, page: Int): EngineResult<Any?> =
        evaluateWithResult(script, key, page, "")

    override suspend fun evaluateWithResult(
        script: String,
        key: String,
        page: Int,
        result: String,
    ): EngineResult<Any?> = when (val evaluated = runtime.evaluate(
        source,
        script,
        mapOf(
            "key" to key,
            "page" to page.toString(),
            "result" to result,
            // Legado URL scripts commonly resolve relative URLs and branch by source metadata.
            // Keep these as primitive values: never expose Android Context, Java reflection,
            // HTTP clients, cookie stores, or filesystem objects to untrusted source scripts.
            "baseUrl" to source.baseUrl,
            "sourceUrl" to source.id,
            "sourceId" to source.id,
            "sourceName" to source.name,
        ),
    )) {
        is EngineResult.Success -> EngineResult.Success(evaluated.value)
        is EngineResult.Failure -> evaluated
    }
}
