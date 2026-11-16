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
    /** Collection-root rules support CSS/XPath/JSONPath and bounded regex branches. */
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val rule = expression.trim()
        val allowUnprefixedRegexFallback = rule.contains("@regex:", ignoreCase = true) || rule.startsWith(":")
        return evaluateCollectionBranches(document, rule, allowUnprefixedRegexFallback)
    }

    private fun evaluateCollectionBranches(
        document: SourceDocument,
        expression: String,
        allowUnprefixedRegexFallback: Boolean,
    ): EngineResult<List<String>> {
        val normalized = expression.trim()
        if (normalized.isEmpty()) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Legado collection rule is blank",
            )
        }
        val split = try {
            RuleExpressionTokenizer.splitByFirstTopLevelOperator(
                normalized,
                allowLeadingColonRegex = true,
            )
        } catch (error: Exception) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Invalid combined Legado collection rule",
                error,
            )
        }

        if (split.operator == null) {
            val leaf = normalized.trim()
            if (leaf.startsWith("@regex:", ignoreCase = true)) {
                return LegadoRegexElementEvaluator.evaluate(document.body, leaf.substringAfter(':'))
            }
            if (leaf.startsWith(":")) {
                return LegadoRegexElementEvaluator.evaluate(document.body, leaf.substring(1))
            }
            val selectorResult = when (val resolved = resolve(leaf)) {
                is EngineResult.Failure -> resolved
                is EngineResult.Success -> when (resolved.value.type) {
                    SelectorRuleType.CSS -> CssRuleEvaluator.evaluateElements(document, leaf)
                    SelectorRuleType.XPATH -> XPathRuleEvaluator.evaluateElements(document, leaf)
                    SelectorRuleType.JSON_PATH -> JsonPathRuleEvaluator.evaluateElements(document, leaf)
                }
            }
            // In a mixed rule whose root explicitly opts into regex, later branches
            // may be legacy unprefixed regex fragments. Prefer a valid selector;
            // only retry as regex when selector parsing itself fails.
            return if (allowUnprefixedRegexFallback && selectorResult is EngineResult.Failure) {
                val regexResult = LegadoRegexElementEvaluator.evaluate(document.body, leaf)
                if (regexResult is EngineResult.Success) regexResult else selectorResult
            } else {
                selectorResult
            }
        }

        if (split.operator == "||") {
            var firstFailure: EngineResult.Failure? = null
            for (branch in split.rules) {
                when (val result = evaluateCollectionBranches(document, branch, allowUnprefixedRegexFallback)) {
                    is EngineResult.Failure -> if (firstFailure == null) firstFailure = result
                    is EngineResult.Success -> if (result.value.isNotEmpty()) return result
                }
            }
            return firstFailure ?: EngineResult.Success(emptyList())
        }

        val branches = ArrayList<List<String>>()
        for (branch in split.rules) {
            when (val result = evaluateCollectionBranches(document, branch, allowUnprefixedRegexFallback)) {
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
        val split = try {
            RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        } catch (error: Exception) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Invalid combined Legado rule expression",
                error,
            )
        }
        if (split.operator == null) return evaluateBranch(split.rules.single())

        if (split.operator == "||") {
            var firstFailure: EngineResult.Failure? = null
            for (part in split.rules) {
                when (val result = evaluateCombined(part, evaluateBranch)) {
                    is EngineResult.Failure -> if (firstFailure == null) firstFailure = result
                    is EngineResult.Success -> if (result.value.isNotEmpty()) return result
                }
            }
            return firstFailure ?: EngineResult.Success(emptyList())
        }

        val branches = ArrayList<List<String>>()
        for (part in split.rules) {
            when (val result = evaluateCombined(part, evaluateBranch)) {
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
