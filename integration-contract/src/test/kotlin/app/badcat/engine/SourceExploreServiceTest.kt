package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

class SourceExploreServiceTest {
    private class FixtureClient(
        private val status: Int = 200,
    ) : SourceRequestClient {
        val requests = mutableListOf<SourceHttpRequest>()

        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            requests += request
            return EngineResult.Success(
                SourceHttpResponse(
                    statusCode = status,
                    finalUrl = request.url,
                    body = """
                        <div class="book">
                          <h3>Explore Novel</h3>
                          <a href="/book/explore">Read</a>
                        </div>
                    """.trimIndent(),
                ),
            )
        }
    }

    private fun gateway(client: SourceRequestClient) = SourceHttpGateway(
        client,
        embeddedScriptEvaluator = LegadoEmbeddedScriptEvaluator { script, key, page ->
            when (script.trim()) {
                "page" -> EngineResult.Success(page.toString())
                "key" -> EngineResult.Success(key)
                else -> EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected template")
            }
        },
    )

    private fun source(
        enabled: Boolean = true,
        enabledExplore: Boolean = true,
    ) = BookSource(
        id = "fixture",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        enabled = enabled,
        enabledExplore = enabledExplore,
        exploreUrl = "/explore?page={{page}}&key={{key}}",
        ruleExploreJson = """
            {
              "bookList":"div.book",
              "name":"h3@text",
              "bookUrl":"a@href"
            }
        """.trimIndent(),
        // A deliberately different search rule proves EXPLORE uses ruleExplore.
        ruleSearchJson = """{"bookList":"div.missing","name":"h2@text","bookUrl":"a@href"}""",
    )

    @Test
    fun exploresUsingExploreRulesAndResolvesRelativeBookUrls() = runBlocking {
        val client = FixtureClient()
        val service = SourceExploreService(gateway(client))

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            service.explore(source(), page = 3, keyword = "猫"),
        ).value

        assertEquals(listOf("https://reader.example/explore?page=3&key=%E7%8C%AB"), client.requests.map { it.url })
        assertEquals(listOf("Explore Novel"), books.map { it.name })
        assertEquals(listOf("https://reader.example/book/explore"), books.map { it.bookUrl })
    }

    @Test
    fun refusesDisabledExploreWithoutMakingARequest() = runBlocking {
        val client = FixtureClient()
        val result = assertIs<EngineResult.Failure>(
            SourceExploreService(SourceHttpGateway(client)).explore(
                source(enabledExplore = false),
            ),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, result.code)
        assertEquals(0, client.requests.size)
    }

    @Test
    fun reportsNonSuccessfulHttpStatus() = runBlocking {
        val client = FixtureClient(status = 503)
        val result = assertIs<EngineResult.Failure>(
            SourceExploreService(gateway(client)).explore(source()),
        )
        assertEquals(EngineErrorCode.NETWORK, result.code)
    }
}
