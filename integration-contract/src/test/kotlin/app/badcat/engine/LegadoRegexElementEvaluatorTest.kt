package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
            """<div[^>]*book[^>]*>[\\s\\S]*?</div>""",
        )
        println("REGEX EVALUATOR RESULT: $result")
        assertEquals(
            listOf("""<div class='book'><h3>Regex Novel</h3></div>"""),
            assertIs<EngineResult.Success<List<String>>>(result, "Regex evaluation failed: $result").value,
        )
    }

}
