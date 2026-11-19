package app.badcat.engine

import io.legado.app.model.analyzeRule.RuleAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PinnedLegadoRuleAnalyzerTest {
    @Test
    fun matchesPinnedUpstreamRuleAnalyzerForNestedSelectorsAndOperators() {
        val expressions = listOf(
            """div.book&&a[href="x||y"]""",
            """$.store.book[?(@.price < 10 && @.active == true)]||$.fallback""",
            """//div[@class='a&&b']%%//span[text()='x||y']""",
            """<div class="book">one</div>||<div class="book">two</div>""",
            """@regex:<a class="book">[\s\S]*?</a>||@css:.fallback""",
        )
        expressions.forEach { expression ->
            val upstream = RuleAnalyzer(expression).splitRule("&&", "||", "%%")
            val actual = PinnedLegadoRuleAnalyzer.split(expression)
            assertEquals(upstream, actual.rules, expression)
            assertEquals(
                upstreamOperator(expression),
                actual.operator,
                expression,
            )
        }
    }

    @Test
    fun codeModeMatchesPinnedAnalyzerForEmbeddedJsonExpressions() {
        val expression = """$.data[?(@.name == "A&&B")] && $.items[*]"""
        val upstream = RuleAnalyzer(expression, true)
        val expected = upstream.splitRule("&&", "||", "%%")
        val actual = PinnedLegadoRuleAnalyzer.split(expression, code = true)
        assertEquals(expected, actual.rules)
        assertEquals(upstream.elementsType, actual.operator)
    }

    @Test
    fun normalizesPinnedAnalyzerUnbalancedGroupErrors() {
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split("div.book[")
        }
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split("""$.items[?(@.name == 'x')""", code = true)
        }
    }

    @Test
    fun enforcesExpressionAndNestingBudgetsBeforeCallingUpstream() {
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split("x".repeat(PinnedLegadoRuleAnalyzer.MAX_EXPRESSION_LENGTH + 1))
        }
        val nested = "[".repeat(PinnedLegadoRuleAnalyzer.MAX_NESTING_DEPTH + 1) +
            "x" +
            "]".repeat(PinnedLegadoRuleAnalyzer.MAX_NESTING_DEPTH + 1)
        assertFailsWith<IllegalArgumentException> {
            PinnedLegadoRuleAnalyzer.split(nested)
        }
    }

    private fun upstreamOperator(expression: String): String? {
        val analyzer = RuleAnalyzer(expression)
        analyzer.splitRule("&&", "||", "%%")
        return analyzer.elementsType.takeIf { it in setOf("&&", "||", "%%") }
    }
}
