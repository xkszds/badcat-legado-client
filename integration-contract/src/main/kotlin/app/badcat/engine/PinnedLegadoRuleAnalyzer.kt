package app.badcat.engine

import io.legado.app.model.analyzeRule.RuleAnalyzer

/**
 * Bounded entry point to the pinned Legado RuleAnalyzer implementation.
 *
 * The analyzer is the same source used by upstream CSS, XPath and JSONPath
 * evaluators. Keep the guard here because imported source rules are untrusted.
 */
object PinnedLegadoRuleAnalyzer {
    const val MAX_EXPRESSION_LENGTH = 16 * 1024
    const val MAX_NESTING_DEPTH = 64

    fun split(
        expression: String,
        code: Boolean = false,
    ): RuleExpressionParts {
        require(expression.length <= MAX_EXPRESSION_LENGTH) {
            "Rule expression exceeds the $MAX_EXPRESSION_LENGTH character limit"
        }
        require(isWithinNestingLimit(expression, code)) {
            "Rule expression exceeds the $MAX_NESTING_DEPTH nesting depth limit"
        }
        val analyzer = RuleAnalyzer(expression, code)
        val rules = analyzer.splitRule("&&", "||", "%%")
        return RuleExpressionParts(
            operator = analyzer.elementsType.takeIf { it == "&&" || it == "||" || it == "%%" },
            rules = rules,
        )
    }

    private fun isWithinNestingLimit(expression: String, code: Boolean): Boolean {
        var depth = 0
        var maxDepth = 0
        var quote: Char? = null
        var escaped = false
        for (char in expression) {
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\') {
                escaped = true
                continue
            }
            if (quote != null) {
                if (char == quote) quote = null
                continue
            }
            if (char == '\'' || char == '"') {
                quote = char
                continue
            }
            when (char) {
                '[', '(' , '{' -> {
                    depth++
                    if (depth > maxDepth) maxDepth = depth
                    if (maxDepth > MAX_NESTING_DEPTH) return false
                }
                ']', ')', '}' -> if (depth > 0) depth--
            }
        }
        // code mode is used for JSONPath/embedded script balancing. The guard
        // intentionally permits unmatched braces in ordinary text but still
        // bounds the maximum nesting encountered.
        return true
    }
}
