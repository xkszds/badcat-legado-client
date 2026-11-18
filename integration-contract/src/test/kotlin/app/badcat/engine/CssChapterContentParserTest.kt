package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

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
    fun executesJavaScriptContentAndNextPageRules() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"@js:extractContent(result)",
                "title":"@js:extractTitle(result)",
                "nextContentUrl":"@js:nextPage(result)"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/chapter/1",
            body = "<article>raw page</article>",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success(
                when (script) {
                    "extractContent(result)" -> "<p>Script paragraph.</p>"
                    "extractTitle(result)" -> "Script chapter"
                    "nextPage(result)" -> "/chapter/2"
                    else -> error("Unexpected script")
                },
            )
        }
        val content = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parseWithScripts(
                source,
                response,
                BookChapter(0, "Fallback", response.finalUrl),
                runtime,
            ),
        ).value
        assertEquals("Script chapter", content.title)
        assertEquals("Script paragraph.", content.text)
        assertEquals("https://reader.example/chapter/2", content.nextPageUrl)
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

    @Test
    fun extractsRegexCaptureGroupsForChapterTitleAndNextPageUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"article@html",
                "title":"@regex:<h1 class=\"title\">(.*?)</h1>",
                "nextContentUrl":"@regex:class=\"next\" href=\"([^\"]+)\""
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/chapter/1",
            body = """<h1 class="title">Regex Chapter</h1><article><p>Readable text.</p></article><a class="next" href="/chapter/2">Next</a>""",
        )
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parse(
                source,
                response,
                BookChapter(0, "Fallback", response.finalUrl),
            ),
        ).value

        assertEquals("Regex Chapter", result.title)
        assertEquals("Readable text.", result.text)
        assertEquals("https://reader.example/chapter/2", result.nextPageUrl)
    }


    @Test
    fun expandsEmbeddedContentTitleAndNextPageRules() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"{{buildContentRule(result)}}",
                "title":"{{buildTitleRule(result)}}",
                "nextContentUrl":"{{buildNextRule(result)}}"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/chapter/1",
            body = """
                <h1>Embedded chapter title</h1>
                <article><p>Embedded chapter text.</p></article>
                <a class="next" href="/chapter/2">Next</a>
            """.trimIndent(),
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = when (script) {
                "buildContentRule(result)" -> EngineResult.Success("article@html")
                "buildTitleRule(result)" -> EngineResult.Success("h1@text")
                "buildNextRule(result)" -> EngineResult.Success("a.next@href")
                else -> error("Unexpected embedded content script: $script")
            }
        }

        val content = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parseWithScripts(
                source,
                response,
                BookChapter(0, "Fallback", response.finalUrl),
                runtime,
            ),
        ).value
        assertEquals("Embedded chapter title", content.title)
        assertEquals("Embedded chapter text.", content.text)
        assertEquals("https://reader.example/chapter/2", content.nextPageUrl)
    }

    @Test
    fun executesJavaScriptContentReplaceRegexAfterHtmlCleanup() = runBlocking {
        val source = BookSource(
            id = "https://replace-content.example/",
            name = "Replace content",
            baseUrl = "https://replace-content.example/",
            ruleContentJson = """{"content":"article","replaceRegex":"@js:result.replace(/AD_PLACEHOLDER/g, '').trim()"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://replace-content.example/chapter/1",
            body = "<article>Readable AD_PLACEHOLDER chapter.</article>",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(source: BookSource, script: String, bindings: Map<String, String>): EngineResult<String> =
                if (script == "result.replace(/AD_PLACEHOLDER/g, '').trim()") {
                    EngineResult.Success(bindings.getValue("result").replace("AD_PLACEHOLDER", "").trim())
                } else EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected script")
        }
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parseWithScripts(source, response, BookChapter(0, "One", response.finalUrl), runtime),
        ).value
        assertEquals("Readable  chapter.", result.text)
    }

    @Test
    fun rejectsContentRulesThatRequireUnavailableWebViewOrBatchRuntime() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"article","webJs":"document.body.innerText"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/chapter", body = "<article>Body</article>")
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(source: BookSource, script: String, bindings: Map<String, String>): EngineResult<String> =
                EngineResult.Success(script)
        }
        val failure = assertIs<EngineResult.Failure>(
            CssChapterContentParser().parseWithScripts(source, response, BookChapter(0, "One", response.finalUrl), runtime),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
        kotlin.test.assertTrue(failure.message.contains("webJs"))
    }
}
