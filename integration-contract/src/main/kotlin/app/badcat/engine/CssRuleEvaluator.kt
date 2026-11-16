package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * A bounded Legado CSS selector evaluator.
 *
 * Supports @CSS: rules and CSS selectors followed by @text, @textNodes, @ownText, @html, @all,
 * direct attribute suffixes, @href, @src, @tag or @attr(name), plus top-level &&, || and %% operators. It does not
 * implement the full RuleAnalyzer grammar, chained selectors, XPath, JSONPath,
 * regex suffixes or JavaScript.
 */
object CssRuleEvaluator {

    fun evaluate(
        document: SourceDocument,
        expression: String,
    ): EngineResult<List<String>> {
        val rawRule = expression.trim()
        val rule = if (rawRule.startsWith("@CSS:", ignoreCase = true)) {
            rawRule.substring(5).trim()
        } else rawRule
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "CSS rule is blank")
        }

        return runCatching {
            val outerSplit = RuleExpressionTokenizer.splitByFirstTopLevelOperator(rule)
            if (RuleCombinationEvaluator.hasNestedTopLevelOperator(rule)) {
                return@runCatching EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Nested CSS list-combination operators are not supported",
                )
            }

            var firstFailure: EngineResult.Failure? = null
            val combined = RuleCombinationEvaluator.evaluate(rule) { part ->
                when (val result = evaluateSingle(document, part)) {
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

    private fun evaluateSingle(
        document: SourceDocument,
        expression: String,
    ): EngineResult<List<String>> {
        val parsed = parseExpression(expression)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Unsupported CSS rule expression: $expression",
            )

        return runCatching {
            val doc = Jsoup.parse(document.body, document.url)
            val elements = doc.select(parsed.selector)
            val values = when (parsed.valueMode) {
                ValueMode.ALL -> listOf(elements.outerHtml()).filter(String::isNotEmpty)
                ValueMode.HTML -> {
                    elements.select("script, style").remove()
                    listOf(elements.outerHtml()).filter(String::isNotEmpty)
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
