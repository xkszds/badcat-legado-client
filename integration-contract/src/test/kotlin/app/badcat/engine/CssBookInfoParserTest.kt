package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class CssBookInfoParserTest {

    @Test
    fun parsesDetailNameUsingLegacyAtRootIndexedTextNodesRule() {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{"name":"@.w_txt li.4@textNodes"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/7",
            body = """
                <ul class="w_txt">
                  <li>zero</li><li>one</li><li>two</li><li>three</li><li>Novel title</li>
                </ul>
            """.trimIndent(),
        )
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(source, response, response.finalUrl),
        ).value
        assertEquals("Novel title", detail.name)
    }

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
    fun appliesJsonInitAndStoresBookInfoPutVariables() = runBlocking {
        val source = BookSource(
            id = "https://api-reader.example/",
            name = "JSON fixture",
            baseUrl = "https://api-reader.example/",
            ruleBookInfoJson = """{
                "init":"data.book",
                "name":"title@put:{bid:id}",
                "author":"author"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://api-reader.example/book/7",
            contentType = "application/json",
            body = """{"data":{"book":{"id":"42","title":"JSON Novel","author":"Writer"}}}""",
        )
        val stored = linkedMapOf<String, String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "No script expected")

            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                stored[key] = value
                return EngineResult.Success(Unit)
            }
        }

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parseWithScripts(source, response, response.finalUrl, runtime),
        ).value
        assertEquals("JSON Novel", detail.name)
        assertEquals("Writer", detail.author)
        assertEquals("42", stored["bid"])
    }


    @Test
    fun usesSearchResultTitleWhenLegacySourceOmitsBookInfoNameRule() {
        val source = BookSource(
            id = "https://www.quanben.io",
            name = "全本小说",
            baseUrl = "https://www.quanben.io",
            ruleBookInfoJson = """{"kind":"[property='og:novel:category']@content","tocUrl":"text.章节列表@href"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://www.quanben.io/n/guimizhizhu/",
            body = """<html><head><meta property="og:novel:category" content="玄幻"></head>
                <body><h1>诡秘之主</h1><a href="/n/guimizhizhu/list.html">章节列表</a></body></html>""",
        )

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(source, response, response.finalUrl, "诡秘之主"),
        ).value

        assertEquals("诡秘之主", detail.name)
        assertEquals("https://www.quanben.io/n/guimizhizhu/list.html", detail.tocUrl)
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


    @Test
    fun executesLegacyUnquotedPutInitAndReadsStoredBookInfoFields() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleBookInfoJson = """{
                "init":"@put:{n:\"meta[property='book_name']@content\",a:\"meta[property='author']@content\"}",
                "name":"@get:{n}",
                "author":"@get:{a}"
            }""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/book/1",
            body = """<meta property="book_name" content="Stored Novel"><meta property="author" content="Stored Writer">""",
        )
        val values = mutableMapOf<String, String>()
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success(script)
            override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
                values[key] = value
                return EngineResult.Success(Unit)
            }
            override suspend fun getString(source: BookSource, key: String): EngineResult<String?> =
                EngineResult.Success(values[key])
        }

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parseWithScripts(source, response, response.finalUrl, runtime),
        ).value

        assertEquals("Stored Novel", detail.name)
        assertEquals("Stored Writer", detail.author)
        assertEquals("meta[property='book_name']@content", values["n"])
        assertEquals("meta[property='author']@content", values["a"])
    }

}
