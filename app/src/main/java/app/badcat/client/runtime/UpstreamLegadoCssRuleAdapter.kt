package app.badcat.client.runtime

import io.legado.app.model.analyzeRule.UpstreamLegadoCssRuleEvaluator

/** Delegates to the pinned upstream Legado JSoup parser hosted in integration-contract. */
class UpstreamLegadoCssRuleAdapter {
    fun getStringList(html: String, baseUrl: String, rule: String): List<String> =
        UpstreamLegadoCssRuleEvaluator.getStringList(html, baseUrl, rule)

    fun getString(html: String, baseUrl: String, rule: String): String? =
        UpstreamLegadoCssRuleEvaluator.getString(html, baseUrl, rule)

    fun getElementHtmlList(html: String, baseUrl: String, rule: String): List<String> =
        UpstreamLegadoCssRuleEvaluator.getElementHtmlList(html, baseUrl, rule)
}
