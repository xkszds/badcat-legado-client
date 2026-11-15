package app.badcat.engine

/**
 * Applies the list-combination behavior used by Legado's CSS/XPath/JSONPath
 * analyzers after their rule strings have been split.
 */
object RuleCombinationEvaluator {

    fun hasNestedTopLevelOperator(expression: String): Boolean {
        val outer = RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        return outer.operator != null && outer.rules.any { part ->
            RuleExpressionTokenizer.splitByFirstTopLevelOperator(part).operator != null
        }
    }

    fun <T> evaluate(
        expression: String,
        evaluateRule: (String) -> List<T>,
    ): List<T> {
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        if (split.operator == null) return evaluateRule(split.rules.single())

        val results = mutableListOf<List<T>>()
        for (rule in split.rules) {
            val values = evaluateRule(rule)
            if (values.isNotEmpty()) {
                results += values
                if (split.operator == "||") break
            }
        }

        if (results.isEmpty()) return emptyList()

        return if (split.operator == "%%") {
            buildList {
                val maxRows = results.first().size
                for (index in 0 until maxRows) {
                    for (result in results) {
                        result.getOrNull(index)?.let(::add)
                    }
                }
            }
        } else {
            results.flatten()
        }
    }
}
