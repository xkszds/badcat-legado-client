package app.badcat.engine

import java.net.URI

/** Resolves untrusted source links while allowing only credential-free HTTP(S) destinations. */
object WebUrlResolver {
    fun resolve(baseUrl: String, value: String): String {
        val base = URI(baseUrl)
        require(isWebUri(base) && base.userInfo == null) { "Base URL must be credential-free HTTP(S)" }
        val resolved = base.resolve(value.trim()).normalize()
        require(isWebUri(resolved) && resolved.userInfo == null) {
            "Resolved URL must be credential-free HTTP(S)"
        }
        return resolved.toASCIIString()
    }

    private fun isWebUri(uri: URI): Boolean =
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
}
