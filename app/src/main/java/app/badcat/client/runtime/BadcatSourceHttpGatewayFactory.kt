package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.OkHttpSourceRequestClient
import app.badcat.engine.SourceHttpGateway
import app.badcat.engine.SourceRequestClient
import app.badcat.engine.SourceScriptRuntime

/**
 * Creates gateways whose bounded URL-template evaluator is selected per request.
 * The provider receives the actual BookSource passed to execute(), preventing a
 * shared service graph from evaluating every source with the first source's context.
 * URL @js and {{...}} expressions receive bounded string key/page/result bindings.
 * Full bodyJs, WebView and upstream Java host APIs remain unsupported; the Rhino host exposes only an explicit bounded helper allow-list.
 */
class BadcatSourceHttpGatewayFactory(
    private val client: SourceRequestClient = OkHttpSourceRequestClient(),
    private val scriptRuntime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
) {
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
