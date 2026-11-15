package app.badcat.engine

/** Persistence boundary for imported Legado book sources. */
fun interface BookSourceCatalog {
    suspend fun all(): EngineResult<List<BookSource>>
}

/** Resolves the source search URL, rejecting script behavior it cannot execute. */
fun interface SearchUrlResolver {
    suspend fun resolve(source: BookSource, keyword: String): EngineResult<String>
}

/** Orchestrates the supported source services behind the existing ReadingEngine contract. */
class SourceReadingEngine(
    private val catalog: BookSourceCatalog,
    private val searchUrlResolver: SearchUrlResolver,
    private val searchService: SourceSearchService,
    private val detailService: SourceBookDetailService,
    private val tocService: SourceTocService,
    private val contentService: SourceChapterContentService,
) : ReadingEngine {

    override suspend fun sources(): EngineResult<List<BookSource>> = catalog.all()

    override suspend fun search(sourceId: String, keyword: String): EngineResult<List<BookCandidate>> {
        if (keyword.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Search keyword must not be blank",
            )
        }
        val source = when (val result = findSource(sourceId)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        val url = when (val result = searchUrlResolver.resolve(source, keyword)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (url.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Search URL resolver returned a blank URL for source '${source.id}'",
            )
        }
        return searchService.search(source, url)
    }

    override suspend fun bookDetail(sourceId: String, bookUrl: String): EngineResult<BookDetail> {
        val source = when (val result = findSource(sourceId)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return detailService.load(source, bookUrl)
    }

    override suspend fun chapters(sourceId: String, bookUrl: String): EngineResult<List<BookChapter>> {
        val source = when (val result = findSource(sourceId)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        val detail = when (val result = detailService.load(source, bookUrl)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return tocService.load(source, bookUrl, detail.tocUrl)
    }

    override suspend fun chapterContent(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
    ): EngineResult<ChapterContent> {
        val source = when (val result = findSource(sourceId)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return contentService.load(
            source,
            bookUrl,
            BookChapter(index = 0, title = "", url = chapterUrl),
        )
    }

    private suspend fun findSource(sourceId: String): EngineResult<BookSource> =
        when (val result = catalog.all()) {
            is EngineResult.Failure -> result
            is EngineResult.Success -> {
                val source = result.value.firstOrNull { it.id == sourceId }
                    ?: return EngineResult.Failure(
                        EngineErrorCode.NOT_FOUND,
                        "Book source '$sourceId' was not found",
                    )
                if (!source.enabled) {
                    EngineResult.Failure(
                        EngineErrorCode.INVALID_SOURCE,
                        "Book source '${source.id}' is disabled",
                    )
                } else {
                    EngineResult.Success(source)
                }
            }
        }
}
