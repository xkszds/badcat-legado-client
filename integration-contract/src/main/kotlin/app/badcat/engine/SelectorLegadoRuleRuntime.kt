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
    private val ruleTypes: Map<String, SelectorRuleType> = emptyMap(),
    private val scriptRuntime: SourceScriptRuntime = UnsupportedSourceScriptRuntime,
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
            val configuredType = ruleTypes["${stage.name.lowercase()}.$field"]
            // Some imported source objects contain non-selector metadata or
            // legacy javascript: URL fragments in unrelated fields. Do not
            // reinterpret those as CSS when the field has no explicit mode.
            if (configuredType == null &&
                !expression.trim().startsWith("@", ignoreCase = true) &&
                expression.trim().startsWith("javascript:", ignoreCase = true)
            ) continue
            val trimmedExpression = expression.trim()
            val values = when {
                trimmedExpression.startsWith("@js:", ignoreCase = true) ||
                    trimmedExpression.startsWith("javascript:", ignoreCase = true) -> {
                    val script = if (trimmedExpression.startsWith("@js:", ignoreCase = true)) {
                        trimmedExpression.substringAfter(':').trim()
                    } else {
                        trimmedExpression.substringAfter(':').trim()
                    }
                    if (script.isBlank()) {
                        return EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Empty JavaScript rule for field '$field'",
                        )
                    }
                    when (val result = scriptRuntime.evaluate(
                        source = source,
                        script = script,
                        bindings = mapOf(
                            "result" to document.body,
                            "baseUrl" to document.url,
                            "sourceUrl" to source.id,
                            "sourceId" to source.id,
                            "sourceName" to source.name,
                            "ruleName" to field,
                        ),
                    )) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> listOf(result.value)
                    }
                }
                configuredType != null -> {
                    when (val result = RuleCombinationEvaluator.evaluateNestedResult(expression) { branch ->
                        SelectorRuleEvaluator.evaluate(document, configuredType, branch)
                    }) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> result.value
                    }
                }
                trimmedExpression.startsWith("@regex:", ignoreCase = true) ||
                    trimmedExpression.startsWith(":") -> {
                    when (val result = LegadoRuleExpressionDispatcher.evaluateElements(document, trimmedExpression)) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> result.value
                    }
                }
                else -> {
                    when (val result = LegadoRuleExpressionDispatcher.evaluate(document, expression)) {
                        is EngineResult.Failure -> return result
                        is EngineResult.Success -> result.value
                    }
                }
            }
            evaluated[field] = values
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
