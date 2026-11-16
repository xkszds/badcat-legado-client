package app.badcat.engine

import org.jsoup.Jsoup

/**
 * Bounded search result parser for the official Legado SearchRule
 * field names. Each bookList element is evaluated independently so item
 * selectors are not accidentally applied to the whole response document.
 *
 * Bounded regex is supported for collection roots and scalar fields. Full
 * AnalyzeRule semantics (JS, variable interpolation, filters, chained per-item
 * rule modes and direct-detail detection) remain unsupported.
 */
class CssSearchResultsParser {

    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
    ): EngineResult<List<BookCandidate>> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.SEARCH)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val listRule = rules["bookList"]?.takeIf(String::isNotBlank)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Source '${source.id}' has no search bookList rule",
            )
        if (listRule.contains("@js", ignoreCase = true) || listRule.contains("{{")) {
            return unsupported(source, "JavaScript or interpolation in bookList")
        }
        if (hasNonHtmlOutputSuffix(listRule)) {
            return unsupported(source, "bookList must return element HTML, not extracted text or attributes")
        }

        // Preserve the original response bytes/text for regex roots; Jsoup.outerHtml()
        // normalizes attribute quotes and can change the meaning of an imported regex.
        val document = SourceDocument(response.finalUrl, response.body, response.contentType)
        val items = when (val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            listRule,
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }

        val candidates = ArrayList<BookCandidate>()
        try {
            for (itemHtml in items) {
                val item = Jsoup.parse(itemHtml, response.finalUrl)
                val name = field(source, rules, "name", item, response) ?: continue
                val bookUrl = field(source, rules, "bookUrl", item, response)
                    ?.takeIf(String::isNotBlank) ?: continue
                candidates += BookCandidate(
                    sourceId = source.id,
                    name = name,
                    author = field(source, rules, "author", item, response),
                    bookUrl = bookUrl,
                    coverUrl = field(source, rules, "coverUrl", item, response),
                    latestChapter = field(source, rules, "lastChapter", item, response),
                    intro = field(source, rules, "intro", item, response),
                )
            }
        } catch (failure: SearchParseFailure) {
            return failure.failure
        } catch (failure: UnsupportedSearchRule) {
            return unsupported(source, failure.message ?: "unsupported search field")
        } catch (failure: Exception) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Could not parse search results for source '${source.id}'",
                failure,
            )
        }
        return EngineResult.Success(candidates.distinctBy { it.bookUrl })
    }

    private fun field(
        source: BookSource,
        rules: Map<String, String>,
        key: String,
        item: org.jsoup.nodes.Document,
        response: SourceHttpResponse,
    ): String? {
        val expression = rules[key]?.takeIf(String::isNotBlank) ?: return null
        if (expression.contains("@js", ignoreCase = true) || expression.contains("{{")) {
            throw UnsupportedSearchRule(source.id, key)
        }
        val extractionRule = when (key) {
            "bookUrl" -> expression.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
            "coverUrl" -> expression.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
            else -> expression
        }
        return when (val result = LegadoRuleExpressionDispatcher.evaluate(
            SourceDocument(response.finalUrl, item.outerHtml(), response.contentType),
            extractionRule,
        )) {
            is EngineResult.Failure -> throw SearchParseFailure(result)
            is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(extractionRule, result.value)
                ?.trim()?.takeIf(String::isNotEmpty)
        }?.let { value ->
            if (key == "bookUrl" || key == "coverUrl") {
                return try {
                    WebUrlResolver.resolve(response.finalUrl, value)
                } catch (_: Exception) {
                    throw UnsupportedSearchRule(source.id, "$key must resolve to an HTTP(S) URL")
                }
            }
            value
        }
    }

    /**
     * bookList is a collection of element nodes, not a scalar field. Apply
     * @html to every top-level branch so && / || / %% never mix serialized
     * elements with text values before each card is parsed independently.
     */
    private fun normalizeHtmlOutput(rule: String): String {
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(rule)
        if (split.operator == null) {
            return if (hasHtmlOutputSuffix(rule)) rule else "$rule@html"
        }
        return split.rules.joinToString(split.operator) { normalizeHtmlOutput(it) }
    }

    private fun hasHtmlOutputSuffix(rule: String): Boolean =
        rule.trim().endsWith("@html", ignoreCase = true)

    private fun hasNonHtmlOutputSuffix(rule: String): Boolean {
        val root = rule.trim()
        if (root.startsWith(":") || root.startsWith("@regex:", ignoreCase = true)) return false
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(rule)
        if (split.operator != null) return split.rules.any(::hasNonHtmlOutputSuffix)
        val value = rule.trim()
        return listOf("@text", "@ownText", "@href", "@src", "@tag")
            .any { value.endsWith(it, ignoreCase = true) } ||
            Regex("@attr\\([^)]*\\)$", RegexOption.IGNORE_CASE).containsMatchIn(value)
    }

    private fun unsupported(source: BookSource, detail: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' search rule requires unsupported feature: $detail",
        )

    private class UnsupportedSearchRule(sourceId: String, field: String) :
        RuntimeException("Source '$sourceId' search field '$field' requires JavaScript/interpolation")

    private class SearchParseFailure(val failure: EngineResult.Failure) : RuntimeException(failure.message)
}
