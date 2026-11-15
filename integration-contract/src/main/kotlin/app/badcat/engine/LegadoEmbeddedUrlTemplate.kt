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
    suspend fun evaluate(script: String, key: String): EngineResult<Any?>
}

object LegadoEmbeddedUrlTemplate {

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
    ): EngineResult<String> {
        if (!template.contains("{{")) return EngineResult.Success(template)

        val output = StringBuilder()
        var searchFrom = 0
        var startX = 0
        while (true) {
            val start = template.indexOf("{{", searchFrom)
            if (start < 0) break
            val expressionStart = start + 2
            val end = template.indexOf("}}", expressionStart)
            if (end < 0) break

            val evaluated = when (
                val result = evaluator.evaluate(template.substring(expressionStart, end), key)
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

            output.append(template, startX, start)
            output.append(replacement)
            startX = end + 2
            searchFrom = startX
        }

        return if (startX == 0) {
            EngineResult.Success(template)
        } else {
            output.append(template, startX, template.length)
            EngineResult.Success(output.toString())
        }
    }
}
