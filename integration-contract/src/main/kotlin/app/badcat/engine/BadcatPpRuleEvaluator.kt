package app.badcat.engine

import org.jsoup.Jsoup
import org.seimicrawler.xpath.JXDocument

data class BadcatVideoLink(val title: String, val url: String)

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

    /**
     * Extracts the href/text pair from nodes selected by a recovered video
     * path/episode rule. The caller supplies the rule explicitly because the
     * original runtime's grouping/order semantics are not fully recovered.
     */
    fun extractVideoLinks(
        document: SourceDocument,
        rule: BadcatPpRule,
    ): EngineResult<List<BadcatVideoLink>> {
        val selected = when (val result = selectElements(document, rule)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return try {
            val links = selected.flatMap { fragment ->
                val root = Jsoup.parseBodyFragment(fragment, document.url).body()
                root.select("a[href]").mapNotNull { element ->
                    val href = element.attr("href").trim()
                    val resolved = runCatching {
                        val base = java.net.URI(document.url)
                        val target = base.resolve(href).normalize()
                        target.takeIf {
                            (it.scheme.equals("http", ignoreCase = true) ||
                                it.scheme.equals("https", ignoreCase = true)) &&
                                !it.host.isNullOrBlank() &&
                                it.userInfo == null
                        }?.toASCIIString()
                    }.getOrNull() ?: return@mapNotNull null
                    // Some providers expose episode names only as tooltip/accessibility metadata.
                    val label = sequenceOf(
                        element.attr("title"),
                        element.attr("aria-label"),
                        element.text(),
                    ).map { it.trim() }.firstOrNull(String::isNotEmpty).orEmpty()
                    if (label.isBlank()) null else BadcatVideoLink(label, resolved)
                }
            }.distinctBy { it.url }
            EngineResult.Success(links)
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Badcat video link rule could not be evaluated",
                error,
            )
        }
    }
    /**
     * Extract comic page image URLs using the recovered original preference:
     * data-original first, then src. Only absolute HTTP(S) targets are returned.
     */
    fun extractComicImageUrls(
        document: SourceDocument,
        rule: BadcatPpRule,
    ): EngineResult<List<String>> {
        val selected = when (val result = selectElements(document, rule)) {
            is EngineResult.Failure -> return result
            is EngineResult.Success -> result.value
        }
        return try {
            val urls = selected.flatMap { fragment ->
                val root = Jsoup.parseBodyFragment(fragment, document.url).body()
                root.select("img[data-original], img[src]").mapNotNull { image ->
                    val raw = image.attr("data-original").trim()
                        .ifBlank { image.attr("src").trim() }
                    if (raw.isBlank()) return@mapNotNull null
                    runCatching {
                        val target = java.net.URI(document.url).resolve(raw).normalize()
                        target.takeIf {
                            (it.scheme.equals("http", true) || it.scheme.equals("https", true)) &&
                                !it.host.isNullOrBlank() && it.userInfo == null
                        }?.toASCIIString()
                    }.getOrNull()
                }
            }.distinct()
            EngineResult.Success(urls)
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Badcat comic image rule could not be evaluated",
                error,
            )
        }
    }

    /**
     * Applies the verified chapterIsReverse flag to an already-extracted
     * chapter/list sequence. This is deliberately separate from element
     * selection so callers cannot accidentally reverse comic pages or video
     * episodes whose ordering contract has not been recovered.
     */
    fun applyChapterListOrder(
        chapters: List<String>,
        rule: BadcatPpRule,
    ): List<String> = if (rule.chapterIsReverse) chapters.asReversed() else chapters


}
