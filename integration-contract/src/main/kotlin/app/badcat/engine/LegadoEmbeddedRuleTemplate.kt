package app.badcat.engine

/**
 * Shared, bounded implementation of Legado's embedded `{{...}}` rule expansion.
 * The caller supplies the current document/item as `result`; scripts receive
 * only the source metadata and request values, never Android or transport objects.
 */
object LegadoEmbeddedRuleTemplate {
    suspend fun resolve(
        source: BookSource,
        template: String,
        result: String,
        baseUrl: String,
        ruleName: String,
        scriptRuntime: SourceScriptRuntime,
        key: String = "",
        page: Int = 1,
    ): EngineResult<String> {
        if (!template.contains("{{")) return EngineResult.Success(template)
        val evaluator = object : LegadoEmbeddedScriptEvaluator {
            override suspend fun evaluate(
                script: String,
                key: String,
                page: Int,
            ): EngineResult<Any?> = evaluateWithResult(script, key, page, result)

            override suspend fun evaluateWithResult(
                script: String,
                key: String,
                page: Int,
                result: String,
            ): EngineResult<Any?> = when (val evaluated = scriptRuntime.evaluate(
                source = source,
                script = script,
                bindings = mapOf(
                    "result" to result,
                    "baseUrl" to baseUrl,
                    "sourceUrl" to source.id,
                    "sourceId" to source.id,
                    "sourceName" to source.name,
                    "ruleName" to ruleName,
                    "key" to key,
                    "page" to page.toString(),
                ),
            )) {
                is EngineResult.Failure -> evaluated
                is EngineResult.Success -> EngineResult.Success(evaluated.value)
            }
        }
        return LegadoEmbeddedUrlTemplate.resolve(
            template = template,
            key = key,
            evaluator = evaluator,
            page = page,
        )
    }
}
