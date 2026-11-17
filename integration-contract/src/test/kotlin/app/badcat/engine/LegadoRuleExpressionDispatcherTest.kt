package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoRuleExpressionDispatcherTest {

    @Test
    fun resolvesExplicitLegadoSelectorPrefixesCaseInsensitively() {
        val css = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@CSS: div.book"),
        ).value
        assertEquals(SelectorRuleType.CSS, css.type)
        assertEquals("div.book", css.expression)

        val xpath = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@XPath: //div[@class='book']"),
        ).value
        assertEquals(SelectorRuleType.XPATH, xpath.type)
        assertEquals("//div[@class='book']", xpath.expression)

        val jsonPath = assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
            LegadoRuleExpressionDispatcher.resolve("@Json: $.books[*].name"),
        ).value
        assertEquals(SelectorRuleType.JSON_PATH, jsonPath.type)
        assertEquals("$.books[*].name", jsonPath.expression)
    }

    @Test
    fun infersOnlyUnambiguousUnprefixedSelectorForms() {
        assertEquals(
            SelectorRuleType.CSS,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("div.book > a.title"),
            ).value.type,
        )
        assertEquals(
            SelectorRuleType.XPATH,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("//div[@class='book']"),
            ).value.type,
        )
        assertEquals(
            SelectorRuleType.JSON_PATH,
            assertIs<EngineResult.Success<ResolvedLegadoRuleExpression>>(
                LegadoRuleExpressionDispatcher.resolve("$.books[*].name"),
            ).value.type,
        )
    }

    @Test
    fun javascriptAndRegexAreNotMisclassifiedAsSelectors() {
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRuleExpressionDispatcher.resolve("@js:result"),
            ).code,
        )
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                LegadoRuleExpressionDispatcher.resolve("@regex:(\\\\d+)"),
            ).code,
        )
    }
}
