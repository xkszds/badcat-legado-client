package io.legado.app.model.analyzeRule

/** Public boundary around the verbatim upstream parser for shared engine callers. */
object UpstreamLegadoCssRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getStringList(rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) }).getString(rule)

    fun getElementTextList(html: String, baseUrl: String, rule: String): List<String> {
        val normalizedRule = normalizeElementRule(rule)
        val analyzer = AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) })
        return analyzer.getElements(normalizedRule).map { it.text() }
    }

    /** Tag extraction is not an attribute lookup in the pinned AnalyzeByJSoup API. */
    fun getElementTagList(html: String, baseUrl: String, rule: String): List<String> {
        val normalizedRule = normalizeElementRule(rule)
        val analyzer = AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) })
        return analyzer.getElements(normalizedRule).map { it.tagName() }
    }

    private fun normalizeElementRule(rule: String): String {
        val extractionRule = if (rule.substringAfterLast("@", "").lowercase() in
            setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag")) {
            rule.substringBeforeLast("@")
        } else rule
        return if (extractionRule.startsWith("@.") ||
            extractionRule.startsWith("@class.", ignoreCase = true) ||
            extractionRule.startsWith("@tag.", ignoreCase = true) ||
            extractionRule.startsWith("@id.", ignoreCase = true)) {
            extractionRule.substring(1)
        } else extractionRule
    }

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> {
        val extractionRule = if (rule.substringAfterLast("@", "").lowercase() in
            setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag")) {
            rule.substringBeforeLast("@")
        } else rule
        // A single leading '@' is the Legado default-CSS mode marker in
        // source rules such as @.w_txt li.4@textNodes. getElements() expects
        // the selector/index expression itself here; unlike getStringList(),
        // it does not route that marker through SourceRule's @CSS: handling.
        val normalizedRule = if (extractionRule.startsWith("@.") ||
            extractionRule.startsWith("@class.", ignoreCase = true) ||
            extractionRule.startsWith("@tag.", ignoreCase = true) ||
            extractionRule.startsWith("@id.", ignoreCase = true)) {
            extractionRule.substring(1)
        } else extractionRule
        val analyzer = AnalyzeByJSoup(parseSourceHtml(html).also { it.setBaseUri(baseUrl) })
        return analyzer.getElements(normalizedRule).map { it.outerHtml() }
    }
}
