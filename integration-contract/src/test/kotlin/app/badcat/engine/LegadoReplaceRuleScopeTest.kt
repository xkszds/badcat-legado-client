package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class LegadoReplaceRuleScopeTest {

    @Test
    fun contentRulesRespectEnabledDomainScopeExclusionAndOrder() {
        val rules = listOf(
            LegadoReplaceRuleScope(1, order = 4),
            LegadoReplaceRuleScope(2, enabled = false, order = 0),
            LegadoReplaceRuleScope(3, scope = "Other Book", order = 1),
            LegadoReplaceRuleScope(4, scope = "My Book", order = 3),
            LegadoReplaceRuleScope(5, scope = "My Book", excludeScope = "origin-x", order = 2),
            LegadoReplaceRuleScope(6, scopeTitle = true, scopeContent = false, order = -1),
        )

        assertEquals(
            listOf(1L, 4L),
            LegadoReplaceRuleSelector.forContent(rules, "My Book", "origin-x").map { it.id },
        )
    }

    @Test
    fun titleAndSourceDomainsAreSelectedIndependently() {
        val rules = listOf(
            LegadoReplaceRuleScope(1, scopeTitle = true, scopeContent = false, order = 2),
            LegadoReplaceRuleScope(2, scopeSource = true, scopeTitle = false, order = 1),
            LegadoReplaceRuleScope(3, scopeSource = true, enabled = false, order = 0),
        )

        assertEquals(
            listOf(1L),
            LegadoReplaceRuleSelector.forTitle(rules, "Book", "origin").map { it.id },
        )
        assertEquals(
            listOf(2L),
            LegadoReplaceRuleSelector.forSource(rules).map { it.id },
        )
    }

    @Test
    fun scopeUsesSqliteLikeWildcardsAndAsciiCaseFolding() {
        val rules = listOf(
            LegadoReplaceRuleScope(1, scope = "FANTASY"),
            LegadoReplaceRuleScope(2, scope = "Sci_i"),
            LegadoReplaceRuleScope(3, scope = "ÄBC"),
        )

        assertEquals(
            listOf(1L, 2L),
            LegadoReplaceRuleSelector.forContent(rules, "fantasy", "SciX").map { it.id },
        )
        assertEquals(
            emptyList(),
            LegadoReplaceRuleSelector.forContent(rules, "äbc", "none").map { it.id },
        )
    }
}
