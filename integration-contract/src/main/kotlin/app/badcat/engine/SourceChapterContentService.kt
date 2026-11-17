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
    private val cache: ChapterContentCache? = null,
    private val cacheTtlMillis: Long = 12L * 60 * 60 * 1000,
    private val scriptRuntime: SourceScriptRuntime? = null,
) {
    suspend fun load(
        source: BookSource,
        bookUrl: String,
        chapter: BookChapter,
        bookName: String = "",
    ): EngineResult<ChapterContent> {
        MainJsSourceGuard.failure(source, "chapter content", scriptRuntimeAvailable = scriptRuntime != null)?.let { return it }
        if (bookUrl.isBlank() || chapter.url.isBlank() || maxPages < 1) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book URL, chapter URL and positive maxPages are required",
            )
        }

        if (cacheTtlMillis < 0) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "cacheTtlMillis must not be negative")
        }
        val cached = try {
            cache?.get(source.id, bookUrl, chapter.url)
        } catch (cancelled: java.util.concurrent.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (cached != null && System.currentTimeMillis() - cached.savedAtEpochMillis in 0..cacheTtlMillis) {
            return EngineResult.Success(cached.content)
        }

        var currentUrl = chapter.url
        val visited = HashSet<String>()
        val pageTexts = ArrayList<String>()
        var title = chapter.title
        var pageCount = 0

        while (currentUrl.isNotBlank() && pageCount < maxPages) {
            val requestedIdentity = paginationUrlIdentity(currentUrl)
            if (!visited.add(requestedIdentity)) {
                return EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "Chapter content pagination contains a URL cycle for source '${source.id}'",
                )
            }
            val response = when (val result = http.execute(source, currentUrl)) {
                is EngineResult.Failure -> return cached?.let { EngineResult.Success(it.content) } ?: result
                is EngineResult.Success -> result.value
            }
            if (response.statusCode !in 200..299) {
                return cached?.let { EngineResult.Success(it.content) } ?: EngineResult.Failure(
                    EngineErrorCode.NETWORK,
                    "Chapter content request for source '${source.id}' returned HTTP ${response.statusCode}",
                )
            }
            // Redirect targets are canonical pagination identities too. Record them
            // before parsing so a next link to the final URL is detected immediately.
            val finalIdentity = paginationUrlIdentity(response.finalUrl)
            if (finalIdentity.isNotBlank() && finalIdentity != requestedIdentity &&
                !visited.add(finalIdentity)
            ) {
                return EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "Chapter content pagination contains a redirect URL cycle for source '${source.id}'",
                )
            }
            val parsed = when (val result = scriptRuntime?.let { parser.parseWithScripts(source, response, chapter, it, page = pageCount + 1) }
                ?: parser.parse(source, response, chapter)) {
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
        if (currentUrl.isNotBlank() && pageCount >= maxPages) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Chapter content pagination exceeded the $maxPages page safety limit",
            )
        }

        val combinedText = pageTexts.joinToString("\n\n")
        val finalContent = try {
            val replacementContext = LegadoReplacementContext(
                book = LegadoReplacementBookContext(
                    bookUrl = bookUrl,
                    origin = source.baseUrl.ifBlank { source.id },
                    originName = source.name,
                    name = bookName,
                ),
                chapter = LegadoReplacementChapterContext(
                    url = chapter.url,
                    title = title,
                    bookUrl = bookUrl,
                    index = chapter.index,
                    baseUrl = source.baseUrl,
                ),
            )
            val replaced = scopedTextReplacement?.replace(
                chapterTitle = title,
                chapterText = combinedText,
                context = replacementContext,
            )
            ChapterContent(
                title = replaced?.title ?: title,
                text = replaced?.text ?: textReplacement.replace(combinedText),
            )
        } catch (error: Exception) {
            // Preserve structured coroutine cancellation instead of converting it
            // into an ordinary chapter parsing failure.
            if (error is java.util.concurrent.CancellationException) throw error
            return EngineResult.Failure(
                when (error) {
                    is LegadoRegexTimeoutException -> EngineErrorCode.RULE_TIMEOUT
                    is UnsupportedOperationException -> EngineErrorCode.UNSUPPORTED_RULE
                    else -> EngineErrorCode.PARSE
                },
                "Chapter text replacement failed for source '${source.id}'",
                error,
            )
        }
        try {
            cache?.put(
                source.id,
                bookUrl,
                chapter.url,
                CachedChapterContent(finalContent, System.currentTimeMillis()),
            )
        } catch (cancelled: java.util.concurrent.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A cache write failure must not discard successfully loaded chapter text.
        }
        return EngineResult.Success(finalContent)
    }
}
