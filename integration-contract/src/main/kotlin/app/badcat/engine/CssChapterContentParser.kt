package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode

/**
 * Parses the bounded CSS subset of Legado ContentRule.
 * It deliberately does not execute webJs/callBackJs or emulate sourceRegex,
 * replaceRegex, payAction, imageDecode, or contentBatch.
 */
class CssChapterContentParser {
    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        chapter: BookChapter,
    ): EngineResult<ChapterContent> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.CONTENT)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val contentRule = rules["content"]?.takeIf(String::isNotBlank)
            ?: return unsupported(source, "missing content rule")

        val expressions = listOf(
            contentRule,
            rules["title"],
            rules["nextContentUrl"],
        ).filterNotNull()
        if (expressions.any {
                it.contains("@js", true) || it.contains("{{") || it.contains("}}")
            } || !rules["webJs"].isNullOrBlank() || !rules["callBackJs"].isNullOrBlank()) {
            return unsupported(source, "JavaScript or interpolation")
        }

        val document = SourceDocument(response.finalUrl, response.body, response.contentType)
        val contentValues = when (val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            if (contentRule.contains("@", ignoreCase = true)) contentRule else "$contentRule@html",
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (contentValues.isEmpty()) {
            return EngineResult.Failure(
                EngineErrorCode.NOT_FOUND,
                "No chapter content matched source '${source.id}' rule",
            )
        }

        val title = when (val result = evaluateOptional(document, rules["title"], response.finalUrl)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value?.takeIf(String::isNotBlank) ?: chapter.title
        }
        val nextRule = rules["nextContentUrl"]?.trim().orEmpty()
        val nextUrl = if (nextRule.isEmpty()) null else {
            val rawLink = when (val result = LegadoRuleExpressionDispatcher.evaluate(
                document,
                normalizeLinkExpression(nextRule),
            )) {
                is EngineResult.Failure -> {
                    if (result.code == EngineErrorCode.PARSE) {
                        return unsupported(source, "invalid nextContentUrl selector")
                    }
                    return result
                }
                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(
                    normalizeLinkExpression(nextRule), result.value,
                )?.trim()?.takeIf(String::isNotEmpty)
            }
            if (rawLink.isNullOrBlank()) null else {
                runCatching { WebUrlResolver.resolve(response.finalUrl, rawLink) }.getOrElse {
                    return unsupported(source, "nextContentUrl must resolve to a credential-free HTTP(S) URL")
                }
            }
        }

        val text = contentValues.mapNotNull(::htmlToReadableText)
            .filter(String::isNotBlank)
            .joinToString("\n\n")
        if (text.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.NOT_FOUND,
                "Chapter content is empty after HTML cleanup for source '${source.id}'",
            )
        }
        return EngineResult.Success(ChapterContent(title, text, nextUrl))
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

    private fun evaluateOptional(
        document: SourceDocument,
        expression: String?,
        baseUrl: String,
        defaultAttribute: String? = null,
    ): EngineResult<String?> {
        if (expression.isNullOrBlank()) return EngineResult.Success(null)
        val normalized = if (defaultAttribute != null &&
            expression.substringAfterLast("@", "").let { it !in setOf("href", "src", "text", "html") }
        ) "$expression@attr($defaultAttribute)" else expression
        return when (val result = LegadoRuleExpressionDispatcher.evaluate(document, normalized.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)").replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)"))) {
            is EngineResult.Failure -> result
            is EngineResult.Success -> EngineResult.Success(
                LegadoRuleExpressionDispatcher.firstScalarValue(normalized, result.value)
                    ?.trim()?.takeIf(String::isNotEmpty),
            )
        }
    }

    private fun htmlToReadableText(html: String): String? = runCatching {
        val body = Jsoup.parseBodyFragment(html).body()
        body.select("script,style,noscript,iframe").remove()
        body.select("br").forEach { it.replaceWith(TextNode("\n")) }
        body.select("p,div,section,article,li,h1,h2,h3,h4,h5,h6,blockquote").forEach {
            it.appendChild(TextNode("
"))
        }
        body.wholeText()
            .replace('\u00a0', ' ')
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString("\n")
    }.getOrNull()

    private fun unsupported(source: BookSource, reason: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' content rule requires unsupported behavior: $reason",
        )
}
