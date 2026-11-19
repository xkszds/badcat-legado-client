package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoJsonPathRuleEvaluator

/**
 * JSONPath bridge backed by pinned Legado semantics.
 * Upstream deliberately reports selector misses through its diagnostic callback
 * and returns an empty list; only failures escaping the upstream parser are fatal.
 */
object JsonPathRuleEvaluator {
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> =
        runCatching {
            EngineResult.Success(
                UpstreamLegadoJsonPathRuleEvaluator.getElementList(document.body, expression) { },
            )
        }.getOrElse { error ->
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> =
        runCatching {
            EngineResult.Success(
                UpstreamLegadoJsonPathRuleEvaluator.getStringList(document.body, expression) { },
            )
        }.getOrElse { error ->
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
}
