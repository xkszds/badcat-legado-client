package io.legado.app.model.analyzeRule

/**
 * Public facade compiled in the same Kotlin source set as the pinned upstream
 * AnalyzeByJSoup class. Upstream intentionally marks its parser entry points
 * internal; this facade exposes only the operations needed by the host.
 */
object PinnedLegadoJsoupBridge {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getStringList(rule)
    }

    fun getString(html: String, baseUrl: String, rule: String): String? {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getString(rule)
    }

    fun getElementTextList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getElements(rule).map { it.text() }
    }

    fun getElementTagList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getElements(rule).map { it.tagName() }
    }

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getElements(rule).map { it.outerHtml() }
    }
}
