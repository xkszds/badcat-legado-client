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
        if (raw.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "JSONPath collection rule is blank")
        }
        val directRule = raw.removePrefixIgnoreCase("@jsonpath:").removePrefixIgnoreCase("@json:").trim()
        if (directRule.startsWith("$") && isSingleJsonPath(directRule)) {
            return evaluateCollectionPath(document, directRule)
        }
        return try {
            RuleCombinationEvaluator.evaluateNestedResult(raw) { branch ->
                val rule = branch.trim().let {
                    when {
                        it.startsWith("@jsonpath:", true) -> it.substringAfter(':').trim()
                        it.startsWith("@json:", true) -> it.substringAfter(':').trim()
                        else -> it
                    }
                }
                if (rule.isEmpty()) {
                    EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "JSONPath collection rule is blank")
                } else {
                    try {
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
                    } catch (_: com.jayway.jsonpath.PathNotFoundException) {
                        EngineResult.Success(emptyList())
                    } catch (error: Exception) {
                        EngineResult.Failure(
                            EngineErrorCode.PARSE,
                            "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                            error,
                        )
                    }
                }
            }
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "JSONPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
    }

    private fun evaluateCollectionPath(document: SourceDocument, path: String): EngineResult<List<String>> {
        if (!hasBalancedJsonPathDelimiters(path)) {
            return EngineResult.Failure(EngineErrorCode.PARSE, "Malformed JSONPath expression")
        }
        return try {
        val selected = JsonPath.parse(document.body).read<Any?>(path)
        val values = when (selected) {
            null -> emptyList()
            is Iterable<*> -> selected.mapNotNull { it?.let { value -> Gson().toJson(normalizeJsonValue(value)) } }
            is Array<*> -> selected.mapNotNull { it?.let { value -> Gson().toJson(normalizeJsonValue(value)) } }
            else -> listOf(Gson().toJson(normalizeJsonValue(selected)))
        }
        EngineResult.Success(values)
    } catch (_: com.jayway.jsonpath.PathNotFoundException) {
        EngineResult.Success(emptyList())
    } catch (error: Exception) {
        EngineResult.Failure(EngineErrorCode.PARSE, "JSONPath collection evaluation failed: " + (error.message ?: error::class.java.simpleName), error)
    }
    }

    private fun isSingleJsonPath(path: String): Boolean = runCatching {
        RuleExpressionTokenizer.splitByFirstTopLevelOperator(path).operator == null
    }.getOrDefault(true)

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

        val directRule = rule.removePrefixIgnoreCase("@jsonpath:").removePrefixIgnoreCase("@json:").trim()
        if (directRule.startsWith("$") && isSingleJsonPath(directRule)) {
            return evaluateScalarPath(document, directRule)
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
    private fun evaluateScalarPath(document: SourceDocument, path: String): EngineResult<List<String>> {
        if (!hasBalancedJsonPathDelimiters(path)) {
            return EngineResult.Failure(EngineErrorCode.PARSE, "Malformed JSONPath expression")
        }
        return try {
        val result = JsonPath.compile(path).read<Any?>(document.body)
        val values = when (result) {
            null -> emptyList()
            is Iterable<*> -> result.map { it?.toString().orEmpty() }.filter(String::isNotEmpty)
            is Array<*> -> result.map { it?.toString().orEmpty() }.filter(String::isNotEmpty)
            else -> listOf(result.toString())
        }
        EngineResult.Success(values)
    } catch (_: com.jayway.jsonpath.PathNotFoundException) {
        EngineResult.Success(emptyList())
    } catch (error: Exception) {
        EngineResult.Failure(EngineErrorCode.PARSE, "JSONPath evaluation failed: " + (error.message ?: error::class.java.simpleName), error)
    }
    }

    private fun hasBalancedJsonPathDelimiters(path: String): Boolean {
        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (char in path) {
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == quote -> quote = null
                }
            } else {
                when (char) {
                    '\'', '"' -> quote = char
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth < 0) return false
                    }
                }
            }
        }
        return depth == 0 && quote == null && !path.trim().endsWith(".")
    }

}

private fun String.removePrefixIgnoreCase(prefix: String): String =
    if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
