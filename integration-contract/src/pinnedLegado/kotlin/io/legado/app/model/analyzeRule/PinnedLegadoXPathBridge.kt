package io.legado.app.model.analyzeRule

/** Public boundary for the pinned upstream XPath analyzer's internal APIs. */
object PinnedLegadoXPathBridge {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByXPath(document).getStringList(rule)
    }

    fun getString(html: String, baseUrl: String, rule: String): String? {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByXPath(document).getString(rule)
    }

    fun getElementList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByXPath(document).getElements(rule).orEmpty().map { node ->
            if (node.isElement) node.asElement().outerHtml() else node.asString()
        }
    }
}
