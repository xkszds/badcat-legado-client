package app.badcat.engine

/** Loads a TOC and follows CSS next-page links with loop and page-count guards. */
class SourceTocService(
    private val http: SourceHttpGateway,
    private val parser: CssTocParser = CssTocParser(),
    private val maxPages: Int = 20,
) {
    suspend fun load(source: BookSource, bookUrl: String, tocUrl: String? = null): EngineResult<List<BookChapter>> {
        if (maxPages < 1) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "maxPages must be positive")
        }
        val pending = ArrayDeque<String>()
        pending.add(tocUrl?.takeIf(String::isNotBlank) ?: bookUrl)
        val visited = HashSet<String>()
        val chapters = ArrayList<BookChapter>()

        while (pending.isNotEmpty() && visited.size < maxPages) {
            val url = pending.removeFirst()
            if (!visited.add(url)) continue
            val response = when (val result = http.execute(source, url)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            if (response.statusCode !in 200..299) {
                return EngineResult.Failure(
                    EngineErrorCode.NETWORK,
                    "TOC request for source '${source.id}' returned HTTP ${response.statusCode}",
                )
            }
            val page = when (val result = parser.parse(source, response, bookUrl)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            chapters.addAll(page.chapters)
            page.nextPageUrls.forEach { next ->
                if (next !in visited && next !in pending) pending.addLast(next)
            }
        }

        if (pending.isNotEmpty()) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "TOC pagination exceeded the $maxPages page safety limit",
            )
        }

        if (chapters.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.NOT_FOUND, "No chapters found for source '${source.id}'")
        }
        return EngineResult.Success(
            chapters.distinctBy { it.url }.mapIndexed { index, chapter -> chapter.copy(index = index) },
        )
    }
}
