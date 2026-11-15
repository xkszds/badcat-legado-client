package app.badcat.engine

/** The executable fields needed for the literal-only subset of Legado replacement rules. */
data class LegadoReplaceRule(
    val scopeRule: LegadoReplaceRuleScope,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean = true,
)

/**
 * Executes only non-regex rules after applying Legado's content-scope selection.
 *
 * Regex rules fail closed rather than being silently skipped or run without
 * Legado's timeout and @js replacement semantics.
 */
class LegadoLiteralReplacement(
    private val rules: List<LegadoReplaceRule>,
    private val bookName: String,
    private val bookOrigin: String,
) : BookTextReplacement {

    override fun replace(chapterText: String): String {
        val selected = LegadoReplaceRuleSelector.forContent(
            rules = rules.map { it.scopeRule },
            bookName = bookName,
            bookOrigin = bookOrigin,
        ).mapNotNull { selectedScope ->
            rules.firstOrNull { it.scopeRule.id == selectedScope.id }
        }.filter { it.pattern.isNotEmpty() }

        val unsupported = selected.firstOrNull { it.isRegex }
        if (unsupported != null) {
            throw UnsupportedOperationException(
                "Regex replacement rule '${unsupported.scopeRule.id}' requires the Legado regex runtime"
            )
        }

        return selected.fold(chapterText) { text, rule ->
            text.replace(rule.pattern, rule.replacement)
        }
    }
}
