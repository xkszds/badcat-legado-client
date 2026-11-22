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
        ruleReviewJson = """{"reviewList":"div.review"}""",
    )

    @Test
    fun resolvesEachOperationToItsOwnRuleObject() {
        val expected = mapOf(
            SourceRuleStage.EXPLORE to "div.explore",
            SourceRuleStage.SEARCH to "div.search",
            SourceRuleStage.BOOK_INFO to "h1@text",
            SourceRuleStage.TOC to "a.chapter",
            SourceRuleStage.CONTENT to "#content",
            SourceRuleStage.REVIEW to "div.review",
        )

        expected.forEach { (stage, selector) ->
            val result = SourceRuleResolver.resolve(source, stage)
            val raw = assertIs<EngineResult.Success<String>>(result).value
            assertTrue(raw.contains(selector))
        }
    }

    @Test
    fun resolvesNamedFieldsFromNestedRuleObject() {
        val result = SourceRuleResolver.resolveObject(source, SourceRuleStage.SEARCH)
        val rules = assertIs<EngineResult.Success<SourceRuleObject>>(result).value
        assertEquals("div.search", rules.rule("bookList"))
        assertEquals(SourceRuleStage.SEARCH, rules.stage)
        assertEquals(source.id, rules.sourceId)
    }

    @Test
    fun evaluatesNamedFieldThroughSelectedParser() {
        val document = SourceDocument(
            url = "https://reader.example/search",
            body = """<div class="search">Alpha</div><div class="search">Beta</div>""",
        )
        val result = SourceRuleResolver.evaluateField(
            source = source,
            stage = SourceRuleStage.SEARCH,
            field = "bookList",
            document = document,
            type = SelectorRuleType.CSS,
        )
        assertEquals(
            listOf("Alpha", "Beta"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun missingNamedFieldIsReportedAsUnsupported() {
        val result = SourceRuleResolver.evaluateField(
            source = source,
            stage = SourceRuleStage.SEARCH,
            field = "missing",
            document = SourceDocument("https://reader.example/", "<p/>"),
            type = SelectorRuleType.CSS,
        )
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(result).code,
        )
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
