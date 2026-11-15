package com.script.rhino

import com.script.ScriptBindings
import kotlinx.coroutines.withTimeout

/**
 * Minimal, fail-closed bridge for URL-template JavaScript.
 *
 * Only primitive string bindings cross the host boundary. Android/application
 * objects, Java service objects, and arbitrary collections are deliberately
 * not accepted here. This is not the complete Legado AnalyzeUrl adapter.
 */
class LegadoRhinoSandbox(
    private val maxScriptChars: Int = DEFAULT_MAX_SCRIPT_CHARS,
    private val maxBindings: Int = DEFAULT_MAX_BINDINGS,
    private val maxValueChars: Int = DEFAULT_MAX_VALUE_CHARS,
    private val maxTimeoutMillis: Long = DEFAULT_MAX_TIMEOUT_MILLIS,
) {
    init {
        require(maxScriptChars in 1..DEFAULT_MAX_SCRIPT_CHARS)
        require(maxBindings in 1..DEFAULT_MAX_BINDINGS)
        require(maxValueChars in 1..DEFAULT_MAX_VALUE_CHARS)
        require(maxTimeoutMillis in 1L..DEFAULT_MAX_TIMEOUT_MILLIS)
    }

    /**
     * Evaluates a source-rule fragment and returns only a primitive result.
     * The coroutine timeout is enforced by Rhino's instruction observer.
     */
    suspend fun evaluate(
        script: String,
        bindings: Map<String, String>,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): String? {
        require(script.isNotBlank()) { "script must not be blank" }
        require(script.length <= maxScriptChars) { "script exceeds size limit" }
        require(bindings.size <= maxBindings) { "too many script bindings" }
        require(timeoutMillis in 1L..maxTimeoutMillis) { "timeout is outside the allowed range" }

        val scope = ScriptBindings()
        bindings.forEach { (name, value) ->
            require(BINDING_NAME.matches(name)) { "invalid binding name" }
            require(value.length <= maxValueChars) { "binding value exceeds size limit" }
            scope.put(name, value)
        }

        val runtimeScope = RhinoScriptEngine.getRuntimeScope(scope)
        val result = withTimeout(timeoutMillis) {
            RhinoScriptEngine.evalSuspend(script, runtimeScope)
        }

        return when (result) {
            null -> null
            is String -> result
            is CharSequence -> result.toString()
            is Number, is Boolean -> result.toString()
            else -> throw IllegalArgumentException(
                "script result must be a primitive value, got ${result.javaClass.name}"
            )
        }
    }

    private companion object {
        val BINDING_NAME = Regex("[A-Za-z_$][A-Za-z0-9_$]{0,63}")
        const val DEFAULT_MAX_SCRIPT_CHARS = 128 * 1024
        const val DEFAULT_MAX_BINDINGS = 32
        const val DEFAULT_MAX_VALUE_CHARS = 64 * 1024
        const val DEFAULT_TIMEOUT_MILLIS = 1_500L
        const val DEFAULT_MAX_TIMEOUT_MILLIS = 10_000L
    }
}
