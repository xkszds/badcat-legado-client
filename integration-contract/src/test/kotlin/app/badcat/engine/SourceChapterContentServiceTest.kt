package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
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
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val service = SourceChapterContentService(
            http = SourceHttpGateway(client),
            textReplacement = BookTextReplacement { it.replace("First", "Opening") },
        )
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            service.load(source, "https://reader.example/book/1", BookChapter(0, "Chapter", "https://reader.example/chapter/1")),
        ).value

        assertEquals(
            listOf("https://reader.example/chapter/1", "https://reader.example/chapter/1?page=2"),
            client.requests,
        )
        assertEquals("Opening page.\n\nSecond page.", result.text)
        assertEquals("Chapter", result.title)
        assertEquals(null, result.nextPageUrl)
    }

    @Test
    fun passesBookIdentityAndAcceptsScopedTitleAndBodyReplacement() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture Source",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        var observedBookName = ""
        var observedBookOrigin = ""
        val service = SourceChapterContentService(
            http = SourceHttpGateway(FixtureClient()),
            scopedTextReplacement = ScopedBookTextReplacement { title, text, context ->
                observedBookName = context.book.name
                observedBookOrigin = context.book.origin
                ReplacedChapterContent(title.replace("Chapter", "Part"), text.replace("First", "Scoped"))
            },
        )
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            service.load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
                bookName = "Novel Title",
            ),
        ).value

        assertEquals("Novel Title", observedBookName)
        assertEquals("https://reader.example/", observedBookOrigin)
        assertEquals("Part", result.title)
        assertEquals("Scoped page.\n\nSecond page.", result.text)
    }

    @Test
    fun propagatesCoroutineCancellationFromReplacementInsteadOfMappingToParseFailure() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content"}""",
        )
        val service = SourceChapterContentService(
            http = SourceHttpGateway(FixtureClient()),
            scopedTextReplacement = ScopedBookTextReplacement { _, _, _ ->
                throw java.util.concurrent.CancellationException("caller cancelled chapter processing")
            },
        )

        assertFailsWith<java.util.concurrent.CancellationException> {
            service.load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
                "Novel",
            )
        }
        Unit
    }

    @Test
    fun mapsUnsupportedReplacementRuntimeToTypedEngineError() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content"}""",
        )
        val service = SourceChapterContentService(
            http = SourceHttpGateway(FixtureClient()),
            scopedTextReplacement = ScopedBookTextReplacement { _, _, _ ->
                throw UnsupportedOperationException("JavaScript replacement runtime unavailable")
            },
        )
        val result = assertIs<EngineResult.Failure>(
            service.load(source, "https://reader.example/book/1", BookChapter(0, "Chapter", "https://reader.example/chapter/1"), "Novel"),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, result.code)
    }

    @Test
    fun mapsRegexDeadlineToTypedRuleTimeoutError() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content"}""",
        )
        val service = SourceChapterContentService(
            http = SourceHttpGateway(FixtureClient()),
            scopedTextReplacement = ScopedBookTextReplacement { _, _, _ ->
                throw LegadoRegexTimeoutException(42, 100)
            },
        )
        val result = assertIs<EngineResult.Failure>(
            service.load(source, "https://reader.example/book/1", BookChapter(0, "Chapter", "https://reader.example/chapter/1"), "Novel"),
        )
        assertEquals(EngineErrorCode.RULE_TIMEOUT, result.code)
    }

    @Test
    fun detectsChapterCyclesWhenOnlyTheUrlFragmentChanges() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        "https://reader.example/canonical-chapter#first",
                        body = """<div id="content"><p>First page.</p></div><a class="next" href="/canonical-chapter#second">Next</a>""",
                    ),
                )
            }
        }

        val result = assertIs<EngineResult.Failure>(
            SourceChapterContentService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf("https://reader.example/chapter/1"), requests)
    }

    @Test
    fun reportsChapterPaginationCycleInsteadOfReturningPartialText() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> =
                EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        request.url,
                        body = """<div id="content"><p>Repeated page.</p></div><a class="next" href="/chapter/1">Next</a>""",
                    ),
                )
        }
        val result = assertIs<EngineResult.Failure>(
            SourceChapterContentService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
    }

    @Test
    fun detectsChapterCycleWhenNextLinkTargetsRedirectDestination() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val requested = mutableListOf<String>()
        val canonicalUrl = "https://reader.example/canonical-chapter"
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requested += request.url
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        canonicalUrl,
                        body = """<div id="content"><p>First page.</p></div><a class="next" href="/canonical-chapter">Next</a>""",
                    ),
                )
            }
        }

        val result = assertIs<EngineResult.Failure>(
            SourceChapterContentService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf("https://reader.example/chapter/1"), requested)
    }

    @Test
    fun redirectAliasesDoNotConsumeTheChapterPageSafetyLimit() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                val secondPage = request.url.endsWith("/canonical/chapter-2")
                val finalUrl = if (secondPage) {
                    "https://reader.example/canonical/chapter-2"
                } else {
                    "https://reader.example/canonical/chapter-1"
                }
                val body = if (secondPage) {
                    """<div id="content"><p>Second page.</p></div>"""
                } else {
                    """<div id="content"><p>First page.</p></div><a class="next" href="chapter-2">Next</a>"""
                }
                return EngineResult.Success(SourceHttpResponse(200, finalUrl, body = body))
            }
        }

        val result = assertIs<EngineResult.Success<ChapterContent>>(
            SourceChapterContentService(SourceHttpGateway(client), maxPages = 2).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        )

        assertEquals(
            listOf(
                "https://reader.example/chapter/1",
                "https://reader.example/canonical/chapter-2",
            ),
            requests,
            "Unexpected request sequence: $requests",
        )
        assertEquals(
            "First page.\n\nSecond page.",
            result.value.text,
            "Unexpected combined text: ${result.value.text.replace("\n", "<LF>")}",
        )
    }


    @Test
    fun returnsFreshCachedChapterWithoutNetworkRequest() = runBlocking {
        val source = BookSource(id = "fixture", name = "Fixture", baseUrl = "https://reader.example/")
        val chapter = BookChapter(0, "Cached", "https://reader.example/chapter/1")
        val client = FixtureClient()
        val cachedContent = ChapterContent("Cached title", "Saved chapter text")
        val cache = object : ChapterContentCache {
            override suspend fun get(sourceId: String, bookUrl: String, chapterUrl: String) =
                CachedChapterContent(cachedContent, System.currentTimeMillis())
            override suspend fun put(sourceId: String, bookUrl: String, chapterUrl: String, value: CachedChapterContent) = Unit
        }
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            SourceChapterContentService(http = SourceHttpGateway(client), cache = cache).load(
                source, "https://reader.example/book/1", chapter,
            ),
        )
        assertEquals(cachedContent, result.value)
        assertEquals(emptyList(), client.requests)
    }

    @Test
    fun fallsBackToExpiredCachedChapterWhenNetworkFails() = runBlocking {
        val source = BookSource(id = "fixture", name = "Fixture", baseUrl = "https://reader.example/")
        val chapter = BookChapter(0, "Cached", "https://reader.example/chapter/1")
        val cachedContent = ChapterContent("Offline title", "Offline chapter text")
        val cache = object : ChapterContentCache {
            override suspend fun get(sourceId: String, bookUrl: String, chapterUrl: String) =
                CachedChapterContent(cachedContent, System.currentTimeMillis() - 60_000)
            override suspend fun put(sourceId: String, bookUrl: String, chapterUrl: String, value: CachedChapterContent) = Unit
        }
        val failingClient = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest) =
                EngineResult.Failure(EngineErrorCode.NETWORK, "offline")
        }
        val result = assertIs<EngineResult.Success<ChapterContent>>(
            SourceChapterContentService(http = SourceHttpGateway(failingClient), cache = cache).load(
                source, "https://reader.example/book/1", chapter,
            ),
        )
        assertEquals(cachedContent, result.value)
    }

    @Test
    fun reportsChapterPaginationSafetyLimitInsteadOfReturningPartialText() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val client = FixtureClient()
        val result = assertIs<EngineResult.Failure>(
            SourceChapterContentService(
                http = SourceHttpGateway(client),
                maxPages = 1,
            ).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", "https://reader.example/chapter/1"),
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf("https://reader.example/chapter/1"), client.requests)
    }


    @Test
    fun detectsNextContentUrlThatPointsToRedirectDestinationBeforeSecondRequest() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
        )
        val requestedUrl = "https://reader.example/chapter/start"
        val redirectDestination = "https://reader.example/chapter/canonical"
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        redirectDestination,
                        body = """<div id="content"><p>First page.</p></div><a class="next" href="$redirectDestination">Next</a>""",
                    ),
                )
            }
        }

        val result = assertIs<EngineResult.Failure>(
            SourceChapterContentService(SourceHttpGateway(client)).load(
                source,
                "https://reader.example/book/1",
                BookChapter(0, "Chapter", requestedUrl),
            ),
        )

        assertEquals(EngineErrorCode.PARSE, result.code)
        assertEquals(listOf(requestedUrl), requests)
    }

}
