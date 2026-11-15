package app.badcat.engine

/**
 * Evaluates only the embedded `{{...}}` URL-template form from the pinned
 * Legado AnalyzeUrl implementation. This is deliberately separate from URL
 * parsing, page tokens, request options, and HTTP execution.
 *
 * The evaluator is injected so the contract module does not depend on Rhino.
 * The Android host must supply a reviewed, bounded evaluator before opting in.
 */
fun interface LegadoEmbeddedScriptEvaluator {
    suspend fun evaluate(script: String, key: String): EngineResult<Any?>
}

object LegadoEmbeddedUrlTemplate {

    /**
     * Mirrors the pinned RuleAnalyzer.innerRule("{{", "}}") replacement shape:
     * each start marker is paired with the next end marker; unclosed markers
     * remain literal. Null becomes an empty string, strings remain unchanged,
     * integral Double values omit the decimal suffix, and other values use
     * toString().
     *
     * An empty evaluated value follows upstream's behavior: it is not appended.
     * When the first embedded rule evaluates empty, the resulting string is
     * empty rather than the original template.
     */
    suspend fun resolve(
        template: String,
        key: String,
        evaluator: LegadoEmbeddedScriptEvaluator,
    ): EngineResult<String> {
        if (!template.contains("{{")) return EngineResult.Success(template)

        val output = StringBuilder()
        var searchFrom = 0
        var copiedThrough = 0
        var replacedAny = false

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
                is Double if evaluated % 1.0 == 0.0 -> String.format(java.util.Locale.US, "%.0f", evaluated)
                else -> evaluated.toString()
            }

            if (replacement.isNotEmpty()) {
                output.append(template, copiedThrough, start)
                output.append(replacement)
                copiedThrough = end + 2
                replacedAny = true
            }
            searchFrom = end + 2
        }

        if (!replacedAny) {
            // Upstream returns the original queue if no complete marker pair was found.
            // A complete pair with an empty first result is the special empty-output case.
            val hasCompletePair = template.indexOf("{{").let { first ->
                first >= 0 && template.indexOf("}}", first + 2) >= 0
            }
            return EngineResult.Success(if (hasCompletePair) "" else template)
        }

        output.append(template, copiedThrough, template.length)
        return EngineResult.Success(output.toString())
    }
}
