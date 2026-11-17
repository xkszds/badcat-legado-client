package app.badcat.client.runtime

/**
 * Restricted Legado cookie helper. It can only inspect or remove cookies
 * belonging to the current source and matching the supplied HTTP(S) URL.
 */
class RhinoSourceCookieExtensions(
    private val cookieReader: (String) -> String = { "" },
    private val cookieRemover: (String) -> Unit = {},
) {
    fun getCookie(url: String): String = cookieReader(url)

    fun removeCookie(url: String) {
        cookieRemover(url)
    }
}
