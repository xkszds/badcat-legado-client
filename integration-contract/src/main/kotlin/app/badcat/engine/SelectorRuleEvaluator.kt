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
            EngineResult.Success(
                UpstreamLegadoCssRuleEvaluator.getStringList(
                    document.body,
                    document.url,
                    expression,
                ),
            )
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
