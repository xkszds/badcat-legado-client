package app.badcat.client.runtime

/**
 * Legado CookieStore JavaScript surface. Values are name=value maps scoped to
 * the effective registrable domain; path and Set-Cookie attributes are not kept.
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
    private val cookieKeyRemover: (String, String) -> Unit = { _, _ ->
        throw UnsupportedOperationException("cookie.removeCookie(url, key) is not connected to the source cookie jar")
    },
) {
    fun getCookie(url: String): String = cookieReader(url)

    fun getKey(url: String, key: String): String =
        parseLegadoCookieMap(cookieReader(url))[key].orEmpty()

    fun setCookie(url: String, cookie: String) {
        cookieWriter(url, cookie)
    }

    fun replaceCookie(url: String, cookie: String) {
        cookieReplacer(url, cookie)
    }

    fun removeCookie(url: String) {
        cookieRemover(url)
    }

    fun removeCookie(url: String, key: String) {
        cookieKeyRemover(url, key)
    }
}
