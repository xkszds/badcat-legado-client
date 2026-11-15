package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
}
