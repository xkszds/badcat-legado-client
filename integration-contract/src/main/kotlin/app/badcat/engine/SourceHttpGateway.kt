package app.badcat.engine

/**
 * Executes the bounded URL-template subset before static URL parsing and
 * request parameter encoding. Script evaluation is opt-in; the default remains
 * fail-closed when a URL contains embedded JavaScript.
 */
class SourceHttpGateway(
    private val client: SourceRequestClient,
    private val embeddedScriptEvaluator: LegadoEmbeddedScriptEvaluator? = null,
) {
    suspend fun execute(
        source: BookSource,
        ruleUrl: String,
        page: Int? = null,
        key: String = "",
    ): EngineResult<SourceHttpResponse> {
        if (!source.enabled) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Book source '${source.id}' is disabled",
            )
        }

        val expandedRuleUrl = if (ruleUrl.contains("{{") && embeddedScriptEvaluator != null) {
            when (val result = LegadoEmbeddedUrlTemplate.resolve(
                template = ruleUrl,
                key = key,
                evaluator = embeddedScriptEvaluator,
            )) {
                is EngineResult.Success -> result.value
                is EngineResult.Failure -> return result
            }
        } else {
            ruleUrl
        }

        val resolved = when (val result = LegadoUrlResolver.resolve(
            baseUrl = source.baseUrl,
            ruleUrl = expandedRuleUrl,
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
