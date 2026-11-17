package app.badcat.engine

import java.net.URI

/** Resolves untrusted source links while allowing only credential-free HTTP(S) destinations. */
object WebUrlResolver {
    fun resolve(baseUrl: String, value: String): String {
        require(value.isNotBlank()) { "URL reference must not be blank" }
        val base = URI(baseUrl)
        require(isWebUri(base) && base.userInfo == null) { "Base URL must be credential-free HTTP(S)" }
        val trimmedValue = value.trim()
        val optionsSeparator = Regex(",\\s*(?=\\{)").findAll(trimmedValue).lastOrNull()
        val referenceText = optionsSeparator?.let { trimmedValue.substring(0, it.range.first).trim() } ?: trimmedValue
        val optionsSuffix = optionsSeparator?.let { trimmedValue.substring(it.range.first) }.orEmpty()
        val reference = URI(referenceText)
        val resolved = resolveReference(base, reference).normalize()
        require(isWebUri(resolved) && resolved.userInfo == null) {
            "Resolved URL must be credential-free HTTP(S)"
        }
        return resolved.toASCIIString() + optionsSuffix
    }

    /**
     * java.net.URI.resolve treats a query-only reference as a sibling path on
     * some JDKs. Web pagination links such as ?page=2 must retain the full
     * current path, replacing only its query component.
     */
    private fun resolveReference(base: URI, reference: URI): URI {
        if (reference.scheme == null && reference.rawAuthority == null &&
            reference.rawPath.isNullOrEmpty() &&
            (reference.rawQuery != null || reference.rawFragment != null)
        ) {
            val prefix = buildString {
                append(base.scheme)
                append("://")
                append(base.rawAuthority)
                append(base.rawPath.orEmpty())
                if (reference.rawQuery != null) {
                    append('?')
                    append(reference.rawQuery)
                } else if (base.rawQuery != null) {
                    append('?')
                    append(base.rawQuery)
                }
                if (reference.rawFragment != null) {
                    append('#')
                    append(reference.rawFragment)
                }
            }
            return URI(prefix)
        }
        return base.resolve(reference)
    }

    private fun isWebUri(uri: URI): Boolean =
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
}
