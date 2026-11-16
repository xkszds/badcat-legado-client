package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class CssBookInfoParserTest {

    @Test
    fun parsesBookInfoFieldsAndResolvesCoverAndTocUrls() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{
                "name":"h1@text",
                "author":".author@text",
                "intro":".intro@text",
                "coverUrl":"img@src",
                "lastChapter":".latest@text",
                "tocUrl":"a.catalog@href"
            }""".trimIndent(),
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/book/7",
            body = """
                <h1>Example Novel</h1>
                <span class="author">Writer</span>
                <p class="intro">A short introduction.</p>
                <img src="../covers/7.jpg">
                <a class="catalog" href="/book/7/catalog">Catalog</a>
                <span class="latest">Chapter 8</span>
            """.trimIndent(),
        )

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(source, response, response.finalUrl),
        ).value

        assertEquals("Example Novel", detail.name)
        assertEquals("Writer", detail.author)
        assertEquals("A short introduction.", detail.intro)
        assertEquals("https://reader.example/covers/7.jpg", detail.coverUrl)
        assertEquals("https://reader.example/book/7/catalog", detail.tocUrl)
        assertEquals("Chapter 8", detail.latestChapter)
    }

    @Test
    fun dispatchesXPathAndJsonPathBookInfoFields() {
        val xpathSource = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"@xpath://h1/text()"}""",
        )
        val htmlResponse = SourceHttpResponse(
            200, "https://reader.example/book/7", body = "<h1>XPath Novel</h1>",
        )
        val xpathDetail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(xpathSource, htmlResponse, htmlResponse.finalUrl),
        ).value
        assertEquals("XPath Novel", xpathDetail.name)

    }

    @Test
    fun executesJavaScriptBookInfoFieldsWithSourceMetadata() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"@js:extractName(result)","author":"p.author@text"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/9",
            body = "<h1>Script title</h1><p class='author'>Writer</p>",
        )
        val seen = mutableMapOf<String, String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                seen.putAll(bindings)
                assertEquals("extractName(result)", script)
                return EngineResult.Success("Resolved title")
            }
        }
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parseWithScripts(source, response, response.finalUrl, runtime),
        ).value
        assertEquals("Resolved title", detail.name)
        assertEquals("Writer", detail.author)
        assertEquals("https://reader.example/", seen["sourceId"])
        assertEquals("https://reader.example/book/9", seen["baseUrl"])
        assertEquals(response.body, seen["result"])
    }

    @Test
    fun rejectsBlankRequestedBookUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"h1@text"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/7",
            body = "<h1>Novel</h1>",
        )

        val failure = assertIs<EngineResult.Failure>(
            CssBookInfoParser().parse(source, response, "  "),
        )

        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun rejectsUnsafeRequestedBookUrl() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"h1@text"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/7",
            body = "<h1>Novel</h1>",
        )

        val failure = assertIs<EngineResult.Failure>(
            CssBookInfoParser().parse(source, response, "javascript:alert(1)"),
        )

        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun missingBookNameIsNotFound() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"h1@text"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/book", body = "<h2>Not the title</h2>")

        val result = CssBookInfoParser().parse(source, response, response.finalUrl)
        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun scriptFieldsAreReportedAsUnsupported() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"@js:java.getName()"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/book", body = "<h1>Novel</h1>")

        val result = CssBookInfoParser().parse(source, response, response.finalUrl)
        assertIs<EngineResult.Failure>(result)
    }
    @Test
    fun rejectsUnsafeCoverAndTocLinks() {
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/7",
            body = "<h1>Novel</h1><img src=\"file:///etc/passwd\"><a class=\"catalog\" href=\"javascript:alert(1)\">Catalog</a>",
        )
        val coverSource = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{\"name\":\"h1@text\",\"coverUrl\":\"img@src\"}""",
        )
        val tocSource = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{\"name\":\"h1@text\",\"tocUrl\":\"a.catalog@href\"}""",
        )
        assertIs<EngineResult.Failure>(CssBookInfoParser().parse(coverSource, response, response.finalUrl))
        assertIs<EngineResult.Failure>(CssBookInfoParser().parse(tocSource, response, response.finalUrl))
    }

    @Test
    fun parsesRegexScalarBookNameAndAuthorFields() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{
                "name":"@regex:<h1>(.*?)</h1>",
                "author":"@regex:Author:\\s*(.*?)<"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/regex",
            body = "<h1>Regex Novel</h1><p>Author: Regex Writer</p>",
        )
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(source, response, response.finalUrl),
        ).value
        assertEquals("Regex Novel", detail.name)
        assertEquals("Regex Writer", detail.author, "Regex author extraction returned: ${detail.author}")
    }


    @Test
    fun expandsEmbeddedBookInfoFieldRules() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"{{buildNameRule(result)}}" }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/10",
            body = "<h1>Embedded novel</h1>",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                assertEquals("buildNameRule(result)", script)
                assertEquals(response.body, bindings["result"])
                return EngineResult.Success("h1@text")
            }
        }

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parseWithScripts(source, response, response.finalUrl, runtime),
        ).value
        assertEquals("Embedded novel", detail.name)
    }

}
