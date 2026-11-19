package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoJsonPathRuleEvaluator
import com.jayway.jsonpath.PathNotFoundException

/** JSONPath bridge backed by the pinned Legado AnalyzeByJSonPath implementation. */
object JsonPathRuleEvaluator {
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> {
        var parseError: Exception? = null
        val values = runCatching {
            UpstreamLegadoJsonPathRuleEvaluator.getElementList(document.body, expression) { parseError = it }
        }.getOrElse { error ->
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
        val failure = parseError
        if (values.isEmpty() && failure != null && failure !is PathNotFoundException) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath collection evaluation failed: ${failure.message ?: failure::class.java.simpleName}",
                failure,
            )
        }
        return EngineResult.Success(values)
    }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        var parseError: Exception? = null
        val values = runCatching {
            UpstreamLegadoJsonPathRuleEvaluator.getStringList(document.body, expression) { parseError = it }
        }.getOrElse { error ->
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
        val failure = parseError
        if (values.isEmpty() && failure != null && failure !is PathNotFoundException) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath evaluation failed: ${failure.message ?: failure::class.java.simpleName}",
                failure,
            )
        }
        return EngineResult.Success(values)
    }
}
