package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
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
                "bookList":":<div class='book'>[\s\S]*?</div>",
                "name":"h3@text",
                "bookUrl":"a@href"
            }""",
        )
        val response = SourceHttpResponse(
            200, "https://reader.example/search",
            body = """<div class="book"><h3>Regex Novel</h3><a href="/book/1">Read</a></div><div class="other">Skip</div>""",
        )
        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(source, response),
        ).value
        assertEquals(listOf("Regex Novel"), books.map { it.name })
        assertEquals(listOf("https://reader.example/book/1"), books.map { it.bookUrl })
    }

}
