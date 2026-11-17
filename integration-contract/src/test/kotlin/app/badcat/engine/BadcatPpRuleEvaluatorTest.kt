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


    @Test
    fun videoLinkExtractionRejectsMalformedHttpUrisAndCredentials() {
        val document = SourceDocument(
            url = "https://video.example/watch/",
            body = """<a href="https:relative-target">Relative scheme</a>
                <a href="https://user:pass@evil.example/watch">Credential URL</a>
                <a href="//cdn.example/episode">Protocol-relative</a>""",
        )
        val result = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//a"),
        )
        assertEquals(
            listOf(BadcatVideoLink("Protocol-relative", "https://cdn.example/episode")),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(result).value,
        )
    }

    @Test
    fun videoLinkExtractionDeduplicatesResolvedUrls() {
        val document = SourceDocument(
            url = "https://video.example/list",
            body = """<a href="/episode/1">Episode one</a>
                <a href="https://video.example/episode/1">Duplicate label</a>""",
        )
        val result = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//a"),
        )
        assertEquals(
            listOf(BadcatVideoLink("Episode one", "https://video.example/episode/1")),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(result).value,
        )
    }
    @Test
    fun videoLinkExtractionUsesTitleAndAriaLabelWhenVisibleTextIsMissing() {
        val document = SourceDocument(
            url = "https://video.example/list",
            body = """<a href="/episode/1" title="Tooltip episode"></a>
                <a href="/episode/2" aria-label="Accessible episode"></a>""",
        )
        val result = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//a"),
        )
        assertEquals(
            listOf(
                BadcatVideoLink("Tooltip episode", "https://video.example/episode/1"),
                BadcatVideoLink("Accessible episode", "https://video.example/episode/2"),
            ),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(result).value,
        )
    }
    @Test
    fun extractsComicImagesPreferringDataOriginalAndFallingBackToSrc() {
        val document = SourceDocument(
            url = "https://comic.example/reader/page/1",
            body = """<div class="pages">
                <img data-original="../images/one.jpg" src="/placeholder.gif">
                <img src="//cdn.example/two.webp">
                <img data-original="javascript:alert(1)" src="/images/three.png">
                <img src="file:///etc/passwd">
            </div>""",
        )
        val result = BadcatPpRuleEvaluator.extractComicImageUrls(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//div[@class='pages']/img"),
        )
        assertEquals(
            listOf(
                "https://comic.example/reader/images/one.jpg",
                "https://cdn.example/two.webp",
                "https://comic.example/reader/page/images/three.png",
            ),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }

    @Test
    fun comicImageExtractionDeduplicatesNormalizedUrls() {
        val document = SourceDocument(
            url = "https://comic.example/reader/",
            body = """<img src="../page/1.jpg"><img data-original="/page/1.jpg">""",
        )
        val result = BadcatPpRuleEvaluator.extractComicImageUrls(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//img"),
        )
        assertEquals(
            listOf("https://comic.example/page/1.jpg"),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }



    @Test
    fun extractsEveryNestedVideoLinkFromOneSelectedContainer() {
        val document = SourceDocument(
            url = "https://video.example/watch/list",
            body = """<section id="episodes">
                <a href="/play/1">Episode 1</a>
                <div><a href="../play/2" aria-label="Episode 2"></a></div>
                <a href="javascript:alert(1)">Unsafe</a>
            </section>""",
        )
        val result = BadcatPpRuleEvaluator.extractVideoLinks(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//section[@id='episodes']"),
        )
        assertEquals(
            listOf(
                BadcatVideoLink("Episode 1", "https://video.example/play/1"),
                BadcatVideoLink("Episode 2", "https://video.example/play/2"),
            ),
            assertIs<EngineResult.Success<List<BadcatVideoLink>>>(result).value,
        )
    }

    @Test
    fun extractsEveryNestedComicImageFromOneSelectedContainer() {
        val document = SourceDocument(
            url = "https://comic.example/reader/page/1",
            body = """<section id="pages">
                <img data-original="/images/one.jpg" src="/placeholder.gif">
                <div><img src="../images/two.jpg"></div>
                <img src="data:image/png;base64,AAAA">
            </section>""",
        )
        val result = BadcatPpRuleEvaluator.extractComicImageUrls(
            document,
            BadcatPpRule(ruleTypeValue = 1, pageRule = "//section[@id='pages']"),
        )
        assertEquals(
            listOf(
                "https://comic.example/images/one.jpg",
                "https://comic.example/reader/images/two.jpg",
            ),
            assertIs<EngineResult.Success<List<String>>>(result).value,
        )
    }


    @Test
    fun chapterListOrderUsesOnlyTheVerifiedReverseFlag() {
        val chapters = listOf("Chapter 1", "Chapter 2", "Chapter 3")
        assertEquals(
            chapters,
            BadcatPpRuleEvaluator.applyChapterListOrder(
                chapters,
                BadcatPpRule(chapterIsReverse = false),
            ),
        )
        assertEquals(
            listOf("Chapter 3", "Chapter 2", "Chapter 1"),
            BadcatPpRuleEvaluator.applyChapterListOrder(
                chapters,
                BadcatPpRule(chapterIsReverse = true),
            ),
        )
    }

}
