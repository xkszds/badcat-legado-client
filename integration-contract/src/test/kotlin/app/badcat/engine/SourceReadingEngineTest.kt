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
}
