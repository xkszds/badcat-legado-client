package io.legado.app.model.analyzeRule

/** Routes XPath evaluation through the pinned upstream AnalyzeByXPath parser and evaluator. */
object UpstreamLegadoXPathRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        PinnedLegadoXPathBridge.getStringList(html, baseUrl, rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        PinnedLegadoXPathBridge.getString(html, baseUrl, rule)

    fun getElementList(html: String, baseUrl: String, rule: String): List<String> =
        PinnedLegadoXPathBridge.getElementList(html, baseUrl, rule)
}
