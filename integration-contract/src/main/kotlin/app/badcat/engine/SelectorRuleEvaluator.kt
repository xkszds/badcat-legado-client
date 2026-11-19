package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoCssRuleEvaluator

/** Explicit rule type avoids guessing CSS/XPath/JSONPath from ambiguous syntax. */
enum class SelectorRuleType {
    CSS,
    XPATH,
    JSON_PATH,
}

object SelectorRuleEvaluator {

    fun evaluate(
        document: SourceDocument,
        type: SelectorRuleType,
        expression: String,
    ): EngineResult<List<String>> = when (type) {
        SelectorRuleType.CSS -> runCatching {
            val suffix = expression.substringAfterLast("@", "").lowercase()
            val extractionSuffix = suffix in setOf("text", "textnodes", "owntext", "html", "all", "href", "src", "tag") || suffix.startsWith("attr(")
            val values = if (extractionSuffix) {
                UpstreamLegadoCssRuleEvaluator.getStringList(document.body, document.url, expression)
            } else {
                UpstreamLegadoCssRuleEvaluator.getElementTextList(document.body, document.url, expression)
            }
            EngineResult.Success(values)
        }.getOrElse { error ->
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Pinned Legado CSS evaluation failed: " + (error.message ?: error::class.java.simpleName),
                error,
            )
        }
        SelectorRuleType.XPATH -> XPathRuleEvaluator.evaluate(document, expression)
        SelectorRuleType.JSON_PATH -> JsonPathRuleEvaluator.evaluate(document, expression)
    }
}
