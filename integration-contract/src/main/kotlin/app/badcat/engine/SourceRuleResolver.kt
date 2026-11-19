package app.badcat.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Parses a single Legado stage's nested rule object and exposes named rules.
 * Rule values remain opaque strings: callers must supply the selector mode
 * explicitly because the same text can be ambiguous across parser grammars.
 */
data class SourceRuleObject(
    val sourceId: String,
    val stage: SourceRuleStage,
    val fields: Map<String, String>,
) {
    fun rule(name: String): String? = fields[name]?.takeIf(String::isNotBlank)
}

object SourceRuleResolver {

    fun resolve(
        source: BookSource,
        stage: SourceRuleStage,
    ): EngineResult<String> {
        val raw = stageJson(source, stage)
            ?: return missing(source, stage)
        val isObject = runCatching { JsonParser.parseString(raw).isJsonObject }.getOrDefault(false)
        if (!isObject) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source '${source.id}' has an invalid ${stage.name.lowercase()} rule object",
            )
        }
        return EngineResult.Success(raw)
    }

    fun resolveObject(
        source: BookSource,
        stage: SourceRuleStage,
    ): EngineResult<SourceRuleObject> {
        return when (val raw = resolve(source, stage)) {
            is EngineResult.Failure -> raw
            is EngineResult.Success -> {
                val json = JsonParser.parseString(raw.value).asJsonObject
                val fields = json.entrySet().mapNotNull { (key, value) ->
                    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        key to value.asString
                    } else null
                }.toMap()
                EngineResult.Success(SourceRuleObject(source.id, stage, fields))
            }
        }
    }

    /**
     * Resolves one named field from the selected stage object and dispatches it
     * through the chosen parser. This is the first executable bridge from
     * imported nested source JSON to the selector primitives.
     */
    fun evaluateField(
        source: BookSource,
        stage: SourceRuleStage,
        field: String,
        document: SourceDocument,
        type: SelectorRuleType,
    ): EngineResult<List<String>> {
        val rules = when (val resolved = resolveObject(source, stage)) {
            is EngineResult.Failure -> return resolved
            is EngineResult.Success -> resolved.value
        }
        val expression = rules.rule(field)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Source '${source.id}' has no '$field' rule in ${stage.name.lowercase()}",
            )
        return SelectorRuleEvaluator.evaluate(document, type, expression)
    }


    /** Resolves a source's selected stage JSON and runs the configured runtime. */
    suspend fun evaluateStage(
        source: BookSource,
        stage: SourceRuleStage,
        document: SourceDocument,
        runtime: LegadoRuleRuntime,
    ): EngineResult<List<Map<String, String>>> {
        val raw = when (val resolved = resolve(source, stage)) {
            is EngineResult.Failure -> return resolved
            is EngineResult.Success -> resolved.value
        }
        return runtime.evaluate(stage, raw, source, document)
    }

    private fun stageJson(source: BookSource, stage: SourceRuleStage): String? =
        when (stage) {
            SourceRuleStage.EXPLORE -> source.ruleExploreJson
            SourceRuleStage.SEARCH -> source.ruleSearchJson
            SourceRuleStage.BOOK_INFO -> source.ruleBookInfoJson
            SourceRuleStage.TOC -> source.ruleTocJson
            SourceRuleStage.CONTENT -> source.ruleContentJson
            SourceRuleStage.REVIEW -> source.ruleReviewJson
        }?.takeIf(String::isNotBlank)

    private fun missing(source: BookSource, stage: SourceRuleStage) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' has no ${stage.name.lowercase()} rule",
        )
}
