package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LegadoRegexElementEvaluatorTest {
    @Test
    fun extractsFullMatchesInOrder() {
        val result = LegadoRegexElementEvaluator.evaluate(
            "<li>One</li><li>Two</li>",
            """<li>[\s\S]*?</li>""",
        )
        assertEquals(
            listOf("<li>One</li>", "<li>Two</li>"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun appliesChainedRegexStagesToPriorMatches() {
        val result = LegadoRegexElementEvaluator.evaluate(
            "<section><li>One</li></section><section><li>Two</li></section>",
            """<section>[\s\S]*?</section> && <li>[\s\S]*?</li>""",
        )
        assertEquals(
            listOf("<li>One</li>", "<li>Two</li>"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun rejectsInvalidPatternsAndOversizedStageLists() {
        assertEquals(
            EngineErrorCode.PARSE,
            assertIs<EngineResult.Failure>(
                LegadoRegexElementEvaluator.evaluate("body", "["),
            ).code,
        )
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRegexElementEvaluator.evaluate("body", List(9) { "x" }.joinToString(" && ")),
            ).code,
        )
    }

    @Test
    fun rejectsOutOfRangeBudgets() {
        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(
                LegadoRegexElementEvaluator.evaluate("body", "body", timeoutMilliseconds = 0),
            ).code,
        )
    }
    @Test
    fun extractsHtmlWithSingleQuotedAttributesAndRegexCharacterClasses() {
        val result = LegadoRegexElementEvaluator.evaluate(
            """<div class='book'><h3>Regex Novel</h3></div><div class='other'>Skip</div>""",
            """(?s)<div.*?book.*?>.*?</div>""",
        )
        assertEquals(
            listOf("""<div class='book'><h3>Regex Novel</h3></div>"""),
            assertIs<EngineResult.Success<List<String>>>(result, "Regex evaluation failed: $result").value,
        )
    }

    @Test
    fun enforcesDeadlineDuringCatastrophicBacktracking() {
        val input = "a".repeat(100_000) + "!"
        val started = System.nanoTime()
        val result = LegadoRegexElementEvaluator.evaluate(
            input,
            "^(a+)+$",
            timeoutMilliseconds = 10,
        )
        val elapsedMillis = (System.nanoTime() - started) / 1_000_000
        assertEquals(
            EngineErrorCode.RULE_TIMEOUT,
            assertIs<EngineResult.Failure>(result).code,
        )
        assertTrue(elapsedMillis < 1_000, "Regex deadline was not enforced promptly: ${elapsedMillis}ms")
    }

}
