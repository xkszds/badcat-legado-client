package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoRuleReplacementTest {
    @Test
    fun appliesRegexReplacementSuffixToScalarRuleResults() {
        val document = SourceDocument(
            "https://reader.example/",
            "<h1>old title and old subtitle</h1>",
        )

        val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            "h1@text##old##new",
        )

        assertEquals(
            listOf("new title and new subtitle"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun appliesOnlyFirstRegexReplacementWhenThirdSeparatorIsPresent() {
        val result = LegadoRegexElementEvaluator.replace(
            input = "banana",
            expression = "a",
            replacement = "o",
            replaceFirst = true,
        )

        assertEquals("o", assertIs<EngineResult.Success<String>>(result).value)
    }

    @Test
    fun fallsBackToLiteralReplacementForInvalidRegex() {
        val result = LegadoRegexElementEvaluator.replace(
            input = "a{b{",
            expression = "{",
            replacement = "x",
        )

        assertEquals("axbx", assertIs<EngineResult.Success<String>>(result).value)
    }

    @Test
    fun trimsTheSelectorBeforeApplyingTheReplacementSuffixLikeUpstreamSourceRule() {
        val parsed = LegadoRuleReplacement.parse("  h1@text  ##old##new")
        assertEquals("h1@text", parsed.expression)
        assertEquals("h1@text##old##new", parsed.toRuleString())
        assertEquals(
            listOf("new"),
            assertIs<EngineResult.Success<List<String>>>(
                parsed.apply(listOf("old title")),
            ).value,
        )
    }

    @Test
    fun trimsPlainRulesWithoutReplacementSuffix() {
        assertEquals(
            "h1@text",
            LegadoRuleReplacement.parse("  h1@text  ").expression,
        )
    }


    @Test
    fun ignoresReplacementSegmentsAfterTheThirdLikeUpstreamSourceRule() {
        val parsed = LegadoRuleReplacement.parse("h1@text##old##new##ignored##also-ignored")
        assertEquals("old", parsed.replaceRegex)
        assertEquals("new", parsed.replacement)
        assertEquals(true, parsed.replaceFirst)
        assertEquals(
            listOf("new title"),
            assertIs<EngineResult.Success<List<String>>>(
                parsed.apply(listOf("old title")),
            ).value,
        )
    }

}
