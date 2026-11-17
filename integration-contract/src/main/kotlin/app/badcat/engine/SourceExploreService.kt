package app.badcat.engine

/**
 * Runs a Legado exploreUrl through the same bounded transport and result parser
 * as search, while keeping the EXPLORE rule object and enable flag distinct.
 */
class SourceExploreService(
    private val http: SourceHttpGateway,
    private val parser: CssSearchResultsParser = CssSearchResultsParser(),
    private val scriptRuntime: SourceScriptRuntime? = null,
) {
    suspend fun explore(
        source: BookSource,
        exploreRuleUrl: String? = source.exploreUrl,
        page: Int? = null,
        keyword: String = "",
    ): EngineResult<List<BookCandidate>> {
        MainJsSourceGuard.failure(source, "explore", scriptRuntimeAvailable = scriptRuntime != null)?.let { return it }
        if (!source.enabled || !source.enabledExplore) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Explore is disabled for source '${source.id}'",
            )
        }
        val url = exploreRuleUrl?.trim().orEmpty()
        if (url.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book source '${source.id}' has no exploreUrl",
            )
        }
        val response = when (val result = http.execute(source, url, page, keyword)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        if (response.statusCode !in 200..299) {
            return EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "Explore request for source '${source.id}' returned HTTP ${response.statusCode}",
            )
        }
        return scriptRuntime?.let {
            parser.parseWithScripts(
                source,
                response,
                it,
                SourceRuleStage.EXPLORE,
                key = keyword,
                page = page ?: 1,
            )
        } ?: parser.parse(source, response, SourceRuleStage.EXPLORE)
    }
}
