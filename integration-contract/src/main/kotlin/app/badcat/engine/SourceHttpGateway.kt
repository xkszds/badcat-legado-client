package app.badcat.engine

/**
 * Executes the supported static subset of a book-source URL rule end to end:
 * resolve URL/options -> merge source headers -> perform HTTP request.
 *
 * This is a transport gateway, not a complete Legado rule engine. It does not
 * evaluate URL JavaScript, parse result documents, or run stage selectors.
 */
class SourceHttpGateway(
    private val client: SourceRequestClient,
) {
    suspend fun execute(
        source: BookSource,
        ruleUrl: String,
        page: Int? = null,
    ): EngineResult<SourceHttpResponse> {
        if (!source.enabled) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book source '${source.id}' is disabled",
            )
        }

        val resolved = when (val result = LegadoUrlResolver.resolve(
            baseUrl = source.baseUrl,
            ruleUrl = ruleUrl,
            page = page,
        )) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> return result
        }

        val request = when (val result = SourceRequestFactory.create(source, resolved)) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> return result
        }
        return client.execute(request)
    }
}
