package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoCssRuleEvaluator

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
        val parsed = LegadoRuleReplacement.parse(expression)
        val rule = parsed.expression.trim()
        val allowUnprefixedRegexFallback = rule.contains("@regex:", ignoreCase = true) || rule.startsWith(":")
        val evaluated = evaluateCollectionBranches(document, rule, allowUnprefixedRegexFallback)
        return when (evaluated) {
            is EngineResult.Failure -> evaluated
            is EngineResult.Success -> parsed.apply(evaluated.value)
        }
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
            if (allowUnprefixedRegexFallback && leaf.startsWith("<")) {
                return EngineResult.Success(listOf(leaf))
            }
            val selectorResult = when (val resolved = resolve(leaf)) {
                is EngineResult.Failure -> resolved
                is EngineResult.Success -> when (resolved.value.type) {
                    // Always pass the normalized expression. In particular, a
                    // single leading '@' is Legado's CSS mode marker, not part
                    // of the selector text.
                    SelectorRuleType.CSS -> runCatching {
                        EngineResult.Success(
                            UpstreamLegadoCssRuleEvaluator.getElementHtmlList(
                                document.body,
                                document.url,
                                toUpstreamCssRule(stripExtractionSuffix(leaf)),
                            ),
                        )
                    }.getOrElse { error ->
                        EngineResult.Failure(
                            EngineErrorCode.PARSE,
                            "Legado CSS collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                            error,
                        )
                    }
                    SelectorRuleType.XPATH -> XPathRuleEvaluator.evaluateElements(document, resolved.value.expression)
                    SelectorRuleType.JSON_PATH -> JsonPathRuleEvaluator.evaluateElements(document, resolved.value.expression)
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

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val parsed = LegadoRuleReplacement.parse(expression)
        val evaluated = evaluateCombined(parsed.expression) { branch ->
            val scalarRule = branch.trim()
            if (scalarRule.startsWith("@regex:", ignoreCase = true) || scalarRule.startsWith(":")) {
                LegadoRegexElementEvaluator.evaluateScalar(
                    document.body,
                    if (scalarRule.startsWith("@regex:", ignoreCase = true)) {
                        scalarRule.substringAfter(':')
                    } else {
                        scalarRule.substring(1)
                    },
                )
            } else {
                when (val resolved = resolve(branch)) {
                    is EngineResult.Failure -> resolved
                    is EngineResult.Success ->
                        if (resolved.value.type == SelectorRuleType.CSS) {
                            runCatching {
                                val suffix = branch.substringAfterLast("@", "").lowercase()
                                val upstreamRule = toUpstreamCssRule(withDefaultTextExtraction(branch))
                                val upstreamValues = if (suffix == "tag") {
                                    UpstreamLegadoCssRuleEvaluator.getElementTagList(
                                        document.body, document.url, upstreamRule,
                                    )
                                } else {
                                    UpstreamLegadoCssRuleEvaluator.getStringList(
                                        document.body, document.url, upstreamRule,
                                    )
                                }
                                // Empty is a valid upstream result. Never revive a
                                // no-match with the retired bounded CSS implementation.
                                EngineResult.Success(upstreamValues)
                            }.getOrElse { error ->
                                EngineResult.Failure(
                                    EngineErrorCode.PARSE,
                                    "Legado CSS evaluation failed: ${error.message ?: error::class.java.simpleName}",
                                    error,
                                )
                            }
                        } else {
                            SelectorRuleEvaluator.evaluate(document, resolved.value.type, resolved.value.expression)
                        }
                }
            }
        }
        return when (evaluated) {
            is EngineResult.Failure -> evaluated
            is EngineResult.Success -> parsed.apply(evaluated.value)
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
            RuleExpressionTokenizer.splitByFirstTopLevelOperator(
                expression,
                allowLeadingColonRegex = true,
            )
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


    private fun withDefaultTextExtraction(expression: String): String {
        val raw = expression.trim()
        val lastAt = raw.lastIndexOf("@")
        val hasLeadingCssModeOnly = raw.startsWith("@") && raw.indexOf("@", 1) < 0
        val suffix = raw.substringAfterLast("@", "").lowercase()
        if (!hasLeadingCssModeOnly && lastAt >= 0 &&
            (suffix in setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag") ||
                suffix.startsWith("attr(") || suffix.matches(Regex("[a-z_:][a-z0-9_:-]*")))) {
            return raw
        }
        return "$raw@text"
    }

    private fun stripExtractionSuffix(expression: String): String {
        val suffix = expression.substringAfterLast("@", "").lowercase()
        return if (suffix in setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag") ||
            suffix.startsWith("attr(")) expression.substringBeforeLast("@") else expression
    }

    private fun toUpstreamCssRule(expression: String): String {
        val raw = expression.trim()
        val normalized = raw.replace(Regex("@attr\\(([^)]+)\\)", RegexOption.IGNORE_CASE)) {
            "@${it.groupValues[1]}"
        }
        return when {
            normalized.startsWith("@@") -> "@CSS:${normalized.substring(2).trim()}"
            normalized.startsWith("@css:", ignoreCase = true) -> normalized
            else -> normalized
        }
    }

    fun firstScalarValue(expression: String, values: List<String>): String? {
        if (values.isEmpty()) return null
        val normalized = expression.trim()
        return if (normalized.startsWith("@regex:", ignoreCase = true) || normalized.startsWith(":")) {
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
        // In upstream AnalyzeRule, a leading "@@" forces the default
        // JSoup/CSS mode and is removed before selector evaluation.
        if (raw.startsWith("@@")) {
            val body = raw.substring(2).trim()
            if (body.isEmpty()) {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Legado forced-CSS expression is blank",
                )
            }
            return EngineResult.Success(ResolvedLegadoRuleExpression(SelectorRuleType.CSS, body))
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
        // Legado's default JSoup mode uses a single leading '@' to mark a
        // selector rule (for example @.w_txt li.4@textNodes). Strip that
        // marker here, before the expression is dispatched to any evaluator;
        // it is not a Rhino/JavaScript expression.
        val legacyCss = raw.startsWith("@") && !raw.startsWith("@@")
        val normalized = if (legacyCss) raw.substring(1).trim() else raw
        if (normalized.isBlank()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Legado CSS selector is blank")
        }
        val inferred = when {
            normalized.startsWith("$") -> SelectorRuleType.JSON_PATH
            normalized.startsWith("/") || normalized.startsWith("(") && normalized.contains("/") ->
                SelectorRuleType.XPATH
            else -> SelectorRuleType.CSS
        }
        return EngineResult.Success(ResolvedLegadoRuleExpression(inferred, normalized))
    }
}
