package app.badcat.engine

/**
 * Applies the list-combination behavior used by Legado's CSS/XPath/JSONPath
 * analyzers after their rule strings have been split.
 */
object RuleCombinationEvaluator {

    fun hasNestedTopLevelOperator(expression: String): Boolean = runCatching {
        val outer = PinnedLegadoRuleAnalyzer.split(expression)
        outer.operator != null && outer.rules.any { part ->
            PinnedLegadoRuleAnalyzer.split(part.trim()).operator != null
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
        val split = PinnedLegadoRuleAnalyzer.split(expression)
        if (split.operator == null) return evaluateRule(split.rules.single())
        if (split.operator == "||") {
            // Fallback is intentionally lazy: once a branch produces values,
            // later branches must not run or turn a valid result into a failure.
            for (part in split.rules) {
                val values = evaluateNested(part.trim(), evaluateRule)
                if (values.isNotEmpty()) return values
            }
            return emptyList()
        }

        val branches = split.rules.map { part -> evaluateNested(part.trim(), evaluateRule) }
            .filter { it.isNotEmpty() }
        if (branches.isEmpty()) return emptyList()
        return when (split.operator) {
            "%%" -> buildList {
                val rows = branches.maxOfOrNull { it.size } ?: 0
                for (index in 0 until rows) branches.forEach { it.getOrNull(index)?.let(::add) }
            }
            else -> branches.flatten()
        }
    }

    /**
     * Result-aware variant used by selector runtimes. A failed || branch is
     * allowed to fall through to a later successful branch; failures in &&/%%
     * remain visible instead of being silently converted to empty values.
     */
    fun <T> evaluateNestedResult(
        expression: String,
        evaluateRule: (String) -> EngineResult<List<T>>,
    ): EngineResult<List<T>> {
        val split = PinnedLegadoRuleAnalyzer.split(expression)
        if (split.operator == null) return evaluateRule(split.rules.single())

        if (split.operator == "||") {
            var firstFailure: EngineResult.Failure? = null
            for (part in split.rules) {
                when (val result = evaluateNestedResult(part.trim(), evaluateRule)) {
                    is EngineResult.Failure -> if (firstFailure == null) firstFailure = result
                    is EngineResult.Success -> if (result.value.isNotEmpty()) return result
                }
            }
            return firstFailure ?: EngineResult.Success(emptyList())
        }

        val branches = ArrayList<List<T>>()
        for (part in split.rules) {
            when (val result = evaluateNestedResult(part.trim(), evaluateRule)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> if (result.value.isNotEmpty()) branches += result.value
            }
        }
        if (branches.isEmpty()) return EngineResult.Success(emptyList())
        val combined = if (split.operator == "%%") {
            buildList {
                val rows = branches.maxOfOrNull { it.size } ?: 0
                for (index in 0 until rows) {
                    branches.forEach { it.getOrNull(index)?.let(::add) }
                }
            }
        } else {
            branches.flatten()
        }
        return EngineResult.Success(combined)
    }

    fun <T> evaluate(
        expression: String,
        evaluateRule: (String) -> List<T>,
    ): List<T> {
        val split = PinnedLegadoRuleAnalyzer.split(expression)
        if (split.operator == null) return evaluateRule(split.rules.single())

        val results = mutableListOf<List<T>>()
        for (rule in split.rules) {
            val values = evaluateRule(rule.trim())
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
