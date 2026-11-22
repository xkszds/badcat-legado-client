package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoJsonPathRuleEvaluator

/**
 * JSONPath bridge backed by pinned Legado semantics.
 * Top-level mixed operators are composed by the shared balanced-rule tokenizer;
 * each leaf is evaluated by the upstream JSONPath implementation.
 */
object JsonPathRuleEvaluator {
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> =
        RuleCombinationEvaluator.evaluateNestedResult(expression) { branch ->
            runCatching {
                EngineResult.Success(
                    UpstreamLegadoJsonPathRuleEvaluator.getElementList(document.body, branch) { },
                )
            }.getOrElse { error ->
                EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                    error,
                )
            }
        }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> =
        RuleCombinationEvaluator.evaluateNestedResult(expression) { branch ->
            runCatching {
                EngineResult.Success(
                    UpstreamLegadoJsonPathRuleEvaluator.getStringList(document.body, branch) { },
                )
            }.getOrElse { error ->
                EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "JSONPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                    error,
                )
            }
        }
}
