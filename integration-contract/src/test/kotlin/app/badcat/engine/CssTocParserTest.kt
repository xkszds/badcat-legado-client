package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CssTocParserTest {
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
        assertEquals(listOf("https://reader.example/catalog?page=2"), page.nextPageUrls)
    }

    @Test
    fun extractsTocItemsFromLegadoRegexChapterList() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":":<li[^>]*chapter[^>]*>[\\s\\S]*?</li>",
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
                "nextTocUrl":"@regex:class=\\"next\\" href=\\"([^\\"]+)\\""
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/catalog?page=1",
            body = """<ul class="chapters"><li><a href="/chapter/1">One</a></li></ul><a class="next" href="/catalog?page=2">Next</a>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(source, response, "https://reader.example/book"),
        ).value

        assertEquals(listOf("https://reader.example/catalog?page=2"), page.nextPageUrls)
    }

}
