package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CssTocParserTest {
    @Test
    fun parsesJsonChapterListWithBareFieldPaths() {
        val source = BookSource(
            id = "fixture",
            name = "JSON fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"data.chapter_lists",
                "chapterName":"title",
                "chapterUrl":"id"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/api/toc",
            contentType = "application/json",
            body = """{"data":{"chapter_lists":[{"id":"42","title":"Chapter 42"}]}}""",
        )
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book/7"),
        ).value
        assertEquals(listOf("Chapter 42"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/api/42"), page.chapters.map { it.url })
    }

    @Test
    fun parsesLegacySingleAtIndexedTextNodesChapterListEndToEnd() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"@.w_txt li.4@textNodes",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/42/catalog",
            body = """
                <ul class="w_txt">
                  <li><a href="/chapter/1">Chapter One</a></li>
                  <li><a href="/chapter/2">Chapter Two</a></li>
                  <li><a href="/chapter/3">Chapter Three</a></li>
                  <li><a href="/chapter/4">Chapter Four</a></li>
                  <li><a href="/chapter/5">Novel title</a></li>
                </ul>
            """.trimIndent(),
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book/42"),
        ).value

        assertEquals(listOf("Novel title"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/chapter/5"), page.chapters.map { it.url })
    }

    @Test
    fun parsesChapterItemsAndResolvesRelativeLinksAndNextPage() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "nextTocUrl":"a.next"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/1/catalog?page=1",
            body = """
                <ul class="chapters">
                  <li><a href="../chapter/1">Chapter One</a></li>
                  <li><a href="/chapter/2">Chapter Two</a></li>
                </ul>
                <a class="next" href="?page=2">Next</a>
            """.trimIndent(),
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book/1"),
        ).value

        assertEquals(listOf("Chapter One", "Chapter Two"), page.chapters.map { it.title })
        assertEquals(
            listOf("https://reader.example/book/chapter/1", "https://reader.example/chapter/2"),
            page.chapters.map { it.url },
        )
        assertEquals(listOf("https://reader.example/book/1/catalog?page=2"), page.nextPageUrls)
    }


    @Test
    fun resolvesRelativeChapterLinksAgainstRedirectedTocResponseUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://cdn.reader.example/novel/42/catalog/index.html",
            body = """<ul class="chapters"><li><a href="../chapter/1.html">Chapter One</a></li></ul>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book/42"),
        ).value

        assertEquals(
            listOf("https://cdn.reader.example/novel/42/chapter/1.html"),
            page.chapters.map { it.url },
        )
    }

    @Test
    fun parsesJsonPathChapterListAndPaginationLinks() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"@jsonpath:$.chapters[*]",
                "chapterName":"$.title",
                "chapterUrl":"$.url",
                "nextTocUrl":"$.next"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog?page=1",
            body = """{"chapters":[{"title":"JSON Chapter 1","url":"/chapter/1"},{"title":"JSON Chapter 2","url":"/chapter/2"}],"next":"/catalog?page=2"}""",
            contentType = "application/json",
        )

        val parsed = CssTocParser().parse(source, response, "https://reader.example/book")
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            parsed,
            "JSONPath TOC parsing returned: $parsed",
        ).value

        assertEquals(listOf("JSON Chapter 1", "JSON Chapter 2"), page.chapters.map { it.title })
        assertEquals(
            listOf("https://reader.example/chapter/1", "https://reader.example/chapter/2"),
            page.chapters.map { it.url },
        )
        assertEquals(listOf("https://reader.example/catalog?page=2"), page.nextPageUrls)
    }

    @Test
    fun explainsWhenTocSelectorMatchesNoRecords() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{"chapterList":"ul.chapters li","chapterName":"a@text","chapterUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = "<html><body>Access denied</body></html>",
            contentType = "text/html",
        )

        val failure = assertIs<EngineResult.Failure>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        )
        assertTrue(failure.message.contains("chapterList matched zero records"))
        assertTrue(failure.message.contains("Access denied"))
        assertTrue(failure.message.contains("text/html"))
    }

    @Test
    fun rejectsScriptBasedTocRules() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{"chapterList":"@js:loadToc()"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/catalog", body = "<p>empty</p>")

        assertIs<EngineResult.Failure>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        )
    }
    @Test
    fun rejectsExecutableAndCredentialBearingChapterAndNextPageUrls() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "nextTocUrl":"a.next"
            }""",
        )
        val parser = CssTocParser()
        val unsafeChapter = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="javascript:alert(1)">Unsafe</a></li></ul>""",
        )
        val credentialNextPage = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Chapter</a></li></ul>
                <a class="next" href="https://user:pass@reader.example/catalog?page=2">Next</a>""",
        )

        assertIs<EngineResult.Failure>(
            parser.parse(source, unsafeChapter, "https://reader.example/book"),
        )
        assertIs<EngineResult.Failure>(
            parser.parse(source, credentialNextPage, "https://reader.example/book"),
        )
    }


    @Test
    fun rejectsMalformedNextTocSelectorWithoutThrowing() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "nextTocUrl":"["
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>""",
        )

        val failure = assertIs<EngineResult.Failure>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        )

        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsMissingChapterUrlRuleInsteadOfUsingTocPageAsChapterUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>""",
        )

        val result = CssTocParser().parse(source, response, "https://reader.example/book")
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsChapterItemWithoutResolvableChapterUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@attr(href)"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a>Chapter One</a></li></ul>""",
        )

        val result = CssTocParser().parse(source, response, "https://reader.example/book")
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun dispatchesXPathForNextTocPageLinks() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "nextTocUrl":"@xpath://a[@class='next']/@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog?page=1",
            body = """<ul class="chapters"><li><a href="/chapter/1">One</a></li></ul><a class="next" href="?page=2">Next</a>""",
        )
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value
        assertEquals(
            listOf("https://reader.example/catalog?page=2"),
            page.nextPageUrls,
            "Regex nextTocUrl extraction produced: ${page.nextPageUrls}",
        )
    }

    @Test
    fun extractsTocItemsFromLegadoRegexChapterList() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"@regex:<li[^>]*chapter[^>]*>[\\s\\S]*?</li>",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog",
            body = """<ul><li class='chapter'><a href='/chapter/1'>Regex Chapter</a></li><li>Ignored</li></ul>""",
        )
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value
        assertEquals(listOf("Regex Chapter"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/chapter/1"), page.chapters.map { it.url })
    }


    @Test
    fun executesJavaScriptChapterFieldsWithIsolatedItemContext() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"@js:chapterTitle(result)",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">One</a></li><li><a href="/chapter/2">Two</a></li></ul>""",
        )
        val seen = mutableListOf<String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                seen += bindings["result"].orEmpty()
                return EngineResult.Success(if (seen.size == 1) "Chapter A" else "Chapter B")
            }
        }
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parseWithScripts(source, response, "https://reader.example/book", runtime),
        ).value
        assertEquals(listOf("Chapter A", "Chapter B"), page.chapters.map { it.title })
        assertEquals(2, seen.size)
        assertEquals(true, seen.first().contains("One"))
        assertEquals(true, seen.last().contains("Two"))
    }

    @Test
    fun extractsRegexCaptureGroupForChapterTitles() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"@regex:<a[^>]*>(.*?)</a>",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Regex Chapter</a></li></ul>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value

        assertEquals(listOf("Regex Chapter"), page.chapters.map { it.title })
    }


    @Test
    fun extractsRegexCaptureGroupForNextTocPageUrl() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "nextTocUrl":"@regex:class=\"next\" href=\"([^\"]+)\""
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog?page=1",
            body = """<ul class="chapters"><li><a href="/chapter/1">One</a></li></ul><a class="next" href="/catalog?page=2">Next</a>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
            "TOC regex pagination result: ${CssTocParser().parse(source, response, "https://reader.example/book")}",
        ).value

        assertEquals(listOf("https://reader.example/catalog?page=2"), page.nextPageUrls)
    }

    @Test
    fun expandsEmbeddedChapterListAndRowRules() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"{{buildListRule(result)}}",
                "chapterName":"{{buildNameRule(result)}}",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Embedded chapter</a></li></ul>""",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = when (script) {
                "buildListRule(result)" -> {
                    assertEquals(response.body, bindings["result"])
                    EngineResult.Success("ul.chapters li")
                }
                "buildNameRule(result)" -> {
                    assertEquals(true, bindings["result"]!!.contains("Embedded chapter"))
                    EngineResult.Success("a@text")
                }
                else -> error("Unexpected embedded TOC script: $script")
            }
        }

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parseWithScripts(source, response, "https://reader.example/book", runtime),
        ).value
        assertEquals(listOf("Embedded chapter"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/chapter/1"), page.chapters.map { it.url })
    }

    @Test
    fun appliesLegacyAllInOneRegexModeToTocRows() {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":":<li>.*?</li>",
                "chapterName":"<a[^>]*>(.*?)</a>",
                "chapterUrl":"href=\"([^\"]+)\""
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul><li><a href="/chapter/legacy">Legacy Chapter</a></li></ul>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value

        assertEquals(listOf("Legacy Chapter"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/chapter/legacy"), page.chapters.map { it.url })
    }

    @Test
    fun executesJavaScriptTocCollectionRootAndPassesPageContext() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"@js:renderToc(result, page)",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog?page=2",
            body = "<p>source body</p>",
        )
        var capturedScript = ""
        var capturedBindings: Map<String, String> = emptyMap()
        val scriptRuntime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                capturedScript = script
                capturedBindings = bindings
                return EngineResult.Success(
                    """<li><a href="/chapter/3">Chapter Three</a></li><li><a href="/chapter/4">Chapter Four</a></li>""",
                )
            }
        }

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parseWithScripts(
                source,
                response,
                "https://reader.example/book",
                scriptRuntime,
                page = 2,
            ),
        ).value

        assertEquals("renderToc(result, page)", capturedScript)
        assertEquals("2", capturedBindings["page"])
        assertEquals(listOf("Chapter Three", "Chapter Four"), page.chapters.map { it.title })
        assertEquals(
            listOf("https://reader.example/chapter/3", "https://reader.example/chapter/4"),
            page.chapters.map { it.url },
        )
    }


    @Test
    fun supportsCommonUnquotedPutGetDirectivesAcrossTocRows() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"$.data[*]",
                "chapterName":"$.title@put:{cid:chapter_id}",
                "chapterUrl":"https://reader.example/chapter/@get:{cid}"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            contentType = "application/json",
            body = """{"data":[{"chapter_id":"c-19","title":"Stored Chapter"}]}""",
        )
        val values = mutableMapOf<String, String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(source: BookSource, script: String, bindings: Map<String, String>) =
                EngineResult.Success(script)
            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                values[key] = value
                return EngineResult.Success(Unit)
            }
            override suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
                EngineResult.Success(values[key])
        }

        val parseResult = CssTocParser().parseWithScripts(source, response, "https://reader.example/book", runtime)
        val page = assertIs<EngineResult.Success<ParsedTocPage>>(parseResult, "TOC @put result: $parseResult").value

        assertEquals("c-19", values["cid"])
        assertEquals(listOf("Stored Chapter"), page.chapters.map { it.title })
        assertEquals(listOf("https://reader.example/chapter/c-19"), page.chapters.map { it.url })
    }

    @Test
    fun projectsVipPayAndUpdateTimeFieldsFromSynchronousTocRules() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "isVip":"a@attr(data-vip)",
                "isPay":"a@attr(data-pay)",
                "updateTime":"span@text"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1" data-vip="true" data-pay="no">Chapter One</a><span>Updated today</span></li></ul>""",
        )

        val chapter = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value.chapters.single()

        assertEquals(true, chapter.isVip)
        assertEquals(false, chapter.isPay)
        assertEquals("Updated today", chapter.tag)
        assertEquals(false, chapter.copy(index = 1).isVip.not())
    }

    @Test
    fun projectsVipPayAndUpdateTimeFieldsThroughScriptEnabledTocRules() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "isVip":"@js:vip(result)",
                "isPay":"@js:pay(result)",
                "updateTime":"@js:time(result)"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/catalog",
            body = """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>""",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = when (script) {
                "vip(result)" -> EngineResult.Success("0")
                "pay(result)" -> EngineResult.Success("yes")
                "time(result)" -> EngineResult.Success("Yesterday")
                else -> EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "unexpected script")
            }
        }

        val chapter = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parseWithScripts(source, response, "https://reader.example/book", runtime),
        ).value.chapters.single()

        assertEquals(false, chapter.isVip)
        assertEquals(true, chapter.isPay)
        assertEquals("Yesterday", chapter.tag)
    }

    @Test
    fun sourceTocPaginationPreservesMetadataWhenMergedAndReindexed() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href",
                "isVip":"a@attr(data-vip)",
                "isPay":"a@attr(data-pay)",
                "updateTime":"span@text",
                "nextTocUrl":"a.next"
            }""",
        )
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                val second = request.url.endsWith("page=2")
                val body = if (second) {
                    """<ul class="chapters"><li><a href="/chapter/2" data-vip="0" data-pay="true">Chapter Two</a><span>New</span></li></ul>"""
                } else {
                    """<ul class="chapters"><li><a href="/chapter/1" data-vip="true" data-pay="false">Chapter One</a><span>Old</span></li></ul><a class="next" href="?page=2">Next</a>"""
                }
                return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
            }
        }

        val chapters = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client)).load(source, "https://reader.example/book"),
        ).value

        assertEquals(listOf(0, 1), chapters.map { it.index })
        assertEquals(listOf(true, false), chapters.map { it.isVip })
        assertEquals(listOf(false, true), chapters.map { it.isPay })
        assertEquals(listOf("Old", "New"), chapters.map { it.tag })
    }

}
