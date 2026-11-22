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
 * The HTTP gateway evaluates URL JavaScript before calling this URI-only resolver.
 * Any script/template marker that reaches this layer is rejected rather than sent literally.
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
                "Unresolved Legado URL script/template marker reached URI parsing; URL preprocessing did not complete",
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

            val options = if (optionJson == null) null else parseOptions(optionJson)
            // URI rejects raw Unicode and spaces in query values; Legado encodes them later.
            expanded = encodeQueryForUri(expanded, options?.string("charset"))
            val base = URI(baseUrl)
            require(
                base.scheme.equals("http", true) &&
                    !base.host.isNullOrBlank() ||
                    base.scheme.equals("https", true) && !base.host.isNullOrBlank(),
            ) { "Base URL must be an absolute HTTP(S) URL" }
            require(base.rawUserInfo == null) { "Base URL must not contain embedded credentials" }

            // Apply the shared fragment sanitizer as well as URI resolution. Source URLs
            // frequently contain raw Unicode or a second '#' inside a hash-router fragment.
            val resolvedUri = URI(WebUrlResolver.resolve(baseUrl, expanded))
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
                    else -> throw UnsupportedOperationException("Unsupported URL option method: $it")
                }
            } ?: "GET"
            val body = options?.get("body")?.let(::bodyValue)
            val timeout = options?.get("timeout")?.let(::positiveLong)
            val redirects = options?.get("followRedirects")?.let(::booleanValue)
            // Pinned AnalyzeUrl defaults to zero retries; only an explicit URL option enables retries.
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
            val js = options?.string("js")
            val bodyJs = options?.string("bodyJs")
            if (options?.has("webJs") == true || webView) {
                throw UnsupportedOperationException(
                    "URL options webJs and webView require the Legado WebView runtime",
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
                    js = js,
                    bodyJs = bodyJs,
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

    private fun parseOptions(raw: String): JsonObject {
        val parsed = runCatching { JsonParser.parseString(raw) }.getOrNull()
        if (parsed != null && parsed.isJsonObject) return parsed.asJsonObject

        // Many established Legado sources use the historical Python/JS-style
        // single-quoted option dictionary: {'method':'POST','body':'q={{key}}'}.
        // Convert only quoted-string delimiters, preserving escaped content,
        // then let Gson validate the resulting JSON structure and value types.
        val normalized = normalizeSingleQuotedObject(raw)
        return JsonParser.parseString(normalized).takeIf { it.isJsonObject }?.asJsonObject
            ?: throw IllegalArgumentException("URL options must be an object")
    }

    private fun normalizeSingleQuotedObject(raw: String): String {
        val output = StringBuilder(raw.length + 8)
        var inSingle = false
        var inDouble = false
        var escaped = false
        raw.forEach { char ->
            when {
                inSingle && escaped -> {
                    when (char) {
                        '\\' -> output.append("\\\\")
                        '\'' -> output.append('\'')
                        '"' -> output.append("\\\"")
                        else -> output.append("\\\\").append(char)
                    }
                    escaped = false
                }
                inSingle && char == '\\' -> escaped = true
                inSingle && char == '\'' -> {
                    output.append('"')
                    inSingle = false
                }
                inSingle && char == '"' -> output.append("\\\"")
                inSingle && char == '\n' -> output.append("\\n")
                inSingle && char == '\r' -> output.append("\\r")
                inSingle && char == '\t' -> output.append("\\t")
                inSingle -> output.append(char)
                inDouble && escaped -> {
                    output.append(char)
                    escaped = false
                }
                inDouble && char == '\\' -> {
                    output.append(char)
                    escaped = true
                }
                inDouble && char == '"' -> {
                    output.append(char)
                    inDouble = false
                }
                !inDouble && char == '\'' -> {
                    output.append('"')
                    inSingle = true
                }
                !inDouble && char == '"' -> {
                    output.append(char)
                    inDouble = true
                }
                else -> output.append(char)
            }
        }
        require(!inSingle && !escaped) { "Unterminated single-quoted URL option string" }
        return output.toString()
    }

    private fun encodeQueryForUri(rule: String, charsetName: String?): String {
        val fragmentAt = rule.indexOf('#').let { if (it < 0) rule.length else it }
        val queryAt = rule.indexOf('?')
        if (queryAt < 0 || queryAt >= fragmentAt) return rule
        val query = rule.substring(queryAt + 1, fragmentAt)
        val encoded = LegadoUrlParameterEncoder.encodeQuery(query, charsetName)
        return rule.substring(0, queryAt) +
            if (encoded.isEmpty()) rule.substring(fragmentAt) else "?$encoded" + rule.substring(fragmentAt)
    }
    private fun replacePageTokens(rule: String, page: Int?): String {
        if (page == null) return rule
        return PAGE_TOKEN.replace(rule) { match ->
            val values = match.groupValues[1].split(',')
            values[(page - 1).coerceAtMost(values.lastIndex)].trim()
        }
    }

    private fun containsScript(value: String): Boolean =
        Regex("@js\\s*:|<js>", RegexOption.IGNORE_CASE).containsMatchIn(value) ||
            value.contains("{{")

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
    val js: String? = null,
    val bodyJs: String? = null,
)
