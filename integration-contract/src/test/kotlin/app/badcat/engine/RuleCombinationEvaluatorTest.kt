package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class RuleCombinationEvaluatorTest {

    private val fixture = mapOf(
        "primary" to listOf("A", "B"),
        "secondary" to listOf("C"),
        "empty" to emptyList(),
        "third" to listOf("D", "E", "F"),
    )

    @Test
    fun andConcatenatesEveryNonEmptyResult() {
        assertEquals(
            listOf("A", "B", "C"),
            RuleCombinationEvaluator.evaluate("primary && secondary") { fixture[it].orEmpty() },
        )
    }

    @Test
    fun orStopsAtFirstNonEmptyResult() {
        assertEquals(
            listOf("C"),
            RuleCombinationEvaluator.evaluate("empty || secondary || primary") { fixture[it].orEmpty() },
        )
    }

    @Test
    fun percentPercentInterleavesResultsByPosition() {
        assertEquals(
            listOf("A", "C", "D", "B", "E"),
            RuleCombinationEvaluator.evaluate("primary %% secondary %% third") { fixture[it].orEmpty() },
        )
    }

    @Test
    fun detectsOnlyMixedNestedTopLevelCombinations() {
        assertEquals(false, RuleCombinationEvaluator.hasNestedTopLevelOperator("primary && secondary && third"))
        assertEquals(true, RuleCombinationEvaluator.hasNestedTopLevelOperator("primary && secondary || third"))
        assertEquals(false, RuleCombinationEvaluator.hasNestedTopLevelOperator("""primary[data-x="x || y"] && secondary"""))
    }

    @Test
    fun nestedOperatorsDoNotSelectTheOuterCombination() {
        val expression = """primary[data-x="x || y"] && secondary"""
        val split = RuleExpressionTokenizer.splitByFirstTopLevelOperator(expression)
        assertEquals("&&", split.operator)
        assertEquals(2, split.rules.size)
        assertEquals("""primary[data-x="x || y"]""", split.rules[0])
    }
}
