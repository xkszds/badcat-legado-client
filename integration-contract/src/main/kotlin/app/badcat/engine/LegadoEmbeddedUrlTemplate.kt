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
            val end = findExpressionEnd(template, expressionStart)
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

    /**
     * Finds the closing template marker without treating braces inside JavaScript
     * strings, comments, regular-expression literals or object literals as the
     * end of the expression.
     */
    private fun findExpressionEnd(source: String, start: Int): Int {
        var index = start
        var braceDepth = 0
        var quote: Char? = null
        var escaped = false
        var lineComment = false
        var blockComment = false
        var regex = false
        var regexCharacterClass = false
        var canStartRegex = true
        val expressionPrefixKeywords = setOf(
            "await", "case", "delete", "do", "else", "in", "instanceof",
            "new", "of", "return", "throw", "typeof", "void", "yield",
        )

        while (index < source.length) {
            val char = source[index]
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == quote -> quote = null
                }
                index++
                continue
            }
            if (lineComment) {
                if (char == '\n' || char == '\r') lineComment = false
                index++
                continue
            }
            if (blockComment) {
                if (char == '*' && source.getOrNull(index + 1) == '/') {
                    blockComment = false
                    index += 2
                } else index++
                continue
            }
            if (regex) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '[' -> regexCharacterClass = true
                    char == ']' -> regexCharacterClass = false
                    char == '/' && !regexCharacterClass -> {
                        regex = false
                        index++
                        while (index < source.length && source[index].isLetter()) index++
                        canStartRegex = false
                        continue
                    }
                    char == '\n' || char == '\r' -> {
                        regex = false
                        canStartRegex = true
                    }
                }
                index++
                continue
            }

            if (char == '/' && source.getOrNull(index + 1) == '/') {
                lineComment = true
                index += 2
                continue
            }
            if (char == '/' && source.getOrNull(index + 1) == '*') {
                blockComment = true
                index += 2
                continue
            }
            if (char == '/' && canStartRegex) {
                regex = true
                regexCharacterClass = false
                escaped = false
                index++
                continue
            }
            if (char == '\'' || char == '"' || char == '`') {
                quote = char
                escaped = false
                canStartRegex = false
                index++
                continue
            }
            if (char == '}' && braceDepth == 0 && source.getOrNull(index + 1) == '}') {
                return index
            }
            when {
                char.isLetter() || char == '_' || char == '$' -> {
                    val wordStart = index++
                    while (index < source.length &&
                        (source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '$')
                    ) index++
                    canStartRegex = source.substring(wordStart, index) in expressionPrefixKeywords
                    continue
                }
                char.isDigit() -> {
                    index++
                    while (index < source.length &&
                        (source[index].isLetterOrDigit() || source[index] == '.' || source[index] == '_')
                    ) index++
                    canStartRegex = false
                    continue
                }
                char == '{' -> {
                    braceDepth++
                    canStartRegex = true
                }
                char == '}' -> {
                    if (braceDepth > 0) braceDepth--
                    canStartRegex = false
                }
                char == ')' || char == ']' || char == '.' -> canStartRegex = false
                char == '+' || char == '-' -> {
                    val doubled = source.getOrNull(index + 1) == char
                    if (doubled) {
                        index++
                        canStartRegex = false
                    } else canStartRegex = true
                }
                char.isWhitespace() -> Unit
                else -> canStartRegex = char in "([{,:;?=!*%&|^~<>"
            }
            index++
        }
        return -1
    }
}
