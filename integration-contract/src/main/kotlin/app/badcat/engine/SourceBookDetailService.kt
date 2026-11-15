package app.badcat.engine

/** Fetches and parses a book detail page using the supported CSS rule subset. */
class SourceBookDetailService(
    private val http: SourceHttpGateway,
    private val parser: CssBookInfoParser = CssBookInfoParser(),
) {
    suspend fun load(
        source: BookSource,
        bookUrl: String,
    ): EngineResult<BookDetail> {
        val response = when (val result = http.execute(source, bookUrl)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (response.statusCode !in 200..299) {
            return EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "Book detail request for source '${source.id}' returned HTTP ${response.statusCode}",
            )
        }
        return parser.parse(source, response, bookUrl)
    }
}
