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
                        """<h1 class="title">Novel</h1><a class="toc" href="/toc?q={{key}}">Catalog</a>"""
                    request.url.contains("/toc?q=Novel") ->
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
            searchUrlResolver = BookSourceSearchUrlResolver(),
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
        assertEquals("https://reader.example/toc?q={{key}}", detail.tocUrl)

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
                "https://reader.example/toc?q=Novel",
                "https://reader.example/chapter/1",
            ),
            requests,
        )
    }


    @Test
    fun completesSearchToReadingAcrossPaginatedTocAndChapterPages() = runBlocking {
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
                "chapterUrl":"a@href",
                "nextTocUrl":"a.next"
            }""",
            ruleContentJson = """{"content":"div#content","nextContentUrl":"a.next"}""",
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
                    request.url.contains("/toc?page=2") ->
                        """<ul class="chapters"><li><a href="/chapter/2">Chapter Two</a></li></ul>"""
                    request.url.endsWith("/toc") ->
                        """<ul class="chapters"><li><a href="/chapter/1">Chapter One</a></li></ul>
                           <a class="next" href="?page=2">Next</a>"""
                    request.url.contains("/chapter/1?page=2") ->
                        """<div id="content"><p>Second half.</p></div>"""
                    request.url.endsWith("/chapter/1") ->
                        """<div id="content"><p>First half.</p></div>
                           <a class="next" href="?page=2">Next</a>"""
                    else -> error("Unexpected source request: " + request.url)
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
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val candidate = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.search("fixture", "Novel"),
        ).value.single()
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            engine.bookDetail("fixture", candidate.bookUrl),
        ).value
        val chapters = assertIs<EngineResult.Success<List<BookChapter>>>(
            engine.chapters("fixture", detail.bookUrl),
        ).value
        assertEquals(listOf("Chapter One", "Chapter Two"), chapters.map { it.title })

        val content = assertIs<EngineResult.Success<ChapterContent>>(
            engine.chapterContent(
                "fixture",
                detail.bookUrl,
                chapters.first().url,
                bookName = detail.name,
            ),
        ).value
        assertEquals("First half.\n\nSecond half.", content.text)

        assertEquals(
            listOf(
                "https://reader.example/search?q=Novel",
                "https://reader.example/book/1",
                "https://reader.example/book/1",
                "https://reader.example/toc",
                "https://reader.example/toc?page=2",
                "https://reader.example/chapter/1",
                "https://reader.example/chapter/1?page=2",
            ),
            requests,
        )
    }


    @Test
    fun forwardsRequestedSearchPageIntoEmbeddedLegadoUrlTemplate() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            searchUrl = "/search?q={{key}}&p={{page}}",
            ruleSearchJson = """{"bookList":"div.result","name":"h2@text","bookUrl":"a@href"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                return EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        request.url,
                        body = """<div class="result"><h2>Novel</h2><a href="/book/1">Open</a></div>""",
                    ),
                )
            }
        }
        val gateway = SourceHttpGateway(
            client,
            embeddedScriptEvaluator = LegadoEmbeddedScriptEvaluator { script, key, page ->
                when (script.trim()) {
                    "key" -> EngineResult.Success(key)
                    "page" -> EngineResult.Success(page)
                    else -> EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected template expression")
                }
            },
        )
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val result = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.searchPage("fixture", "Novel", 3),
        )

        assertEquals(1, result.value.size)
        assertEquals(listOf("https://reader.example/search?q=Novel&p=3"), requests)
    }

    @Test
    fun rejectsNonPositiveSearchPageBeforeMakingARequest() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            searchUrl = "/search?q={{key}}",
        )
        var requested = false
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requested = true
                error("No request should be made for an invalid page")
            }
        }
        val gateway = SourceHttpGateway(client)
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val failure = assertIs<EngineResult.Failure>(engine.searchPage("fixture", "Novel", 0))

        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
        assertEquals(false, requested)
    }

    @Test
    fun runsAtJsSearchUrlThroughTheImportedSourceReadingFlow() = runBlocking {
        val source = BookSource(
            id = "fixture",
            name = "Fixture",
            baseUrl = "https://reader.example/",
            searchUrl = "https://reader.example/search?q=@js:return result + key",
            ruleSearchJson = """{"bookList":"div.result","name":"h2@text","bookUrl":"a@href"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                return EngineResult.Success(
                    SourceHttpResponse(
                        statusCode = 200,
                        finalUrl = request.url,
                        body = """<div class="result"><h2>Script Novel</h2><a href="/book/1">Open</a></div>""",
                    ),
                )
            }
        }
        val evaluator = object : LegadoEmbeddedScriptEvaluator {
            override suspend fun evaluate(script: String, key: String, page: Int): EngineResult<Any?> =
                EngineResult.Success("unused")

            override suspend fun evaluateWithResult(
                script: String,
                key: String,
                page: Int,
                result: String,
            ): EngineResult<Any?> {
                assertEquals("return result + key", script)
                assertEquals("https://reader.example/search?q=", result)
                assertEquals("cat", key)
                assertEquals(1, page)
                return EngineResult.Success(result + key)
            }
        }
        val gateway = SourceHttpGateway(client, evaluator)
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )

        val candidates = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.search("fixture", "cat"),
        ).value
        assertEquals("Script Novel", candidates.single().name)
        assertEquals("https://reader.example/book/1", candidates.single().bookUrl)
        assertEquals(listOf("https://reader.example/search?q=cat"), requests)
    }


    @Test
    fun routesExploreThroughTheExploreStageOfAnImportedSource() = runBlocking {
        val source = BookSource(
            id = "explore",
            name = "Explore",
            baseUrl = "https://reader.example/",
            exploreUrl = "/explore",
            enabledExplore = true,
            ruleExploreJson = """{"bookList":"div.book","name":"h2@text","bookUrl":"a@href"}""",
        )
        val gateway = SourceHttpGateway(object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> =
                EngineResult.Success(
                    SourceHttpResponse(
                        200,
                        request.url,
                        body = """<div class="book"><h2>Explore result</h2><a href="/book/2">Open</a></div>""",
                    ),
                )
        })
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
            exploreService = SourceExploreService(gateway),
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.explore("explore", page = 1),
        ).value
        assertEquals(listOf("Explore result"), books.map { it.name })
    }

    @Test
    fun rejectsExploreWhenTheSourceHasItDisabled() = runBlocking {
        val source = BookSource(
            id = "explore-off",
            name = "Explore off",
            baseUrl = "https://reader.example/",
            enabledExplore = false,
            exploreUrl = "/explore",
        )
        val gateway = SourceHttpGateway(NoNetworkClient())
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
            exploreService = SourceExploreService(gateway),
        )

        val failure = assertIs<EngineResult.Failure>(engine.explore("explore-off"))
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }


    @Test
    fun routesExploreThroughConfiguredExploreService() = runBlocking {
        val source = BookSource(
            id = "explore-source",
            name = "Explore source",
            baseUrl = "https://reader.example/",
            enabledExplore = true,
            exploreUrl = "/discover?page={{page}}&key={{key}}",
            ruleExploreJson = """{"bookList":"div.book","name":"h3@text","bookUrl":"a@href"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                return EngineResult.Success(SourceHttpResponse(
                    200,
                    request.url,
                    body = """<div class="book"><h3>Found</h3><a href="/book/9">Read</a></div>""",
                ))
            }
        }
        val gateway = SourceHttpGateway(
            client,
            embeddedScriptEvaluator = LegadoEmbeddedScriptEvaluator { script, key, page ->
                when (script.trim()) {
                    "page" -> EngineResult.Success(page.toString())
                    "key" -> EngineResult.Success(key)
                    else -> EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected template")
                }
            },
        )
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
            exploreService = SourceExploreService(gateway),
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            engine.explore("explore-source", keyword = "猫", page = 2),
        ).value

        assertEquals(listOf("https://reader.example/discover?page=2&key=%E7%8C%AB"), requests)
        assertEquals(listOf("Found"), books.map { it.name })
        assertEquals("https://reader.example/book/9", books.single().bookUrl)
    }

    @Test
    fun completesSearchDetailTocAndChapterFlowWhenBookInfoOmitsName() = runBlocking {
        val source = BookSource(
            id = "fallback-fixture",
            name = "Fallback fixture",
            baseUrl = "https://reader.example/",
            searchUrl = "/search?q={{key}}",
            ruleSearchJson = """{"bookList":"div.result","name":"h2@text","bookUrl":"a@href"}""",
            ruleBookInfoJson = """{"intro":"p.intro@text","tocUrl":"a.toc@href"}""",
            ruleTocJson = """{"chapterList":"ul.chapters li","chapterName":"a@text","chapterUrl":"a@href"}""",
            ruleContentJson = """{"content":"div#content"}""",
        )
        val requests = mutableListOf<String>()
        val client = object : SourceRequestClient {
            override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
                requests += request.url
                val body = when {
                    request.url.contains("/search?") -> "<div class='result'><h2>Search Result Novel</h2><a href='/book/1'>Open</a></div>"
                    request.url.endsWith("/book/1") -> "<p class='intro'>A fixture introduction</p><a class='toc' href='/toc'>Catalog</a>"
                    request.url.endsWith("/toc") -> "<ul class='chapters'><li><a href='/chapter/1'>First Chapter</a></li></ul>"
                    request.url.endsWith("/chapter/1") -> "<div id='content'><p>Fixture chapter body.</p></div>"
                    else -> error("Unexpected request: ${request.url}")
                }
                return EngineResult.Success(SourceHttpResponse(200, request.url, body = body))
            }
        }
        val gateway = SourceHttpGateway(
            client,
            embeddedScriptEvaluator = LegadoEmbeddedScriptEvaluator { script, key, _ ->
                if (script == "key") EngineResult.Success(key)
                else EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Unexpected template")
            },
        )
        val engine = SourceReadingEngine(
            catalog = BookSourceCatalog { EngineResult.Success(listOf(source)) },
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway),
            detailService = SourceBookDetailService(gateway),
            tocService = SourceTocService(gateway),
            contentService = SourceChapterContentService(gateway),
        )
        val candidate = assertIs<EngineResult.Success<List<BookCandidate>>>(engine.search("fallback-fixture", "Search Result Novel")).value.single()
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            engine.bookDetailWithFallback("fallback-fixture", candidate.bookUrl, candidate.name),
        ).value
        assertEquals("Search Result Novel", detail.name)
        assertEquals("A fixture introduction", detail.intro)
        val chapters = assertIs<EngineResult.Success<List<BookChapter>>>(
            engine.chaptersIncrementalWithFallback("fallback-fixture", candidate.bookUrl, candidate.name) { },
        ).value
        assertEquals("First Chapter", chapters.single().title)
        val content = assertIs<EngineResult.Success<ChapterContent>>(
            engine.chapterContent("fallback-fixture", candidate.bookUrl, chapters.single().url, detail.name),
        ).value
        assertEquals("Fixture chapter body.", content.text)
        assertEquals(5, requests.size)
    }
}
