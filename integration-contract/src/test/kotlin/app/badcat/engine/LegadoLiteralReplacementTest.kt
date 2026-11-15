package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LegadoLiteralReplacementTest {

    @Test
    fun appliesEnabledLiteralContentRulesInLegadoOrderAndScope() {
        val rules = listOf(
            LegadoReplaceRule(
                LegadoReplaceRuleScope(1, order = 0),
                "foo",
                "bar",
                isRegex = false,
            ),
            LegadoReplaceRule(
                LegadoReplaceRuleScope(2, scope = "Other Book", order = 0),
                "foo",
                "wrong",
                isRegex = false,
            ),
            LegadoReplaceRule(
                LegadoReplaceRuleScope(3, order = 1),
                "bar",
                "done",
                isRegex = false,
            ),
            LegadoReplaceRule(
                LegadoReplaceRuleScope(4, enabled = false, order = -1),
                "foo",
                "disabled",
                isRegex = false,
            ),
        )

        val replacement = LegadoLiteralReplacement(rules, "Current Book", "source-x")
        assertEquals("done done", replacement.replace("foo foo"))
    }

    @Test
    fun failsClosedWhenApplicableRegexRuleNeedsFullLegadoRuntime() {
        val rules = listOf(
            LegadoReplaceRule(
                LegadoReplaceRuleScope(9),
                "\\d+",
                "#",
                isRegex = true,
            ),
        )
        val replacement = LegadoLiteralReplacement(rules, "Book", "source")

        assertFailsWith<IllegalArgumentException> {
            replacement.replace("chapter 123")
        }
    }

    @Test
    fun ignoresEmptyPatternsLikeLegadoContentProcessor() {
        val replacement = LegadoLiteralReplacement(
            listOf(
                LegadoReplaceRule(
                    LegadoReplaceRuleScope(1),
                    "",
                    "ignored",
                    isRegex = false,
                ),
            ),
            "Book",
            "source",
        )

        assertEquals("unchanged", replacement.replace("unchanged"))
    }
}
