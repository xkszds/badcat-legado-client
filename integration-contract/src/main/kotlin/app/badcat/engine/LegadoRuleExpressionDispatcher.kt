package app.badcat.engine

/**
 * Resolves explicit parser prefixes and unambiguous path syntax used by imported
 * Legado rule fields. Unprefixed selectors default to CSS, matching the common
 * Legado selector form. Regex and JavaScript are deliberately not guessed as
 * selectors; callers receive a typed unsupported-rule result instead.
 */
data class ResolvedLegadoRuleExpression(
    val type: SelectorRuleType,
    val expression: String,
)

object LegadoRuleExpressionDispatcher {
    /** Collection-root rules use Legado's all-in-one regex mode when prefixed with ':' or @regex:. */
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> =
        evaluateCombined(expression) { branch ->
            val rule = branch.trim()
            when {
                rule.startsWith("@jsonpath:", true) || rule.startsWith("@json:", true) || rule.startsWith("$") ->
                    JsonPathRuleEvaluator.evaluateElements(document, rule)
                rule.startsWith("@regex:", true) ->
                    LegadoRegexElementEvaluator.evaluate(document.body, rule.substringAfter(':'))
                rule.startsWith(":") ->
                    LegadoRegexElementEvaluator.evaluate(document.body, rule.substring(1))
                else -> CssRuleEvaluator.evaluateElements(document, rule)
            }
        }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> =
        evaluateCombined(expression) { branch ->
            val scalarRule = branch.trim()
            if (scalarRule.startsWith("@regex:", ignoreCase = true)) {
                LegadoRegexElementEvaluator.evaluateScalar(
                    document.body,
                    scalarRule.substringAfter(':'),
                )
            } else {
                when (val resolved = resolve(branch)) {
                    is EngineResult.Failure -> resolved
                    is EngineResult.Success ->
                        SelectorRuleEvaluator.evaluate(document, resolved.value.type, resolved.value.expression)
                }
            }
        }

    /**
     * Evaluate combined selectors through the same dispatcher used by search,
     * detail, TOC and content parsing. The first non-empty || branch wins;
     * && concatenates results and %% interleaves rows.
     */
    private fun evaluateCombined(
        expression: String,
        evaluateBranch: (String) -> EngineResult<List<String>>,
    ): EngineResult<List<String>> {
        var branchFailure: EngineResult.Failure? = null
        val values = try {
            RuleCombinationEvaluator.evaluateNested(expression) { branch ->
                when (val result = evaluateBranch(branch)) {
                    is EngineResult.Success -> result.value
                    is EngineResult.Failure -> {
                        branchFailure = result
                        emptyList()
                    }
                }
            }
        } catch (error: Exception) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Invalid combined Legado rule expression",
                error,
            )
        }
        branchFailure?.let { return it }
        return EngineResult.Success(values)
    }

    fun firstScalarValue(expression: String, values: List<String>): String? {
        if (values.isEmpty()) return null
        return if (expression.trim().startsWith("@regex:", ignoreCase = true)) {
            values.getOrNull(1) ?: values.firstOrNull()
        } else {
            values.firstOrNull()
        }
    }

    fun resolve(expression: String): EngineResult<ResolvedLegadoRuleExpression> {
        val raw = expression.trim()
        if (raw.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Legado rule expression is blank")
        }
        val prefixes = listOf(
            "@css:" to SelectorRuleType.CSS,
            "@xpath:" to SelectorRuleType.XPATH,
            "@jsonpath:" to SelectorRuleType.JSON_PATH,
            "@json:" to SelectorRuleType.JSON_PATH,
        )
        val prefix = prefixes.firstOrNull { raw.startsWith(it.first, ignoreCase = true) }
        if (prefix != null) {
            val body = raw.substring(prefix.first.length).trim()
            if (body.isEmpty()) {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Legado ${prefix.second.name.lowercase()} expression is blank",
                )
            }
            return EngineResult.Success(ResolvedLegadoRuleExpression(prefix.second, body))
        }
        if (raw.startsWith("@regex:", ignoreCase = true) || raw.startsWith("@js:", ignoreCase = true)) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Regex and JavaScript rule expressions require their dedicated Legado runtime",
            )
        }
        val inferred = when {
            raw.startsWith("$") -> SelectorRuleType.JSON_PATH
            raw.startsWith("/") || raw.startsWith("(") && raw.contains("/") ->
                SelectorRuleType.XPATH
            else -> SelectorRuleType.CSS
        }
        return EngineResult.Success(ResolvedLegadoRuleExpression(inferred, raw))
    }
}
