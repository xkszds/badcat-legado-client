package io.legado.app.model.analyzeRule

import com.google.gson.Gson

/** Adapter for the pinned upstream AnalyzeByJSonPath rule semantics. */
object UpstreamLegadoJsonPathRuleEvaluator {
    fun getStringList(json: String, rule: String, onError: (Exception) -> Unit): List<String> =
        AnalyzeByJSonPath(json, onError).getStringList(stripModePrefix(rule))

    fun getElementList(json: String, rule: String, onError: (Exception) -> Unit): List<String> {
        val values = AnalyzeByJSonPath(json, onError).getList(stripModePrefix(rule)).orEmpty()
        return values.map { Gson().toJson(it) }
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
