package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PinnedRuleCombinationEvaluatorTest {
    @Test
    fun detectsMixedTopLevelOperatorsWithoutSplittingQuotedSelectorText() {
        assertEquals(
            true,
            RuleCombinationEvaluator.hasNestedTopLevelOperator(
                """@CSS: div.book&&@CSS: a.title||@CSS: span.author""",
            ),
        )
        assertEquals(
            false,
            RuleCombinationEvaluator.hasNestedTopLevelOperator(
                """@CSS: div[data-label="left&&right"]""",
            ),
        )
    }

    @Test
    fun recursivelyEvaluatesNestedFallbackUsingPinnedRuleSplitting() {
        val visited = mutableListOf<String>()
        val result = RuleCombinationEvaluator.evaluateNestedResult("a||b&&c") { branch ->
            visited += branch
            when (branch.trim()) {
                "a" -> EngineResult.Success(emptyList())
                "b" -> EngineResult.Success(listOf("B"))
                "c" -> EngineResult.Success(listOf("C"))
                else -> EngineResult.Success(emptyList())
            }
        }
        assertEquals(listOf("B", "C"), assertIs<EngineResult.Success<List<String>>>(result).value)
        assertEquals(listOf("a", "b", "c"), visited)
    }

    @Test
    fun nestedOperatorSplitterMatchesPinnedUpstreamOnQuotedAttributes() {
        val expression = """primary[data-x="x || y"] && secondary"""
        val upstream = io.legado.app.model.analyzeRule.RuleAnalyzer(expression)
        val expected = upstream.splitRule("&&", "||", "%%")
        val actual = PinnedLegadoRuleAnalyzer.split(expression)
        assertEquals(expected.map { it.trim() }, actual.rules.map { it.trim() })
        assertEquals("&&", actual.operator)
        assertEquals("""primary[data-x="x || y"]""", actual.rules.first().trim())
    }
}
