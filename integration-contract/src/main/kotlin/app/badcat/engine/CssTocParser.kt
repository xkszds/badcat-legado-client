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
    /** Executes bounded JavaScript chapter fields for each isolated TOC row. */
    suspend fun parseWithScripts(
        source: BookSource,
        response: SourceHttpResponse,
        bookUrl: String,
        scriptRuntime: SourceScriptRuntime,
        page: Int = 1,
    ): EngineResult<ParsedTocPage> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.TOC)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val listRule = rules["chapterList"]?.takeIf(String::isNotBlank)
            ?: return unsupported(source, "missing chapterList rule")
        var resolvedListRule = listRule.trim()
        resolvedListRule = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                source = source,
                template = resolvedListRule,
                result = response.body,
                baseUrl = response.finalUrl,
                ruleName = "chapterList",
                scriptRuntime = scriptRuntime,
                page = page,
            )) {
                is EngineResult.Failure -> return expanded
                is EngineResult.Success -> expanded.value
            }
        val legacyRegexMode = resolvedListRule.startsWith(":")
        val root = SourceDocument(response.finalUrl, response.body, response.contentType, page = page)
        val items = if (resolvedListRule.startsWith("@js:", true) ||
            resolvedListRule.startsWith("javascript:", true)
        ) {
            val script = resolvedListRule.substringAfter(':').trim()
            if (script.isBlank()) return unsupported(source, "empty JavaScript chapterList")
            val rendered = when (val result = scriptRuntime.evaluate(
                source = source,
                script = script,
                bindings = mapOf(
                    "result" to response.body,
                    "baseUrl" to response.finalUrl,
                    "sourceUrl" to source.id,
                    "sourceId" to source.id,
                    "sourceName" to source.name,
                    "ruleName" to "chapterList",
                    "page" to page.toString(),
                ),
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val trimmed = rendered.trim()
            if (trimmed.startsWith("[")) {
                runCatching {
                    com.google.gson.JsonParser.parseString(trimmed).asJsonArray.map { element ->
                        require(element.isJsonObject || element.isJsonArray)
                        element.toString()
                    }
                }.getOrElse {
                    return unsupported(source, "JavaScript chapterList must return a valid JSON array or HTML fragments")
                }
            } else {
                org.jsoup.Jsoup.parseBodyFragment(rendered).body().children().map { it.outerHtml() }
            }
        } else {
            if (resolvedListRule.contains("{{")) {
                return unsupported(source, "unresolved chapterList interpolation")
            }
            when (val result = LegadoRuleExpressionDispatcher.evaluateElements(root, resolvedListRule)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
        }

        suspend fun extract(itemHtml: String, key: String): EngineResult<String?> {
            val expression = rules[key]?.takeIf(String::isNotBlank)
                ?: return EngineResult.Success(null)
            var trimmed = expression.trim()
            trimmed = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                    source = source,
                    template = trimmed,
                    result = itemHtml,
                    baseUrl = response.finalUrl,
                    ruleName = key,
                    scriptRuntime = scriptRuntime,
                    page = page,
                )) {
                    is EngineResult.Failure -> return expanded
                    is EngineResult.Success -> expanded.value
                }
            if (trimmed.startsWith("@js:", true) || trimmed.startsWith("javascript:", true)) {
                val script = trimmed.substringAfter(':').trim()
                if (script.isBlank()) return unsupported(source, "empty JavaScript rule for '$key'")
                return when (val result = scriptRuntime.evaluate(
                    source,
                    script,
                    mapOf(
                        "result" to itemHtml,
                        "baseUrl" to response.finalUrl,
                        "sourceUrl" to source.id,
                        "sourceId" to source.id,
                        "sourceName" to source.name,
                        "ruleName" to key,
                        "page" to page.toString(),
                    ),
                )) {
                    is EngineResult.Failure -> result
                    is EngineResult.Success -> EngineResult.Success(result.value.trim().takeIf(String::isNotEmpty))
                }
            }
            val normalized = if (key == "chapterUrl") LegadoRuleReplacement.transformExpression(trimmed) {
                it.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
                    .replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
            } else trimmed
            if (legacyRegexMode) {
                val parsed = LegadoRuleReplacement.parse(normalized)
                val raw = when (val result = LegadoRegexElementEvaluator.evaluateScalar(itemHtml, parsed.expression)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> (result.value.getOrNull(1) ?: result.value.firstOrNull())
                        ?.trim()?.takeIf(String::isNotEmpty)
                } ?: return EngineResult.Success(null)
                return when (val replaced = parsed.apply(listOf(raw))) {
                    is EngineResult.Failure -> replaced
                    is EngineResult.Success -> EngineResult.Success(
                        replaced.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty),
                    )
                }
            }
            return when (val result = LegadoRuleExpressionDispatcher.evaluate(
                SourceDocument(response.finalUrl, itemHtml, response.contentType, page = page),
                normalized,
            )) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    LegadoRuleExpressionDispatcher.firstScalarValue(normalized, result.value)
                        ?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        }

        val chapters = ArrayList<BookChapter>()
        for (item in items) {
            val title = when (val result = extract(item, "chapterName")) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            } ?: continue
            val rawUrl = when (val result = extract(item, "chapterUrl")) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            } ?: continue
            val url = runCatching { WebUrlResolver.resolve(response.finalUrl, rawUrl) }.getOrElse {
                return unsupported(source, "chapterUrl must resolve to a credential-free HTTP(S) URL")
            }
            chapters += BookChapter(chapters.size, title, url)
        }

        var nextRule = rules["nextTocUrl"]?.trim().orEmpty()
        nextRule = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
            source = source,
            template = nextRule,
            result = response.body,
            baseUrl = response.finalUrl,
            ruleName = "nextTocUrl",
            scriptRuntime = scriptRuntime,
            page = page,
        )) {
            is EngineResult.Failure -> return expanded
            is EngineResult.Success -> expanded.value
        }
        val nextUrls = if (nextRule.isBlank()) emptyList() else {
            val raw = if (nextRule.startsWith("@js:", true) || nextRule.startsWith("javascript:", true)) {
                val script = nextRule.substringAfter(':').trim()
                when (val result = scriptRuntime.evaluate(
                    source,
                    script,
                    mapOf(
                        "result" to response.body,
                        "baseUrl" to response.finalUrl,
                        "sourceUrl" to source.id,
                        "sourceId" to source.id,
                        "sourceName" to source.name,
                        "ruleName" to "nextTocUrl",
                        "page" to page.toString(),
                    ),
                )) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> result.value.trim()
                }
            } else if (legacyRegexMode) {
                val parsed = LegadoRuleReplacement.parse(nextRule)
                val raw = when (val result = LegadoRegexElementEvaluator.evaluateScalar(response.body, parsed.expression)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> (result.value.getOrNull(1) ?: result.value.firstOrNull()).orEmpty()
                }
                when (val replaced = parsed.apply(listOf(raw))) {
                    is EngineResult.Failure -> return replaced
                    is EngineResult.Success -> replaced.value.firstOrNull().orEmpty()
                }
            } else {
                val normalized = normalizeLinkExpression(nextRule)
                when (val result = LegadoRuleExpressionDispatcher.evaluate(root, normalized)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(normalized, result.value).orEmpty()
                }
            }
            if (raw.isBlank()) emptyList() else listOf(
                runCatching { WebUrlResolver.resolve(response.finalUrl, raw) }.getOrElse {
                    return unsupported(source, "nextTocUrl must resolve to a credential-free HTTP(S) URL")
                },
            ).filter { it != response.finalUrl }
        }
        return EngineResult.Success(ParsedTocPage(chapters, nextUrls.distinct()))
    }

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
                .filterNotNull().any { it.contains("@js:", true) || it.contains("{{") || it.contains("}}") }) {
            return unsupported(source, "JavaScript or interpolation")
        }

        val legacyRegexMode = listRule.trim().startsWith(":")
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
            if (legacyRegexMode) {
                val parsed = LegadoRuleReplacement.parse(expression)
                val raw = when (val result = LegadoRegexElementEvaluator.evaluateScalar(itemHtml, parsed.expression)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> (result.value.getOrNull(1) ?: result.value.firstOrNull())
                        ?.trim()?.takeIf(String::isNotEmpty)
                } ?: return EngineResult.Success(null)
                return when (val replaced = parsed.apply(listOf(raw))) {
                    is EngineResult.Failure -> replaced
                    is EngineResult.Success -> EngineResult.Success(
                        replaced.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty),
                    )
                }
            }
            return when (val result = LegadoRuleExpressionDispatcher.evaluate(
                SourceDocument(response.finalUrl, itemHtml, response.contentType),
                expression,
            )) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    LegadoRuleExpressionDispatcher.firstScalarValue(expression, result.value)
                        ?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        }

        val chapters = ArrayList<BookChapter>()
        for (item in items) {
            val title = when (val result = extract(item, rules["chapterName"])) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            } ?: continue
            val rawUrl = when (val result = extract(item, rules["chapterUrl"]?.let {
                LegadoRuleReplacement.transformExpression(it) { expression ->
                    expression.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
                        .replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
                }
            })) {
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
            val values = if (legacyRegexMode) {
                val parsed = LegadoRuleReplacement.parse(nextRule)
                val raw = when (val result = LegadoRegexElementEvaluator.evaluateScalar(response.body, parsed.expression)) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> result.value.getOrNull(1) ?: result.value.firstOrNull()
                }
                if (raw == null) emptyList() else when (val replaced = parsed.apply(listOf(raw))) {
                    is EngineResult.Failure -> return replaced
                    is EngineResult.Success -> replaced.value
                }
            } else {
                when (val result = LegadoRuleExpressionDispatcher.evaluate(
                    root,
                    normalizeLinkExpression(nextRule),
                )) {
                    is EngineResult.Failure -> {
                        if (result.code == EngineErrorCode.PARSE) {
                            return unsupported(source, "invalid nextTocUrl selector")
                        }
                        return result
                    }
                    is EngineResult.Success -> listOfNotNull(
                        LegadoRuleExpressionDispatcher.firstScalarValue(normalizeLinkExpression(nextRule), result.value),
                    )
                }
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

    private fun unsupported(source: BookSource, reason: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' TOC rule requires unsupported behavior: $reason",
        )
}
