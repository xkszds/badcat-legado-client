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
        // Keep Legado's {{key}}/{{page}} placeholders intact until the
        // request stage has the book name and page context needed to expand them.
        val templates = linkedMapOf<String, String>()
        val templateSafeReference = Regex("""\{\{.*?}}""").replace(referenceText) { match ->
            var token: String
            do {
                token = "__BADCAT_LEGADO_TEMPLATE_" + templates.size + "__"
            } while (referenceText.contains(token))
            templates[token] = match.value
            token
        }
        val reference = URI(encodeInvalidFragmentCharacters(templateSafeReference))
        val resolved = resolveReference(base, reference).normalize()
        require(isWebUri(resolved) && resolved.userInfo == null) {
            "Resolved URL must be credential-free HTTP(S)"
        }
        var resolvedText = resolved.toASCIIString()
        templates.forEach { (token, template) -> resolvedText = resolvedText.replace(token, template) }
        return resolvedText + optionsSuffix
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

    /** Encode invalid characters inside fragments while preserving valid percent escapes. */
    private fun encodeInvalidFragmentCharacters(value: String): String {
        val marker = value.indexOf('#')
        if (marker < 0) return value
        val out = StringBuilder(value.substring(0, marker + 1))
        val fragment = value.substring(marker + 1)
        var index = 0
        while (index < fragment.length) {
            val ch = fragment[index]
            if (ch == '%' && index + 2 < fragment.length &&
                fragment[index + 1].digitToIntOrNull(16) != null &&
                fragment[index + 2].digitToIntOrNull(16) != null
            ) {
                out.append(fragment, index, index + 3)
                index += 3
            } else if (ch == '#' || ch.isWhitespace() || ch.isISOControl()) {
                ch.toString().toByteArray(Charsets.UTF_8).forEach { byte ->
                    out.append("%%%02X".format(byte.toInt() and 0xff))
                }
                index++
            } else {
                out.append(ch)
                index++
            }
        }
        return out.toString()
    }

    private fun isWebUri(uri: URI): Boolean =
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
}
