package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoUrlResolver
import app.badcat.engine.OkHttpSourceRequestClient
import app.badcat.engine.SourceHttpGateway
import app.badcat.engine.SourceRequestClient
import app.badcat.engine.SourceRequestFactory
import app.badcat.engine.SourceScriptRuntime

/**
 * Creates gateways whose bounded URL-template evaluator is selected per request.
 * The provider receives the actual BookSource passed to execute(), preventing a
 * shared service graph from evaluating every source with the first source's context.
 * The default Rhino host routes java.ajax through the same bounded source HTTP
 * transport as ordinary rules; it does not open an unrestricted network bridge.
 * Full bodyJs, WebView and upstream Java host APIs remain unsupported.
 */
class BadcatSourceHttpGatewayFactory(
    private val client: SourceRequestClient = OkHttpSourceRequestClient(),
    scriptRuntime: SourceScriptRuntime? = null,
) {
    private val scriptRuntime: SourceScriptRuntime = scriptRuntime ?: RhinoSourceScriptRuntime(
        sourceAjax = { source, baseUrl, raw ->
            val resolved = when (val result = LegadoUrlResolver.resolve(baseUrl, raw)) {
                is EngineResult.Success -> result.value
                is EngineResult.Failure -> throw IllegalArgumentException(result.message, result.cause)
            }
            val request = when (val result = SourceRequestFactory.create(source, resolved)) {
                is EngineResult.Success -> result.value
                is EngineResult.Failure -> throw IllegalArgumentException(result.message, result.cause)
            }
            when (val response = client.execute(request)) {
                is EngineResult.Success -> response.value.body
                is EngineResult.Failure -> throw IllegalStateException(response.message, response.cause)
            }
        },
    )

    /** Use this gateway in shared Source*Service instances handling many sources. */
    fun create(): SourceHttpGateway =
        SourceHttpGateway(
            client = client,
            embeddedScriptEvaluatorProvider = { source ->
                RhinoEmbeddedUrlScriptEvaluator(source = source, runtime = scriptRuntime)
            },
        )

    /** Convenience for an intentionally single-source service graph. */
    fun forSource(source: BookSource): SourceHttpGateway =
        SourceHttpGateway(
            client = client,
            embeddedScriptEvaluator = RhinoEmbeddedUrlScriptEvaluator(
                source = source,
                runtime = scriptRuntime,
            ),
        )
}
