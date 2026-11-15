package app.badcat.engine

import java.net.URI
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
            val rawUrl = when (val result = extract(item, rules["chapterUrl"])) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val url = rawUrl?.let { resolve(bookUrl, it) } ?: response.finalUrl
            chapters += BookChapter(index = chapters.size, title = title, url = url)
        }

        val nextUrls = ArrayList<String>()
        val nextRule = rules["nextTocUrl"]?.takeIf(String::isNotBlank)
        if (nextRule != null) {
            val urlRule = if (nextRule.substringAfterLast("@", "").let { it in setOf("href", "src", "text", "html") }) nextRule else "$nextRule@href"
            when (val result = SelectorRuleEvaluator.evaluate(root, SelectorRuleType.CSS, urlRule)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value.map { resolve(response.finalUrl, it) }
                    .filter { it != response.finalUrl }
                    .forEach { if (it !in nextUrls) nextUrls += it }
            }
        }
        return EngineResult.Success(ParsedTocPage(chapters, nextUrls))
    }

    private fun resolve(base: String, value: String): String =
        runCatching { URI(base).resolve(value).toString() }.getOrDefault(value)

    private fun unsupported(source: BookSource, reason: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' TOC rule requires unsupported behavior: $reason",
        )
}
