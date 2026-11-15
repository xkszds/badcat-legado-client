package app.badcat.engine

import org.jsoup.Jsoup

data class ParsedTocPage(
    val chapters: List<BookChapter>,
    val nextPageUrls: List<String>,
)

/**
 * CSS-only parser for official Legado TocRule fields.
 * Pagination links are returned to the service; script formatting, VIP/pay,
 * volume semantics and full AnalyzeRule expressions are not interpreted.
 */
class CssTocParser {
    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        bookUrl: String,
    ): EngineResult<ParsedTocPage> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.TOC)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val listRule = rules["chapterList"]?.takeIf(String::isNotBlank)
            ?: return unsupported(source, "missing chapterList rule")
        if (listOf(listRule, rules["chapterName"], rules["chapterUrl"], rules["nextTocUrl"])
                .filterNotNull().any { it.contains("@js", true) || it.contains("{{") || it.contains("}}") }) {
            return unsupported(source, "JavaScript or interpolation")
        }

        val root = SourceDocument(response.finalUrl, response.body, response.contentType)
        val items = when (val result = SelectorRuleEvaluator.evaluate(
            root, SelectorRuleType.CSS,
            if (listRule.contains("@html", true)) listRule else "$listRule@html",
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }

        fun extract(itemHtml: String, expression: String?): EngineResult<String?> {
            if (expression.isNullOrBlank()) return EngineResult.Success(null)
            return when (val result = SelectorRuleEvaluator.evaluate(
                SourceDocument(response.finalUrl, itemHtml, response.contentType),
                SelectorRuleType.CSS,
                expression,
            )) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    result.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        }

        val chapters = ArrayList<BookChapter>()
        for (item in items) {
            val title = when (val result = extract(item, rules["chapterName"])) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            } ?: continue
            val rawUrl = when (val result = extract(item, rules["chapterUrl"]?.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")?.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)"))) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val url = if (rawUrl == null) response.finalUrl else {
                runCatching { WebUrlResolver.resolve(response.finalUrl, rawUrl) }.getOrElse {
                    return unsupported(source, "chapterUrl must resolve to a credential-free HTTP(S) URL")
                }
            }
            chapters += BookChapter(index = chapters.size, title = title, url = url)
        }

        val nextUrls = ArrayList<String>()
        val nextRule = rules["nextTocUrl"]?.takeIf(String::isNotBlank)
        if (nextRule != null) {
            val trimmed = nextRule.trim()
            val suffix = trimmed.substringAfterLast("@", "").lowercase()
            val attribute = if (suffix == "src" || suffix == "attr(src)") "src" else "href"
            val selector = trimmed.replace(
                Regex("@(?:href|src|attr\\((?:href|src)\\))$", RegexOption.IGNORE_CASE),
                "",
            ).trim()
            val values = Jsoup.parse(response.body, response.finalUrl).select(selector).map { it.attr(attribute) }
            for (value in values.filter(String::isNotBlank)) {
                val url = runCatching { WebUrlResolver.resolve(response.finalUrl, value) }.getOrElse {
                    return unsupported(source, "nextTocUrl must resolve to a credential-free HTTP(S) URL")
                }
                if (url != response.finalUrl && url !in nextUrls) nextUrls += url
            }
        }
        return EngineResult.Success(ParsedTocPage(chapters, nextUrls))
    }

    private fun unsupported(source: BookSource, reason: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' TOC rule requires unsupported behavior: $reason",
        )
}
