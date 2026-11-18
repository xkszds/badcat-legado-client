package app.badcat.client.runtime

/**
 * Source-scoped Legado cookie helper. All operations are delegated to the
 * source's durable cookie jar and retain OkHttp domain/path/secure matching.
 */
class RhinoSourceCookieExtensions(
    private val cookieReader: (String) -> String = { "" },
    private val cookieRemover: (String) -> Unit = {},
    private val cookieWriter: (String, String) -> Unit = { _, _ ->
        throw UnsupportedOperationException("cookie.setCookie is not connected to the source cookie jar")
    },
    private val cookieReplacer: (String, String) -> Unit = { _, _ ->
        throw UnsupportedOperationException("cookie.replaceCookie is not connected to the source cookie jar")
    },
) {
    fun getCookie(url: String): String = cookieReader(url)

    fun getKey(url: String, key: String): String =
        cookieReader(url).split(';').asSequence()
            .map(String::trim)
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) null else pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
            }
            .firstOrNull { it.first == key }
            ?.second.orEmpty()

    fun setCookie(url: String, cookie: String) {
        cookieWriter(url, cookie)
    }

    fun replaceCookie(url: String, cookie: String) {
        cookieReplacer(url, cookie)
    }

    fun removeCookie(url: String) {
        cookieRemover(url)
    }
}
