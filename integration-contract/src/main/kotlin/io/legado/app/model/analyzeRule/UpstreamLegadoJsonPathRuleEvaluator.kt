package io.legado.app.model.analyzeRule

import com.google.gson.Gson

/** Adapter over the exact pinned upstream AnalyzeByJSonPath implementation. */
object UpstreamLegadoJsonPathRuleEvaluator {
    fun getStringList(json: String, rule: String, onError: (Exception) -> Unit): List<String> =
        PinnedLegadoJsonPathBridge.getStringList(json, stripModePrefix(rule), onError)

    fun getElementList(json: String, rule: String, onError: (Exception) -> Unit): List<String> {
        val normalizedRule = stripModePrefix(rule)
        val list = PinnedLegadoJsonPathBridge.getList(json, normalizedRule, onError)
        if (!list.isNullOrEmpty()) return list.map(::toElementString)

        // Legado init rules may select one JSON object rather than an array.
        val value = PinnedLegadoJsonPathBridge.getObject(json, normalizedRule)
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
