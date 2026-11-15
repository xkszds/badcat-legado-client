package app.badcat.engine

/**
 * Lexical groundwork for Legado's rule-combination operators.
 *
 * Splits only when the delimiter is outside quoted strings and balanced
 * (), [] and {} groups. This prevents operators embedded in CSS attribute
 * values, function arguments or script/object literals from being mistaken
 * for top-level separators.
 *
 * This class tokenizes rule operators; evaluation behavior lives separately.
 */
data class RuleExpressionParts(
    val operator: String?,
    val rules: List<String>,
)

object RuleExpressionTokenizer {

    private val supportedDelimiters = setOf("&&", "||", "%%")

    /** Matches the upstream first-operator choice, then splits on that operator. */
    fun splitByFirstTopLevelOperator(expression: String): RuleExpressionParts {
        val candidates = supportedDelimiters.mapNotNull { delimiter ->
            val parts = splitTopLevel(expression, delimiter)
            if (parts.size > 1) Triple(parts.first().length, delimiter, parts) else null
        }
        val first = candidates.minByOrNull { it.first }
            ?: return RuleExpressionParts(null, listOf(expression.trim()))
        return RuleExpressionParts(first.second, first.third)
    }

    fun splitTopLevel(expression: String, delimiter: String): List<String> {
        require(delimiter in supportedDelimiters) {
            "Unsupported rule delimiter: $delimiter"
        }

        val parts = mutableListOf<String>()
        val stack = ArrayDeque<Char>()
        var quote: Char? = null
        var escaped = false
        var start = 0
        var index = 0

        while (index < expression.length) {
            val char = expression[index]

            if (quote != null) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == quote -> quote = null
                }
                index++
                continue
            }

            when {
                char == '"' || char == '\'' -> {
                    quote = char
                    index++
                }
                char == '(' || char == '[' || char == '{' -> {
                    stack.addLast(char)
                    index++
                }
                char == ')' || char == ']' || char == '}' -> {
                    val expected = when (char) {
                        ')' -> '('
                        ']' -> '['
                        else -> '{'
                    }
                    require(stack.isNotEmpty() && stack.removeLast() == expected) {
                        "Unbalanced rule expression near index $index"
                    }
                    index++
                }
                stack.isEmpty() && expression.startsWith(delimiter, index) -> {
                    parts += expression.substring(start, index).trim()
                    index += delimiter.length
                    start = index
                }
                else -> index++
            }
        }

        require(quote == null) { "Unterminated quoted string in rule expression" }
        require(stack.isEmpty()) { "Unclosed group in rule expression" }

        parts += expression.substring(start).trim()
        return parts
    }
}
