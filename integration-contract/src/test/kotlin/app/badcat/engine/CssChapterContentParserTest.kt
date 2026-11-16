package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CssChapterContentParserTest {
    @Test
    fun extractsReadableTextAndResolvesNextContentPageUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"div#content",
                "title":"h1.title@text",
                "nextContentUrl":"a.next"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/chapter/1",
            body = """
                <h1 class="title">Chapter One</h1>
                <div id="content">
                  <p>First paragraph.</p>
                  <p>Second <b>paragraph</b>.</p>
                  <script>adPayload()</script>
                </div>
                <a class="next" href="../chapter/2">Next</a>
            """.trimIndent(),
        )
        val chapter = BookChapter(0, "Fallback title", response.finalUrl)

        val result = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parse(source, response, chapter),
        ).value

        assertEquals("Chapter One", result.title)
        assertEquals("First paragraph.\nSecond paragraph.", result.text)
        assertEquals("https://reader.example/chapter/2", result.nextPageUrl)
    }

    @Test
    fun rejectsScriptDrivenContentRules() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"@js:getContent()"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/chapter", body = "<p>body</p>")

        assertIs<EngineResult.Failure>(
            CssChapterContentParser().parse(source, response, BookChapter(0, "Title", response.finalUrl)),
        )
    }

    @Test
    fun rejectsMalformedNextContentUrlSelectorWithoutThrowing() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"article","nextContentUrl":"["}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/chapter",
            body = "<article>Readable body</article>",
        )

        val failure = assertIs<EngineResult.Failure>(
            CssChapterContentParser().parse(
                source,
                response,
                BookChapter(0, "Chapter", response.finalUrl),
            ),
        )

        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun usesChapterTitleWhenNoTitleRuleExists() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"article@html"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/chapter", body = "<article>Body text</article>")

        val result = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parse(source, response, BookChapter(0, "Known title", response.finalUrl)),
        ).value

        assertEquals("Known title", result.title)
        assertEquals("Body text", result.text)
    }
    @Test
    fun dispatchesXPathForNextChapterPageLinks() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"article",
                "nextContentUrl":"@xpath://a[@class='next']/@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/chapter/1",
            body = """<article>Readable text</article><a class="next" href="/chapter/2">Next</a>""",
        )
        val chapter = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parse(source, response, BookChapter(0, "One", response.finalUrl)),
        ).value
        assertEquals("https://reader.example/chapter/2", chapter.nextPageUrl)
    }

}
