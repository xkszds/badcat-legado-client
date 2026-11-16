package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceScriptRuntime
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android host adapter for bounded source JavaScript.
 *
 * The current bridge intentionally exposes primitive string bindings only.
 * It now keeps an isolated Rhino global per source, so common Legado scripts
 * can retain source-level variables between search/detail/TOC/content calls.
 * This is still not a complete AnalyzeUrl/AnalyzeRule host: java/http/cookie,
 * WebView, login and cache service objects are not injected here.
 */
class RhinoSourceScriptRuntime(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : SourceScriptRuntime {

    init {
        require(timeoutMillis in 1L..MAX_TIMEOUT_MILLIS)
    }

    override suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String>,
    ): EngineResult<String> {
        return try {
            require(script.isNotBlank()) { "Source JavaScript must not be blank" }
            require(script.length <= MAX_SCRIPT_CHARS) { "Source JavaScript exceeds the size limit" }
            require(bindings.size <= MAX_BINDINGS) { "Too many source JavaScript bindings" }
            require(bindings.keys.all(BINDING_NAME::matches)) { "Invalid source JavaScript binding name" }
            require(bindings.values.all { it.length <= MAX_BINDING_VALUE_CHARS }) {
                "Source JavaScript binding exceeds the size limit"
            }
            require(bindings.values.sumOf { it.length } <= MAX_TOTAL_BINDING_CHARS) {
                "Source JavaScript bindings exceed the total size limit"
            }

            val jsLib = source.jsLib.orEmpty()
            require(jsLib.length <= MAX_SCRIPT_CHARS) { "Source JavaScript library exceeds the size limit" }
            require(script.length + jsLib.length <= MAX_TOTAL_SCRIPT_CHARS) {
                "Source JavaScript and library exceed the combined size limit"
            }

            val scope = ScriptBindings()
            bindings.forEach { (name, value) -> scope[name] = value }
            scope.chainTo(
                RhinoScriptEngine.newStandardTopLevel(),
                sourceGlobals.computeIfAbsent(source.id) {
                    ScriptBindings.getSharedGlobalStateHandle(GLOBAL_KEY_PREFIX + it)
                },
            )
            val result = withTimeout(timeoutMillis) {
                // Legado jsLib is source-scoped shared code. Evaluate it in the same
                // isolated scope before each rule so its functions are available to
                // search/detail/TOC/content scripts without exposing host capabilities.
                if (jsLib.isNotBlank()) RhinoScriptEngine.evalSuspend(jsLib, scope)
                RhinoScriptEngine.evalSuspend(script, scope)
            }
            when (result) {
                null -> EngineResult.Success("")
                is String -> EngineResult.Success(result)
                is CharSequence -> EngineResult.Success(result.toString())
                is Number, is Boolean -> EngineResult.Success(result.toString())
                else -> EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Source JavaScript result must be a primitive value",
                )
            }
        } catch (error: TimeoutCancellationException) {
            EngineResult.Failure(
                EngineErrorCode.RULE_TIMEOUT,
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

    /** Drop source-level globals after a source is replaced or removed. */
    fun clearSourceState(sourceId: String) {
        if (sourceId.isBlank()) return
        sourceGlobals.remove(sourceId)
        ScriptBindings.removeSharedGlobalStates(GLOBAL_KEY_PREFIX + sourceId)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 1_500L
        const val MAX_TIMEOUT_MILLIS = 10_000L
        const val MAX_SCRIPT_CHARS = 128 * 1024
        const val MAX_BINDINGS = 32
        const val MAX_BINDING_VALUE_CHARS = 64 * 1024
        const val MAX_TOTAL_BINDING_CHARS = 128 * 1024
        const val GLOBAL_KEY_PREFIX = "badcat-source:"
        val BINDING_NAME = Regex("[A-Za-z_$][A-Za-z0-9_$]{0,63}")
        val sourceGlobals = ConcurrentHashMap<String, com.script.SharedGlobalStateHandle>()
    }
}
