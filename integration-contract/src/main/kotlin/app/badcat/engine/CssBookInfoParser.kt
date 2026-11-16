package app.badcat.engine

/**
 * Bounded adapter for official Legado BookInfoRule field names.
 * Script/template expansion and source-specific initialization remain unsupported.
 */
class CssBookInfoParser {

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
                "coverUrl" -> expression.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
                "tocUrl" -> expression.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
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
