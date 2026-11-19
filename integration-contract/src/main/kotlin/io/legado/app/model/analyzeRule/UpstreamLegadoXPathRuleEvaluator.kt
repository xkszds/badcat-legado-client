package io.legado.app.model.analyzeRule

import org.jsoup.Jsoup

/** Routes XPath evaluation through the pinned upstream AnalyzeByXPath implementation. */
object UpstreamLegadoXPathRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByXPath(Jsoup.parse(html, baseUrl)).getStringList(rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        AnalyzeByXPath(Jsoup.parse(html, baseUrl)).getString(rule)

    fun getElementList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByXPath(Jsoup.parse(html, baseUrl)).getElements(rule).orEmpty().map { node ->
            if (node.isElement) node.asElement().outerHtml() else node.asString()
        }
}
