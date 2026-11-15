package app.badcat.engine

/**
 * Runs the CSS-only search-result path. Embedded URL expressions are delegated
 * to the opt-in gateway evaluator, which expands them before URL encoding.
 * The @js URL form remains unsupported.
 */
class SourceSearchService(
    private val http: SourceHttpGateway,
    private val parser: CssSearchResultsParser = CssSearchResultsParser(),
) {
    suspend fun search(
        source: BookSource,
        searchRuleUrl: String,
        page: Int? = null,
        keyword: String = "",
    ): EngineResult<List<BookCandidate>> {
        if (searchRuleUrl.contains("@js", ignoreCase = true)) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Search URL for source '${source.id}' uses the unsupported @js form",
            )
        }
        val response = when (val result = http.execute(source, searchRuleUrl, page, keyword)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (response.statusCode !in 200..299) {
            return EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "Search request for source '${source.id}' returned HTTP ${response.statusCode}",
            )
        }
        return parser.parse(source, response)
    }
}
