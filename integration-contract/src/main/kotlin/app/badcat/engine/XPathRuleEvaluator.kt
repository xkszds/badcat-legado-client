package app.badcat.engine

import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument

/**
 * XPath extraction through the JsoupXpath version pinned by Legado.
 * Upstream's node-type conversion and full AnalyzeRule chaining are not
 * reproduced here; this adapter returns each selected node's string form.
 */
object XPathRuleEvaluator {

    /** Select independent XPath nodes for bookList/chapterList collection rules. */
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val raw = expression.trim()
        if (raw.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "XPath collection rule is blank")
        }
        return try {
            val context = JXDocument.create(Jsoup.parse(document.body, document.url))
            RuleCombinationEvaluator.evaluateNestedResult(raw) { branch ->
                val rule = branch.trim().let {
                    if (it.startsWith("@xpath:", ignoreCase = true)) it.substringAfter(':').trim() else it
                }
                if (rule.isEmpty()) {
                    EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "XPath collection rule is blank")
                } else {
                    try {
                        val nodes = context.selN(rule).orEmpty()
                        EngineResult.Success(nodes.map { node ->
                            if (node.isElement) node.asElement().outerHtml() else node.asString()
                        })
                    } catch (error: Exception) {
                        EngineResult.Failure(
                            EngineErrorCode.PARSE,
                            "XPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                            error,
                        )
                    }
                }
            }
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "XPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }
    }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "XPath rule is blank")
        }

        return try {
            val parsed = Jsoup.parse(document.body, document.url)
            val context = JXDocument.create(parsed)
            val values = RuleCombinationEvaluator.evaluateNested(rule) { part ->
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
