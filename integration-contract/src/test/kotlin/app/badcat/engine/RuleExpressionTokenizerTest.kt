package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuleExpressionTokenizerTest {

    @Test
    fun splitsOnlyTopLevelOperators() {
        assertEquals(
            listOf("div.a@text", "span.b@text", "p.c@text"),
            RuleExpressionTokenizer.splitTopLevel(
                "div.a@text && span.b@text && p.c@text",
                "&&",
            ),
        )
    }

    @Test
    fun preservesOperatorsInsideQuotesAndNestedGroups() {
        val expression = """a[data-label="A && B"] || :contains('x || y') || :not(.a%%.b)"""
        assertEquals(
            listOf(
                """a[data-label="A && B"]""",
                """:contains('x || y')""",
                """:not(.a%%.b)""",
            ),
            RuleExpressionTokenizer.splitTopLevel(expression, "||"),
        )
    }

    @Test
    fun preservesJsonAndScriptLikeGroups() {
        val expression = """{ "left": "a %% b", "nested": { "ok": true } } %% fallback"""
        assertEquals(
            listOf("""{ "left": "a %% b", "nested": { "ok": true } }""", "fallback"),
            RuleExpressionTokenizer.splitTopLevel(expression, "%%"),
        )
    }

    @Test
    fun rejectsMalformedGroupsAndUnknownDelimiter() {
        assertFailsWith<IllegalArgumentException> {
            RuleExpressionTokenizer.splitTopLevel("a[foo && b", "&&")
        }
        assertFailsWith<IllegalArgumentException> {
            RuleExpressionTokenizer.splitTopLevel("a && b", "??")
        }
    }
}
