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

    fun splitByFirstTopLevelOperator(
        expression: String,
        allowLeadingColonRegex: Boolean = false,
    ): RuleExpressionParts {
        val candidates = supportedDelimiters.mapNotNull { delimiter ->
            val parts = splitTopLevel(expression, delimiter, allowLeadingColonRegex)
            if (parts.size > 1) Triple(parts.first().length, delimiter, parts) else null
        }
        val first = candidates.minByOrNull { it.first }
            ?: return RuleExpressionParts(null, listOf(expression.trim()))
        return RuleExpressionParts(first.second, first.third)
    }

    fun splitTopLevel(
        expression: String,
        delimiter: String,
        allowLeadingColonRegex: Boolean = false,
    ): List<String> {
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
        fun isRegexBranch(value: String): Boolean {
            val trimmed = value.trimStart()
            return trimmed.startsWith("@regex:", ignoreCase = true) ||
                (allowLeadingColonRegex && trimmed.startsWith(":"))
        }
        var regexBranch = isRegexBranch(expression)
        var regexParentheses = 0
        var regexBrackets = 0

        while (index < expression.length) {
            val char = expression[index]

            // Regex rules contain literal quotes around HTML attributes and
            // character classes such as [^"]. Track quotes only to protect
            // separators, while still tracking regex groups inside attributes.
            if (regexBranch) {
                // Regex syntax is not CSS/XPath syntax. Keep separator detection
                // bounded without rejecting escaped quotes in regex character classes.
                when {
                    char == '\\' -> index = (index + 2).coerceAtMost(expression.length)
                    char == '"' || char == '\'' -> {
                        if (quote == null) quote = char
                        else if (quote == char) quote = null
                        index++
                    }
                    quote == null && char == '(' -> { regexParentheses++; index++ }
                    quote == null && char == ')' -> { regexParentheses = (regexParentheses - 1).coerceAtLeast(0); index++ }
                    quote == null && char == '[' -> { regexBrackets++; index++ }
                    quote == null && char == ']' -> { regexBrackets = (regexBrackets - 1).coerceAtLeast(0); index++ }
                    quote == null && regexParentheses == 0 && regexBrackets == 0 &&
                        expression.startsWith(delimiter, index) -> {
                        parts += expression.substring(start, index).trim()
                        index += delimiter.length
                        start = index
                        regexParentheses = 0
                        regexBrackets = 0
                        regexBranch = isRegexBranch(expression.substring(start))
                    }
                    else -> index++
                }
                continue
            }

            if (quote != null) {
                when {
                    char == '\\' -> index = (index + 2).coerceAtMost(expression.length)
                    char == quote -> { quote = null; index++ }
                    else -> index++
                }
                continue
            }

            when {
                char == '"' || char == '\'' -> {
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
                stack.isEmpty() && expression.startsWith(delimiter, index) -> {
                    parts += expression.substring(start, index).trim()
                    index += delimiter.length
                    start = index
                    regexBranch = isRegexBranch(expression.substring(start))
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
