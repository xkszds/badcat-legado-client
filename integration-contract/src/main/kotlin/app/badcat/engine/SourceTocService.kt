package app.badcat.engine

/** Loads a TOC and follows CSS next-page links with loop and page-count guards. */
class SourceTocService(
    private val http: SourceHttpGateway,
    private val parser: CssTocParser = CssTocParser(),
    private val maxPages: Int = 20,
    private val scriptRuntime: SourceScriptRuntime? = null,
) {
    suspend fun load(source: BookSource, bookUrl: String, tocUrl: String? = null): EngineResult<List<BookChapter>> {
        MainJsSourceGuard.failure(source, "table of contents")?.let { return it }
        if (maxPages < 1) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "maxPages must be positive")
        }
        val pending = ArrayDeque<String>()
        pending.add(tocUrl?.takeIf(String::isNotBlank) ?: bookUrl)
        val visited = HashSet<String>()
        val chapters = ArrayList<BookChapter>()
        var pageCount = 0

        while (pending.isNotEmpty() && pageCount < maxPages) {
            val url = pending.removeFirst()
            val requestedIdentity = paginationUrlIdentity(url)
            if (!visited.add(requestedIdentity)) continue
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
            pageCount++
            // Track the post-redirect URL as well as the requested URL. A source
            // may point its next-page link at the redirect destination, which
            // would otherwise trigger one redundant fetch before cycle detection.
            val finalIdentity = paginationUrlIdentity(response.finalUrl)
            if (finalIdentity.isNotBlank() && finalIdentity != requestedIdentity &&
                !visited.add(finalIdentity)
            ) {
                return EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "TOC pagination contains a redirect URL cycle",
                )
            }
            val page = when (val result = scriptRuntime?.let { parser.parseWithScripts(source, response, bookUrl, it, page = pageCount + 1) }
                ?: parser.parse(source, response, bookUrl)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            chapters.addAll(page.chapters)
            for (next in page.nextPageUrls) {
                val nextIdentity = paginationUrlIdentity(next)
                if (nextIdentity in visited) {
                    return EngineResult.Failure(
                        EngineErrorCode.PARSE,
                        "TOC pagination contains a URL cycle",
                    )
                }
                if (pending.none { paginationUrlIdentity(it) == nextIdentity }) pending.addLast(next)
            }
        }

        if (pending.isNotEmpty() && pageCount >= maxPages) {
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
