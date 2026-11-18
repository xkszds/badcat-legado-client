package app.badcat.client.runtime

import io.legado.app.model.analyzeRule.AnalyzeByJSoup
import io.legado.app.model.analyzeRule.parseSourceHtml

/**
 * Entry point for the pinned upstream Legado JSoup rule parser.
 *
 * This intentionally delegates parsing and selector semantics to the upstream
 * implementation instead of translating rules through Badcat's bounded CSS
 * evaluator. It is the first parser adapter in the staged runtime migration.
 */
class UpstreamLegadoCssRuleAdapter {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getStringList(rule)
    }

    fun getString(html: String, baseUrl: String, rule: String): String? {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getString(rule)
    }

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> {
        val document = parseSourceHtml(html).also { it.setBaseUri(baseUrl) }
        return AnalyzeByJSoup(document).getElements(rule).map { it.outerHtml() }
    }
}
