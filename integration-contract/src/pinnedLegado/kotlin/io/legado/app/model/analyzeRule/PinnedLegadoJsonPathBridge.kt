package io.legado.app.model.analyzeRule

/** Public boundary for the pinned upstream JSONPath analyzer's internal collection APIs. */
object PinnedLegadoJsonPathBridge {
    fun getStringList(json: String, rule: String, onError: (Exception) -> Unit): List<String> =
        AnalyzeByJSonPath(json, onError).getStringList(rule)

    fun getList(json: String, rule: String, onError: (Exception) -> Unit): List<Any>? =
        AnalyzeByJSonPath(json, onError).getList(rule)

    fun getObject(json: String, rule: String): Any =
        AnalyzeByJSonPath(json).getObject(rule)
}
