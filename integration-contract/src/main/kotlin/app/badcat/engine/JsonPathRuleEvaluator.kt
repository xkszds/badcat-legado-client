package app.badcat.engine

import com.google.gson.Gson
import com.jayway.jsonpath.JsonPath

/**
 * JSONPath extraction using the same JsonPath library/version as the pinned
 * Legado baseline. Embedded {$...} rule interpolation and the full upstream
 * AnalyzeRule dispatch are not implemented in this adapter.
 */
object JsonPathRuleEvaluator {

    /** Select JSON values as independent JSON fragments for bookList/chapterList stages. */
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val raw = expression.trim()
        val rule = when {
            raw.startsWith("@jsonpath:", true) -> raw.substringAfter(':').trim()
            raw.startsWith("@json:", true) -> raw.substringAfter(':').trim()
            else -> raw
        }
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "JSONPath collection rule is blank")
        }
        return try {
            val selected = JsonPath.parse(document.body).read<Any?>(rule)
            val values = when (selected) {
                null -> emptyList()
                is Iterable<*> -> selected.mapNotNull { value ->
                    value?.let { Gson().toJson(normalizeJsonValue(it)) }
                }
                is Array<*> -> selected.mapNotNull { value ->
                    value?.let { Gson().toJson(normalizeJsonValue(it)) }
                }
                else -> listOf(Gson().toJson(normalizeJsonValue(selected)))
            }
            EngineResult.Success(values)
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
    }

    private fun normalizeJsonValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (key, item) -> key.toString() to normalizeJsonValue(item) }
        is Iterable<*> -> value.map(::normalizeJsonValue)
        is Array<*> -> value.map(::normalizeJsonValue)
        else -> value
    }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "JSONPath rule is blank")
        }

        return try {
            val values = RuleCombinationEvaluator.evaluateNested(rule) { part ->
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
