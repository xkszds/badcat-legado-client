package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.OkHttpSourceRequestClient
import app.badcat.engine.SourceHttpGateway
import app.badcat.engine.SourceRequestClient
import app.badcat.engine.SourceScriptRuntime

/**
 * Creates a source-scoped HTTP gateway with the bounded embedded-template
 * adapter explicitly enabled.
 *
 * This factory enables only the `{{...}}` expression subset and the string
 * `key` binding. It does not enable @js, bodyJs, WebView, Java host objects,
 * login/session services, or the other bindings required by full AnalyzeUrl.
 * Hosts that are not ready to execute embedded expressions should construct
 * SourceHttpGateway directly, whose default remains fail-closed.
 */
class BadcatSourceHttpGatewayFactory(
    private val client: SourceRequestClient = OkHttpSourceRequestClient(),
    private val scriptRuntime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
) {
    fun forSource(source: BookSource): SourceHttpGateway =
        SourceHttpGateway(
            client = client,
            embeddedScriptEvaluator = RhinoEmbeddedUrlScriptEvaluator(
                source = source,
                runtime = scriptRuntime,
            ),
        )
}
