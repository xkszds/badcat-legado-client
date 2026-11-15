package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SourceRuleResolverTest {

    private val source = BookSource(
        id = "https://reader.example/",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        ruleExploreJson = """{"bookList":"div.explore"}""",
        ruleSearchJson = """{"bookList":"div.search"}""",
        ruleBookInfoJson = """{"name":"h1@text"}""",
        ruleTocJson = """{"chapterList":"a.chapter"}""",
        ruleContentJson = """{"content":"#content"}""",
    )

    @Test
    fun resolvesEachOperationToItsOwnRuleObject() {
        val expected = mapOf(
            SourceRuleStage.EXPLORE to "div.explore",
            SourceRuleStage.SEARCH to "div.search",
            SourceRuleStage.BOOK_INFO to "h1@text",
            SourceRuleStage.TOC to "a.chapter",
            SourceRuleStage.CONTENT to "#content",
        )

        expected.forEach { (stage, selector) ->
            val result = SourceRuleResolver.resolve(source, stage)
            val raw = assertIs<EngineResult.Success<String>>(result).value
            assertTrue(raw.contains(selector))
        }
    }

    @Test
    fun missingRuleIsReportedAsUnsupported() {
        val result = SourceRuleResolver.resolve(source.copy(ruleContentJson = null), SourceRuleStage.CONTENT)
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun malformedOrNonObjectRuleIsRejected() {
        val malformed = SourceRuleResolver.resolve(
            source.copy(ruleSearchJson = """["not","an","object"]"""),
            SourceRuleStage.SEARCH,
        )
        val failure = assertIs<EngineResult.Failure>(malformed)
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }
}
