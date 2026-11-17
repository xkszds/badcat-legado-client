package app.badcat.engine

/**
 * Runs the bounded search-result parser. URL script expressions are handled by
 * the gateway's opt-in, string-only Rhino evaluator before the HTTP request.
 */
class SourceSearchService(
    private val http: SourceHttpGateway,
    private val parser: CssSearchResultsParser = CssSearchResultsParser(),
    private val scriptRuntime: SourceScriptRuntime? = null,
) {
    suspend fun search(
        source: BookSource,
        searchRuleUrl: String,
        page: Int? = null,
        keyword: String = "",
    ): EngineResult<List<BookCandidate>> {
        MainJsSourceGuard.failure(source, "search", scriptRuntimeAvailable = scriptRuntime != null)?.let { return it }
        if (!source.enabled) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Search is disabled for source '${source.id}'",
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
        return scriptRuntime?.let {
            parser.parseWithScripts(source, response, it, key = keyword, page = page ?: 1)
        } ?: parser.parse(source, response)
    }
}
