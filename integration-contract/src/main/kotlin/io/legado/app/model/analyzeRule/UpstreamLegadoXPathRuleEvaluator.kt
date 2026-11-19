package io.legado.app.model.analyzeRule

/** Routes XPath evaluation through the pinned upstream AnalyzeByXPath parser and evaluator. */
object UpstreamLegadoXPathRuleEvaluator {
    fun getStringList(html: String, @Suppress("UNUSED_PARAMETER") baseUrl: String, rule: String): List<String> =
        AnalyzeByXPath(html).getStringList(rule)

    fun getString(html: String, @Suppress("UNUSED_PARAMETER") baseUrl: String, rule: String): String? =
        AnalyzeByXPath(html).getString(rule)

    fun getElementList(html: String, @Suppress("UNUSED_PARAMETER") baseUrl: String, rule: String): List<String> =
        AnalyzeByXPath(html).getElements(rule).orEmpty().map { node ->
            if (node.isElement) node.asElement().outerHtml() else node.asString()
        }
}
