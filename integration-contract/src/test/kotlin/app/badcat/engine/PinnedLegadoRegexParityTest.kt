package app.badcat.engine

import io.legado.app.model.analyzeRule.AnalyzeByRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Differential contract tests against the exact AnalyzeByRegex.kt in the pinned
 * Legado submodule. The production evaluator remains deadline-bounded; the
 * upstream implementation is the semantic oracle, not an unbounded runtime path.
 */
class PinnedLegadoRegexParityTest {

    @Test
    fun scalarExtractionMatchesPinnedUpstreamCaptureGroups() {
        val input = "Title: Regex Novel; Author: Writer"
        val pattern = """Title:\s*(Regex\s+Novel);\s*Author:\s*(Writer)"""

        val upstream = AnalyzeByRegex.getElement(input, arrayOf(pattern))
        val bounded = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluateScalar(input, pattern),
        ).value

        assertEquals(upstream, bounded)
    }

    @Test
    fun collectionExtractionMatchesPinnedUpstreamFullMatches() {
        val input = "<li>One</li><li>Two</li>"
        val pattern = """<li>([\s\S]*?)</li>"""

        val upstream = AnalyzeByRegex.getElements(input, arrayOf(pattern))
        val bounded = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluate(input, pattern),
        ).value

        assertEquals(upstream.map { it.first() }, bounded)
    }

    @Test
    fun chainedScalarExtractionMatchesPinnedUpstream() {
        val input = "<section><h1>Regex Novel</h1></section><section><h1>Other</h1></section>"
        val patterns = arrayOf("""<section>[\s\S]*?</section>""", """<h1>(.*?)</h1>""")

        val upstream = AnalyzeByRegex.getElement(input, patterns)
        val bounded = assertIs<EngineResult.Success<List<String>>>(
            LegadoRegexElementEvaluator.evaluateScalar(input, patterns.joinToString(" && ")),
        ).value

        assertEquals(upstream, bounded)
    }

    @Test
    fun missingMatchesRetainUpstreamEmptyCollectionAndNullScalarSemantics() {
        val input = "no match"
        val pattern = """<li>(.*?)</li>"""

        assertEquals(emptyList(), AnalyzeByRegex.getElements(input, arrayOf(pattern)))
        assertEquals(null, AnalyzeByRegex.getElement(input, arrayOf(pattern)))
        assertEquals(
            emptyList(),
            assertIs<EngineResult.Success<List<String>>>(
                LegadoRegexElementEvaluator.evaluate(input, pattern),
            ).value,
        )
        assertEquals(
            emptyList(),
            assertIs<EngineResult.Success<List<String>>>(
                LegadoRegexElementEvaluator.evaluateScalar(input, pattern),
            ).value,
        )
    }
}
