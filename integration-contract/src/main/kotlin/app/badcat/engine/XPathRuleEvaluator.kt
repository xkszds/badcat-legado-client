package app.badcat.engine

import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument

/**
 * XPath extraction through the JsoupXpath version pinned by Legado.
 * Upstream's node-type conversion and full AnalyzeRule chaining are not
 * reproduced here; this adapter returns each selected node's string form.
 */
object XPathRuleEvaluator {

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "XPath rule is blank")
        }

        if (RuleCombinationEvaluator.hasNestedTopLevelOperator(rule)) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Nested XPath list-combination operators are not supported",
            )
        }

        return try {
            val parsed = Jsoup.parse(document.body, document.url)
            val context = JXDocument.create(parsed)
            val values = RuleCombinationEvaluator.evaluate(rule) { part ->
                context.selN(part).orEmpty().map { it.asString() }
            }
            EngineResult.Success(values)
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "XPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
    }
}
