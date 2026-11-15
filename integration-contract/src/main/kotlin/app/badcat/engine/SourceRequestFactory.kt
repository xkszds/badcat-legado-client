package app.badcat.engine

import com.google.gson.JsonParser
import java.net.URI

/**
 * Converts source-level request settings into the transport contract.
 *
 * This handles static source headers, per-request overrides and the cookie policy
 * flag. The resolved URL overload also carries static HTTP options supported by
 * the transport. Script-generated options remain outside this factory.
 */
object SourceRequestFactory {

    fun create(
        source: BookSource,
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): EngineResult<SourceHttpRequest> = createRequest(
        source = source,
        url = url,
        method = method,
        headers = headers,
        body = body,
    )

    fun create(
        source: BookSource,
        resolved: LegadoResolvedUrl,
    ): EngineResult<SourceHttpRequest> = createRequest(
        source = source,
        url = resolved.url,
        method = resolved.method,
        headers = resolved.headers,
        body = resolved.body,
        timeoutMillis = resolved.timeoutMillis,
        followRedirects = resolved.followRedirects,
        retryCount = resolved.retryCount,
        responseCharset = resolved.charset,
    )

    private fun createRequest(
        source: BookSource,
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
        timeoutMillis: Long? = null,
        followRedirects: Boolean? = null,
        retryCount: Int = 0,
        responseCharset: String? = null,
    ): EngineResult<SourceHttpRequest> {
        if (source.id.isBlank() || url.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source identity and request URL must not be blank",
            )
        }

        val parsedUrl = runCatching { URI(url) }.getOrNull()
        if (parsedUrl == null ||
            parsedUrl.scheme?.lowercase() !in setOf("http", "https") ||
            parsedUrl.host.isNullOrBlank() ||
            parsedUrl.rawUserInfo != null
        ) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Request URL must be an absolute HTTP(S) URL without embedded credentials",
            )
        }

        val normalizedMethod = method.uppercase()
        if (normalizedMethod !in SUPPORTED_METHODS) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "HTTP method '$normalizedMethod' is not supported by the source transport",
            )
        }

        val sourceHeaders = parseHeaders(source.header)
            ?: return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source header must be a JSON object with string values",
            )

        val merged = LinkedHashMap<String, String>()
        sourceHeaders.forEach { (name, value) -> merged[name] = value }
        headers.forEach { (name, value) ->
            val existing = merged.keys.firstOrNull { it.equals(name, ignoreCase = true) }
            if (existing != null) merged.remove(existing)
            merged[name] = value
        }
        if (merged.any { (name, value) -> !isValidHeader(name, value) }) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Request headers contain an invalid name or control character",
            )
        }

        return EngineResult.Success(
            SourceHttpRequest(
                sourceId = source.id,
                url = url,
                method = normalizedMethod,
                headers = merged,
                body = body,
                useCookieJar = source.enabledCookieJar != false,
                timeoutMillis = timeoutMillis,
                followRedirects = followRedirects,
                retryCount = retryCount,
                responseCharset = responseCharset,
            ),
        )
    }

    private val SUPPORTED_METHODS = setOf(
        "GET", "HEAD", "POST", "PUT", "PATCH", "PROPPATCH", "REPORT",
    )

    private fun isValidHeader(name: String, value: String): Boolean {
        if (name.isEmpty() || name.any { it !in HEADER_NAME_CHARS }) return false
        return value.none { char ->
            char == '\r' || char == '\n' ||
                (char.code < 0x20 && char != '\t') || char.code == 0x7f
        }
    }

    private val HEADER_NAME_CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$%&'*+-.^_`|~"

    private fun parseHeaders(raw: String?): Map<String, String>? {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JsonParser.parseString(raw)
            require(root.isJsonObject)
            buildMap {
                root.asJsonObject.entrySet().forEach { (name, value) ->
                    require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                    put(name, value.asString)
                }
            }
        }.getOrNull()
    }
}
