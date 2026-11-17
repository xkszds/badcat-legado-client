package app.badcat.client

import java.net.URI

/**
 * Faithful local representation of the original Badcat AdMark payload.
 *
 * markRules are retained as opaque source values until the original matching
 * semantics are fully verified; they are never treated as Legado text rules.
 */
data class BadcatAdMarkItem(
    val host: String,
    val urlPattern: String,
    val markRules: List<String>,
) {
    fun isValid(): Boolean =
        host.isNotBlank() &&
            host.length <= MAX_HOST_CHARS &&
            host.none { it.isWhitespace() || it == '/' || it == '@' } &&
            urlPattern.isNotBlank() &&
            urlPattern.length <= MAX_PATTERN_CHARS &&
            markRules.isNotEmpty() &&
            markRules.size <= MAX_RULE_COUNT &&
            markRules.all { it.isNotBlank() && it.length <= MAX_RULE_CHARS }

    fun appliesToHost(url: String): Boolean {
        val requestHost = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return false
        return requestHost == host.lowercase()
    }

    companion object {
        const val MAX_HOST_CHARS = 253
        const val MAX_PATTERN_CHARS = 2_048
        const val MAX_RULE_COUNT = 64
        const val MAX_RULE_CHARS = 2_048
    }
}

/**
 * One shared view of Badcat purification settings and rule catalogs.
 * Legado replacement rules remain separately accessible and separately enabled.
 */
data class BadcatPurificationSnapshot(
    val webViewAdFilterEnabled: Boolean,
    val webViewInjectionScript: String?,
    val adMarks: List<BadcatAdMarkItem>,
)

object BadcatPurificationUrlPolicy {
    /** Exact host matching only; URL-pattern and markRules semantics stay opaque. */
    fun marksForUrl(url: String, marks: List<BadcatAdMarkItem>): List<BadcatAdMarkItem> =
        marks.filter { it.isValid() && it.appliesToHost(url) }

    fun injectionForPage(snapshot: BadcatPurificationSnapshot): String? =
        snapshot.webViewInjectionScript
            ?.takeIf { snapshot.webViewAdFilterEnabled && it.isNotBlank() && it.length <= MAX_SCRIPT_CHARS }

    const val MAX_SCRIPT_CHARS = 256 * 1024
}
