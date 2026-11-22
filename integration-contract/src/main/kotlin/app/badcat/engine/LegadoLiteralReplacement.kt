package app.badcat.engine

import java.util.regex.Matcher
import java.util.regex.Pattern

/** The Legado replacement-rule target selected from DAO scope flags. */
enum class LegadoReplaceRuleTarget { CONTENT, TITLE }

/** Executable subset of a Legado replacement rule. */
data class LegadoReplaceRule(
    val scopeRule: LegadoReplaceRuleScope,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean = true,
    val timeoutMillisecond: Long = LegadoReplaceRuleCodec.DEFAULT_TIMEOUT_MILLISECOND,
    val name: String = scopeRule.id.toString(),
)

/** Raised when a rule's regular expression or JavaScript exceeds its time budget. */
class LegadoRegexTimeoutException(
    val ruleId: Long,
    val timeoutMillisecond: Long,
) : RuntimeException(
    "Legado replacement rule '" + ruleId + "' exceeded " + timeoutMillisecond + "ms execution budget"
)

/**
 * Executes scoped Legado replacement rules. Regex matching uses a deadline-aware
 * CharSequence. @js expressions are delegated to the host's bounded Rhino evaluator.
 */
class LegadoLiteralReplacement(
    private val rules: List<LegadoReplaceRule>,
    private val bookName: String,
    private val bookOrigin: String,
    private val target: LegadoReplaceRuleTarget = LegadoReplaceRuleTarget.CONTENT,
    private val replacementContext: LegadoReplacementContext? = null,
    private val jsEvaluator: LegadoJsReplacementEvaluator? = null,
) : BookTextReplacement {

    override fun replace(chapterText: String): String {
        val scopes = rules.map { it.scopeRule }
        val selectedScopes = when (target) {
            LegadoReplaceRuleTarget.CONTENT ->
                LegadoReplaceRuleSelector.forContent(scopes, bookName, bookOrigin)
            LegadoReplaceRuleTarget.TITLE ->
                LegadoReplaceRuleSelector.forTitle(scopes, bookName, bookOrigin)
        }
        val selected = selectedScopes.mapNotNull { selectedScope ->
            rules.firstOrNull { it.scopeRule.id == selectedScope.id }
        }.filter { it.pattern.isNotEmpty() }

        return selected.fold(chapterText) { text, rule ->
            if (!rule.isRegex) {
                text.replace(rule.pattern, rule.replacement)
            } else {
                replaceRegexBounded(text, rule)
            }
        }
    }

    private fun replaceRegexBounded(input: String, rule: LegadoReplaceRule): String {
        val timeout = rule.timeoutMillisecond.takeIf { it > 0 }
            ?: LegadoReplaceRuleCodec.DEFAULT_TIMEOUT_MILLISECOND
        val timeoutNanos = timeout.coerceAtMost(Long.MAX_VALUE / 1_000_000L) * 1_000_000L
        val startedAtNanos = System.nanoTime()
        val boundedInput = DeadlineCharSequence(input, startedAtNanos, timeoutNanos, rule.scopeRule.id, timeout)
        val matcher = Pattern.compile(rule.pattern).matcher(boundedInput)
        val output = StringBuffer()
        val isJs = rule.replacement.startsWith("@js:")
        val jsScript = if (isJs) rule.replacement.substring(4) else rule.replacement
        val jsSession = if (isJs) {
            jsEvaluator?.createSession(rule.name) ?: throw UnsupportedOperationException(
                "JavaScript replacement for rule '" + rule.scopeRule.id + "' has no Rhino host"
            )
        } else {
            null
        }
        while (matcher.find()) {
            val replacement = if (isJs) {
                val context = replacementContext ?: throw UnsupportedOperationException(
                    "JavaScript replacement for rule '" + rule.scopeRule.id + "' has no book/chapter context"
                )
                jsSession!!.evaluate(
                    jsScript,
                    LegadoJsReplacementContext(
                        ruleId = rule.scopeRule.id,
                        ruleName = rule.name,
                        result = matcher.group(),
                        timeoutMillisecond = boundedInput.remainingMilliseconds(),
                        configuredTimeoutMillisecond = timeout,
                        book = context.book,
                        chapter = context.chapter,
                    ),
                )
            } else {
                rule.replacement
            }
            matcher.appendReplacement(
                output,
                if (isJs) Matcher.quoteReplacement(replacement) else replacement,
            )
        }
        matcher.appendTail(output)
        return output.toString()
    }

    private class DeadlineCharSequence(
        private val delegate: CharSequence,
        private val startedAtNanos: Long,
        private val timeoutNanos: Long,
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

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            checkDeadline()
            return DeadlineCharSequence(
                delegate.subSequence(startIndex, endIndex),
                startedAtNanos,
                timeoutNanos,
                ruleId,
                timeoutMillisecond,
            )
        }

        override fun toString(): String {
            checkDeadline()
            return delegate.toString()
        }

        fun remainingMilliseconds(): Long {
            val elapsedNanos = System.nanoTime() - startedAtNanos
            val remainingNanos = timeoutNanos - elapsedNanos
            if (remainingNanos <= 0L) {
                throw LegadoRegexTimeoutException(ruleId, timeoutMillisecond)
            }
            return (remainingNanos / 1_000_000L).coerceAtLeast(1L)
        }

        private fun checkDeadline() {
            if (System.nanoTime() - startedAtNanos >= timeoutNanos) {
                throw LegadoRegexTimeoutException(ruleId, timeoutMillisecond)
            }
        }
    }
}
