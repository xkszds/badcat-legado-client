package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceScriptRuntime
import com.script.rhino.LegadoRhinoSandbox
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android host adapter for the bounded Rhino evaluator.
 *
 * This adapter intentionally exposes only string bindings. It does not provide
 * Legado's java/http/cookie/cache/WebView objects and therefore is not a full
 * AnalyzeUrl or AnalyzeRule implementation.
 */
class RhinoSourceScriptRuntime(
    private val sandbox: LegadoRhinoSandbox = LegadoRhinoSandbox(),
) : SourceScriptRuntime {

    override suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String>,
    ): EngineResult<String> {
        return try {
            EngineResult.Success(sandbox.evaluate(script, bindings).orEmpty())
        } catch (error: TimeoutCancellationException) {
            EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Source JavaScript exceeded the execution time limit",
                error,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: IllegalArgumentException) {
            EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                error.message ?: "Unsupported JavaScript input or result",
                error,
            )
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                error.message ?: "Source JavaScript evaluation failed",
                error,
            )
        }
    }
}
