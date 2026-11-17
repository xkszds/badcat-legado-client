package app.badcat.client.runtime

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

/**
 * Explicitly allow-listed subset of Legado's `java` helper for source scripts.
 *
 * This object is deliberately not a general Java bridge: no class lookup,
 * reflection, filesystem, process, Android, network, Cookie or WebView access.
 * The small key/value store is isolated per book source, size-bounded, and backed by the host's storage adapter.
 */
class RhinoSafeSourceJavaExtensions(
    private val sourceId: String,
    private val store: SourceScriptKeyValueStore,
    private val cookieReader: (String) -> String = { "" },
) {

    fun getString(key: String): String {
        requireValidKey(key)
        return store.get(sourceId, key).orEmpty()
    }

    fun putString(key: String, value: String): String {
        requireValidKey(key)
        require(value.length <= MAX_VALUE_CHARS) { "Legado java.putString value exceeds the size limit" }
        store.put(sourceId, key, value)
        return value
    }

    /** Legado-compatible, source-scoped cookie lookup. */
    fun getCookie(tag: String): String = cookieReader(tag)

    fun getCookie(tag: String, key: String?): String {
        if (key == null) return cookieReader(tag)
        return cookieReader(tag).split(';').asSequence()
            .map(String::trim)
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) null else pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
            }
            .firstOrNull { it.first == key }
            ?.second.orEmpty()
    }

    fun log(value: Any?): String = value?.toString().orEmpty()

    fun md5Encode(value: String): String = digest("MD5", value)
    fun md5Encode16(value: String): String = md5Encode(value).substring(8, 24)

    fun base64Encode(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    fun base64Decode(value: String): String =
        String(Base64.getDecoder().decode(value), Charsets.UTF_8)

    fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    fun urlDecode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun digest(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun requireValidKey(key: String) {
        require(key.isNotBlank() && key.length <= MAX_KEY_CHARS) {
            "Legado java storage key is blank or too long"
        }
    }

    companion object {
        private const val MAX_KEY_CHARS = 256
        private const val MAX_VALUE_CHARS = 16 * 1024
    }
}
