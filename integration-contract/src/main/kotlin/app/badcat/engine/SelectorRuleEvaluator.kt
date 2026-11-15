package app.badcat.engine

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
        SelectorRuleType.CSS -> CssRuleEvaluator.evaluate(document, expression)
        SelectorRuleType.XPATH -> XPathRuleEvaluator.evaluate(document, expression)
        SelectorRuleType.JSON_PATH -> JsonPathRuleEvaluator.evaluate(document, expression)
    }
}
