package app.badcat.engine

import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument

/**
 * Executes the recovered PPRule pageRule extraction subset.
 *
 * RE is the original Matcher.find()/group(1) scalar path. XPATH selects the
 * first node; meta nodes yield their content attribute and other elements
 * yield text. JavaScript fields and nextPageRule pagination remain opaque.
 */
object BadcatPpRuleEvaluator {

    fun extractScalar(
        document: SourceDocument,
        rule: BadcatPpRule,
    ): EngineResult<String?> {
        if (rule.pageRule.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPRule pageRule is blank",
            )
        }
        return when (rule.ruleType) {
            BadcatPpRuleType.RE -> when (
                val result = LegadoRegexElementEvaluator.evaluateScalar(document.body, rule.pageRule)
            ) {
                is EngineResult.Failure -> result
                is EngineResult.Success -> {
                    if (result.value.size < 2) {
                        EngineResult.Failure(
                            EngineErrorCode.PARSE,
                            "Badcat RE PPRule requires capture group 1",
                        )
                    } else {
                        EngineResult.Success(result.value[1])
                    }
                }
            }
            BadcatPpRuleType.XPATH -> try {
                val context = JXDocument.create(Jsoup.parse(document.body, document.url))
                val node = context.selN(rule.pageRule).orEmpty().firstOrNull()
                    ?: return EngineResult.Success(null)
                val value = if (node.isElement) {
                    val element = node.asElement()
                    if (element.tagName().equals("meta", ignoreCase = true)) {
                        element.attr("content")
                    } else {
                        element.text()
                    }
                } else {
                    node.asString()
                }
                EngineResult.Success(value)
            } catch (error: Exception) {
                EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "Badcat XPath PPRule could not be evaluated",
                    error,
                )
            }
            BadcatPpRuleType.UNRECOGNIZED -> EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPRule uses an unknown ruleType value",
            )
        }
    }

    /** Returns selected node fragments for list/comic adapters without guessing their ordering. */
    fun selectElements(
        document: SourceDocument,
        rule: BadcatPpRule,
    ): EngineResult<List<String>> {
        if (rule.pageRule.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPRule pageRule is blank",
            )
        }
        return when (rule.ruleType) {
            BadcatPpRuleType.RE ->
                LegadoRegexElementEvaluator.evaluate(document.body, rule.pageRule)
            BadcatPpRuleType.XPATH ->
                XPathRuleEvaluator.evaluateElements(document, rule.pageRule)
            BadcatPpRuleType.UNRECOGNIZED -> EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPRule uses an unknown ruleType value",
            )
        }
    }
}
