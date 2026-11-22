package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoLoginUiV2
import app.badcat.engine.SourceScriptRuntime
import com.google.gson.JsonParser

/**
 * Executes declarative Login UI v2 render/action hooks in the bounded,
 * per-source Rhino context. the runtime exposes the source login info/header persistence helpers. It still does not
 * emulate java.ajax, WebView or the full upstream CookieStore surface, so this is not
 * a complete authentication host.
 */
class LegadoSourceLoginUiV2Service(
    private val scriptRuntime: SourceScriptRuntime = RhinoSourceScriptRuntime(),
) {
    suspend fun render(
        source: BookSource,
        stateJson: String = "{}",
        invocation: LegadoLoginUiV2.InvocationContext = LegadoLoginUiV2.InvocationContext(source.id),
    ): EngineResult<List<LegadoLoginUiV2.Row>> {
        if (!LegadoLoginUiV2.isV2(source.loginUi)) {
            return unsupported("Book source '${source.id}' does not declare Login UI v2")
        }
        invocation.validationError(source.id)?.let { reason ->
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Invalid login invocation context: $reason")
        }
        if (!isJsonObject(stateJson)) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Login UI state must be a JSON object")
        }
        val loginScript = loginScript(source)
            ?: return unsupported("Book source '${source.id}' has no login JavaScript")
        val evaluated = scriptRuntime.evaluate(
            source = source,
            script = "$loginScript\nloginUi(JSON.parse(String(__loginState)))",
            bindings = mapOf(
                "__loginState" to stateJson,
                "__badcatLoginInvocation" to invocation.toJson(),
            ),
        )
        val renderJson = when (evaluated) {
            is EngineResult.Failure -> return evaluated
            is EngineResult.Success -> evaluated.value
        }
        val rows = LegadoLoginUiV2.parseRows(renderJson)
            ?: return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Book source '${source.id}' returned an invalid Login UI v2 rows document",
            )
        return EngineResult.Success(rows)
    }

    suspend fun dispatchAction(
        source: BookSource,
        action: String,
        stateJson: String,
        formJson: String,
        invocation: LegadoLoginUiV2.InvocationContext = LegadoLoginUiV2.InvocationContext(source.id),
    ): EngineResult<LegadoLoginUiV2.ActionResult> {
        if (!LegadoLoginUiV2.isV2(source.loginUi)) {
            return unsupported("Book source '${source.id}' does not declare Login UI v2")
        }
        invocation.validationError(source.id)?.let { reason ->
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Invalid login invocation context: $reason")
        }
        if (action.isBlank() || action.length > LegadoLoginUiV2.MAX_TEXT_CHARS ||
            !isJsonObject(stateJson) || !isJsonObject(formJson)
        ) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Login action, state or form data is invalid",
            )
        }
        val loginScript = loginScript(source)
            ?: return unsupported("Book source '${source.id}' has no login JavaScript")
        val evaluated = scriptRuntime.evaluate(
            source = source,
            script = "$loginScript\nloginAction(String(__loginAction), JSON.parse(String(__loginState)), JSON.parse(String(__loginForm)))",
            bindings = mapOf(
                "__loginAction" to action,
                "__loginState" to stateJson,
                "__loginForm" to formJson,
                "__badcatLoginInvocation" to invocation.toJson(),
            ),
        )
        val actionJson = when (evaluated) {
            is EngineResult.Failure -> return evaluated
            is EngineResult.Success -> evaluated.value
        }
        val result = LegadoLoginUiV2.parseActionResult(actionJson)
            ?: return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Book source '${source.id}' returned an invalid Login UI v2 action result",
            )
        return EngineResult.Success(result)
    }

    suspend fun checkLogin(source: BookSource): EngineResult<Boolean> {
        val checkScript = source.loginCheckJs?.trim().orEmpty()
        if (checkScript.isBlank()) {
            return unsupported("Book source '${source.id}' has no loginCheckJs")
        }
        return when (val evaluated = scriptRuntime.evaluate(source, checkScript)) {
            is EngineResult.Failure -> evaluated
            is EngineResult.Success -> when (evaluated.value.trim().lowercase()) {
                "true", "1" -> EngineResult.Success(true)
                "false", "0", "" -> EngineResult.Success(false)
                else -> EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "loginCheckJs must return a boolean value",
                )
            }
        }
    }

    private fun loginScript(source: BookSource): String? {
        val raw = source.loginUrl?.takeIf { it.isNotBlank() } ?: return null
        val text = raw.trim()
        val inline = INLINE_JS.matchEntire(text)?.groupValues?.get(1)
            ?: if (text.startsWith("@js:", ignoreCase = true)) text.substringAfter(':').trim() else text
        return inline.takeIf { it.isNotBlank() }
    }

    private fun isJsonObject(value: String): Boolean = runCatching {
        JsonParser.parseString(value).isJsonObject
    }.getOrDefault(false)

    private fun unsupported(message: String) =
        EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, message)

    private companion object {
        val INLINE_JS = Regex("<js>([\\s\\S]*?)</js>", RegexOption.IGNORE_CASE)
    }
}
