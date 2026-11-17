package app.badcat.engine

/**
 * Bounded search result parser for the official Legado SearchRule
 * field names. Each bookList element is evaluated independently so item
 * selectors are not accidentally applied to the whole response document.
 *
 * Bounded regex is supported for collection roots and scalar fields. Full
 * AnalyzeRule semantics (JS, variable interpolation, filters, chained per-item
 * rule modes and direct-detail detection) remain unsupported.
 */
class CssSearchResultsParser {

    fun parse(
        source: BookSource,
        response: SourceHttpResponse,
        stage: SourceRuleStage = SourceRuleStage.SEARCH,
    ): EngineResult<List<BookCandidate>> {
        require(stage == SourceRuleStage.SEARCH || stage == SourceRuleStage.EXPLORE) {
            "Search-result parser only supports search and explore stages"
        }
        val rules = when (val result = SourceRuleResolver.resolveObject(source, stage)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val listRule = rules["bookList"]?.takeIf(String::isNotBlank)
            ?: return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Source '${source.id}' has no ${stage.name.lowercase()} bookList rule",
            )
        if (listRule.contains("@js:", ignoreCase = true) || listRule.contains("{{")) {
            return unsupported(source, "JavaScript or interpolation in bookList")
        }
        if (hasNonHtmlOutputSuffix(listRule)) {
            return unsupported(source, "bookList must return element HTML, not extracted text or attributes")
        }

        // Preserve the original response bytes/text for regex roots; Jsoup.outerHtml()
        // normalizes attribute quotes and can change the meaning of an imported regex.
        val document = SourceDocument(response.finalUrl, response.body, response.contentType)
        val legacyRegexMode = listRule.trim().startsWith(":")
        val items = when (val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            listRule,
        )) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }

        val candidates = ArrayList<BookCandidate>()
        try {
            for (itemHtml in items) {
                // Collection roots may return either HTML fragments or JSON values.
                // Keep the selected fragment as a SourceDocument so each field is
                // evaluated by the same CSS/XPath/JSONPath dispatcher as its root.
                val itemDocument = SourceDocument(response.finalUrl, itemHtml, response.contentType)
                val name = field(source, rules, "name", itemDocument, legacyRegexMode = legacyRegexMode) ?: continue
                val bookUrl = field(source, rules, "bookUrl", itemDocument, legacyRegexMode = legacyRegexMode)
                    ?.takeIf(String::isNotBlank) ?: continue
                candidates += BookCandidate(
                    sourceId = source.id,
                    name = name,
                    author = field(source, rules, "author", itemDocument, legacyRegexMode = legacyRegexMode),
                    bookUrl = bookUrl,
                    coverUrl = field(source, rules, "coverUrl", itemDocument, legacyRegexMode = legacyRegexMode),
                    latestChapter = field(source, rules, "lastChapter", itemDocument, legacyRegexMode = legacyRegexMode),
                    intro = field(source, rules, "intro", itemDocument, legacyRegexMode = legacyRegexMode),
                    status = field(source, rules, "bookStatus", itemDocument, legacyRegexMode = legacyRegexMode),
                )
            }
        } catch (failure: SearchParseFailure) {
            return failure.failure
        } catch (failure: UnsupportedSearchRule) {
            return unsupported(source, failure.message ?: "unsupported search field")
        } catch (failure: Exception) {
            return EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Could not parse search results for source '${source.id}'",
                failure,
            )
        }
        return EngineResult.Success(candidates.distinctBy { it.bookUrl })
    }

    /**
     * Asynchronous variant used by the production search/explore services.
     * It executes bounded @js field rules per isolated result card; the ordinary
     * parse() API remains synchronous and fail-closed for callers without a script host.
     */
    suspend fun parseWithScripts(
        source: BookSource,
        response: SourceHttpResponse,
        scriptRuntime: SourceScriptRuntime,
        stage: SourceRuleStage = SourceRuleStage.SEARCH,
        key: String = "",
        page: Int = 1,
    ): EngineResult<List<BookCandidate>> {
        require(stage == SourceRuleStage.SEARCH || stage == SourceRuleStage.EXPLORE) {
            "Search-result parser only supports search and explore stages"
        }
        val rules = when (val result = SourceRuleResolver.resolveObject(source, stage)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value.fields
        }
        val listRule = rules["bookList"]?.takeIf(String::isNotBlank)
            ?: return unsupported(source, "missing bookList rule")
        var rootExpression = listRule.trim()
        rootExpression = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                source = source,
                template = rootExpression,
                result = response.body,
                baseUrl = response.finalUrl,
                ruleName = "bookList",
                scriptRuntime = scriptRuntime,
                key = key,
                page = page,
            )) {
                is EngineResult.Failure -> return expanded
                is EngineResult.Success -> expanded.value
            }
        val legacyRegexMode = rootExpression.startsWith(":")
        val items = if (rootExpression.startsWith("@js:", true) ||
            rootExpression.startsWith("javascript:", true)
        ) {
            val script = rootExpression.substringAfter(':').trim()
            if (script.isBlank()) return unsupported(source, "empty JavaScript bookList")
            val rendered = when (val result = scriptRuntime.evaluate(
                source = source,
                script = script,
                bindings = mapOf(
                    "result" to response.body,
                    "baseUrl" to response.finalUrl,
                    "sourceUrl" to source.id,
                    "sourceId" to source.id,
                    "sourceName" to source.name,
                    "ruleName" to "bookList",
                    "key" to key,
                    "page" to page.toString(),
                ),
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            // A script can return serialized JSON arrays or HTML fragments.
            // Preserve each JSON object as its own document for JSONPath field rules.
            val trimmed = rendered.trim()
            if (trimmed.startsWith("[")) {
                runCatching {
                    val array = com.google.gson.JsonParser.parseString(trimmed).asJsonArray
                    array.map { element ->
                        require(element.isJsonObject || element.isJsonArray)
                        element.toString()
                    }
                }.getOrElse {
                    return unsupported(source, "JavaScript bookList must return a valid JSON array or HTML fragments")
                }
            } else {
                val fragment = org.jsoup.Jsoup.parseBodyFragment(rendered)
                fragment.body().children().map { it.outerHtml() }
            }
        } else {
            if (rootExpression.contains("{{")) {
                return unsupported(source, "unresolved bookList interpolation")
            }
            if (hasNonHtmlOutputSuffix(rootExpression)) {
                return unsupported(source, "bookList must return element HTML")
            }
            when (val result = LegadoRuleExpressionDispatcher.evaluateElements(
                SourceDocument(response.finalUrl, response.body, response.contentType, key, page),
                rootExpression,
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
        }

        suspend fun fieldValue(fieldName: String, itemHtml: String): EngineResult<String?> {
            val expression = rules[fieldName]?.takeIf(String::isNotBlank) ?: return EngineResult.Success(null)
            val trimmed = when (val expanded = LegadoEmbeddedRuleTemplate.resolve(
                source = source,
                template = expression,
                result = itemHtml,
                baseUrl = response.finalUrl,
                ruleName = fieldName,
                scriptRuntime = scriptRuntime,
                key = key,
                page = page,
            )) {
                is EngineResult.Failure -> return expanded
                is EngineResult.Success -> expanded.value
            }
            if (trimmed.startsWith("@js:", true) || trimmed.startsWith("javascript:", true)) {
                val script = trimmed.substringAfter(':').trim()
                if (script.isBlank()) return unsupported(source, "empty JavaScript rule for '$fieldName'")
                val evaluated = when (val result = scriptRuntime.evaluate(
                    source = source,
                    script = script,
                    bindings = mapOf(
                        "result" to itemHtml,
                        "baseUrl" to response.finalUrl,
                        "sourceUrl" to source.id,
                        "sourceId" to source.id,
                        "sourceName" to source.name,
                        "ruleName" to fieldName,
                        "key" to key,
                        "page" to page.toString(),
                    ),
                )) {
                    is EngineResult.Failure -> return result
                    is EngineResult.Success -> result.value.trim().takeIf(String::isNotEmpty)
                }
                if (evaluated == null) return EngineResult.Success(null)
                if (fieldName == "bookUrl" || fieldName == "coverUrl") {
                    return runCatching { WebUrlResolver.resolve(response.finalUrl, evaluated) }.fold(
                        onSuccess = { EngineResult.Success(it) },
                        onFailure = { unsupported(source, "$fieldName script result must be a credential-free HTTP(S) URL") },
                    )
                }
                return EngineResult.Success(evaluated)
            }
            return try {
                EngineResult.Success(field(
                    source,
                    rules,
                    fieldName,
                    SourceDocument(response.finalUrl, itemHtml, response.contentType, key, page),
                    expressionOverride = trimmed,
                    legacyRegexMode = legacyRegexMode,
                ))
            } catch (failure: SearchParseFailure) {
                failure.failure
            } catch (failure: UnsupportedSearchRule) {
                unsupported(source, failure.message ?: "unsupported search field")
            }
        }

        val candidates = ArrayList<BookCandidate>()
        for (item in items) {
            val name = when (val result = fieldValue("name", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            } ?: continue
            val bookUrl = when (val result = fieldValue("bookUrl", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }?.takeIf(String::isNotBlank) ?: continue
            val author = when (val result = fieldValue("author", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val cover = when (val result = fieldValue("coverUrl", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val latest = when (val result = fieldValue("lastChapter", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val intro = when (val result = fieldValue("intro", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            val bookStatus = when (val result = fieldValue("bookStatus", item)) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> result.value
            }
            candidates += BookCandidate(source.id, name, author, bookUrl, cover, latest, intro, bookStatus)
        }
        return EngineResult.Success(candidates.distinctBy { it.bookUrl })
    }

    private fun field(
        source: BookSource,
        rules: Map<String, String>,
        key: String,
        itemDocument: SourceDocument,
        expressionOverride: String? = null,
        legacyRegexMode: Boolean = false,
    ): String? {
        val expression = expressionOverride?.takeIf(String::isNotBlank)
            ?: rules[key]?.takeIf(String::isNotBlank)
            ?: return null
        if (expression.contains("@js:", ignoreCase = true) || expression.contains("{{")) {
            throw UnsupportedSearchRule(source.id, key)
        }
        val extractionRule = when (key) {
            "bookUrl" -> LegadoRuleReplacement.transformExpression(expression) {
                it.replace(Regex("@href$", RegexOption.IGNORE_CASE), "@attr(href)")
            }
            "coverUrl" -> LegadoRuleReplacement.transformExpression(expression) {
                it.replace(Regex("@src$", RegexOption.IGNORE_CASE), "@attr(src)")
            }
            else -> expression
        }
        val extracted = if (legacyRegexMode) {
            val parsed = LegadoRuleReplacement.parse(extractionRule)
            val raw = when (val result = LegadoRegexElementEvaluator.evaluateScalar(itemDocument.body, parsed.expression)) {
                is EngineResult.Failure -> throw SearchParseFailure(result)
                is EngineResult.Success -> (result.value.getOrNull(1) ?: result.value.firstOrNull())
                    ?.trim()?.takeIf(String::isNotEmpty)
            }
            if (raw == null) null else when (val replaced = parsed.apply(listOf(raw))) {
                is EngineResult.Failure -> throw SearchParseFailure(replaced)
                is EngineResult.Success -> replaced.value.firstOrNull()?.trim()?.takeIf(String::isNotEmpty)
            }
        } else {
            when (val result = LegadoRuleExpressionDispatcher.evaluate(itemDocument, extractionRule)) {
                is EngineResult.Failure -> throw SearchParseFailure(result)
                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(extractionRule, result.value)
                    ?.trim()?.takeIf(String::isNotEmpty)
            }
        }
        return extracted?.let { value ->
            if (key == "bookUrl" || key == "coverUrl") {
                return try {
                    WebUrlResolver.resolve(itemDocument.url, value)
                } catch (_: Exception) {
                    throw UnsupportedSearchRule(source.id, "$key must resolve to an HTTP(S) URL")
                }
            }
            value
        }
    }

    /**
     * bookList is a collection of element nodes, not a scalar field. Apply
     * @html to every top-level branch so && / || / %% never mix serialized
     * elements with text values before each card is parsed independently.
     */
    private fun normalizeHtmlOutput(rule: String): String {
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(rule)
        if (split.operator == null) {
            return if (hasHtmlOutputSuffix(rule)) rule else "$rule@html"
        }
        return split.rules.joinToString(split.operator) { normalizeHtmlOutput(it) }
    }

    private fun hasHtmlOutputSuffix(rule: String): Boolean =
        rule.trim().endsWith("@html", ignoreCase = true)

    private fun hasNonHtmlOutputSuffix(rule: String): Boolean {
        val root = LegadoRuleReplacement.parse(rule).expression.trim()
        if (root.startsWith(":") || root.startsWith("@regex:", ignoreCase = true)) return false
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(root)
        if (split.operator != null) return split.rules.any(::hasNonHtmlOutputSuffix)
        val value = root
        return listOf("@text", "@ownText", "@href", "@src", "@tag")
            .any { value.endsWith(it, ignoreCase = true) } ||
            Regex("@attr\\([^)]*\\)$", RegexOption.IGNORE_CASE).containsMatchIn(value)
    }

    private fun unsupported(source: BookSource, detail: String) =
        EngineResult.Failure(
            EngineErrorCode.UNSUPPORTED_RULE,
            "Source '${source.id}' search/explore rule requires unsupported feature: $detail",
        )

    private class UnsupportedSearchRule(sourceId: String, field: String) :
        RuntimeException("Source '$sourceId' search field '$field' requires JavaScript/interpolation")

    private class SearchParseFailure(val failure: EngineResult.Failure) : RuntimeException(failure.message)
}
