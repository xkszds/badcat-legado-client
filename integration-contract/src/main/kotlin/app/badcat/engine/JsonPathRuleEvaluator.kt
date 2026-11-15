package app.badcat.engine

import com.jayway.jsonpath.JsonPath

/**
 * JSONPath extraction using the same JsonPath library/version as the pinned
 * Legado baseline. Embedded {$...} rule interpolation and the full upstream
 * AnalyzeRule dispatch are not implemented in this adapter.
 */
object JsonPathRuleEvaluator {

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "JSONPath rule is blank")
        }

        return try {
            val values = RuleCombinationEvaluator.evaluate(rule) { part ->
                val result = try {
                    JsonPath.parse(document.body).read<Any?>(part)
                } catch (_: com.jayway.jsonpath.PathNotFoundException) {
                    null
                }
                when (result) {
                    null -> emptyList()
                    is Iterable<*> -> result.map { it?.toString().orEmpty() }.filter(String::isNotEmpty)
                    is Array<*> -> result.map { it?.toString().orEmpty() }.filter(String::isNotEmpty)
                    else -> listOf(result.toString())
                }
            }
            EngineResult.Success(values)
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
    }
}
