package io.legado.app.model.analyzeRule

/** Adapter for the exact pinned upstream AnalyzeByJSoup implementation. */
object UpstreamLegadoCssRuleEvaluator {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        PinnedLegadoJsoupBridge.getStringList(html, baseUrl, rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        PinnedLegadoJsoupBridge.getString(html, baseUrl, rule)

    fun getElementTextList(html: String, baseUrl: String, rule: String): List<String> =
        PinnedLegadoJsoupBridge.getElementTextList(html, baseUrl, normalizeElementRule(rule))

    /** Tag extraction is not an attribute lookup in the pinned AnalyzeByJSoup API. */
    fun getElementTagList(html: String, baseUrl: String, rule: String): List<String> =
        PinnedLegadoJsoupBridge.getElementTagList(html, baseUrl, normalizeElementRule(rule))

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
        val normalizedRule = if (extractionRule.startsWith("@.") ||
            extractionRule.startsWith("@class.", ignoreCase = true) ||
            extractionRule.startsWith("@tag.", ignoreCase = true) ||
            extractionRule.startsWith("@id.", ignoreCase = true)) {
            extractionRule.substring(1)
        } else extractionRule
        return PinnedLegadoJsoupBridge.getElementHtmlList(html, baseUrl, normalizedRule)
    }
}
