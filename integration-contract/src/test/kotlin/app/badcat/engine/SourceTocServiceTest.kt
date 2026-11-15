package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class SourceTocServiceTest {
    private class PaginatedFixtureClient : SourceRequestClient {
        val requests = mutableListOf<String>()

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            requests += request.url
            return EngineResult.Success(
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = request.url,
                    body = """
                        <ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>
                        <a class="next" href="?page=2">Next</a>
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun reportsPartialTocWhenPageSafetyLimitIsReached() = runBlocking {
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
        val client = PaginatedFixtureClient()
        val service = SourceTocService(
            http = SourceHttpGateway(client),
            maxPages = 1,
        )

        val result = assertIs<EngineResult.Failure>(
            service.load(source, "https://reader.example/book/1"),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf("https://reader.example/book/1"), client.requests)
    }
}
