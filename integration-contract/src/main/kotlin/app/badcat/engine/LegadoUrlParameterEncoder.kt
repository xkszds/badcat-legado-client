package app.badcat.engine

import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Reproduces the query/form parameter encoding branch used by Legado AnalyzeUrl.
 *
 * This is deliberately independent of URL resolution and JavaScript evaluation.
 * Upstream reference: LegadoTeam/legado AnalyzeUrl.kt and NetworkUtils.kt.
 */
object LegadoUrlParameterEncoder {

    private val querySafe = buildSet<Char> {
        addAll("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        addAll("!$%&()*+,/:;=?@[\\]^\u0060{|}")
    }

    private val queryAlreadyEncodedSafe = buildSet<Char> {
        addAll("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
        addAll("!$&()*+,-./:;=?@[\\\\]^_\u0060{|}~")
    }

    private val formAlreadyEncodedSafe = buildSet<Char> {
        addAll("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
        addAll("*-._")
    }

    /**
     * Encodes a complete query string. Legado first preserves strings accepted by
     * NetworkUtils.encodedQuery; otherwise it applies its RFC3986 PercentCodec.
     */
    fun encodeQuery(params: String, charsetName: String? = null): String {
        if (charsetName == "escape") return encodeForm(params, charsetName)
        val charset = resolveCharset(charsetName) ?: Charsets.UTF_8
        if (isAlreadyEncoded(params, queryAlreadyEncodedSafe)) return params
        return percentEncode(params, charset, querySafe)
    }

    /**
     * Encodes form fields using Legado's segment-by-segment algorithm.
     * Only the first '=' in each '&'-delimited segment separates key and value.
     */
    fun encodeForm(params: String, charsetName: String? = null): String {
        val checkEncoded = charsetName.isNullOrEmpty()
        val charset = resolveCharset(charsetName)
        val result = StringBuilder()
        var position = 0
        while (position <= params.length) {
            if (result.isNotEmpty()) result.append('&')
            val ampOffset = params.indexOf('&', position).takeIf { it >= 0 } ?: params.length
            val eqOffset = params.indexOf('=', position)
            val hasValue = eqOffset >= 0 && eqOffset <= ampOffset
            val keyEnd = if (hasValue) eqOffset else ampOffset
            result.append(encodeFormPart(params.substring(position, keyEnd), checkEncoded, charset))
            if (hasValue) {
                result.append('=')
                result.append(
                    encodeFormPart(params.substring(eqOffset + 1, ampOffset), checkEncoded, charset),
                )
            }
            position = ampOffset + 1
        }
        return result.toString()
    }

    /** Legado's legacy JavaScript-style escape(char) encoding, not URI encoding. */
    fun escape(value: String): String = buildString {
        value.forEach { char ->
            val code = char.code
            if (code in 48..57 || code in 65..90 || code in 97..122) {
                append(char)
            } else {
                val prefix = when {
                    code < 16 -> "%0"
                    code < 256 -> "%"
                    else -> "%u"
                }
                append(prefix).append(code.toString(16))
            }
        }
    }

    private fun encodeFormPart(value: String, checkEncoded: Boolean, charset: Charset?): String =
        when {
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
            if (char in safe) {
                index++
                continue
            }
            if (char == '%' && index + 2 < value.length &&
                value[index + 1].isHexDigit() && value[index + 2].isHexDigit()
            ) {
                index += 3
                continue
            }
            return false
        }
        return true
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'A'..'F' || this in 'a'..'f'

    private fun percentEncode(value: String, charset: Charset, safe: Set<Char>): String {
        val bytes = value.toByteArray(charset)
        return buildString(bytes.size) {
            bytes.forEach { byte ->
                val unsigned = byte.toInt() and 0xff
                val char = unsigned.toChar()
                if (unsigned < 128 && char in safe) {
                    append(char)
                } else {
                    append('%')
                    append(HEX[unsigned ushr 4])
                    append(HEX[unsigned and 0x0f])
                }
            }
        }
    }

    private const val HEX = "0123456789ABCDEF"
}
