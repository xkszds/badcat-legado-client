package app.badcat.engine

import io.legado.app.model.analyzeRule.AnalyzeByRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpstreamLegadoRegexCompatibilityTest {
    @Test
    fun boundedCollectionRegexMatchesPinnedAnalyzeByRegexResults() {
        val html = "<li>One</li><li>Two</li>"
        val patterns = arrayOf("""<li>(.*?)</li>""")
        val expected = AnalyzeByRegex.getElements(html, patterns).map { it.first() }
        val actual = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluate(html, patterns.joinToString(" && ")),
        ).value
        assertEquals(expected, actual)
    }

    @Test
    fun boundedScalarRegexMatchesPinnedCaptureGroupSemantics() {
        val input = "Title: Alpha; Author: Writer"
        val patterns = arrayOf("""Title:\s*(\w+);\s*Author:\s*(\w+)""")
        val expected = AnalyzeByRegex.getElement(input, patterns)
        val actual = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluateScalar(input, patterns.joinToString(" && ")),
        ).value
        assertEquals(expected, actual)
    }

    @Test
    fun boundedChainedRegexMatchesPinnedIntermediateConcatenationSemantics() {
        val html = "<section><li>One</li></section><section><li>Two</li></section>"
        val patterns = arrayOf(
            """<section>[\s\S]*?</section>""",
            """<li>(.*?)</li>""",
        )
        val expected = AnalyzeByRegex.getElements(html, patterns).map { it.first() }
        val actual = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluate(html, patterns.joinToString(" && ")),
        ).value
        assertEquals(expected, actual)
    }

    @Test
    fun scalarNoMatchRetainsUpstreamNullAsEmptyResultAtEngineBoundary() {
        val expected = AnalyzeByRegex.getElement("no match", arrayOf("title:(.*)"))
        val actual = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluateScalar("no match", "title:(.*)"),
        ).value
        assertEquals(null, expected)
        assertEquals(emptyList(), actual)
    }
}
