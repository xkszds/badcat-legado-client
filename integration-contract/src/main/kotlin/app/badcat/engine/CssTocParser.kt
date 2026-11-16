package app.badcat.engine

import org.jsoup.Jsoup

data class ParsedTocPage(
    val chapters: List<BookChapter>,
    val nextPageUrls: List<String>,
)

/**
 * Bounded parser for official Legado TocRule fields; chapterList supports CSS and regex roots.
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
        if (rules["chapterName"].isNullOrBlank()) {
            return unsupported(source, "missing chapterName rule")
        }
        if (rules["chapterUrl"].isNullOrBlank()) {
            return unsupported(source, "missing chapterUrl rule")
        }
        if (listOf(listRule, rules["chapterName"], rules["chapterUrl"], rules["nextTocUrl"])
                .filterNotNull().any { it.contains("@js", true) || it.contains("{{") || it.contains("}}") }) {
            return unsupported(source, "JavaScript or interpolation")
        }

        val root = SourceDocument(response.finalUrl, response.body, response.contentType)
        val items = when (val result = LegadoRuleExpressionDispatcher.evaluateElements(
            root,
            listRule,
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }

        fun extract(itemHtml: String, expression: String?): EngineResult<String?> {
            if (expression.isNullOrBlank()) return EngineResult.Success(null)
            return when (val result = LegadoRuleExpressionDispatcher.evaluate(
                SourceDocument(response.finalUrl, itemHtml, response.contentType),
                expression,
            )) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    LegadoRuleExpressionDispatcher.firstScalarValue(expression, result.value)\n                        ?.trim()?.takeIf(String::isNotEmpty),
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
            if (rawUrl.isNullOrBlank()) {
                return unsupported(source, "chapterUrl did not produce a URL for chapter '$title'")
            }
            val url = runCatching { WebUrlResolver.resolve(response.finalUrl, rawUrl) }.getOrElse {
                return unsupported(source, "chapterUrl must resolve to a credential-free HTTP(S) URL")
            }
            chapters += BookChapter(index = chapters.size, title = title, url = url)
        }

        val nextUrls = ArrayList<String>()
        val nextRule = rules["nextTocUrl"]?.takeIf(String::isNotBlank)
        if (nextRule != null) {
            val values = when (val result = LegadoRuleExpressionDispatcher.evaluate(
                root,
                normalizeLinkExpression(nextRule),
            )) {
                is EngineResult.Failure -> {
                    if (result.code == EngineErrorCode.PARSE) {
                        return unsupported(source, "invalid nextTocUrl selector")
                    }
                    return result
                }
                is EngineResult.Success -> result.value
            }
            for (value in values.filter(String::isNotBlank)) {
                val url = runCatching { WebUrlResolver.resolve(response.finalUrl, value) }.getOrElse {
                    return unsupported(source, "nextTocUrl must resolve to a credential-free HTTP(S) URL")
                }
                if (url != response.finalUrl && url !in nextUrls) nextUrls += url
            }
        }
        return EngineResult.Success(ParsedTocPage(chapters, nextUrls))
    }

    private fun normalizeLinkExpression(expression: String): String {
        val rule = expression.trim()
        val explicitPath = rule.startsWith("@xpath:", true) ||
            rule.startsWith("@jsonpath:", true) || rule.startsWith("@json:", true) ||
            rule.startsWith("/")
        if (explicitPath) return rule
        val normalized = rule
            .replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
            .replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
        val suffix = normalized.substringAfterLast("@", "").lowercase()
        return if (suffix in setOf("text", "owntext", "html", "attr(href)", "attr(src)") ||
            normalized.contains("@attr(", true)
        ) normalized else "$normalized@attr(href)"
    }

    private fun unsupported(source: BookSource, reason: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' TOC rule requires unsupported behavior: $reason",
        )
}
