package app.badcat.engine

/** Executable subset of a Legado replacement rule. */
data class LegadoReplaceRule(
    val scopeRule: LegadoReplaceRuleScope,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean = true,
    val timeoutMillisecond: Long = LegadoReplaceRuleCodec.DEFAULT_TIMEOUT_MILLISECOND,
)

/** Raised when a rule's regular expression exceeds its configured wall-clock budget. */
class LegadoRegexTimeoutException(
    val ruleId: Long,
    val timeoutMillisecond: Long,
) : RuntimeException(
    "Legado replacement rule '$ruleId' exceeded ${timeoutMillisecond}ms regex budget"
)

/**
 * Executes scoped Legado replacement rules. Regex matching is bounded by a
 * deadline-aware CharSequence; unsupported @js replacement expressions fail closed.
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

        return selected.fold(chapterText) { text, rule ->
            if (!rule.isRegex) {
                text.replace(rule.pattern, rule.replacement)
            } else {
                require(!rule.replacement.trimStart().startsWith("@js:")) {
                    "JavaScript replacement for rule '${rule.scopeRule.id}' requires the Legado @js runtime"
                }
                replaceRegexBounded(text, rule)
            }
        }
    }

    private fun replaceRegexBounded(input: String, rule: LegadoReplaceRule): String {
        val timeout = rule.timeoutMillisecond.takeIf { it > 0 }
            ?: LegadoReplaceRuleCodec.DEFAULT_TIMEOUT_MILLISECOND
        val timeoutNanos = timeout.coerceAtMost(Long.MAX_VALUE / 1_000_000L) * 1_000_000L
        val deadline = System.nanoTime() + timeoutNanos
        val boundedInput = DeadlineCharSequence(input, deadline, rule.scopeRule.id, timeout)
        return try {
            Regex(rule.pattern).replace(boundedInput, rule.replacement)
        } catch (error: LegadoRegexTimeoutException) {
            throw error
        }
    }

    private class DeadlineCharSequence(
        private val delegate: CharSequence,
        private val deadlineNanos: Long,
        private val ruleId: Long,
        private val timeoutMillisecond: Long,
    ) : CharSequence {
        override val length: Int
            get() {
                checkDeadline()
                return delegate.length
            }

        override fun get(index: Int): Char {
            checkDeadline()
            return delegate[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            DeadlineCharSequence(
                delegate.subSequence(startIndex, endIndex),
                deadlineNanos,
                ruleId,
                timeoutMillisecond,
            )

        override fun toString(): String {
            checkDeadline()
            return delegate.toString()
        }

        private fun checkDeadline() {
            if (System.nanoTime() - deadlineNanos >= 0L) {
                throw LegadoRegexTimeoutException(ruleId, timeoutMillisecond)
            }
        }
    }
}
