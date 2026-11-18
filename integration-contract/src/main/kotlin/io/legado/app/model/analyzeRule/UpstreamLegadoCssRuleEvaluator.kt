package io.legado.app.model.analyzeRule

/** Public boundary around the verbatim upstream parser for shared engine callers. */
object UpstreamLegadoCssRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getStringList(rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getString(rule)

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) })
            .getElements(rule).map { it.outerHtml() }
}
