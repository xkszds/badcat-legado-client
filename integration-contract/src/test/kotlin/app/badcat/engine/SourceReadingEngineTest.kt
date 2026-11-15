package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class SourceReadingEngineTest {
    private class NoNetworkClient : SourceRequestClient {
        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> =
            error("No network call is expected in this test")
    }

    @Test
    fun doesNotResolveOrRequestSearchForDisabledSource() = runBlocking {
        var resolverCalled = false
        val source = BookSource("off", "Disabled", "https://reader.example/", enabled = false)
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                resolverCalled = true
                EngineResult.Success("https://reader.example/search")
            },
            searchService = SourceSearchService(SourceHttpGateway(NoNetworkClient())),
            detailService = SourceBookDetailService(SourceHttpGateway(NoNetworkClient())),
            tocService = SourceTocService(SourceHttpGateway(NoNetworkClient())),
            contentService = SourceChapterContentService(SourceHttpGateway(NoNetworkClient())),
        )

        val failure = assertIs<EngineResult.Failure>(engine.search("off", "cat"))
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
        assertEquals(false, resolverCalled)
    }

    @Test
    fun rejectsBlankSearchKeywordBeforeResolvingSource() = runBlocking {
        var resolverCalled = false
        val source = BookSource("on", "Enabled", "https://reader.example/")
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                resolverCalled = true
                EngineResult.Success("https://reader.example/search")
            },
            searchService = SourceSearchService(SourceHttpGateway(NoNetworkClient())),
            detailService = SourceBookDetailService(SourceHttpGateway(NoNetworkClient())),
            tocService = SourceTocService(SourceHttpGateway(NoNetworkClient())),
            contentService = SourceChapterContentService(SourceHttpGateway(NoNetworkClient())),
        )

        val failure = assertIs<EngineResult.Failure>(engine.search("on", "  "))
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
        assertEquals(false, resolverCalled)
    }

    @Test
    fun disabledSourceIsRejectedForBookDetailToo() = runBlocking {
        val source = BookSource("off", "Disabled", "https://reader.example/", enabled = false)
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "not expected")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val failure = assertIs<EngineResult.Failure>(
            engine.bookDetail("off", "https://reader.example/book"),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun returnsNotFoundWhenSourceIdIsUnknown() = runBlocking {
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(emptyList()) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "not expected")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val result = engine.bookDetail("missing", "https://reader.example/book")

        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.NOT_FOUND, failure.code)
    }

    @Test
    fun exposesImportedSourcesFromCatalog() = runBlocking {
        val source = BookSource("one", "One", "https://reader.example/")
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "not expected")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val result = assertIs<EngineResult.Success<List<BookSource>>>(engine.sources())
        assertEquals(listOf(source), result.value)
    }

    @Test
    fun disabledSourceIsRejectedBeforeTocDetailRequest() = runBlocking {
        val source = BookSource("off", "Disabled", "https://reader.example/", enabled = false)
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "not expected")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val failure = assertIs<EngineResult.Failure>(
            engine.chapters("off", "https://reader.example/book"),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun disabledSourceIsRejectedBeforeChapterContentRequest() = runBlocking {
        val source = BookSource("off", "Disabled", "https://reader.example/", enabled = false)
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "not expected")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val failure = assertIs<EngineResult.Failure>(
            engine.chapterContent(
                "off",
                "https://reader.example/book",
                "https://reader.example/chapter/1",
            ),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun completesBookDetailTocAndChapterContentFlowWithOneImportedSource() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            searchUrl = "/search?q={{key}}",
            ruleSearchJson = """{"bookList":"div.result","name":"h2@text","bookUrl":"a@href"}""",
            ruleBookInfoJson = """{"name":"h1.title@text","tocUrl":"a.toc@href"}""",
            ruleTocJson = """{
                "chapterList":"ul.chapters li",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
            ruleContentJson = """{"content":"div#content"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                val body = when {
                    request.url.contains("/search?") ->
                        """<div class="result"><h2>Novel</h2><a href="/book/1">Open</a></div>"""
                    request.url.endsWith("/book/1") ->
                        """<h1 class="title">Novel</h1><a class="toc" href="/toc">Catalog</a>"""
                    request.url.endsWith("/toc") ->
                        """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>"""
                    request.url.endsWith("/chapter/1") ->
                        """<div id="content"><p>Readable chapter text.</p></div>"""
                    else -> error("Unexpected source request: ${request.url}")
                }
                return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
            }
        }
        val gateway = SourceHttpGateway(
            client,
            embeddedScriptEvaluator = LegadoEmbeddedScriptEvaluator { script, key, _ ->
                if (script == "key") EngineResult.Success(key)
                else EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected search template")
            },
        )
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = SearchUrlResolver { _, _ ->
                EngineResult.Failure(EngineErrorCode.UNKNOWN, "Search is not used in this test")
            },
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val candidates = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.search("fixture", "Novel"),
        ).value
        assertEquals(1, candidates.size)
        assertEquals("Novel", candidates.single().name)
        assertEquals("https://reader.example/book/1", candidates.single().bookUrl)

        val detail = assertIs<EngineResult.Success<BookDetail>>(
            engine.bookDetail("fixture", candidates.single().bookUrl),
        ).value
        assertEquals("Novel", detail.name)
        assertEquals("https://reader.example/toc", detail.tocUrl)

        val chapters = assertIs<EngineResult.Success<List<BookChapter>>>(
            engine.chapters("fixture", detail.bookUrl),
        ).value
        assertEquals(listOf("Chapter One"), chapters.map { it.title })

        val content = assertIs<EngineResult.Success<ChapterContent>>(
            engine.chapterContent(
                "fixture",
                detail.bookUrl,
                chapters.single().url,
                bookName = detail.name,
            ),
        ).value
        assertEquals("Readable chapter text.", content.text)

        assertEquals(
            listOf(
                "https://reader.example/search?q=Novel",
                "https://reader.example/book/1",
                "https://reader.example/book/1",
                "https://reader.example/toc",
                "https://reader.example/chapter/1",
            ),
            requests,
        )
    }

}
