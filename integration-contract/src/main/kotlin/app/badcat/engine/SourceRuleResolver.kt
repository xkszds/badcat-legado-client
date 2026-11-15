package app.badcat.engine

import com.google.gson.JsonParser

/**
 * Selects the exact nested rule object for an operation.
 *
 * This is rule routing and shape validation only. It does not evaluate CSS,
 * XPath, JSONPath, regex or JavaScript; those belong to LegadoRuleRuntime.
 */
object SourceRuleResolver {

    fun resolve(
        source: BookSource,
        stage: SourceRuleStage,
    ): EngineResult<String> {
        val raw = when (stage) {
            SourceRuleStage.EXPLORE -> source.ruleExploreJson
            SourceRuleStage.SEARCH -> source.ruleSearchJson
            SourceRuleStage.BOOK_INFO -> source.ruleBookInfoJson
            SourceRuleStage.TOC -> source.ruleTocJson
            SourceRuleStage.CONTENT -> source.ruleContentJson
        }?.takeIf(String::isNotBlank)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Source '${source.id}' has no ${stage.name.lowercase()} rule",
            )

        val isObject = runCatching {
            JsonParser.parseString(raw).isJsonObject
        }.getOrDefault(false)

        if (!isObject) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source '${source.id}' has an invalid ${stage.name.lowercase()} rule object",
            )
        }

        return EngineResult.Success(raw)
    }
}
