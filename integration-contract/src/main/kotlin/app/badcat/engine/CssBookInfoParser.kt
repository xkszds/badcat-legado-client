package app.badcat.engine

import java.net.URI

/**
 * CSS-only adapter for the official Legado BookInfoRule field names.
 * Init scripts, URL templates, JS and regex-based rule expansion are deliberately
 * rejected rather than interpreted as selectors.
 */
class CssBookInfoParser {

    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        requestedBookUrl: String,
    ): EngineResult<BookDetail> {
        val rules = when (val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.BOOK_INFO)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val document = SourceDocument(response.finalUrl, response.body, response.contentType)

        fun field(name: String): EngineResult<String?> {
            val expression = rules[name]?.takeIf(String::isNotBlank) ?: return EngineResult.Success(null)
            if (expression.contains("@js", true) || expression.contains("{{") ||
                expression.contains("}}") || expression.contains("替换")
            ) {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Book-info field '$name' requires unsupported script/template/replace behavior",
                )
            }
            return when (val result = SelectorRuleEvaluator.evaluate(document, SelectorRuleType.CSS, expression)) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> EngineResult.Success(
                    result.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty),
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

        fun resolveUrl(value: String?): String? = value?.let {
            runCatching { URI(response.finalUrl).resolve(it).toString() }.getOrDefault(it)
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

        return EngineResult.Success(
            BookDetail(
                sourceId = source.id,
                name = name,
                author = author,
                bookUrl = requestedBookUrl,
                coverUrl = resolveUrl(cover),
                intro = intro,
                latestChapter = latest,
                tocUrl = resolveUrl(toc),
            ),
        )
    }
}
