package app.badcat.engine

import io.legado.app.model.analyzeRule.RuleAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Differential checks against the pinned upstream RuleAnalyzer compiled from
 * upstream/legado. These cases guard both separator output and selected operator.
 */
class PinnedLegadoRuleAnalyzerParityTest {

    @Test
    fun combinedRuleSplitsMatchPinnedUpstream() {
        val cases = listOf(
            "a && b",
            "a || b",
            "a %% b",
            "a && b || c",
            "a || b && c",
            "a && (b || c)",
            "@xpath://div[@class='a&&b'] && @css:.item",
            "class.title@text && class.author@text",
            "a || (b && c) || d",
            "a %% b && c",
        )

        cases.forEach { expression ->
            val upstream = RuleAnalyzer(expression)
            val upstreamRules = upstream.splitRule("&&", "||", "%%")
            val adapted = PinnedLegadoRuleAnalyzer.split(expression)
            assertEquals(upstream.elementsType, adapted.operator, expression)
            assertEquals(upstreamRules, adapted.rules, expression)
        }
    }

    @Test
    fun codeModeSplitsMatchPinnedUpstreamWithNestedBraces() {
        val expressions = listOf(
            "${'$'}{let value = {a: 1, b: 2}; value.a} && .title",
            "${'$'}{function f(){ return {x: 'a&&b'} }} || .fallback",
            "${'$'}{const text = \"a||b\"; text} && .next",
        )
        expressions.forEach { expression ->
            val upstream = RuleAnalyzer(expression, true)
            val upstreamRules = upstream.splitRule("&&", "||", "%%")
            val adapted = PinnedLegadoRuleAnalyzer.split(expression, code = true)
            assertEquals(upstream.elementsType, adapted.operator, expression)
            assertEquals(upstreamRules, adapted.rules, expression)
        }
    }

    @Test
    fun expressionSizeAndNestingGuardsRejectOversizedInputBeforeUpstreamParser() {
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split("x".repeat(PinnedLegadoRuleAnalyzer.MAX_EXPRESSION_LENGTH + 1))
        }
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split("(".repeat(PinnedLegadoRuleAnalyzer.MAX_NESTING_DEPTH + 1) +
                "x" + ")".repeat(PinnedLegadoRuleAnalyzer.MAX_NESTING_DEPTH + 1))
        }
    }
}
