package app.badcat.engine

/**
 * Bridges imported nested source-rule objects to the selector primitives.
 *
 * Only explicitly configured fields are evaluated. This avoids treating URL
 * templates, JavaScript, regex replacements or metadata as CSS by accident.
 * The adapter is a bounded selector-only runtime, not the complete Legado
 * AnalyzeRule implementation.
 */
class SelectorLegadoRuleRuntime(
    private val ruleTypes: Map<String, SelectorRuleType>,
) : LegadoRuleRuntime {

    override suspend fun evaluate(
        stage: SourceRuleStage,
        ruleJson: String,
        source: BookSource,
        document: SourceDocument,
    ): EngineResult<List<Map<String, String>>> {
        val fields = runCatching {
            val json = com.google.gson.JsonParser.parseString(ruleJson)
            require(json.isJsonObject)
            json.asJsonObject.entrySet().mapNotNull { (key, value) ->
                if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                    key to value.asString
                } else null
            }.toMap()
        }.getOrElse {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Invalid ${stage.name.lowercase()} rule JSON for source '${source.id}'",
                it,
            )
        }

        val evaluated = linkedMapOf<String, List<String>>()
        for ((field, expression) in fields) {
            val type = ruleTypes["${stage.name.lowercase()}.$field"] ?: continue
            when (val result = SelectorRuleEvaluator.evaluate(document, type, expression)) {
                is EngineResult.Success -> evaluated[field] = result.value
                is EngineResult.Failure -> return result
            }
        }

        if (evaluated.isEmpty()) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "No configured selector fields for ${stage.name.lowercase()} in source '${source.id}'",
            )
        }

        val rowCount = evaluated.values.maxOfOrNull { it.size } ?: 0
        return EngineResult.Success(
            (0 until rowCount).map { index ->
                evaluated.mapNotNull { (field, values) ->
                    values.getOrNull(index)?.let { field to it }
                }.toMap()
            },
        )
    }
}
