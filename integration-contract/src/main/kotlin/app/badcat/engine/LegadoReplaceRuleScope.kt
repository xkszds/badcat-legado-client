package app.badcat.engine

/**
 * The replacement-rule scope fields used by the pinned Legado DAO.
 *
 * This models Legado's title/content/source scope selection. Execution is
 * delegated to the bounded replacement runtime.
 */
data class LegadoReplaceRuleScope(
    val id: Long,
    val enabled: Boolean = true,
    val scope: String? = null,
    val excludeScope: String? = null,
    val scopeTitle: Boolean = false,
    val scopeSource: Boolean = false,
    val scopeContent: Boolean = true,
    val order: Int = 0,
)

object LegadoReplaceRuleSelector {

    /** Matches the pinned DAO's content/title scope query and sort order. */
    fun forContent(
        rules: List<LegadoReplaceRuleScope>,
        bookName: String,
        bookOrigin: String,
    ): List<LegadoReplaceRuleScope> = select(
        rules = rules,
        bookName = bookName,
        bookOrigin = bookOrigin,
        enabledForDomain = { it.scopeContent },
    )

    fun forTitle(
        rules: List<LegadoReplaceRuleScope>,
        bookName: String,
        bookOrigin: String,
    ): List<LegadoReplaceRuleScope> = select(
        rules = rules,
        bookName = bookName,
        bookOrigin = bookOrigin,
        enabledForDomain = { it.scopeTitle },
    )

    /** Source-scoped rules are selected separately by Legado's DAO. */
    fun forSource(rules: List<LegadoReplaceRuleScope>): List<LegadoReplaceRuleScope> =
        rules.asSequence()
            .filter { it.enabled && it.scopeSource }
            .sortedBy { it.order }
            .toList()

    private fun select(
        rules: List<LegadoReplaceRuleScope>,
        bookName: String,
        bookOrigin: String,
        enabledForDomain: (LegadoReplaceRuleScope) -> Boolean,
    ): List<LegadoReplaceRuleScope> = rules.asSequence()
        .filter { it.enabled && enabledForDomain(it) }
        .filter {
            val include = it.scope.isNullOrEmpty() ||
                sqliteLikeContains(it.scope, bookName) ||
                sqliteLikeContains(it.scope, bookOrigin)
            val excluded = !it.excludeScope.isNullOrEmpty() &&
                (sqliteLikeContains(it.excludeScope!!, bookName) ||
                    sqliteLikeContains(it.excludeScope, bookOrigin))
            include && !excluded
        }
        .sortedBy { it.order }
        .toList()

    /**
     * SQLite LIKE without an ESCAPE clause: '%' and '_' in the searched value
     * remain wildcards. SQLite's default LIKE folds ASCII letters only.
     */
    private fun sqliteLikeContains(scope: String, value: String): Boolean {
        val pattern = "%$value%"
        val text = scope.codePoints().toArray()
        val tokens = pattern.codePoints().toArray()
        var previous = BooleanArray(text.size + 1)
        previous[0] = true
        for (token in tokens) {
            val current = BooleanArray(text.size + 1)
            when (token) {
                '%'.code -> {
                    current[0] = previous[0]
                    for (index in 1..text.size) {
                        current[index] = previous[index] || current[index - 1]
                    }
                }
                '_'.code -> {
                    for (index in 1..text.size) current[index] = previous[index - 1]
                }
                else -> {
                    for (index in 1..text.size) {
                        current[index] = previous[index - 1] &&
                            asciiFold(token) == asciiFold(text[index - 1])
                    }
                }
            }
            previous = current
        }
        return previous[text.size]
    }

    private fun asciiFold(codePoint: Int): Int =
        if (codePoint in 'A'.code..'Z'.code) codePoint + ('a'.code - 'A'.code) else codePoint
}
