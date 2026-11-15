package app.badcat.engine

/**
 * Transport boundary for the Legado-compatible source runtime.
 *
 * Implementations must apply source headers, cookie-jar policy, redirects and
 * response decoding consistently. This interface intentionally does not perform
 * HTTP itself; the Android host supplies the concrete client.
 */
interface SourceRequestClient {
    suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse>
}

data class SourceHttpRequest(
    val sourceId: String,
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val useCookieJar: Boolean = true,
)

data class SourceHttpResponse(
    val statusCode: Int,
    val finalUrl: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String,
    val contentType: String? = null,
)

/** The five rule stages are kept distinct to prevent accidental rule cross-use. */
enum class SourceRuleStage {
    EXPLORE,
    SEARCH,
    BOOK_INFO,
    TOC,
    CONTENT,
}

/**
 * Parser boundary for upstream Legado rule objects.
 *
 * The concrete adapter must dispatch to the matching upstream rule model and
 * selector implementation (CSS/JSONPath/XPath/regex as configured). A raw JSON
 * rule string is not executable until that adapter exists.
 */
interface LegadoRuleRuntime {
    suspend fun evaluate(
        stage: SourceRuleStage,
        ruleJson: String,
        source: BookSource,
        document: SourceDocument,
    ): EngineResult<List<Map<String, String>>>
}

data class SourceDocument(
    val url: String,
    val body: String,
    val contentType: String? = null,
)

/** JavaScript execution is separate from document selectors and HTTP transport. */
interface SourceScriptRuntime {
    suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String> = emptyMap(),
    ): EngineResult<String>
}
