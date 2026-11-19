package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoXPathRuleEvaluator

/**
 * XPath bridge backed by the pinned Legado AnalyzeByXPath implementation.
 * The adapter preserves typed failures while upstream owns rule splitting,
 * XML parsing, node conversion and scalar/list extraction semantics.
 */
object XPathRuleEvaluator {
    fun evaluateElements(document: SourceDocument, expression: String): EngineResult<List<String>> =
        runCatching {
            EngineResult.Success(
                UpstreamLegadoXPathRuleEvaluator.getElementList(
                    document.body, document.url, expression.trim().removePrefixIgnoreCase("@xpath:"),
                ),
            )
        }.getOrElse { error ->
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "XPath collection evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }

    fun evaluate(document: SourceDocument, expression: String): EngineResult<List<String>> =
        runCatching {
            EngineResult.Success(
                UpstreamLegadoXPathRuleEvaluator.getStringList(
                    document.body, document.url, expression.trim().removePrefixIgnoreCase("@xpath:"),
                ),
            )
        }.getOrElse { error ->
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "XPath evaluation failed: ${error.message ?: error::class.java.simpleName}",
                error,
            )
        }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length).trim() else this
}
