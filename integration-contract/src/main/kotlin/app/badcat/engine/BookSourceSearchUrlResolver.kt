package app.badcat.engine

/**
 * Resolves the search URL stored on an imported Legado source.
 *
 * The URL expression itself is deliberately left intact: the HTTP gateway
 * performs the bounded {{...}} expansion with the actual keyword before URI
 * parsing and parameter encoding. @js and other full AnalyzeUrl behaviors are
 * not interpreted here.
 */
class BookSourceSearchUrlResolver : SearchUrlResolver {
    override suspend fun resolve(
        source: BookSource,
        keyword: String,
    ): EngineResult<String> {
        if (!source.enabled) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book source '${source.id}' is disabled",
            )
        }
        if (keyword.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Search keyword must not be blank",
            )
        }
        val searchUrl = source.searchUrl?.trim().orEmpty()
        if (searchUrl.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book source '${source.id}' has no searchUrl",
            )
        }
        if (searchUrl.contains("@js", ignoreCase = true)) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Search URL for source '${source.id}' uses unsupported @js syntax",
            )
        }
        return EngineResult.Success(searchUrl)
    }
}
