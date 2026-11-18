package io.legado.app.model.analyzeRule

/** Public boundary around the verbatim upstream parser for shared engine callers. */
object UpstreamLegadoCssRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getStringList(rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getString(rule)

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> {
        val extractionRule = if (rule.substringAfterLast("@", "").lowercase() in
            setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag")) {
            rule.substringBeforeLast("@")
        } else rule
        return getStringList(html, baseUrl, "${extractionRule}@html")
    }
}
