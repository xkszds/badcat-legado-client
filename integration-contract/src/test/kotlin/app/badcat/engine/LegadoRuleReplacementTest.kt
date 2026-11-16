package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoRuleReplacementTest {
    @Test
    fun appliesRegexReplacementSuffixToScalarRuleResults() {
        val document = SourceDocument(
            "https://reader.example/",
            "<h1>Old title and old subtitle</h1>",
        )

        val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            "h1@text##old##new",
        )

        assertEquals(
            listOf("New title and new subtitle"),
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

        assertEquals("bonana", assertIs<EngineResult.Success<String>>(result).value)
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
}
