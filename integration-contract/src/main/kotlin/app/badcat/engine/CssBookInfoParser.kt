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
        fallbackBookName: String? = null,
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
        var currentResult = response.body
        var document = SourceDocument(response.finalUrl, currentResult, response.contentType)
        val initRule = rules["init"]?.trim().orEmpty()
        if (initRule.isNotBlank()) {
            val withoutPut = when (val put = LegadoPutRule.resolve(source, initRule, document, scriptRuntime)) {
                is EngineResult.Failure -> return put
                is EngineResult.Success -> put.value
            }
            if (withoutPut.isNotBlank()) {
                val initialized = if (withoutPut.startsWith("@js:", true) || withoutPut.startsWith("javascript:", true)) {
                    when (val result = scriptRuntime.evaluate(
                        source,
                        withoutPut.substringAfter(':').trim(),
                        mapOf(
                            "result" to currentResult,
                            "baseUrl" to response.finalUrl,
                            "sourceUrl" to source.id,
                            "sourceId" to source.id,
                            "sourceName" to source.name,
                            "ruleName" to "init",
                        ),
                    )) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> result.value
                    }
                } else {
                    val path = normalizeJsonRule(withoutPut)
                    when (val result = JsonPathRuleEvaluator.evaluateElements(document, path)) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> result.value.firstOrNull()
                            ?: return EngineResult.Failure(EngineErrorCode.NOT_FOUND, "Book-info init rule returned no data")
                    }
                }
                currentResult = initialized
                document = SourceDocument(response.finalUrl, currentResult, "application/json")
            }
        }

        suspend fun field(name: String): EngineResult<String?> {
            val expression = rules[name]?.takeIf(String::isNotBlank) ?: return EngineResult.Success(null)
            val directGet = LegadoPutRule.isDirectGet(expression)
            var trimmed = when (val put = LegadoPutRule.resolve(source, expression.trim(), document, scriptRuntime)) {
                is EngineResult.Failure -> return put
                is EngineResult.Success -> put.value
            }
            trimmed = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                    source = source,
                    template = trimmed,
                    result = currentResult,
                    baseUrl = response.finalUrl,
                    ruleName = name,
                    scriptRuntime = scriptRuntime,
                )) {
                    is EngineResult.Failure -> return expanded
                    is EngineResult.Success -> expanded.value
                }
            if (directGet) {
                val value = trimmed.trim().takeIf(String::isNotEmpty) ?: return EngineResult.Success(null)
                if (name == "coverUrl" || name == "tocUrl") {
                    return runCatching { EngineResult.Success(WebUrlResolver.resolve(response.finalUrl, value)) }
                        .getOrElse { EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Book-info $name value must resolve to a credential-free HTTP(S) URL", it) }
                }
                return EngineResult.Success(value)
            }
            if ((name == "coverUrl" || name == "tocUrl") && isLiteralUrlRule(trimmed)) {
                return runCatching { EngineResult.Success(WebUrlResolver.resolve(response.finalUrl, trimmed)) }
                    .getOrElse { EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Book-info $name literal must resolve to a credential-free HTTP(S) URL", it) }
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
                        "result" to currentResult,
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
            trimmed = normalizeJsonRuleForDocument(document, trimmed)
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
        for (name in listOf("name", "author", "intro", "coverUrl", "lastChapter", "tocUrl", "bookStatus")) {
            when (val result = field(name)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> values[name] = result.value
            }
        }
        val name = values["name"] ?: fallbackBookName?.trim()?.takeIf(String::isNotEmpty) ?: return EngineResult.Failure(
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
                status = values["bookStatus"],
            ),
        )
    }


    private fun isLiteralUrlRule(value: String): Boolean =
        value.startsWith("http://", true) || value.startsWith("https://", true) ||
            value.startsWith("/") || value.startsWith("./") || value.startsWith("../")

    private fun normalizeJsonRule(rule: String): String {
        val trimmed = rule.trim()
        return if (trimmed.startsWith("$") || trimmed.startsWith("@json", true)) trimmed else "$.$trimmed"
    }

    private fun normalizeJsonRuleForDocument(document: SourceDocument, rule: String): String {
        val trimmed = rule.trim()
        val isJson = document.body.trimStart().let { it.startsWith("{") || it.startsWith("[") }
        val isBarePath = trimmed.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*"))
        return if (isJson && isBarePath) "$.$trimmed" else trimmed
    }

    private fun splitPutAssignments(expression: String): Pair<String, List<Pair<String, String>>> {
        val match = Regex("@put\\s*:\\s*\\{([^}]*)\\}", RegexOption.IGNORE_CASE).find(expression)
            ?: return expression to emptyList()
        val assignments = match.groupValues[1].split(',').mapNotNull { entry ->
            val separator = entry.indexOf(':')
            if (separator <= 0) null else {
                val key = entry.substring(0, separator).trim()
                val value = entry.substring(separator + 1).trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
        }
        return expression.removeRange(match.range).trim() to assignments
    }

    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        requestedBookUrl: String,
        fallbackBookName: String? = null,
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
        } ?: fallbackBookName?.trim()?.takeIf(String::isNotEmpty) ?: return EngineResult.Failure(
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
        val bookStatus = when (val value = field("bookStatus")) {
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
                status = bookStatus,
            ),
        )
    }
}
