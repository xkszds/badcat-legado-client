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

    @Test
    fun loadsEveryTocPageAndResolvesQueryOnlyPaginationAgainstCurrentPath() = runBlocking {
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
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                val secondPage = request.url.contains("page=2")
                val html = if (secondPage) {
                    """<ul class="chapters"><li><a href="/chapter/2">Chapter Two</a></li></ul>"""
                } else {
                    """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>
                        <a class="next" href="?page=2">Next</a>"""
                }
                return EngineResult.Success(
                    SourceHttpResponse(200, request.url, body = html),
                )
            }
        }

        val result = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
            ),
        )

        assertEquals(
            listOf(
                "https://reader.example/book/1",
                "https://reader.example/book/1?page=2",
            ),
            requests,
        )
        assertEquals(listOf("Chapter One", "Chapter Two"), result.value.map { it.title })
        assertEquals(
            listOf("https://reader.example/chapter/1", "https://reader.example/chapter/2"),
            result.value.map { it.url },
        )
    }

}
