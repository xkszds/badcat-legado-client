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
}
