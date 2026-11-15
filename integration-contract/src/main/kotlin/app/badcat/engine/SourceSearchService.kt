package app.badcat.engine

/**
 * Runs the currently supported CSS-only search path after the caller supplies
 * a URL rule with any keyword/script substitutions already resolved.
 *
 * Keeping URL scripting outside this service prevents silently sending an
 * unresolved keyword template to a website.
 */
class SourceSearchService(
    private val http: SourceHttpGateway,
    private val parser: CssSearchResultsParser = CssSearchResultsParser(),
) {
    suspend fun search(
        source: BookSource,
        resolvedSearchRuleUrl: String,
        page: Int? = null,
    ): EngineResult<List<BookCandidate>> {
        if (resolvedSearchRuleUrl.contains("{{") ||
            resolvedSearchRuleUrl.contains("@js", ignoreCase = true)
        ) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Search URL for source '${source.id}' still contains JavaScript/interpolation",
            )
        }
        val response = when (val result = http.execute(source, resolvedSearchRuleUrl, page)) {
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
