package app.badcat.engine

/**
 * Bounded port of the delimiter-scanning part of Legado's RuleAnalyzer.
 * Rule separators inside XPath/CSS bracket or parenthesis groups and quoted
 * strings are ignored. Embedded JS/JSON rule expansion is a separate phase
 * in upstream and is intentionally not handled here.
 */
data class RuleExpressionParts(
    val operator: String?,
    val rules: List<String>,
)

object RuleExpressionTokenizer {

    const val MAX_RULE_EXPRESSION_LENGTH = 16 * 1024

    private val supportedDelimiters = setOf("&&", "||", "%%")

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
        require(expression.length <= MAX_RULE_EXPRESSION_LENGTH) {
            "Rule expression exceeds the $MAX_RULE_EXPRESSION_LENGTH character limit"
        }
        require(delimiter in supportedDelimiters) {
            "Unsupported rule delimiter: $delimiter"
        }

        val parts = mutableListOf<String>()
        val stack = ArrayDeque<Char>()
        var quote: Char? = null
        var start = 0
        var index = 0
        // Determine the current branch mode once; recompute only after a split.
        // Avoid allocating a growing substring for every scanned character.
        var regexBranch = expression.trimStart().startsWith("@regex:", ignoreCase = true)

        while (index < expression.length) {
            val char = expression[index]

            // Regex rules contain literal quotes around HTML attributes and
            // character classes such as [^"]. Those quotes are regex syntax,
            // not tokenizer string delimiters.

            if (quote != null) {
                when {
                    char == '\\' -> index = (index + 2).coerceAtMost(expression.length)
                    char == quote && !(regexBranch && stack.lastOrNull() == '[') -> { quote = null; index++ }
                    else -> index++
                }
                continue
            }

            when {
                (char == '"' || char == '\'') &&
                    !(regexBranch && stack.lastOrNull() == '[') -> {
                    quote = char
                    index++
                }
                char == '\\' -> {
                    // RuleAnalyzer treats an escape outside a quoted string
                    // as escaping the following character.
                    index = (index + 2).coerceAtMost(expression.length)
                }
                char == '(' || char == '[' -> {
                    stack.addLast(char)
                    index++
                }
                char == ')' || char == ']' -> {
                    val expected = if (char == ')') '(' else '['
                    require(stack.isNotEmpty() && stack.removeLast() == expected) {
                        "Unbalanced rule expression near index $index"
                    }
                    index++
                }
                stack.isEmpty() && !(regexBranch && delimiter == "&&") &&
                    expression.startsWith(delimiter, index) -> {
                    parts += expression.substring(start, index).trim()
                    index += delimiter.length
                    start = index
                    regexBranch = expression.substring(start).trimStart()
                        .startsWith("@regex:", ignoreCase = true)
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
