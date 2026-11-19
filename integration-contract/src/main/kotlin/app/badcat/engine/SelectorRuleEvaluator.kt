package app.badcat.engine

import io.legado.app.model.analyzeRule.UpstreamLegadoCssRuleEvaluator
import io.legado.app.model.analyzeRule.UpstreamLegadoJsonPathRuleEvaluator
import io.legado.app.model.analyzeRule.UpstreamLegadoXPathRuleEvaluator

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
        SelectorRuleType.CSS -> {
            // Match Legado AnalyzeRule's CSS mode markers even when a caller
            // supplies an explicit parser type instead of using the dispatcher.
            // "@@" forces JSoup mode; a single leading "@" selects the default
            // JSoup mode. Neither marker is part of the actual CSS selector.
            val raw = expression.trim()
            val normalized = when {
                raw.startsWith("@@") -> raw.substring(2).trim()
                raw.startsWith("@css:", ignoreCase = true) -> raw
                raw.startsWith("@") -> raw.substring(1).trim()
                else -> raw
            }
            if (normalized.isEmpty()) {
                EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Legado CSS selector is blank",
                )
            } else runCatching {
                val suffix = normalized.substringAfterLast("@", "").lowercase()
                val extractionSuffix = suffix in setOf(
                    "text", "textnodes", "owntext", "html", "all", "href", "src", "tag",
                ) || suffix.startsWith("attr(")
                val values = if (extractionSuffix) {
                    UpstreamLegadoCssRuleEvaluator.getStringList(
                        document.body, document.url, normalized,
                    )
                } else {
                    UpstreamLegadoCssRuleEvaluator.getElementTextList(
                        document.body, document.url, normalized,
                    )
                }
                EngineResult.Success(values)
            }.getOrElse { error ->
                EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    "Pinned Legado CSS evaluation failed: " + (error.message ?: error::class.java.simpleName),
                    error,
                )
            }
        }
        SelectorRuleType.XPATH -> runCatching {
            EngineResult.Success(UpstreamLegadoXPathRuleEvaluator.getStringList(document.body, document.url, expression))
        }.getOrElse { error ->
            EngineResult.Failure(EngineErrorCode.PARSE, "Pinned Legado XPath evaluation failed: " + (error.message ?: error::class.java.simpleName), error)
        }
        SelectorRuleType.JSON_PATH -> runCatching {
            EngineResult.Success(UpstreamLegadoJsonPathRuleEvaluator.getStringList(document.body, expression) { })
        }.getOrElse { error ->
            EngineResult.Failure(EngineErrorCode.PARSE, "Pinned Legado JSONPath evaluation failed: " + (error.message ?: error::class.java.simpleName), error)
        }
    }
}
