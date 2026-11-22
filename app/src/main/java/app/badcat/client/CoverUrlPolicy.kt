package app.badcat.client

import java.net.URL

/** Restricts cover-image requests and redirects to credential-free HTTP(S) URLs. */
internal object CoverUrlPolicy {
    fun isSafe(url: URL): Boolean {
        val scheme = url.protocol.lowercase()
        return (scheme == "http" || scheme == "https") &&
            url.host.isNotBlank() &&
            url.userInfo == null
    }

    fun resolveRedirect(from: URL, location: String): URL? {
        if (location.isBlank() || !isSafe(from)) return null
        val target = runCatching { URL(from, location.trim()) }.getOrNull() ?: return null
        if (!isSafe(target)) return null
        if (from.protocol.equals("https", ignoreCase = true) &&
            !target.protocol.equals("https", ignoreCase = true)
        ) return null
        return target
    }
}
