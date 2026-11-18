package app.badcat.engine

/**
 * Resolves Legado's AnalyzeRule @put:{key:rule,...} directives.
 * Upstream syntax is not necessarily JSON: keys and selector expressions are
 * commonly unquoted (for example @put:{cid:chapter_id}).
 */
object LegadoPutRule {
    data class Assignment(val key: String, val rule: String)
    data class Parsed(val expression: String, val assignments: List<Assignment>)

    fun parse(expression: String): EngineResult<Parsed> {
        val marker = expression.indexOf("@put:", ignoreCase = true)
        if (marker < 0) return EngineResult.Success(Parsed(expression, emptyList()))
        val start = marker + 5
        if (start >= expression.length || expression[start] != '{') {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Malformed Legado @put directive")
        }
        val end = findClosingBrace(expression, start)
            ?: return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unclosed Legado @put directive")
        val entries = splitTopLevel(expression.substring(start + 1, end), ',')
        val assignments = entries.mapNotNull { entry ->
            val separator = findTopLevelColon(entry)
            if (separator <= 0) null else {
                val key = unquote(entry.substring(0, separator).trim())
                val rule = unquote(entry.substring(separator + 1).trim())
                if (key.isBlank() || rule.isBlank()) null else Assignment(key, rule)
            }
        }
        if (assignments.isEmpty() || assignments.size != entries.size) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Invalid Legado @put assignments")
        }
        return EngineResult.Success(
            Parsed(
                expression.removeRange(marker, end + 1).trim(),
                assignments,
            ),
        )
    }

    suspend fun resolve(
        source: BookSource,
        expression: String,
        document: SourceDocument,
        runtime: SourceScriptRuntime,
    ): EngineResult<String> {
        val parsed = when (val result = parse(expression)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        for (assignment in parsed.assignments) {
            val rule = normalizeJsonRule(document.body, assignment.rule)
            val value = when (val result = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(rule, result.value)
                    ?.trim()?.takeIf(String::isNotEmpty)
            } ?: continue
            when (val stored = runtime.putString(source, assignment.key, value)) {
                is EngineResult.Failure -> return stored
                is EngineResult.Success -> Unit
            }
        }
        return EngineResult.Success(parsed.expression)
    }

    private fun normalizeJsonRule(body: String, rule: String): String {
        val value = rule.trim()
        val json = body.trimStart().let { it.startsWith("{") || it.startsWith("[") }
        val barePath = value.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*"))
        return if (json && barePath && !value.startsWith("$")) "$.$value" else value
    }

    private fun findClosingBrace(value: String, start: Int): Int? {
        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (index in start until value.length) {
            val char = value[index]
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else when (char) {
                '\'', '"' -> quote = char
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    private fun splitTopLevel(value: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        var quote: Char? = null
        var escaped = false
        var depth = 0
        var start = 0
        value.forEachIndexed { index, char ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else when (char) {
                '\'', '"' -> quote = char
                '{', '[', '(' -> depth++
                '}', ']', ')' -> if (depth > 0) depth--
                delimiter -> if (depth == 0) {
                    parts += value.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        parts += value.substring(start).trim()
        return parts
    }

    private fun findTopLevelColon(value: String): Int {
        var quote: Char? = null
        var escaped = false
        var depth = 0
        value.forEachIndexed { index, char ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == quote) quote = null
            } else when (char) {
                '\'', '"' -> quote = char
                '{', '[', '(' -> depth++
                '}', ']', ')' -> if (depth > 0) depth--
                ':' -> if (depth == 0) return index
            }
        }
        return -1
    }

    private fun unquote(value: String): String =
        if (value.length >= 2 && (value.first() == '"' || value.first() == '\'') && value.last() == value.first()) {
            value.substring(1, value.length - 1)
        } else value
}
