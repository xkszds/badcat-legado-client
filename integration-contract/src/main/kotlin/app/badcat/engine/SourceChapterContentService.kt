package app.badcat.engine

/** Fetches one chapter and parses its supported CSS ContentRule fields. */
class SourceChapterContentService(
    private val http: SourceHttpGateway,
    private val parser: CssChapterContentParser = CssChapterContentParser(),
) {
    suspend fun load(
        source: BookSource,
        bookUrl: String,
        chapter: BookChapter,
    ): EngineResult<ChapterContent> {
        val response = when (val result = http.execute(source, chapter.url)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (response.statusCode !in 200..299) {
            return EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "Chapter request for source '${source.id}' returned HTTP ${response.statusCode}",
            )
        }
        return parser.parse(source, response, chapter)
    }
}
