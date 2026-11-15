package app.badcat.engine

import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Isolated port of Legado AnalyzeUrl.encodeParams and NetworkUtils encoded checks.
 * Upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9.
 */
object LegadoUrlParameterEncoder {
    private val querySafe = buildSet<Char> {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".forEach(::add)
        "!${'$'}%&()*+,/:;=?@[\\]^`{|}".forEach(::add)
    }

    private val queryAlreadyEncodedSafe = buildSet<Char> {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".forEach(::add)
        "!${'$'}&()*+,-./:;=?@[\\\\]^_`{|}~".forEach(::add)
    }

    private val formAlreadyEncodedSafe = buildSet<Char> {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".forEach(::add)
        "*-._".forEach(::add)
    }

    fun encodeQuery(params: String, charsetName: String? = null): String {
        val charset = resolveCharset(charsetName)
        if (charset != null) {
            if (isAlreadyEncoded(params, queryAlreadyEncodedSafe)) return params
            return percentEncode(params, charset)
        }
        return encodeFields(params, checkEncoded = false, charset = null)
    }
    fun encodeForm(params: String, charsetName: String? = null): String =
        encodeFields(params, charsetName.isNullOrEmpty(), resolveCharset(charsetName))

    data class PreparedRequest(val url: String, val body: String?)

    /** Applies AnalyzeUrl's method-dependent query/form preparation after headers merge. */
    fun prepareRequest(
        url: String,
        method: String,
        body: String?,
        headers: Map<String, String>,
        charsetName: String? = null,
    ): PreparedRequest {
        if (!method.equals("POST", ignoreCase = true)) {
            val fragmentAt = url.indexOf('#').let { if (it < 0) url.length else it }
            val queryAt = url.indexOf('?')
            if (queryAt < 0 || queryAt >= fragmentAt) return PreparedRequest(url, body)
            val query = url.substring(queryAt + 1, fragmentAt)
            val encoded = encodeQuery(query, charsetName)
            val prefix = url.substring(0, queryAt)
            val fragment = url.substring(fragmentAt)
            return PreparedRequest(
                url = if (encoded.isEmpty()) prefix + fragment else "$prefix?$encoded$fragment",
                body = body,
            )
        }
        val contentType = headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value
        val payload = body ?: return PreparedRequest(url, null)
        if (!contentType.isNullOrBlank() || isJson(payload) || payload.trimStart().startsWith("<")) {
            return PreparedRequest(url, payload)
        }
        return PreparedRequest(url, encodeForm(payload, charsetName))
    }

    private fun isJson(value: String): Boolean {
        val trimmed = value.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        return runCatching { com.google.gson.JsonParser.parseString(trimmed) }.isSuccess
    }

    /** Legacy EncoderUtils.escape, operating on UTF-16 code units like the upstream Kotlin loop. */
    fun escape(value: String): String = buildString {
        value.forEach { char ->
            val code = char.code
            if (code in 48..57 || code in 65..90 || code in 97..122) append(char)
            else {
                val prefix = when { code < 16 -> "%0"; code < 256 -> "%"; else -> "%u" }
                append(prefix).append(code.toString(16))
            }
        }
    }

    private fun encodeFields(params: String, checkEncoded: Boolean, charset: Charset?): String {
        val result = StringBuilder()
        val length = params.length
        var position = 0
        while (position <= length) {
            if (result.isNotEmpty()) result.append('&')
            val ampOffset = params.indexOf('&', position).takeIf { it >= 0 } ?: length
            val eqOffset = params.indexOf('=', position)
            val hasValue = eqOffset >= 0 && eqOffset <= ampOffset
            val keyEnd = if (hasValue) eqOffset else ampOffset
            result.append(encodePart(params.substring(position, keyEnd), checkEncoded, charset))
            if (hasValue) {
                result.append('=')
                result.append(encodePart(params.substring(eqOffset + 1, ampOffset), checkEncoded, charset))
            }
            position = ampOffset + 1
        }
        return result.toString()
    }

    private fun percentEncode(value: String, charset: Charset): String {
        val bytes = value.toByteArray(charset)
        return buildString(bytes.size) {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xff
                val char = unsigned.toChar()
                if (unsigned < 128 && char in querySafe) append(char)
                else {
                    append('%')
                    append(HEX[unsigned ushr 4])
                    append(HEX[unsigned and 0x0f])
                }
            }
        }
    }
    private fun encodePart(value: String, checkEncoded: Boolean, charset: Charset?): String = when {
        checkEncoded && isAlreadyEncoded(value, formAlreadyEncodedSafe) -> value
        charset == null -> escape(value)
        else -> URLEncoder.encode(value, charset.name())
    }

    private fun resolveCharset(charsetName: String?): Charset? = when {
        charsetName.isNullOrEmpty() -> Charsets.UTF_8
        charsetName == "escape" -> null
        else -> Charset.forName(charsetName)
    }

    private fun isAlreadyEncoded(value: String, safe: Set<Char>): Boolean {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char in safe) { index++; continue }
            if (char == '%' && index + 2 < value.length &&
                value[index + 1].isHexDigit() && value[index + 2].isHexDigit()) {
                index += 3
                continue
            }
            return false
        }
        return true
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

    private const val HEX = "0123456789ABCDEF"
}
