package app.badcat.engine

/**
 * Executes the bounded URL-template subset before static URL parsing and
 * request parameter encoding. Script evaluation is opt-in; the default remains
 * fail-closed when a URL contains embedded JavaScript.
 */
class SourceHttpGateway(
    private val client: SourceRequestClient,
    private val embeddedScriptEvaluator: LegadoEmbeddedScriptEvaluator? = null,
    /** Resolve an evaluator for each request source; takes precedence over the fixed evaluator. */
    private val embeddedScriptEvaluatorProvider: ((BookSource) -> LegadoEmbeddedScriptEvaluator?)? = null,
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

        val evaluator = if (embeddedScriptEvaluatorProvider != null) embeddedScriptEvaluatorProvider.invoke(source) else embeddedScriptEvaluator
        val expandedRuleUrl = if (ruleUrl.contains("{{") && evaluator != null) {
            when (val result = LegadoEmbeddedUrlTemplate.resolve(
                template = ruleUrl,
                key = key,
                evaluator = evaluator,
                page = page ?: 1,
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
