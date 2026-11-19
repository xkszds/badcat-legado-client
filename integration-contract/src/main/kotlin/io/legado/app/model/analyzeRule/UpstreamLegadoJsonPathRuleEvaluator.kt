package io.legado.app.model.analyzeRule

import com.google.gson.Gson

/** Adapter for the pinned upstream AnalyzeByJSonPath rule semantics. */
object UpstreamLegadoJsonPathRuleEvaluator {
    fun getStringList(json: String, rule: String, onError: (Exception) -> Unit): List<String> =
        AnalyzeByJSonPath(json, onError).getStringList(stripModePrefix(rule))

    fun getElementList(json: String, rule: String, onError: (Exception) -> Unit): List<String> {
        val analyzer = AnalyzeByJSonPath(json, onError)
        val normalizedRule = stripModePrefix(rule)
        val list = analyzer.getList(normalizedRule)
        if (list != null) return list.map(::toElementString)

        // Legado's init rule can select a single JSON object (for example
        // "data.book"), not only a JSONPath array. getList() intentionally
        // returns no entries for that shape, so preserve the upstream object.
        val value = analyzer.getObject(normalizedRule)
        return when (value) {
            is List<*> -> value.map(::toElementString)
            else -> listOf(toElementString(value))
        }
    }

    private fun toElementString(value: Any?): String = when (value) {
        null -> "null"
        is String -> value
        is Number, is Boolean -> value.toString()
        else -> Gson().toJson(value)
    }

    private fun stripModePrefix(rule: String): String {
        val normalized = rule.trim()
        return when {
            normalized.startsWith("@jsonpath:", ignoreCase = true) -> normalized.substring(10).trim()
            normalized.startsWith("@json:", ignoreCase = true) -> normalized.substring(6).trim()
            else -> normalized
        }
    }
}
