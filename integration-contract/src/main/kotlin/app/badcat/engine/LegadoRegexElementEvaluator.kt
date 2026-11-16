package app.badcat.engine

import java.util.regex.Pattern

/**
 * Bounded port of Legado AnalyzeByRegex.getElements for all-in-one collection
 * rules. Regex is available only for element collections, not guessed for scalar
 * selectors. Each && stage consumes the concatenated full matches of the prior
 * stage, matching the upstream multi-stage extraction shape.
 */
object LegadoRegexElementEvaluator {
    const val DEFAULT_TIMEOUT_MILLISECONDS = 250L
    const val MAX_INPUT_CHARACTERS = 8 * 1024 * 1024
    const val MAX_PATTERN_CHARACTERS = 2_000
    const val MAX_STAGES = 8
    const val MAX_MATCHES = 10_000
    const val MAX_OUTPUT_CHARACTERS = 8 * 1024 * 1024

    fun evaluate(
        input: String,
        expression: String,
        timeoutMilliseconds: Long = DEFAULT_TIMEOUT_MILLISECONDS,
    ): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Legado regex collection rule is blank")
        }
        if (input.length > MAX_INPUT_CHARACTERS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Regex input exceeds the configured limit")
        }
        if (timeoutMilliseconds !in 1..5_000) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Regex timeout must be between 1 and 5000ms")
        }
        val stages = try {
            RuleExpressionTokenizer.splitTopLevel(rule, "&&")
        } catch (error: Exception) {
            return EngineResult.Failure(EngineErrorCode.PARSE, "Malformed regex collection rule", error)
        }
        if (stages.size > MAX_STAGES || stages.any { it.isBlank() || it.length > MAX_PATTERN_CHARACTERS }) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Regex rule exceeds stage or pattern limits")
        }

        val started = System.nanoTime()
        val deadline = timeoutMilliseconds * 1_000_000L
        var current = input
        return try {
            for ((stageIndex, sourcePattern) in stages.withIndex()) {
                val pattern = Pattern.compile(sourcePattern)
                val bounded = DeadlineCharSequence(current, started, deadline)
                val matcher = pattern.matcher(bounded)
                val matches = ArrayList<String>()
                val nextInput = StringBuilder()
                var count = 0
                while (matcher.find()) {
                    if (++count > MAX_MATCHES) {
                        return EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Regex collection produced too many matches",
                        )
                    }
                    val match = matcher.group()
                    if (nextInput.length.toLong() + match.length > MAX_OUTPUT_CHARACTERS) {
                        return EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Regex collection output exceeds the configured limit",
                        )
                    }
                    if (stageIndex == stages.lastIndex) matches += match else nextInput.append(match)
                }
                if (stageIndex != stages.lastIndex) current = nextInput.toString()
                else return EngineResult.Success(matches)
            }
            EngineResult.Success(emptyList())
        } catch (timeout: RegexDeadlineExceeded) {
            EngineResult.Failure(EngineErrorCode.RULE_TIMEOUT, "Legado regex collection rule exceeded its time budget", timeout)
        } catch (error: Exception) {
            EngineResult.Failure(EngineErrorCode.PARSE, "Legado regex collection rule could not be evaluated", error)
        }
    }


    /**
     * Bounded port of Legado AnalyzeByRegex.getElement for scalar rule fields.
     * Intermediate stages concatenate full matches; the final stage returns
     * capture group 0 and every capture group from the first matching result.
     */
    fun evaluateScalar(
        input: String,
        expression: String,
        timeoutMilliseconds: Long = DEFAULT_TIMEOUT_MILLISECONDS,
    ): EngineResult<List<String>> {
        val rule = expression.trim()
        if (rule.isEmpty()) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Legado regex scalar rule is blank")
        }
        if (input.length > MAX_INPUT_CHARACTERS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Regex input exceeds the configured limit")
        }
        if (timeoutMilliseconds !in 1..5_000) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Regex timeout must be between 1 and 5000ms")
        }
        val stages = try {
            RuleExpressionTokenizer.splitTopLevel(rule, "&&")
        } catch (error: Exception) {
            return EngineResult.Failure(EngineErrorCode.PARSE, "Malformed regex scalar rule", error)
        }
        if (stages.size > MAX_STAGES || stages.any { it.isBlank() || it.length > MAX_PATTERN_CHARACTERS }) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Regex rule exceeds stage or pattern limits")
        }

        val started = System.nanoTime()
        val deadline = timeoutMilliseconds * 1_000_000L
        var current = input
        return try {
            for ((stageIndex, sourcePattern) in stages.withIndex()) {
                val matcher = Pattern.compile(sourcePattern).matcher(
                    DeadlineCharSequence(current, started, deadline),
                )
                if (stageIndex == stages.lastIndex) {
                    if (!matcher.find()) return EngineResult.Success(emptyList())
                    return EngineResult.Success(
                        (0..matcher.groupCount()).map { matcher.group(it).orEmpty() },
                    )
                }
                val nextInput = StringBuilder()
                var count = 0
                while (matcher.find()) {
                    if (++count > MAX_MATCHES) {
                        return EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Regex scalar stage produced too many matches",
                        )
                    }
                    val match = matcher.group()
                    if (nextInput.length.toLong() + match.length > MAX_OUTPUT_CHARACTERS) {
                        return EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Regex scalar intermediate output exceeds the configured limit",
                        )
                    }
                    nextInput.append(match)
                }
                current = nextInput.toString()
            }
            EngineResult.Success(emptyList())
        } catch (timeout: RegexDeadlineExceeded) {
            EngineResult.Failure(EngineErrorCode.RULE_TIMEOUT, "Legado regex scalar rule exceeded its time budget", timeout)
        } catch (error: Exception) {
            EngineResult.Failure(EngineErrorCode.PARSE, "Legado regex scalar rule could not be evaluated", error)
        }
    }

    private class RegexDeadlineExceeded : RuntimeException("Regex evaluation deadline exceeded")

    private class DeadlineCharSequence(
        private val delegate: CharSequence,
        private val startedAt: Long,
        private val budgetNanos: Long,
    ) : CharSequence {
        override val length: Int
            get() { checkDeadline(); return delegate.length }

        override fun get(index: Int): Char {
            checkDeadline()
            return delegate[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            checkDeadline()
            return DeadlineCharSequence(delegate.subSequence(startIndex, endIndex), startedAt, budgetNanos)
        }

        override fun toString(): String {
            checkDeadline()
            return delegate.toString()
        }

        private fun checkDeadline() {
            if (System.nanoTime() - startedAt >= budgetNanos) throw RegexDeadlineExceeded()
        }
    }
}
