package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class SourceChapterContentServiceTest {
    private class FixtureClient : SourceRequestClient {
        val requests = mutableListOf<String>()

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            requests += request.url
            val body = if (request.url.endsWith("page=2")) {
                "<div id='content'><p>Second page.</p></div>"
            } else {
                """
                <div id="content"><p>First page.</p></div>
                <a class="next" href="?page=2">Next</a>
                """.trimIndent()
            }
            return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
        }
    }

    @Test
    fun loadsAllPagesOfOneChapterAndAppliesOnlyTextReplacement() = runBlocking {
        val client = FixtureClient()
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{
                "content":"div#content",
                "nextContentUrl":"a.next"
            }""",
        )
        val service = SourceChapterContentService(
            http = SourceHttpGateway(client),
            textReplacement = BookTextReplacement { it.replace("First", "Opening") },
        )

        val result = assertIs<EngineResult.Success<ChapterContent>>(
            service.load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        ).value

        assertEquals(
            listOf("https://reader.example/chapter/1", "https://reader.example/chapter/1?page=2"),
            client.requests,
        )
        assertEquals("Opening page.\n\nSecond page.", result.text)
        assertEquals("Chapter", result.title)
        assertEquals(null, result.nextPageUrl)
    }
}
