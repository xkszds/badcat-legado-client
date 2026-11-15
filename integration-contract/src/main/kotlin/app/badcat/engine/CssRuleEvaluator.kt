package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * A deliberately bounded first parser slice for Legado's default/CSS selector mode.
 *
 * Supports a CSS selector followed by @text, @html, @href, @src or @attr(name).
 * It does not implement Legado's full RuleAnalyzer grammar, chained selectors,
 * XPath, JSONPath, regex suffixes or JavaScript.
 */
object CssRuleEvaluator {

    fun evaluate(
        document: SourceDocument,
        expression: String,
    ): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "CSS rule is blank")
        }

        val parsed = parseExpression(rule)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Unsupported CSS rule expression: $rule",
            )

        return runCatching {
            val doc = Jsoup.parse(document.body, document.url)
            val elements = doc.select(parsed.selector)
            EngineResult.Success(elements.map { extract(it, parsed.valueMode, parsed.attribute) })
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

    private enum class ValueMode { TEXT, HTML, HREF, SRC, ATTRIBUTE }

    private fun parseExpression(expression: String): ParsedExpression? {
        val marker = expression.lastIndexOf('@')
        if (marker < 0) {
            return ParsedExpression(expression, ValueMode.TEXT)
        }

        val selector = expression.substring(0, marker).trim()
        val suffix = expression.substring(marker + 1).trim()
        if (selector.isEmpty()) return null

        return when {
            suffix.equals("text", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.TEXT)
            suffix.equals("html", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.HTML)
            suffix.equals("href", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.HREF)
            suffix.equals("src", ignoreCase = true) ->
                ParsedExpression(selector, ValueMode.SRC)
            suffix.startsWith("attr(", ignoreCase = true) && suffix.endsWith(")") -> {
                val attribute = suffix.substringAfter('(').dropLast(1).trim()
                    .takeIf(String::isNotEmpty) ?: return null
                ParsedExpression(selector, ValueMode.ATTRIBUTE, attribute)
            }
            else -> null
        }
    }

    private fun extract(
        element: Element,
        mode: ValueMode,
        attribute: String?,
    ): String = when (mode) {
        ValueMode.TEXT -> element.text()
        ValueMode.HTML -> element.html()
        ValueMode.HREF -> element.absUrl("href").ifEmpty { element.attr("href") }
        ValueMode.SRC -> element.absUrl("src").ifEmpty { element.attr("src") }
        ValueMode.ATTRIBUTE -> element.attr(attribute.orEmpty())
    }
}
