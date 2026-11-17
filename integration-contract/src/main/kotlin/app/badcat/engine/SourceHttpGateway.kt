package app.badcat.engine

/**
 * Executes bounded AnalyzeUrl script stages around the shared HTTP transport.
 * URL option js transforms the resolved URL before dispatch; bodyJs transforms
 * the response body after dispatch. WebView-only options remain fail-closed.
 */
class SourceHttpGateway(
    private val client: SourceRequestClient,
    private val embeddedScriptEvaluator: LegadoEmbeddedScriptEvaluator? = null,
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
        var expandedRuleUrl = ruleUrl
        if (LegadoUrlScriptRule.containsRule(expandedRuleUrl)) {
            val scriptEvaluator = evaluator ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "This URL uses Legado @js syntax but no bounded script evaluator is configured",
            )
            expandedRuleUrl = when (val result = LegadoUrlScriptRule.resolve(
                ruleUrl = expandedRuleUrl,
                key = key,
                page = page ?: 1,
                evaluator = scriptEvaluator,
            )) {
                is EngineResult.Success -> result.value
                is EngineResult.Failure -> return result
            }
        }
        if (expandedRuleUrl.contains("{{") || LegadoEmbeddedRuleTemplate.containsVariableRule(expandedRuleUrl)) {
            val templateEvaluator = evaluator ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "This URL uses embedded Legado rules but no bounded script evaluator is configured",
            )
            expandedRuleUrl = when (val result = LegadoEmbeddedRuleTemplate.resolve(
                template = expandedRuleUrl,
                key = key,
                page = page ?: 1,
                result = expandedRuleUrl,
                evaluator = templateEvaluator,
            )) {
                is EngineResult.Success -> result.value
                is EngineResult.Failure -> return result
            }
        }
        var resolved = when (val result = LegadoUrlResolver.resolve(
            baseUrl = source.baseUrl,
            ruleUrl = expandedRuleUrl,
            page = page,
        )) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> return result
        }
        resolved.js?.let { script ->
            val scriptEvaluator = evaluator ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "URL option js requires a bounded source script evaluator",
            )
            resolved = when (val evaluated = scriptEvaluator.evaluateWithResult(
                script = script,
                key = key,
                page = page ?: 1,
                result = resolved.url,
            )) {
                is EngineResult.Failure -> return evaluated
                is EngineResult.Success -> {
                    val url = evaluated.value?.toString()?.trim().orEmpty()
                    if (url.isBlank()) return EngineResult.Failure(
                        EngineErrorCode.INVALID_SOURCE,
                        "URL option js returned a blank URL",
                    )
                    resolved.copy(url = url)
                }
            }
        }
        val request = when (val result = SourceRequestFactory.create(source, resolved)) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> return result
        }
        val response = when (val result = client.execute(request)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        resolved.bodyJs?.let { script ->
            val scriptEvaluator = evaluator ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "URL option bodyJs requires a bounded source script evaluator",
            )
            val transformed = when (val evaluated = scriptEvaluator.evaluateWithResult(
                script = script,
                key = key,
                page = page ?: 1,
                result = response.body,
            )) {
                is EngineResult.Failure -> return evaluated
                is EngineResult.Success -> evaluated.value?.toString().orEmpty()
            }
            return EngineResult.Success(response.copy(body = transformed))
        }
        return EngineResult.Success(response)
    }
}
