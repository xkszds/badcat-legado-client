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
    fun preservesOperatorsInsideJsonPathFilterAndEscapedText() {
        val expression = """$.store[?(@.name == 'a || b')] && $.other"""
        assertEquals(
            listOf("""$.store[?(@.name == 'a || b')]""", "$.other"),
            RuleExpressionTokenizer.splitTopLevel(expression, "&&"),
        )

    }

    @Test
    fun preservesQuotedHtmlAttributesAndStagesInsideRegexBranches() {
        val expression = """@regex:class="next||previous" href="([^"]+)""""
        assertEquals(
            listOf(expression, "div.next"),
            RuleExpressionTokenizer.splitTopLevel("$expression||div.next", "||"),
        )
        assertEquals(
            listOf(expression),
            RuleExpressionTokenizer.splitTopLevel(expression, "||"),
        )

        val staged = """@regex:<b>(.*?)</b>&&href="([^"]+)""""
        assertEquals(null, RuleExpressionTokenizer.splitByFirstTopLevelOperator(staged).operator)
    }

    @Test
    fun protectsSeparatorsInsideRegexCharacterClassesAndAttributeValues() {
        val expression = """@regex:<a data-label="left&&right" href="([^"]+)">"""
        assertEquals(
            listOf(expression, "div.result"),
            RuleExpressionTokenizer.splitTopLevel("$expression||div.result", "||"),
        )
        val characterClass = """@regex:[a&&b]+"""
        assertEquals(
            listOf(characterClass, "p@text"),
            RuleExpressionTokenizer.splitTopLevel("$characterClass&&p@text", "&&"),
            "Separators inside a regex character class must remain literal",
        )
    }

    @Test
    fun rejectsUnclosedRegexGroupsAndCharacterClasses() {
        assertFailsWith<IllegalArgumentException> {
            RuleExpressionTokenizer.splitTopLevel("@regex:(unfinished", "||")
        }
        assertFailsWith<IllegalArgumentException> {
            RuleExpressionTokenizer.splitTopLevel("@regex:[unfinished", "||")
        }
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

    @Test
    fun escapedQuotesDoNotExposeCombinationOperators() {
        val expression = """a[data-value="one\\\"&&two"]&&b"""
        assertEquals(
            listOf("""a[data-value="one\\\"&&two"]""", "b"),
            RuleExpressionTokenizer.splitTopLevel(expression, "&&"),
        )
    }

    @Test
    fun rejectsOversizedExpressionsBeforeScanning() {
        assertFailsWith<IllegalArgumentException> {
            RuleExpressionTokenizer.splitTopLevel(
                "a".repeat(RuleExpressionTokenizer.MAX_RULE_EXPRESSION_LENGTH + 1),
                "&&",
            )
        }
    }
}
