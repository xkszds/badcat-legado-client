package app.badcat.engine

/**
 * Applies the list-combination behavior used by Legado's CSS/XPath/JSONPath
 * analyzers after their rule strings have been split.
 */
object RuleCombinationEvaluator {

    fun hasNestedTopLevelOperator(expression: String): Boolean = runCatching {
        val outer = RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        outer.operator != null && outer.rules.any { part ->
            RuleExpressionTokenizer.splitByFirstTopLevelOperator(part).operator != null
        }
    }.getOrDefault(false)

    /**
     * Evaluates mixed nested list operators recursively. Each expression is
     * split at its first top-level operator, then each branch is evaluated
     * independently before the outer operator combines the branch results.
     */
    fun <T> evaluateNested(
        expression: String,
        evaluateRule: (String) -> List<T>,
    ): List<T> {
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        if (split.operator == null) return evaluateRule(split.rules.single())
        val branches = split.rules.map { part -> evaluateNested(part, evaluateRule) }
        val nonEmpty = if (split.operator == "||") {
            listOfNotNull(branches.firstOrNull { it.isNotEmpty() })
        } else {
            branches.filter { it.isNotEmpty() }
        }
        if (nonEmpty.isEmpty()) return emptyList()
        return when (split.operator) {
            "%%" -> buildList {
                val rows = nonEmpty.maxOfOrNull { it.size } ?: 0
                for (index in 0 until rows) nonEmpty.forEach { it.getOrNull(index)?.let(::add) }
            }
            else -> nonEmpty.flatten()
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
                val maxRows = results.maxOfOrNull { it.size } ?: 0
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
