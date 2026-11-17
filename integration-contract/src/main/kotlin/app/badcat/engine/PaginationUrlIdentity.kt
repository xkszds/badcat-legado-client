package app.badcat.engine

import java.net.URI

/**
 * Canonical identity for pagination loop detection. Fragments are client-side
 * only; HTTP scheme/host casing and explicit default ports do not identify a
 * different server resource. Query order and path spelling remain untouched.
 */
internal fun paginationUrlIdentity(url: String): String {
    val withoutFragment = url.substringBefore('#')
    val uri = runCatching { URI(withoutFragment) }.getOrNull() ?: return withoutFragment
    val scheme = uri.scheme?.lowercase() ?: return withoutFragment
    if (scheme != "http" && scheme != "https") return withoutFragment
    val host = uri.host?.takeIf(String::isNotBlank) ?: return withoutFragment
    if (uri.rawUserInfo != null) return withoutFragment
    val port = when {
        scheme == "http" && uri.port == 80 -> ""
        scheme == "https" && uri.port == 443 -> ""
        uri.port >= 0 -> ":\u0024{uri.port}"
        else -> ""
    }
    return "$scheme://\u0024{host.lowercase()}\u0024port\u0024{uri.rawPath.orEmpty()}\u0024{uri.rawQuery?.let { "?\u0024it" }.orEmpty()}"
}
