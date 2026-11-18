package app.badcat.engine

/**
 * Transport boundary for the Legado-compatible source runtime.
 *
 * Implementations apply source headers, cookie-jar policy, redirects and response
 * decoding. [OkHttpSourceRequestClient] is the current concrete transport; the
 * interface remains injectable for deterministic tests and future host adapters.
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
    val timeoutMillis: Long? = null,
    val followRedirects: Boolean? = null,
    val retryCount: Int = 0,
    val responseCharset: String? = null,
)

data class SourceHttpResponse(
    val statusCode: Int,
    val finalUrl: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String,
    val contentType: String? = null,
    /** Actual HTTP reason phrase returned by the server (never synthesized). */
    val statusMessage: String = "",
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
    /** Current search/explore keyword for embedded AnalyzeRule expressions. */
    val key: String = "",
    /** Current pagination page, one-based. */
    val page: Int = 1,
)

/** JavaScript execution is separate from document selectors and HTTP transport. */
interface SourceScriptRuntime {
    suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String> = emptyMap(),
    ): EngineResult<String>

    /** Persistent per-source variables used by AnalyzeRule's @get:{key}. */
    suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
        EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Persistent source variables are unavailable")

    /** Persistent per-source variables used by AnalyzeRule's @put:{...}. */
    suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> =
        EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Persistent source variables are unavailable")
}

/**
 * Fail-closed implementation for hosts that have not integrated the pinned
 * Legado Rhino runtime. It deliberately never treats script as plain text and
 * never evaluates it with a permissive default JavaScript engine.
 */
object UnsupportedSourceScriptRuntime : SourceScriptRuntime {
    override suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String>,
    ): EngineResult<String> = EngineResult.Failure(
        EngineErrorCode.UNSUPPORTED_RULE,
        "JavaScript rules are disabled until the pinned Legado Rhino runtime is integrated",
    )
}
