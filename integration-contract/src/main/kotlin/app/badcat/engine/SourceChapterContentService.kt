package app.badcat.engine

/**
 * Fetches all pages of one chapter's content, then applies the injected
 * book-aware replacement pipeline. nextContentUrl is same-chapter pagination.
 */
class SourceChapterContentService(
    private val http: SourceHttpGateway,
    private val parser: CssChapterContentParser = CssChapterContentParser(),
    private val textReplacement: BookTextReplacement = BookTextReplacement { it },
    private val scopedTextReplacement: ScopedBookTextReplacement? = null,
    private val maxPages: Int = 20,
) {
    suspend fun load(
        source: BookSource,
        bookUrl: String,
        chapter: BookChapter,
        bookName: String = "",
    ): EngineResult<ChapterContent> {
        if (bookUrl.isBlank() || chapter.url.isBlank() || maxPages < 1) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book URL, chapter URL and positive maxPages are required",
            )
        }

        var currentUrl = chapter.url
        val visited = HashSet<String>()
        val pageTexts = ArrayList<String>()
        var title = chapter.title
        var pageCount = 0

        while (currentUrl.isNotBlank() && pageCount < maxPages && visited.add(currentUrl)) {
            val response = when (val result = http.execute(source, currentUrl)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            if (response.statusCode !in 200..299) {
                return EngineResult.Failure(
                    EngineErrorCode.NETWORK,
                    "Chapter content request for source '${source.id}' returned HTTP ${response.statusCode}",
                )
            }
            val parsed = when (val result = parser.parse(source, response, chapter)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            if (pageCount == 0) title = parsed.title
            pageTexts += parsed.text
            pageCount++
            currentUrl = parsed.nextPageUrl.orEmpty()
        }

        if (pageTexts.isEmpty()) {
            return EngineResult.Failure(
                EngineErrorCode.NOT_FOUND,
                "No chapter content found for source '${source.id}'",
            )
        }
        if (currentUrl.isNotBlank() && currentUrl !in visited && pageCount >= maxPages) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Chapter content pagination exceeded the $maxPages page safety limit",
            )
        }

        val combinedText = pageTexts.joinToString("\n\n")
        return runCatching<EngineResult<ChapterContent>> {
            val replaced = scopedTextReplacement?.replace(
                chapterTitle = title,
                chapterText = combinedText,
                bookName = bookName,
                bookOrigin = source.baseUrl.ifBlank { source.id },
            )
            EngineResult.Success(
                ChapterContent(
                    title = replaced?.title ?: title,
                    text = replaced?.text ?: textReplacement.replace(combinedText),
                ),
            )
        }.getOrElse {
            EngineResult.Failure(
                when (it) {
                    is LegadoRegexTimeoutException -> EngineErrorCode.RULE_TIMEOUT
                    is UnsupportedOperationException -> EngineErrorCode.UNSUPPORTED_RULE
                    else -> EngineErrorCode.PARSE
                },
                "Chapter text replacement failed for source '${source.id}'",
                it,
            )
        }
    }
}
