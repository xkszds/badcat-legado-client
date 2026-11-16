package app.badcat.engine

/**
 * Resolves the search URL stored on an imported Legado source.
 *
 * The URL expression itself is deliberately left intact: the HTTP gateway
 * evaluates the bounded @js/<js> and {{...}} forms with the actual keyword
 * before URI parsing and parameter encoding.
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
        return EngineResult.Success(searchUrl)
    }
}
