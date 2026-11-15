package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LegadoLiteralReplacementTest {

    @Test
    fun appliesEnabledLiteralContentRulesInLegadoOrderAndScope() {
        val rules = listOf(
            LegadoReplaceRule(LegadoReplaceRuleScope(1, order = 0), "foo", "bar", isRegex = false),
            LegadoReplaceRule(LegadoReplaceRuleScope(2, scope = "Other Book", order = 0), "foo", "wrong", isRegex = false),
            LegadoReplaceRule(LegadoReplaceRuleScope(3, order = 1), "bar", "done", isRegex = false),
            LegadoReplaceRule(LegadoReplaceRuleScope(4, enabled = false, order = -1), "foo", "disabled", isRegex = false),
        )
        val replacement = LegadoLiteralReplacement(rules, "Current Book", "source-x")
        assertEquals("done done", replacement.replace("foo foo"))
    }

    @Test
    fun appliesRegexReplacementWithCaptureGroups() {
        val rules = listOf(
            LegadoReplaceRule(
                LegadoReplaceRuleScope(9),
                """(\d+)""",
                """[$1]""",
                isRegex = true,
                timeoutMillisecond = 1000,
            ),
        )
        val replacement = LegadoLiteralReplacement(rules, "Book", "source")
        assertEquals("chapter [123]", replacement.replace("chapter 123"))
    }

    @Test
    fun stopsCatastrophicRegexWithinConfiguredDeadline() {
        val rules = listOf(
            LegadoReplaceRule(
                LegadoReplaceRuleScope(10),
                """^(a|aa)+$""",
                "x",
                isRegex = true,
                timeoutMillisecond = 100,
            ),
        )
        val replacement = LegadoLiteralReplacement(rules, "Book", "source")
        assertFailsWith<LegadoRegexTimeoutException> {
            replacement.replace("a".repeat(64) + "!")
        }
    }

    @Test
    fun failsClosedForJavaScriptReplacementExpressions() {
        val rules = listOf(
            LegadoReplaceRule(
                LegadoReplaceRuleScope(11),
                """\d+""",
                "@js:result",
                isRegex = true,
            ),
        )
        val replacement = LegadoLiteralReplacement(rules, "Book", "source")
        assertFailsWith<UnsupportedOperationException> {
            replacement.replace("chapter 123")
        }
    }

    @Test
    fun ignoresEmptyPatternsLikeLegadoContentProcessor() {
        val replacement = LegadoLiteralReplacement(
            listOf(LegadoReplaceRule(LegadoReplaceRuleScope(1), "", "ignored", isRegex = false)),
            "Book",
            "source",
        )
        assertEquals("unchanged", replacement.replace("unchanged"))
    }
}
