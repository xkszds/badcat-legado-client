package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class CssSearchResultsParserTest {

    private fun source(rule: String) = BookSource(
        id = "fixture",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        ruleSearchJson = rule,
    )

    @Test
    fun parsesEachBookCardIndependentlyAndResolvesRelativeLinks() {
        val source = source(
            """{"bookList":"div.book","name":"h2@text","bookUrl":"a@href","author":".author@text","coverUrl":"img@src","lastChapter":".latest@text"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """
                <div class="book"><h2>First</h2><a href="/book/1">open</a><span class="author">A</span><img src="/1.jpg"><i class="latest">Chapter 1</i></div>
                <div class="book"><h2>Second</h2><a href="/book/2">open</a><span class="author">B</span><img src="/2.jpg"><i class="latest">Chapter 2</i></div>
            """.trimIndent(),
        )

        val result = CssSearchResultsParser().parse(source, response)
        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(result).value

        assertEquals(2, books.size)
        assertEquals("First", books[0].name)
        assertEquals("https://reader.example/book/1", books[0].bookUrl)
        assertEquals("A", books[0].author)
        assertEquals("https://reader.example/1.jpg", books[0].coverUrl)
        assertEquals("Chapter 2", books[1].latestChapter)
    }

    @Test
    fun skipsCardsWithoutRequiredNameOrBookUrlAndDeduplicatesUrls() {
        val source = source("""{"bookList":"div.book","name":"h2@text","bookUrl":"a@href"}""")
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """
                <div class="book"><h2>One</h2><a href="/same">a</a></div>
                <div class="book"><h2>Duplicate</h2><a href="/same">b</a></div>
                <div class="book"><h2>No URL</h2></div>
            """.trimIndent(),
        )

        val parsed = CssSearchResultsParser().parse(source, response)
        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            parsed,
            "Regex-root search parsing failed: $parsed",
        ).value

        assertEquals(1, books.size)
        assertEquals("One", books.single().name)
    }

    @Test
    fun preservesElementHtmlForEveryCombinedBookListBranch() {
        val source = source(
            """{"bookList":"div.primary&&div.secondary","name":"h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """
                <div class="primary"><h2>Primary</h2><a href="/p">open</a></div>
                <div class="secondary"><h2>Secondary</h2><a href="/s">open</a></div>
            """.trimIndent(),
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value

        assertEquals(listOf("Primary", "Secondary"), books.map { it.name })
        assertEquals(
            listOf("https://reader.example/p", "https://reader.example/s"),
            books.map { it.bookUrl },
        )
    }

    @Test
    fun parsesJsonPathBookListAndJsonPathFields() {
        val source = source(
            """{"bookList":"@jsonpath:$.books[*]","name":"$.name","bookUrl":"$.url","author":"$.author"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """{"books":[{"name":"JSON Alpha","url":"/json/1","author":"A"},{"name":"JSON Beta","url":"/json/2","author":"B"}]}""",
            contentType = "application/json",
        )

        val parsed = CssSearchResultsParser().parse(source, response)
        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            parsed,
            "JSONPath search parsing returned: $parsed",
        ).value

        assertEquals(listOf("JSON Alpha", "JSON Beta"), books.map { it.name })
        assertEquals(
            listOf("https://reader.example/json/1", "https://reader.example/json/2"),
            books.map { it.bookUrl },
        )
        assertEquals(listOf("A", "B"), books.map { it.author })
    }

    @Test
    fun rejectsScalarExtractionSuffixOnBookList() {
        val source = source(
            """{"bookList":"div.book@text","name":"h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """<div class="book"><h2>Book</h2><a href="/book">open</a></div>""",
        )

        val result = CssSearchResultsParser().parse(source, response)

        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun extractsOwnTextAndTagNameFromSearchFields() {
        val source = source(
            """{"bookList":"div.book","name":"h2.title@ownText","author":"h2.title@tag","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """
                <div class="book">
                    <h2 class="title">Novel <span>translated title</span></h2>
                    <a href="/novel">open</a>
                </div>
            """.trimIndent(),
        )

        val book = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value.single()

        assertEquals("Novel", book.name)
        assertEquals("h2", book.author)
        assertEquals("https://reader.example/novel", book.bookUrl)
    }

    @Test
    fun reportsScriptBasedSearchRuleAsUnsupported() {
        val source = source("""{"bookList":"{{java.getList()}}","name":"h2@text","bookUrl":"a@href"}""")
        val response = SourceHttpResponse(200, "https://reader.example/search", body = "<div></div>")

        val result = CssSearchResultsParser().parse(source, response)
        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun rejectsExecutableAndLocalSchemesInBookAndCoverUrls() {
        val parser = CssSearchResultsParser()
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """<div class="book"><h2>Unsafe</h2><a href="javascript:alert(1)">open</a><img src="file:///etc/passwd"></div>""",
        )
        val bookUrlSource = source(
            """{"bookList":"div.book","name":"h2@text","bookUrl":"a@href"}""",
        )
        val coverUrlSource = source(
            """{"bookList":"div.book","name":"h2@text","bookUrl":"a@href","coverUrl":"img@src"}""",
        )

        assertIs<EngineResult.Failure>(parser.parse(bookUrlSource, response))
        assertIs<EngineResult.Failure>(parser.parse(coverUrlSource, response))
    }

    @Test
    fun rejectsCredentialBearingSearchResultUrls() {
        val source = source(
            """{"bookList":"div.book","name":"h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """<div class="book"><h2>Unsafe</h2><a href="https://user:pass@reader.example/book">open</a></div>""",
        )

        assertIs<EngineResult.Failure>(CssSearchResultsParser().parse(source, response))
    }

    @Test
    fun extractsSearchCardsFromLegadoRegexBookList() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleSearchJson = """{
                "bookList":":(?s)<div.*?book.*?>.*?</div>",
                "name":"h3@text",
                "bookUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/search",
            body = """<div class='book'><h3>Regex Novel</h3><a href='/book/1'>Read</a></div><div class='other'>Skip</div>""",
        )
        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value
        assertEquals(listOf("Regex Novel"), books.map { it.name })
        assertEquals(listOf("https://reader.example/book/1"), books.map { it.bookUrl })
    }


    @Test
    fun extractsRegexCaptureGroupFromScalarSearchFields() {
        val source = source(
            """{"bookList":"div.book","name":"@regex:<h3>(.*?)</h3>","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/search",
            body = """<div class="book"><h3>Regex Search Title</h3><a href="/book/regex">Read</a></div>""",
        )

        val book = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value.single()

        assertEquals("Regex Search Title", book.name)
        assertEquals("https://reader.example/book/regex", book.bookUrl)
    }


    @Test
    fun extractsRegexBookListWithQuotedHtmlAttributes() {
        val source = BookSource(
            id = "fixture", name = "Fixture", baseUrl = "https://reader.example/",
            ruleSearchJson = """{
                "bookList":"@regex:<div class=\"book\">.*?</div>",
                "name":"h3@text",
                "bookUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/search",
            body = """<div class="book"><h3>Quoted Regex Book</h3><a href="/book/quoted">Read</a></div><div class="other">Skip</div>""",
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value

        assertEquals(listOf("Quoted Regex Book"), books.map { it.name })
        assertEquals(listOf("https://reader.example/book/quoted"), books.map { it.bookUrl })
    }

    @Test
    fun appliesFallbackAndUnionOperatorsToRealSearchFields() {
        val source = source(
            """{"bookList":"div.missing||div.secondary","name":"h2.primary@text||h2.fallback@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """
                <div class="secondary"><h2 class="fallback">Fallback title</h2><a href="/fallback">open</a></div>
                <div class="primary"><h2 class="primary">Primary title</h2><a href="/primary">open</a></div>
            """.trimIndent(),
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value
        assertEquals(listOf("Fallback title"), books.map { it.name })
        assertEquals(listOf("https://reader.example/fallback"), books.map { it.bookUrl })
    }

    @Test
    fun fallsBackFromInvalidRegexBranchesToCssSearchRules() {
        val source = source(
            """{"bookList":"@regex:a{2,1}||div.secondary","name":"@regex:a{2,1}||h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://reader.example/search",
            body = """<div class="secondary"><h2>Fallback card</h2><a href="/fallback">open</a></div>""",
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value
        assertEquals(listOf("Fallback card"), books.map { it.name })
        assertEquals(listOf("https://reader.example/fallback"), books.map { it.bookUrl })
    }


    @Test
    fun executesJavaScriptBookListAndParsesReturnedHtmlFragments() = runBlocking {
        val source = source(
            """{"bookList":"@js:renderBooks(result)","name":"h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://reader.example/search",
            body = "<div id='raw'>original response</div>",
        )
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                assertEquals("renderBooks(result)", script)
                assertEquals(response.body, bindings["result"])
                assertEquals(response.finalUrl, bindings["baseUrl"])
                assertEquals(source.id, bindings["sourceId"])
                assertEquals("bookList", bindings["ruleName"])
                return EngineResult.Success(
                    """<article class="book"><h2>Script Novel</h2><a href="/book/script">Read</a></article>""",
                )
            }
        }

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parseWithScripts(source, response, runtime),
        ).value
        assertEquals(1, books.size)
        assertEquals("Script Novel", books.single().name)
        assertEquals("https://reader.example/book/script", books.single().bookUrl)
    }

    @Test
    fun rejectsBlankJavaScriptBookListWithoutExecutingRuntime() = runBlocking {
        val source = source(
            """{"bookList":"@js:   ","name":"h2@text","bookUrl":"a@href"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/search", body = "<div></div>")
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = error("Blank scripts must not execute")
        }

        val failure = assertIs<EngineResult.Failure>(
            CssSearchResultsParser().parseWithScripts(source, response, runtime),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }


    @Test
    fun parsesJsonArrayReturnedByJavaScriptBookListWithJsonPathFields() = runBlocking {
        val source = source(
            """{"bookList":"@js:buildJson(result)","name":"$.name","bookUrl":"$.url","author":"$.author"}""",
        )
        val response = SourceHttpResponse(200, "https://reader.example/search", body = "raw")
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> = EngineResult.Success(
                """[{"name":"JSON Script A","url":"/a","author":"Writer A"},{"name":"JSON Script B","url":"/b","author":"Writer B"}]""",
            )
        }

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parseWithScripts(source, response, runtime),
        ).value
        assertEquals(listOf("JSON Script A", "JSON Script B"), books.map { it.name })
        assertEquals(
            listOf("https://reader.example/a", "https://reader.example/b"),
            books.map { it.bookUrl },
        )
        assertEquals(listOf("Writer A", "Writer B"), books.map { it.author })
    }

}
