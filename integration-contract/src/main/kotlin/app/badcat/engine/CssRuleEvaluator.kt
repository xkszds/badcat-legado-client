package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * A bounded Legado CSS selector evaluator.
 *
 * Supports @CSS: rules and CSS selectors followed by @text, @textNodes, @ownText, @html, @all,
 * direct attribute suffixes, @href, @src, @tag or @attr(name), default-mode chained selector steps,
 * and top-level &&, || and %% operators. It does not implement the full RuleAnalyzer grammar,
 * all ElementsSingle filters, XPath, JSONPath, regex suffixes or JavaScript.
 */
object CssRuleEvaluator {

    fun evaluate(
        document: SourceDocument,
        expression: String,
    ): EngineResult<List<String>> {
        val rawRule = expression.trim()
        val explicitCss = rawRule.startsWith("@CSS:", ignoreCase = true)
        val rule = when {
            explicitCss -> rawRule.substring(5).trim()
            rawRule.startsWith("@@") -> rawRule.substring(2).trim()
            else -> normalizeLegacyRootMarker(rawRule)
        }
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "CSS rule is blank")
        }
        return runCatching {
            val outerSplit = RuleExpressionTokenizer.splitByFirstTopLevelOperator(rule)
            var firstFailure: EngineResult.Failure? = null
            val combined = RuleCombinationEvaluator.evaluateNested(rule) { part ->
                when (val result = evaluateSingle(document, part, allowChainedSelectors = !explicitCss)) {
                    is EngineResult.Success -> result.value
                    is EngineResult.Failure -> {
                        if (firstFailure == null) firstFailure = result
                        emptyList()
                    }
                }
            }

            val operator = outerSplit.operator
            if (firstFailure != null && (operator != "||" || combined.isEmpty())) {
                firstFailure!!
            } else {
                EngineResult.Success(combined)
            }
        }.getOrElse {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "CSS rule evaluation failed: ${it.message ?: it::class.java.simpleName}",
                it,
            )
        }
    }

    /**
     * Select element nodes for collection fields such as bookList/chapterList.
     * Unlike scalar @html/@all extraction, collection parsing must retain one
     * HTML fragment per matched node so callers can evaluate each item in isolation.
     */
    internal fun evaluateElements(
        document: SourceDocument,
        expression: String,
    ): EngineResult<List<String>> {
        val rawRule = expression.trim()
        val explicitCss = rawRule.startsWith("@CSS:", ignoreCase = true)
        val forcedDefaultCss = rawRule.startsWith("@@")
        val rule = when {
            explicitCss -> rawRule.substring(5).trim()
            forcedDefaultCss -> rawRule.substring(2).trim()
            else -> normalizeLegacyRootMarker(rawRule)
        }
        if (rule.isEmpty()) return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "CSS rule is blank")
        return runCatching {
            var firstFailure: EngineResult.Failure? = null
            val values = RuleCombinationEvaluator.evaluateNested(rule) { part ->
                val root = Jsoup.parse(document.body, document.url)
                if (explicitCss) {
                    val parsed = parseExpression(part) ?: run {
                        if (firstFailure == null) firstFailure = EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Unsupported explicit CSS collection rule: $part",
                        )
                        return@evaluateNested emptyList()
                    }
                    root.select(parsed.selector).map { it.outerHtml() }
                } else {
                    // Default Legado mode is a sequence of selector steps:
                    // class.list@tag.ul@tag.li. The old implementation sent the
                    // entire chain to one JSoup select(), so common community
                    // sources (笔仙阁 / 爱下书 / 玄幻阁) could never build a list.
                    // A final @text/@href suffix is extraction metadata; collection
                    // parsing retains the selected element HTML for per-item rules.
                    val chain = splitRuleChain(part)
                    val selectorSteps = if (chain.lastOrNull()?.let(::parseValueMode) != null) {
                        chain.dropLast(1)
                    } else {
                        chain
                    }
                    if (selectorSteps.isEmpty()) {
                        if (firstFailure == null) firstFailure = EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Unsupported CSS collection rule: $part",
                        )
                        return@evaluateNested emptyList()
                    }
                    var selected: List<Element> = listOf(root)
                    for (step in selectorSteps) {
                        val indexedRule = LegadoElementIndex.parse(step)
                        val selector = indexedRule?.selector ?: step
                        if (selector.isBlank()) {
                            selected = emptyList()
                            break
                        }
                        val matched = selected.flatMap { parent -> selectLegadoStep(parent, selector) }
                        selected = if (indexedRule == null) matched
                            else LegadoElementIndex.apply(matched, indexedRule)
                        if (selected.isEmpty()) break
                    }
                    selected.map { it.outerHtml() }
                }
            }
            if (firstFailure != null && values.isEmpty()) firstFailure!! else EngineResult.Success(values)
        }.getOrElse {
            EngineResult.Failure(EngineErrorCode.PARSE, "CSS collection selection failed: ${it.message}", it)
        }
    }

    /**
     * Legado's default JSoup selector grammar permits a leading '@' before
     * the selector itself (for example @.w_txt li.4@textNodes). It is a
     * selector-mode marker, not a JavaScript prefix. Keep explicit parser
     * directives and the forced-CSS @@ form untouched.
     */
    private fun normalizeLegacyRootMarker(rule: String): String {
        val reserved = listOf("@@", "@css:", "@xpath:", "@json:", "@jsonpath:", "@regex:", "@js:")
        return if (rule.startsWith("@") && reserved.none { rule.startsWith(it, ignoreCase = true) }) {
            rule.substring(1).trim()
        } else {
            rule
        }
    }

    private fun evaluateSingle(
        document: SourceDocument,
        expression: String,
        allowChainedSelectors: Boolean,
    ): EngineResult<List<String>> {
        val chain = splitRuleChain(expression)
        if (allowChainedSelectors && chain.size >= 2) {
            return evaluateChained(document, chain, expression)
        }

        val parsed = parseExpression(expression)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Unsupported CSS rule expression: $expression",
            )

        return runCatching {
            val doc = Jsoup.parse(document.body, document.url)
            // Legado's default-mode ElementsSingle index suffix is part of the
            // selector head (for example ".w_txt li.4@textNodes"), not a CSS
            // class named "4". Apply it before extracting text/attributes.
            val indexedRule = if (allowChainedSelectors) LegadoElementIndex.parse(parsed.selector) else null
            val selector = indexedRule?.selector ?: parsed.selector
            val selected = if (allowChainedSelectors) {
                selectLegadoStep(doc, selector)
            } else {
                doc.select(selector).toList()
            }
            val elements = if (indexedRule == null) selected else LegadoElementIndex.apply(selected, indexedRule)
            val values = when (parsed.valueMode) {
                ValueMode.ALL -> listOf(elements.joinToString(separator = "") { it.outerHtml() })
                    .filter(String::isNotEmpty)
                ValueMode.HTML -> {
                    elements.forEach { it.select("script, style").remove() }
                    listOf(elements.joinToString(separator = "") { it.outerHtml() })
                        .filter(String::isNotEmpty)
                }
                ValueMode.ATTRIBUTE -> elements.map { it.attr(parsed.attribute.orEmpty()) }
                    .filter(String::isNotBlank)
                    .distinct()
                else -> elements.map { extract(it, parsed.valueMode, parsed.attribute) }
                    .filter(String::isNotEmpty)
            }
            EngineResult.Success(values)
        }.getOrElse {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "CSS selector evaluation failed: ${it.message ?: it::class.java.simpleName}",
                it,
            )
        }
    }

    /**
     * Default-mode Legado rules may chain selector steps with '@', for example
     * div.book@a.0@href. Each intermediate step selects descendants and applies
     * the same bounded ElementsSingle index suffix used by collection rules.
     * Explicit @CSS: rules intentionally keep their direct-selector behavior.
     */
    private fun evaluateChained(
        document: SourceDocument,
        parts: List<String>,
        original: String,
    ): EngineResult<List<String>> = runCatching {
        val root = Jsoup.parse(document.body, document.url)
        var elements: List<Element> = listOf(root)
        for (step in parts.dropLast(1)) {
            val indexRule = LegadoElementIndex.parse(step)
            val selector = indexRule?.selector ?: step
            require(selector.isNotBlank()) { "Blank selector step in chained CSS rule" }
            elements = elements.flatMap { parent ->
                val matched = selectLegadoStep(parent, selector)
                if (indexRule == null) matched else LegadoElementIndex.apply(matched, indexRule)
            }
            if (elements.isEmpty()) break
        }
        val (mode, attribute) = parseValueMode(parts.last())
            ?: throw IllegalArgumentException("Unsupported CSS value suffix: ${parts.last()}")
        val values = when (mode) {
            ValueMode.ATTRIBUTE -> elements.map { it.attr(attribute.orEmpty()) }
                .filter(String::isNotBlank)
                .distinct()
            ValueMode.HTML -> {
                elements.forEach { it.select("script, style").remove() }
                listOf(elements.joinToString(separator = "") { it.outerHtml() })
                    .filter(String::isNotEmpty)
            }
            ValueMode.ALL -> listOf(elements.joinToString(separator = "") { it.outerHtml() })
                .filter(String::isNotEmpty)
            else -> elements.map { extract(it, mode, attribute) }.filter(String::isNotEmpty)
        }
        EngineResult.Success(values)
    }.getOrElse {
        EngineResult.Failure(
            EngineErrorCode.PARSE,
            "Legado chained CSS rule failed ($original): ${it.message ?: it::class.java.simpleName}",
            it,
        )
    }

    /**
     * Legado's default JSoup mode accepts legacy selector heads in addition to
     * CSS: children, class.name, tag.name, id.name and text.value.
     */
    private fun selectLegadoStep(parent: Element, selector: String): List<Element> {
        val value = selector.trim()
        val type = value.substringBefore('.')
        val argument = value.substringAfter('.', "")
        return when (type) {
            "children" -> parent.children().toList()
            "class" -> if (argument.isNotBlank()) {
                // Legado source rules sometimes list several required class names
                // after class. (for example class.card novel-item). Match the
                // element carrying every token, rather than passing the whole
                // whitespace-separated value to Jsoup as one class name.
                val classNames = argument.split(Regex("\\s+")).filter(String::isNotBlank)
                if (classNames.size > 1 && classNames.all { it.matches(Regex("[A-Za-z0-9_-]+")) }) {
                    parent.select(classNames.joinToString(separator = "") { ".$it" }).toList()
                } else {
                    parent.getElementsByClass(argument).toList()
                }
            } else emptyList()
            "tag" -> if (argument.isNotBlank()) parent.getElementsByTag(argument).toList() else emptyList()
            "id" -> if (argument.isNotBlank()) parent.getElementsByAttributeValue("id", argument).toList() else emptyList()
            "text" -> if (argument.isNotBlank()) parent.getElementsContainingOwnText(argument).toList() else emptyList()
            else -> parent.select(value).toList()
        }
    }

    /** Splits '@' only outside CSS attribute brackets, parentheses and quotes. */
    private fun splitRuleChain(expression: String): List<String> {
        val parts = ArrayList<String>()
        var quote: Char? = null
        var escaped = false
        var bracketDepth = 0
        var parenthesisDepth = 0
        var start = 0
        expression.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            if (char == '\\') {
                escaped = true
                return@forEachIndexed
            }
            if (quote != null) {
                if (char == quote) quote = null
                return@forEachIndexed
            }
            when {
                char == 39.toChar() || char == '"' -> quote = char
                char == '[' -> bracketDepth++
                char == ']' -> if (bracketDepth > 0) bracketDepth--
                char == '(' -> parenthesisDepth++
                char == ')' -> if (parenthesisDepth > 0) parenthesisDepth--
                char == '@' && bracketDepth == 0 && parenthesisDepth == 0 -> {
                    parts += expression.substring(start, index).trim()
                    start = index + 1
                }
            }
        }
        if (start > 0) parts += expression.substring(start).trim()
        return if (parts.isEmpty()) listOf(expression.trim()) else parts
    }

    private fun parseValueMode(suffix: String): Pair<ValueMode, String?>? {
        val value = suffix.trim()
        return when {
            value.equals("text", ignoreCase = true) -> ValueMode.TEXT to null
            value.equals("textNodes", ignoreCase = true) -> ValueMode.TEXT_NODES to null
            value.equals("ownText", ignoreCase = true) -> ValueMode.OWN_TEXT to null
            value.equals("html", ignoreCase = true) -> ValueMode.HTML to null
            value.equals("all", ignoreCase = true) -> ValueMode.ALL to null
            value.equals("href", ignoreCase = true) -> ValueMode.HREF to null
            value.equals("src", ignoreCase = true) -> ValueMode.SRC to null
            value.equals("tag", ignoreCase = true) -> ValueMode.TAG to null
            value.startsWith("attr(", ignoreCase = true) && value.endsWith(")") -> {
                val attribute = value.substringAfter('(').dropLast(1).trim()
                    .takeIf(String::isNotEmpty) ?: return null
                ValueMode.ATTRIBUTE to attribute
            }
            value.matches(Regex("[A-Za-z_:][A-Za-z0-9_.:-]*")) -> ValueMode.ATTRIBUTE to value
            else -> null
        }
    }

    private data class ParsedExpression(
        val selector: String,
        val valueMode: ValueMode,
        val attribute: String? = null,
    )

    private enum class ValueMode { TEXT, TEXT_NODES, OWN_TEXT, HTML, ALL, HREF, SRC, TAG, ATTRIBUTE }

    private fun findSuffixMarker(expression: String): Int {
        var quote: Char? = null
        var escaped = false
        var bracketDepth = 0
        var parenthesisDepth = 0
        var marker = -1

        expression.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            if (char == '\\') {
                escaped = true
                return@forEachIndexed
            }
            if (quote != null) {
                if (char == quote) quote = null
                return@forEachIndexed
            }
            when {
                char == 39.toChar() || char == '"' -> quote = char
                char == '[' -> bracketDepth++
                char == ']' -> if (bracketDepth > 0) bracketDepth--
                char == '(' -> parenthesisDepth++
                char == ')' -> if (parenthesisDepth > 0) parenthesisDepth--
                char == '@' && bracketDepth == 0 && parenthesisDepth == 0 -> marker = index
            }
        }
        return marker
    }

    private fun parseExpression(expression: String): ParsedExpression? {
        val marker = findSuffixMarker(expression)
        if (marker < 0) {
            return ParsedExpression(expression, ValueMode.TEXT)
        }

        val selector = expression.substring(0, marker).trim()
        val suffix = expression.substring(marker + 1).trim()
        if (selector.isEmpty()) return null

        return when {
            suffix.equals("text", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.TEXT)
            suffix.equals("textNodes", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.TEXT_NODES)
            suffix.equals("ownText", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.OWN_TEXT)
            suffix.equals("html", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.HTML)
            suffix.equals("all", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.ALL)
            suffix.equals("href", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.HREF)
            suffix.equals("src", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.SRC)
            suffix.equals("tag", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.TAG)
            suffix.startsWith("attr(", ignoreCase = true) && suffix.endsWith(")") -> {
                val attribute = suffix.substringAfter('(').dropLast(1).trim()
                    .takeIf(String::isNotEmpty) ?: return null
                ParsedExpression(selector, ValueMode.ATTRIBUTE, attribute)
            }
            suffix.matches(Regex("[A-Za-z_:][A-Za-z0-9_.:-]*")) ->
                ParsedExpression(selector, ValueMode.ATTRIBUTE, suffix)
            else -> null
        }
    }

    private fun extract(
        element: Element,
        mode: ValueMode,
        attribute: String?,
    ): String = when (mode) {
        ValueMode.TEXT -> element.text()
        ValueMode.TEXT_NODES -> element.textNodes().map { it.text().trim() }.filter(String::isNotEmpty).joinToString("\n")
        ValueMode.OWN_TEXT -> element.ownText()
        ValueMode.HTML -> element.html()
        ValueMode.ALL -> element.outerHtml()
        ValueMode.HREF -> element.absUrl("href").ifEmpty { element.attr("href") }
        ValueMode.SRC -> element.absUrl("src").ifEmpty { element.attr("src") }
        ValueMode.TAG -> element.tagName()
        ValueMode.ATTRIBUTE -> element.attr(attribute.orEmpty())
    }
}
