package app.badcat.engine

/**
 * Evaluates only the embedded `{{...}}` URL-template form from the pinned
 * Legado AnalyzeUrl implementation. This is separate from URL parsing, page
 * tokens, request options, and HTTP execution.
 *
 * The evaluator is injected so this contract module does not depend on Rhino.
 * The Android host must supply a reviewed, bounded evaluator before opting in.
 */
fun interface LegadoEmbeddedScriptEvaluator {
    suspend fun evaluate(script: String, key: String, page: Int): EngineResult<Any?>

    /** Optional current-result binding used by upstream AnalyzeUrl @js rules. */
    suspend fun evaluateWithResult(
        script: String,
        key: String,
        page: Int,
        result: String,
    ): EngineResult<Any?> = evaluate(script, key, page)

    /** Optional persistent source variables used by AnalyzeRule @get:{key}. */
    suspend fun getString(key: String): EngineResult<String?> =
        EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Persistent source variables are unavailable")

    /** Optional persistent source variables used by AnalyzeRule @put:{...}. */
    suspend fun putString(key: String, value: String): EngineResult<Unit> =
        EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Persistent source variables are unavailable")
}

object LegadoEmbeddedUrlTemplate {
    private const val MAX_TEMPLATE_CHARS = 256 * 1024
    private const val MAX_SCRIPT_CHARS = 64 * 1024
    private const val MAX_EXPRESSIONS = 512
    private const val MAX_OUTPUT_CHARS = 512 * 1024


    /**
     * Mirrors the pinned RuleAnalyzer.innerRule("{{", "}}") behavior: each
     * start marker pairs with the next end marker; unclosed markers stay literal.
     * Null becomes empty, strings stay unchanged, integral Double values omit
     * the decimal suffix, and other values use toString().
     *
     * Empty expression results contribute no replacement text, while the surrounding literal prefix and suffix are retained.
     */
    suspend fun resolve(
        template: String,
        key: String,
        evaluator: LegadoEmbeddedScriptEvaluator,
        page: Int = 1,
    ): EngineResult<String> {
        if (template.length > MAX_TEMPLATE_CHARS) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Embedded URL template exceeds the safe size limit",
            )
        }
        if (!template.contains("{{")) return EngineResult.Success(template)

        val output = StringBuilder()
        var searchFrom = 0
        var startX = 0
        var expressionCount = 0
        while (true) {
            val start = template.indexOf("{{", searchFrom)
            if (start < 0) break
            val expressionStart = start + 2
            val end = template.indexOf("}}", expressionStart)
            if (end < 0) break
            expressionCount++
            val expression = template.substring(expressionStart, end)
            if (expressionCount > MAX_EXPRESSIONS || expression.length > MAX_SCRIPT_CHARS) {
                return EngineResult.Failure(
                    EngineErrorCode.INVALID_SOURCE,
                    "Embedded URL template exceeds the safe expression limits",
                )
            }

            val evaluated = when (
                val result = evaluator.evaluate(expression, key, page)
            ) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val replacement = when (evaluated) {
                null -> ""
                is String -> evaluated
                is Double -> if (evaluated % 1.0 == 0.0) {
                    String.format(java.util.Locale.US, "%.0f", evaluated)
                } else {
                    evaluated.toString()
                }
                else -> evaluated.toString()
            }

            val literalLength = start - startX
            if (output.length + literalLength + replacement.length > MAX_OUTPUT_CHARS) {
                return EngineResult.Failure(
                    EngineErrorCode.INVALID_SOURCE,
                    "Expanded URL template exceeds the safe output limit",
                )
            }
            output.append(template, startX, start)
            output.append(replacement)
            startX = end + 2
            searchFrom = startX
        }

        return if (startX == 0) {
            EngineResult.Success(template)
        } else {
            if (output.length + template.length - startX > MAX_OUTPUT_CHARS) {
                EngineResult.Failure(
                    EngineErrorCode.INVALID_SOURCE,
                    "Expanded URL template exceeds the safe output limit",
                )
            } else {
                output.append(template, startX, template.length)
                EngineResult.Success(output.toString())
            }
        }
    }
}
