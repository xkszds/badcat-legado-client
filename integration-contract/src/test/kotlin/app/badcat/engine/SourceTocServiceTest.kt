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
    fun firstScriptTocPageIsOneBasedAndStartsAtOne() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleTocJson = """{
                "chapterList":"@js:buildChapterList()",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        var observedPage = 0
        val runtime = object : SourceScriptRuntime {
            override suspend fun evaluate(
                source: BookSource,
                script: String,
                bindings: Map<String, String>,
            ): EngineResult<String> {
                observedPage = bindings["page"]?.toIntOrNull() ?: -1
                return EngineResult.Success("<li><a href='/chapter/1'>Chapter One</a></li>")
            }
        }
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> =
                EngineResult.Success(SourceHttpResponse(200, request.url, body = "<html></html>"))
        }

        val result = SourceTocService(SourceHttpGateway(client), scriptRuntime = runtime)
            .load(source, "https://reader.example/book/1")

        assertIs<EngineResult.Success<List<BookChapter>>>(result)
        assertEquals(1, observedPage)
    }

    @Test
    fun redirectAliasesDoNotConsumeTheTocPageSafetyLimit() = runBlocking {
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
                val secondPage = request.url.endsWith("/page-2")
                val finalUrl = if (secondPage) {
                    "https://reader.example/canonical-2"
                } else {
                    "https://reader.example/canonical-1"
                }
                val body = if (secondPage) {
                    """<ul class="chapters"><li><a href="/chapter/2">Chapter Two</a></li></ul>"""
                } else {
                    """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul><a class="next" href="/page-2">Next</a>"""
                }
                return EngineResult.Success(SourceHttpResponse(200, finalUrl, body = body))
            }
        }

        val result = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client), maxPages = 2).load(
                source,
                "https://reader.example/book/1",
            ),
        )

        assertEquals(
            listOf("https://reader.example/book/1", "https://reader.example/page-2"),
            requests,
        )
        assertEquals(listOf("Chapter One", "Chapter Two"), result.value.map { it.title })
    }

    @Test
    fun detectsTocCyclesWhenOnlyTheUrlFragmentChanges() = runBlocking {
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
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        "https://reader.example/canonical-toc#first",
                        body = """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul><a class="next" href="/canonical-toc#second">Next</a>""",
                    ),
                )
            }
        }

        val result = assertIs<EngineResult.Failure>(
            SourceTocService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf("https://reader.example/book/1"), requests)
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

    @Test
    fun reportsTocPaginationCycleInsteadOfSilentlyReturningPartialChapters() = runBlocking {
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
                val next = if (request.url.contains("page=2")) {
                    "https://reader.example/book/1"
                } else {
                    "?page=2"
                }
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        request.url,
                        body = """<ul class="chapters"><li><a href="/chapter/${requests.size}">Chapter ${requests.size}</a></li></ul><a class="next" href="$next">Next</a>""",
                    ),
                )
            }
        }

        val failure = assertIs<EngineResult.Failure>(
            SourceTocService(SourceHttpGateway(client)).load(source, "https://reader.example/book/1"),
        )
        assertEquals(EngineErrorCode.PARSE, failure.code)
        assertEquals(2, requests.size)
    }


    @Test
    fun preservesDistinctChapterRowsSharingUrlAcrossPagesAndReindexesTheMergedToc() = runBlocking {
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
                val body = if (request.url.contains("page=2")) {
                    """<ul class="chapters">
                        <li><a href="/chapter/2">Chapter Two</a></li>
                        <li><a href="/chapter/1">Chapter One (duplicate)</a></li>
                    </ul>"""
                } else {
                    """<ul class="chapters">
                        <li><a href="/chapter/1">Chapter One</a></li>
                    </ul><a class="next" href="?page=2">Next</a>"""
                }
                return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
            }
        }

        val result = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
            ),
        )

        assertEquals(
            listOf("https://reader.example/book/1", "https://reader.example/book/1?page=2"),
            requests,
        )
        assertEquals(
            listOf("Chapter One", "Chapter Two", "Chapter One (duplicate)"),
            result.value.map { it.title },
        )
        assertEquals(listOf(0, 1, 2), result.value.map { it.index })
        assertEquals(
            listOf(
                "https://reader.example/chapter/1",
                "https://reader.example/chapter/2",
                "https://reader.example/chapter/1",
            ),
            result.value.map { it.url },
        )
    }


    @Test
    fun preservesDifferentChaptersSharingUrlAndCollapsesOnlyEqualProjectedRows() = runBlocking {
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
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> =
                EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        request.url,
                        body = """<ul class="chapters">
                            <li><a href="/chapter/shared">Chapter One</a></li>
                            <li><a href="/chapter/shared">Chapter Two</a></li>
                            <li><a href="/chapter/shared">Chapter One</a></li>
                        </ul>""",
                    ),
                )
        }

        val result = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client)).load(source, "https://reader.example/book/1"),
        )

        assertEquals(listOf("Chapter One", "Chapter Two"), result.value.map { it.title })
        assertEquals(listOf(0, 1), result.value.map { it.index })
        assertEquals(
            listOf("https://reader.example/chapter/shared", "https://reader.example/chapter/shared"),
            result.value.map { it.url },
        )
    }

    @Test
    fun incrementalCallbacksPreserveDuplicateUrlsAcrossTocPages() = runBlocking {
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
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                val secondPage = request.url.contains("page=2")
                val body = if (secondPage) {
                    """<ul class="chapters">
                        <li><a href="/chapter/shared">Chapter Two</a></li>
                        <li><a href="/chapter/shared">Chapter One</a></li>
                    </ul>"""
                } else {
                    """<ul class="chapters">
                        <li><a href="/chapter/shared">Chapter One</a></li>
                    </ul><a class="next" href="?page=2">Next</a>"""
                }
                return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
            }
        }
        val callbacks = mutableListOf<List<BookChapter>>()
        val result = assertIs<EngineResult.Success<List<BookChapter>>>(
            SourceTocService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
                onChapters = { callbacks += it },
            ),
        )

        assertEquals(2, callbacks.size)
        assertEquals(listOf("Chapter One", "Chapter Two"), callbacks.last().map { it.title })
        assertEquals(listOf(0, 1), callbacks.last().map { it.index })
        assertEquals(result.value, callbacks.last())
        assertEquals(2, result.value.size)
        assertEquals(1, result.value.map { it.url }.distinct().size)
    }

}
