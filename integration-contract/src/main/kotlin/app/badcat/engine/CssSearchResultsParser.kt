package app.badcat.engine

import org.jsoup.Jsoup

/**
 * Bounded CSS-only search result parser for the official Legado SearchRule
 * field names. Each bookList element is evaluated independently so item
 * selectors are not accidentally applied to the whole response document.
 *
 * Full AnalyzeRule semantics (JS, regex, JSONPath/XPath per-item dispatch,
 * variable interpolation, filters and direct-detail detection) remain outside
 * this adapter and are reported as unsupported.
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

        val root = Jsoup.parse(response.body, response.finalUrl)
        val items = when (val result = SelectorRuleEvaluator.evaluate(
            SourceDocument(response.finalUrl, root.outerHtml(), response.contentType),
            SelectorRuleType.CSS,
            normalizeHtmlOutput(listRule),
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }

        val candidates = ArrayList<BookCandidate>()
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
        return when (val result = SelectorRuleEvaluator.evaluate(
            SourceDocument(response.finalUrl, item.outerHtml(), response.contentType),
            SelectorRuleType.CSS,
            expression,
        )) {
            is EngineResult.Failure -> throw SearchParseFailure(result)
            is EngineResult.Success -> result.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty)
        }?.let { value ->
            if (key == "bookUrl" || key == "coverUrl") {
                Jsoup.parse("", response.finalUrl).apply {
                    val absolute = java.net.URI(response.finalUrl).resolve(value).toString()
                    return absolute
                }
            }
            value
        }
    }

    private fun normalizeHtmlOutput(rule: String): String =
        if (rule.contains("@html", ignoreCase = true)) rule else "$rule@html"

    private fun unsupported(source: BookSource, detail: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' search rule requires unsupported feature: $detail",
        )

    private class UnsupportedSearchRule(sourceId: String, field: String) :
        RuntimeException("Source '$sourceId' search field '$field' requires JavaScript/interpolation")

    private class SearchParseFailure(val failure: EngineResult.Failure) : RuntimeException(failure.message)
}
