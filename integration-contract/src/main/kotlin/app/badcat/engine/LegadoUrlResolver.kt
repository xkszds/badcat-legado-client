package app.badcat.engine

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI

/**
 * Resolves the deterministic, non-script subset of Legado AnalyzeUrl syntax.
 *
 * Upstream reference: LegadoTeam/legado AnalyzeUrl.kt at
 * 330e606c4d57e08adc056ff8ab7a4132c784da9.
 *
 * JavaScript-bearing forms are rejected explicitly until the pinned Rhino runtime
 * and its source bindings are integrated; they are never treated as literal URLs.
 */
object LegadoUrlResolver {

    fun resolve(
        baseUrl: String,
        ruleUrl: String,
        page: Int? = null,
    ): EngineResult<LegadoResolvedUrl> {
        if (baseUrl.isBlank() || ruleUrl.isBlank()) {
            return failure("Base URL and rule URL must not be blank")
        }
        if (page != null && page < 1) {
            return failure("Page number must be 1 or greater")
        }
        if (containsScript(ruleUrl)) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "This URL uses Legado JavaScript syntax; the Rhino runtime is not integrated yet",
            )
        }

        return runCatching {
            var expanded = replacePageTokens(ruleUrl, page)
            val optionStart = OPTION_SEPARATOR.find(expanded)
            val optionJson = optionStart?.let { match ->
                expanded.substring(match.range.last + 1).trim()
            }
            if (optionStart != null) {
                expanded = expanded.substring(0, optionStart.range.first).trim()
            }

            val options = if (optionJson == null) null else {
                JsonParser.parseString(optionJson).takeIf { it.isJsonObject }?.asJsonObject
                    ?: throw IllegalArgumentException("URL options must be a JSON object")
            }
            val base = URI(baseUrl)
            require(
                base.scheme.equals("http", true) &&
                    !base.host.isNullOrBlank() ||
                    base.scheme.equals("https", true) && !base.host.isNullOrBlank(),
            ) { "Base URL must be an absolute HTTP(S) URL" }
            require(base.rawUserInfo == null) { "Base URL must not contain embedded credentials" }

            val resolvedUri = base.resolve(expanded)
            require(
                (resolvedUri.scheme.equals("http", true) ||
                    resolvedUri.scheme.equals("https", true)) &&
                    !resolvedUri.host.isNullOrBlank() &&
                    resolvedUri.rawUserInfo == null,
            ) { "Resolved URL must be an absolute HTTP(S) URL without embedded credentials" }
            val absolute = resolvedUri.toString()

            val headers = parseHeaders(options?.get("headers"))
            val method = options?.string("method")?.uppercase()?.let {
                when (it) {
                    "GET", "HEAD", "POST", "PUT", "PATCH", "PROPPATCH", "REPORT" -> it
                    else -> throw IllegalArgumentException("Unsupported URL option method: $it")
                }
            } ?: "GET"
            val body = options?.get("body")?.let(::bodyValue)
            val timeout = options?.get("timeout")?.let(::positiveLong)
            val redirects = options?.get("followRedirects")?.let(::booleanValue)
            val retry = options?.get("retry")?.let(::nonNegativeInt) ?: 0
            require(retry <= 5) { "URL option retry must be between 0 and 5" }
            val charset = options?.string("charset")
            if (options?.has("dnsIp") == true || options?.has("resolveIp") == true ||
                options?.has("type") == true
            ) {
                throw UnsupportedOperationException(
                    "URL options dnsIp, resolveIp and type are not implemented by the current HTTP transport",
                )
            }
            val webView = options?.get("webView")?.let(::booleanValue) ?: false

            if (options?.has("js") == true || options?.has("bodyJs") == true || webView) {
                throw UnsupportedOperationException(
                    "URL options js, bodyJs and webView require the Legado Rhino/WebView runtime",
                )
            }
            if (charset != null) {
                if (charset.equals("escape", true)) {
                    throw UnsupportedOperationException(
                        "Legado escape response decoding is not implemented by the current transport",
                    )
                }
                java.nio.charset.Charset.forName(charset)
            }

            EngineResult.Success(
                LegadoResolvedUrl(
                    url = absolute,
                    method = method,
                    headers = headers,
                    body = body,
                    timeoutMillis = timeout,
                    followRedirects = redirects,
                    retryCount = retry,
                    charset = charset,
                ),
            )
        }.getOrElse { error ->
            if (error is UnsupportedOperationException) {
                EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, error.message ?: "Unsupported URL option", error)
            } else {
                EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, error.message ?: "Invalid Legado URL rule", error)
            }
        }
    }

    private fun replacePageTokens(rule: String, page: Int?): String {
        if (page == null) return rule
        return PAGE_TOKEN.replace(rule) { match ->
            val values = match.groupValues[1].split(',')
            values[(page - 1).coerceAtMost(values.lastIndex)].trim()
        }
    }

    private fun containsScript(value: String): Boolean =
        value.contains("@js", ignoreCase = true) ||
            (value.contains("{{") && value.contains("}}"))

    private fun parseHeaders(value: JsonElement?): Map<String, String> {
        if (value == null || value.isJsonNull) return emptyMap()
        val objectValue = when {
            value.isJsonObject -> value.asJsonObject
            value.isJsonPrimitive && value.asJsonPrimitive.isString ->
                JsonParser.parseString(value.asString).takeIf { it.isJsonObject }?.asJsonObject
                    ?: throw IllegalArgumentException("URL option headers string must contain a JSON object")
            else -> throw IllegalArgumentException("URL option headers must be an object or JSON object string")
        }
        return buildMap {
            objectValue.entrySet().forEach { (name, headerValue) ->
                require(headerValue.isJsonPrimitive) { "Header values must be scalar" }
                put(name, headerValue.asString)
            }
        }
    }

    private fun bodyValue(value: JsonElement): String? = when {
        value.isJsonNull -> null
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString
        else -> value.toString()
    }

    private fun positiveLong(value: JsonElement): Long {
        val number = scalarString(value).toLongOrNull()
            ?: throw IllegalArgumentException("URL option timeout must be an integer in milliseconds")
        require(number > 0) { "URL option timeout must be positive" }
        return number
    }

    private fun nonNegativeInt(value: JsonElement): Int {
        val number = scalarString(value).toIntOrNull()
            ?: throw IllegalArgumentException("URL option retry must be an integer")
        require(number >= 0) { "URL option retry must not be negative" }
        return number
    }

    private fun booleanValue(value: JsonElement): Boolean = when {
        value.isJsonNull -> false
        value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> value.asBoolean
        value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> when (value.asInt) {
            0 -> false
            1 -> true
            else -> throw IllegalArgumentException("Boolean URL option must be 0 or 1")
        }
        value.isJsonPrimitive && value.asJsonPrimitive.isString -> when (value.asString.trim().lowercase()) {
            "true", "1" -> true
            "false", "0", "" -> false
            else -> throw IllegalArgumentException("Invalid boolean URL option")
        }
        else -> throw IllegalArgumentException("Invalid boolean URL option")
    }

    private fun scalarString(value: JsonElement): String {
        require(value.isJsonPrimitive) { "URL option must be a scalar" }
        return value.asString
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeUnless { it.isJsonNull }?.let(::scalarString)

    private fun failure(message: String) =
        EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, message)

    private val PAGE_TOKEN = Regex("<([^<>]*)>")
    private val OPTION_SEPARATOR = Regex("\\s*,\\s*(?=\\{)")
}

data class LegadoResolvedUrl(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeoutMillis: Long? = null,
    val followRedirects: Boolean? = null,
    val retryCount: Int = 0,
    val charset: String? = null,
)
