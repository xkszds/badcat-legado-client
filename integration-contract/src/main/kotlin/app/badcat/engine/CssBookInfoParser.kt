package app.badcat.engine

/**
 * Bounded adapter for official Legado BookInfoRule field names.
 * Script/template expansion and source-specific initialization remain unsupported.
 */
class CssBookInfoParser {

    /** Executes bounded JavaScript scalar fields for production detail loading. */
    suspend fun parseWithScripts(
        source: BookSource,
        response: SourceHttpResponse,
        requestedBookUrl: String,
        scriptRuntime: SourceScriptRuntime,
    ): EngineResult<BookDetail> {
        if (requestedBookUrl.isBlank()) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Requested book URL must not be blank")
        }
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.BOOK_INFO)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val safeBookUrl = runCatching { WebUrlResolver.resolve(response.finalUrl, requestedBookUrl) }
            .getOrElse {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Requested book URL must be a credential-free HTTP(S) URL",
                    it,
                )
            }
        val document = SourceDocument(response.finalUrl, response.body, response.contentType)

        suspend fun field(name: String): EngineResult<String?> {
            val expression = rules[name]?.takeIf(String::isNotBlank) ?: return EngineResult.Success(null)
            var trimmed = expression.trim()
            if (trimmed.contains("{{")) {
                trimmed = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                    source = source,
                    template = trimmed,
                    result = response.body,
                    baseUrl = response.finalUrl,
                    ruleName = name,
                    scriptRuntime = scriptRuntime,
                )) {
                    is EngineResult.Failure -> return expanded
                    is EngineResult.Success -> expanded.value
                }
            }
            if (trimmed.startsWith("@js:", true) || trimmed.startsWith("javascript:", true)) {
                val script = trimmed.substringAfter(':').trim()
                if (script.isBlank()) return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE, "Empty JavaScript rule for book-info field '$name'",
                )
                return when (val result = scriptRuntime.evaluate(
                    source,
                    script,
                    mapOf(
                        "result" to response.body,
                        "baseUrl" to response.finalUrl,
                        "sourceUrl" to source.id,
                        "sourceId" to source.id,
                        "sourceName" to source.name,
                        "ruleName" to name,
                    ),
                )) {
                    is EngineResult.Failure -> result
                    is EngineResult.Success -> EngineResult.Success(result.value.trim().takeIf(String::isNotEmpty))
                }
            }
            val normalized = when (name) {
                "coverUrl" -> LegadoRuleReplacement.transformExpression(trimmed) {
                    it.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
                }
                "tocUrl" -> LegadoRuleReplacement.transformExpression(trimmed) {
                    it.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
                }
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

        val values = linkedMapOf<String, String?>()
        for (name in listOf("name", "author", "intro", "coverUrl", "lastChapter", "tocUrl")) {
            when (val result = field(name)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> values[name] = result.value
            }
        }
        val name = values["name"] ?: return EngineResult.Failure(
            EngineErrorCode.NOT_FOUND,
            "Book-info rule did not produce a book name for source '${source.id}'",
        )
        fun resolveOptionalUrl(key: String): EngineResult<String?> {
            val raw = values[key] ?: return EngineResult.Success(null)
            return runCatching { WebUrlResolver.resolve(response.finalUrl, raw) }.fold(
                onSuccess = { EngineResult.Success(it) },
                onFailure = {
                    EngineResult.Failure(
                        EngineErrorCode.UNSUPPORTED_RULE,
                        "Book-info $key must resolve to a credential-free HTTP(S) URL",
                        it,
                    )
                },
            )
        }
        val cover = when (val result = resolveOptionalUrl("coverUrl")) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        val toc = when (val result = resolveOptionalUrl("tocUrl")) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return EngineResult.Success(
            BookDetail(
                sourceId = source.id,
                name = name,
                author = values["author"],
                bookUrl = safeBookUrl,
                coverUrl = cover,
                intro = values["intro"],
                latestChapter = values["lastChapter"],
                tocUrl = toc,
            ),
        )
    }

    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        requestedBookUrl: String,
    ): EngineResult<BookDetail> {
        if (requestedBookUrl.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Requested book URL must not be blank",
            )
        }
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.BOOK_INFO)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val safeBookUrl = runCatching {
            WebUrlResolver.resolve(response.finalUrl, requestedBookUrl)
        }.getOrElse {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Requested book URL must be a credential-free HTTP(S) URL",
                it,
            )
        }
        val document = SourceDocument(response.finalUrl, response.body, response.contentType)

        fun field(name: String): EngineResult<String?> {
            val expression = rules[name]?.takeIf(String::isNotBlank) ?: return EngineResult.Success(null)
            if (expression.contains("@js:", true) || expression.contains("{{") ||
                expression.contains("}}") || expression.contains("替换")
            ) {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Book-info field '$name' requires unsupported script/template/replace behavior",
                )
            }
            val extractionRule = when (name) {
                "coverUrl" -> LegadoRuleReplacement.transformExpression(expression) {
                    it.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
                }
                "tocUrl" -> LegadoRuleReplacement.transformExpression(expression) {
                    it.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
                }
                else -> expression
            }
            return when (val result = LegadoRuleExpressionDispatcher.evaluate(document, extractionRule)) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    LegadoRuleExpressionDispatcher.firstScalarValue(extractionRule, result.value)
                        ?.trim()?.takeIf(String::isNotEmpty),
                )
            }
        }

        val name = when (val value = field("name")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        } ?: return EngineResult.Failure(
            EngineErrorCode.NOT_FOUND,
            "Book-info rule did not produce a book name for source '${source.id}'",
        )

        fun resolveUrl(value: String?): EngineResult<String?> = if (value == null) {
            EngineResult.Success(null)
        } else {
            runCatching { WebUrlResolver.resolve(response.finalUrl, value) }.fold(
                onSuccess = { EngineResult.Success(it) },
                onFailure = { EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Book-info URL must resolve to a credential-free HTTP(S) URL", it) },
            )
        }

        val author = when (val value = field("author")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }
        val intro = when (val value = field("intro")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }
        val cover = when (val value = field("coverUrl")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }
        val latest = when (val value = field("lastChapter")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }
        val toc = when (val value = field("tocUrl")) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }

        val resolvedCover = when (val value = resolveUrl(cover)) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }
        val resolvedToc = when (val value = resolveUrl(toc)) {
            is EngineResult.Failure -> return value
            is EngineResult.Success -> value.value
        }

        return EngineResult.Success(
            BookDetail(
                sourceId = source.id,
                name = name,
                author = author,
                bookUrl = safeBookUrl,
                coverUrl = resolvedCover,
                intro = intro,
                latestChapter = latest,
                tocUrl = resolvedToc,
            ),
        )
    }
}
