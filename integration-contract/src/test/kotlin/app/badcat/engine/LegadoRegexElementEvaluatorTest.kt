package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LegadoRegexElementEvaluatorTest {
    @Test
    fun evaluatesSingleRegexPredicatesAndReportsMalformedPatterns() {
        assertEquals(
            true,
            assertIs<EngineResult.Success<Boolean>>(
                LegadoRegexElementEvaluator.matches("https://example.test/ad?id=42", """ad\\?id=\\d+"""),
            ).value,
        )
        assertEquals(
            false,
            assertIs<EngineResult.Success<Boolean>>(
                LegadoRegexElementEvaluator.matches("https://example.test/book", "ad"),
            ).value,
        )
        assertEquals(
            EngineErrorCode.PARSE,
            assertIs<EngineResult.Failure>(
                LegadoRegexElementEvaluator.matches("url", "["),
            ).code,
        )
    }

    @Test
    fun enforcesDeadlineForSingleRegexPredicates() {
        val result = LegadoRegexElementEvaluator.matches(
            "a".repeat(100_000) + "!",
            "^(a+)+$",
            timeoutMilliseconds = 10,
        )
        assertEquals(EngineErrorCode.RULE_TIMEOUT, assertIs<EngineResult.Failure>(result).code)
    }

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
    fun dispatcherSupportsFallbackBetweenRegexCollectionRules() {
        val document = SourceDocument(
            url = "https://example.test/books",
            body = "<li>Fallback book</li>",
        )
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "@regex:missing||@regex:<li>[\\s\\S]*?</li>",
        )
        assertEquals(
            listOf("<li>Fallback book</li>"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun dispatcherInterleavesRegexCollectionBranches() {
        val document = SourceDocument(
            url = "https://example.test/books",
            body = "<li>A</li><li>B</li>",
        )
        val result = LegadoRuleExpressionDispatcher.evaluateElements(
            document,
            "@regex:<li>A</li>%%<li>B</li>",
        )
        assertEquals(
            listOf("<li>A</li>", "<li>B</li>"),
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

    @Test
    fun scalarRegexReturnsFirstMatchAndCaptureGroups() {
        val result = LegadoRegexElementEvaluator.evaluateScalar(
            "Title: Regex Novel; Author: Writer",
            """Title:\s*(Regex\s+Novel);\s*Author:\s*(Writer)""",
        )
        assertEquals(
            listOf("Title: Regex Novel; Author: Writer", "Regex Novel", "Writer"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun scalarRegexSupportsChainedExtractionAndNoMatch() {
        val result = LegadoRegexElementEvaluator.evaluateScalar(
            "<section><h1>Regex Novel</h1></section><section><h1>Other</h1></section>",
            """<section>[\s\S]*?</section> && <h1>(.*?)</h1>""",
        )
        assertEquals(
            listOf("<h1>Regex Novel</h1>", "Regex Novel"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
        assertEquals(
            emptyList(),
            assertIs<EngineResult.Success<List<String>>>(
                LegadoRegexElementEvaluator.evaluateScalar("nothing", "title:(.*)"),
            ).value,
        )
    }



    @Test
    fun scalarRegexSupportsQuotedHtmlAttributesAndCharacterClassOperators() {
        val result = LegadoRegexElementEvaluator.evaluateScalar(
            """<a class="book">Regex Novel</a>""",
            """<a class="book">([\s\S]*?)</a>""",
        )
        assertEquals(
            listOf("""<a class="book">Regex Novel</a>""", "Regex Novel"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }


    @Test
    fun collectionRegexSupportsQuotedHtmlAttributes() {
        val result = LegadoRegexElementEvaluator.evaluate(
            """<div class="book">One</div><div class="book">Two</div>""",
            """<div class="book">.*?</div>""",
        )
        assertEquals(
            listOf("""<div class="book">One</div>""", """<div class="book">Two</div>"""),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun regexStageSplitterIgnoresNestedCharacterClassIntersections() {
        val result = LegadoRegexElementEvaluator.evaluateScalar(
            "abcxyz",
            """[a-z&&[^m-p]]+ && ([a-z]+)""",
        )
        assertEquals(
            listOf("abcxyz", "abcxyz"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }


}
