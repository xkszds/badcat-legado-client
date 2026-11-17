package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BadcatPpRuleEvaluatorTest {

    @Test
    fun regexPpRuleReturnsFirstCaptureGroup() {
        val document = SourceDocument(
            url = "https://reader.example/book",
            body = "<h1>Book: Alpha</h1><p>Summary</p>",
        )
        val result = BadcatPpRuleEvaluator.extractScalar(
            document,
            BadcatPpRule(ruleTypeValue = 0, pageRule = """Book:\s*(\w+)"""),
        )
        assertEquals("Alpha", assertIs<EngineResult.Success<String?>>(result).value)
    }

    @Test
    fun xpathPpRuleReturnsMetaContentOrElementText() {
        val document = SourceDocument(
            url = "https://reader.example/book",
            body = """<html><head><meta property="og:novel:book_name" content="Alpha Book"></head>
                <body><h1>Chapter One</h1></body></html>""",
        )
        val meta = BadcatPpRuleEvaluator.extractScalar(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = """//meta[@property="og:novel:book_name"]"""),
        )
        val heading = BadcatPpRuleEvaluator.extractScalar(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//h1"),
        )
        assertEquals("Alpha Book", assertIs<EngineResult.Success<String?>>(meta).value)
        assertEquals("Chapter One", assertIs<EngineResult.Success<String?>>(heading).value)
    }

    @Test
    fun xpathPpRuleReturnsNullWhenNoNodeMatches() {
        val result = BadcatPpRuleEvaluator.extractScalar(
            SourceDocument("https://reader.example/", "<p>text</p>"),
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//h1"),
        )
        assertNull(assertIs<EngineResult.Success<String?>>(result).value)
    }

    @Test
    fun rejectsUnknownRuleTypesAndRegexWithoutCaptureGroup() {
        val document = SourceDocument("https://reader.example/", "Alpha")
        assertEquals(
            EngineErrorCode.UNSUPPORTED_RULE,
            assertIs<EngineResult.Failure>(
                BadcatPpRuleEvaluator.extractScalar(
                    document,
                    BadcatPpRule(ruleTypeValue = 99, pageRule = "Alpha"),
                ),
            ).code,
        )
        assertEquals(
            EngineErrorCode.PARSE,
            assertIs<EngineResult.Failure>(
                BadcatPpRuleEvaluator.extractScalar(
                    document,
                    BadcatPpRule(ruleTypeValue = 0, pageRule = "Alpha"),
                ),
            ).code,
        )
    }

    @Test
    fun selectsPpRuleCollectionFragmentsForRegexAndXpath() {
        val document = SourceDocument(
            url = "https://reader.example/",
            body = "<ul><li>One</li><li>Two</li></ul>",
        )
        val regex = BadcatPpRuleEvaluator.selectElements(
            document,
            BadcatPpRule(ruleTypeValue = 0, pageRule = """<li>(.*?)</li>"""),
        )
        val xpath = BadcatPpRuleEvaluator.selectElements(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//li"),
        )
        assertEquals(
            listOf("<li>One</li>", "<li>Two</li>"),
            assertIs<EngineResult.Success<List<String>>>(regex).value,
        )
        assertEquals(
            listOf("<li>One</li>", "<li>Two</li>"),
            assertIs<EngineResult.Success<List<String>>>(xpath).value,
        )
    }

    @Test
    fun extractsVideoLinksAndResolvesRelativeHrefFromXpathAndRegexRules() {
        val document = SourceDocument(
            url = "https://video.example/watch/list",
            body = """<ul><li><a href="../play/1">Episode 1</a></li>
                <li><a href="https://cdn.example/play/2">Episode 2</a></li></ul>""",
        )
        val xpath = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//li"),
        )
        assertEquals(
            listOf(
                BadcatVideoLink("Episode 1", "https://video.example/play/1"),
                BadcatVideoLink("Episode 2", "https://cdn.example/play/2"),
            ),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(xpath).value,
        )

        val regex = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 0, pageRule = """<a href="([^"]+)">([^<]+)</a>"""),
        )
        assertEquals(
            listOf(
                BadcatVideoLink("Episode 1", "https://video.example/play/1"),
                BadcatVideoLink("Episode 2", "https://cdn.example/play/2"),
            ),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(regex).value,
        )
    }

    @Test
    fun videoLinkExtractionRejectsNonHttpTargetsAndMissingLabels() {
        val document = SourceDocument(
            url = "https://video.example/",
            body = """<a href="javascript:alert(1)">Unsafe</a><a href="/empty"></a>""",
        )
        val result = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//a"),
        )
        assertEquals(emptyList(), assertIs<EngineResult.Success<List<BadcatVideoLink>>>(result).value)
    }

}