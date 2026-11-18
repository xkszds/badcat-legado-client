package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode

/**
 * Parses the bounded CSS subset of Legado ContentRule.
 * It deliberately does not execute webJs/callBackJs or emulate sourceRegex,
 * replaceRegex, payAction, imageDecode, or contentBatch.
 */
class CssChapterContentParser {
    /** Executes bounded JavaScript content/title/next-page fields in production reads. */
    suspend fun parseWithScripts(
        source: BookSource,
        response: SourceHttpResponse,
        chapter: BookChapter,
        scriptRuntime: SourceScriptRuntime,
        page: Int = 1,
    ): EngineResult<ChapterContent> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.CONTENT)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val contentRule = rules["content"]?.takeIf(String::isNotBlank)
            ?: return unsupported(source, "missing content rule")
        val unsupportedWebFields = listOf("webJs", "sourceRegex", "subContent", "imageDecode", "payAction", "callBackJs", "contentBatch")
            .filter { !rules[it].isNullOrBlank() }
        if (unsupportedWebFields.isNotEmpty()) {
            return unsupported(
                source,
                "content rule fields require unavailable WebView, image, payment or batch runtime: ${unsupportedWebFields.joinToString()}",
            )
        }
        val document = SourceDocument(response.finalUrl, response.body, response.contentType, page = page)

        suspend fun evaluateField(key: String, expression: String): EngineResult<String?> {
            val directGet = LegadoPutRule.isDirectGet(expression)
            var trimmed = when (val put = LegadoPutRule.resolve(source, expression.trim(), document, scriptRuntime)) {
                is EngineResult.Failure -> return put
                is EngineResult.Success -> put.value
            }
            trimmed = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                    source = source,
                    template = trimmed,
                    result = response.body,
                    baseUrl = response.finalUrl,
                    ruleName = key,
                    scriptRuntime = scriptRuntime,
                    page = page,
                )) {
                    is EngineResult.Failure -> return expanded
                    is EngineResult.Success -> expanded.value
                }
            if (directGet) return EngineResult.Success(trimmed.trim().takeIf(String::isNotEmpty))
            if (trimmed.startsWith("@js:", true) || trimmed.startsWith("javascript:", true)) {
                val script = trimmed.substringAfter(':').trim()
                if (script.isBlank()) return unsupported(source, "empty JavaScript rule for '$key'")
                return when (val result = scriptRuntime.evaluate(
                    source,
                    script,
                    mapOf(
                        "result" to response.body,
                        "baseUrl" to response.finalUrl,
                        "sourceUrl" to source.id,
                        "sourceId" to source.id,
                        "sourceName" to source.name,
                        "ruleName" to key,
                        "page" to page.toString(),
                    ),
                )) {
                    is EngineResult.Failure -> result
                    is EngineResult.Success -> EngineResult.Success(result.value.takeIf(String::isNotBlank))
                }
            }
            val normalized = when (key) {
                "nextContentUrl" -> normalizeLinkExpression(trimmed)
                else -> trimmed
            }
            return when (val result = LegadoRuleExpressionDispatcher.evaluate(document, normalized)) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    LegadoRuleExpressionDispatcher.firstScalarValue(normalized, result.value)
                        ?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        }

        val directContentGet = LegadoPutRule.isDirectGet(contentRule)
        var resolvedContentRule = when (val put = LegadoPutRule.resolve(source, contentRule.trim(), document, scriptRuntime)) {
            is EngineResult.Failure -> return put
            is EngineResult.Success -> put.value
        }
        resolvedContentRule = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                source = source,
                template = resolvedContentRule,
                result = response.body,
                baseUrl = response.finalUrl,
                ruleName = "content",
                scriptRuntime = scriptRuntime,
                page = page,
            )) {
                is EngineResult.Failure -> return expanded
                is EngineResult.Success -> expanded.value
            }
        val contentValue = when {
            directContentGet -> resolvedContentRule
            resolvedContentRule.startsWith("@js:", true) ||
                resolvedContentRule.startsWith("javascript:", true) -> when (val result = evaluateField("content", resolvedContentRule)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> result.value
                }
            else -> when (val result = LegadoRuleExpressionDispatcher.evaluate(
                document,
                if (resolvedContentRule.contains("@", true)) resolvedContentRule else "$resolvedContentRule@html",
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value.joinToString("\n")
            }
        } ?: return EngineResult.Failure(
            EngineErrorCode.NOT_FOUND,
            "No chapter content matched source '${source.id}' rule",
        )

        val title = rules["title"]?.takeIf(String::isNotBlank)?.let {
            when (val result = evaluateField("title", it)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
        }?.takeIf(String::isNotBlank) ?: chapter.title

        val nextUrl = rules["nextContentUrl"]?.takeIf(String::isNotBlank)?.let {
            when (val result = evaluateField("nextContentUrl", it)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
        }?.let { raw ->
            runCatching { WebUrlResolver.resolve(response.finalUrl, raw) }.getOrElse {
                return unsupported(source, "nextContentUrl must resolve to a credential-free HTTP(S) URL")
            }
        }

        var text = htmlToReadableText(contentValue).orEmpty()
        val replaceRule = rules["replaceRegex"]?.takeIf(String::isNotBlank)
        if (replaceRule != null) {
            val replacementDocument = SourceDocument(response.finalUrl, text, "text/plain", page = page)
            val resolvedReplacement = when (val put = LegadoPutRule.resolve(source, replaceRule, replacementDocument, scriptRuntime)) {
                is EngineResult.Failure -> return put
                is EngineResult.Success -> put.value
            }
            val expandedReplacement = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                source = source,
                template = resolvedReplacement,
                result = text,
                baseUrl = response.finalUrl,
                ruleName = "replaceRegex",
                scriptRuntime = scriptRuntime,
                page = page,
            )) {
                is EngineResult.Failure -> return expanded
                is EngineResult.Success -> expanded.value
            }
            val parsedReplacement = LegadoRuleReplacement.parse(expandedReplacement)
            val replacementValue = when {
                parsedReplacement.expression.isBlank() -> text
                parsedReplacement.expression.startsWith("@js:", true) ||
                    parsedReplacement.expression.startsWith("javascript:", true) -> {
                    val script = parsedReplacement.expression.substringAfter(':').trim()
                    when (val evaluated = scriptRuntime.evaluate(
                        source,
                        script,
                        mapOf(
                            "result" to text,
                            "baseUrl" to response.finalUrl,
                            "sourceUrl" to source.id,
                            "sourceId" to source.id,
                            "sourceName" to source.name,
                            "ruleName" to "replaceRegex",
                            "page" to page.toString(),
                        ),
                    )) {
                        is EngineResult.Failure -> return evaluated
                        is EngineResult.Success -> evaluated.value
                    }
                }
                else -> when (val evaluated = LegadoRuleExpressionDispatcher.evaluate(
                    replacementDocument.copy(body = text),
                    parsedReplacement.expression,
                )) {
                    is EngineResult.Failure -> return evaluated
                    is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(
                        parsedReplacement.expression,
                        evaluated.value,
                    ).orEmpty()
                }
            }
            text = when (val applied = parsedReplacement.apply(listOf(replacementValue))) {
                is EngineResult.Failure -> return applied
                is EngineResult.Success -> applied.value.firstOrNull().orEmpty()
            }
        }
        if (text.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.NOT_FOUND,
                "Chapter content is empty after HTML cleanup for source '${source.id}'",
            )
        }
        return EngineResult.Success(ChapterContent(title, text, nextUrl))
    }

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

        val unsupportedFields = listOf(
            "webJs", "sourceRegex", "replaceRegex", "subContent", "imageDecode", "payAction", "callBackJs", "contentBatch",
        ).filter { !rules[it].isNullOrBlank() }
        if (unsupportedFields.isNotEmpty()) {
            return unsupported(source, "content rule fields are not supported by the synchronous parser: ${unsupportedFields.joinToString()}")
        }
        val expressions = listOf(
            contentRule,
            rules["title"],
            rules["nextContentUrl"],
        ).filterNotNull()
        if (expressions.any {
                it.contains("@js:", true) || it.contains("{{") || it.contains("}}")
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
            val rawLink = when (val result = evaluateOptional(
                document,
                normalizeLinkExpression(nextRule),
                response.finalUrl,
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
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

    private fun normalizeLinkExpression(expression: String): String =
        LegadoRuleReplacement.transformExpression(expression) { raw ->
            val rule = raw.trim()
            val explicitPath = rule.startsWith("@regex:", true) || rule.startsWith("@xpath:", true) ||
                rule.startsWith("@jsonpath:", true) || rule.startsWith("@json:", true) ||
                rule.startsWith("/") || rule.startsWith("$")
            if (explicitPath) rule else {
                val normalized = rule
                    .replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
                    .replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
                val suffix = normalized.substringAfterLast("@", "").lowercase()
                if (suffix in setOf("text", "owntext", "html", "attr(href)", "attr(src)") ||
                    normalized.contains("@attr(", true)
                ) normalized else "$normalized@attr(href)"
            }
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
            it.appendChild(TextNode("\n"))
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
