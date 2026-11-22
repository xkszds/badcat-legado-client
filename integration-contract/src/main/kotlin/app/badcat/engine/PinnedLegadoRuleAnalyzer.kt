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
        val rules = try {
            analyzer.splitRule("&&", "||", "%%")
        } catch (error: Error) {
            // The pinned RuleAnalyzer signals unbalanced selector groups with
            // java.lang.Error rather than an Exception. Normalize that parser
            // failure so the engine can return a typed PARSE/UNSUPPORTED result.
            throw IllegalArgumentException("Malformed Legado rule expression", error)
        }
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
                '[', '(' -> {
                    depth++
                    if (depth > maxDepth) maxDepth = depth
                    if (maxDepth > MAX_NESTING_DEPTH) return false
                }
                '{' -> if (code) {
                    depth++
                    if (depth > maxDepth) maxDepth = depth
                    if (maxDepth > MAX_NESTING_DEPTH) return false
                }
                ']', ')' -> if (depth > 0) depth--
                '}' -> if (code && depth > 0) depth--
            }
        }
        // Upstream's code-mode balancer also tracks curly braces; ordinary
        // CSS/XPath text does not treat them as structural delimiters.
        return true
    }
}
