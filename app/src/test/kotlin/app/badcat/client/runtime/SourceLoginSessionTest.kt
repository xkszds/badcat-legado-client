package app.badcat.client.runtime

import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpRequest
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.SourceRequestClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceLoginSessionTest {
    private class RecordingClient : SourceRequestClient {
        var lastRequest: SourceHttpRequest? = null
        override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
            lastRequest = request
            return EngineResult.Success(SourceHttpResponse(200, request.url, body = "ok"))
        }
    }

    @Test
    fun savedLoginHeadersAreMergedCaseInsensitivelyForOnlyTheirSource() = runBlocking {
        val store = InMemorySourceLoginSessionStore
        val sourceA = "header-merge-source-A"
        val sourceB = "header-merge-source-B"
        store.clearSource(sourceA)
        store.clearSource(sourceB)
        val bridge = RhinoSourceLoginExtensions(sourceA, store)
        bridge.putLoginHeader("""{"Authorization":"Bearer token-A","X-Session":"session-A"}""")
        val delegate = RecordingClient()
        val client = SourceLoginSessionRequestClient(delegate, store)

        client.execute(SourceHttpRequest(
            sourceId = sourceA,
            url = "https://example.test/book",
            headers = mapOf("authorization" to "Bearer stale", "Accept" to "application/json"),
        ))
        val request = requireNotNull(delegate.lastRequest)
        assertEquals("Bearer token-A", request.headers["Authorization"])
        assertEquals("session-A", request.headers["X-Session"])
        assertEquals("application/json", request.headers["Accept"])

        client.execute(SourceHttpRequest(sourceId = sourceB, url = "https://example.test/book"))
        assertTrue(requireNotNull(delegate.lastRequest).headers.isEmpty())
        store.clearSource(sourceA)
        store.clearSource(sourceB)
    }

    @Test
    fun loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite() = runBlocking {
        val sourceKey = "https://www.example.co.uk/booksource.json"
        val store = InMemorySourceLoginSessionStore
        store.putLoginHeader(sourceKey, """{"Authorization":"Bearer secret"}""")
        val delegate = RecordingClient()
        val client = SourceLoginSessionRequestClient(delegate, store)

        client.execute(SourceHttpRequest(sourceKey, "https://cdn.example.co.uk/cover.jpg"))
        assertEquals("Bearer secret", delegate.lastRequest?.headers?.get("Authorization"))

        client.execute(SourceHttpRequest(sourceKey, "https://third-party-cdn.net/cover.jpg"))
        assertTrue(delegate.lastRequest?.headers?.isEmpty() == true)
    }

    @Test
    fun invalidSourceKeyPreservesPinnedUpstreamFailOpenBehavior() = runBlocking {
        val sourceKey = "not-a-url"
        val store = InMemorySourceLoginSessionStore
        store.putLoginHeader(sourceKey, """{"Authorization":"Bearer secret"}""")
        val delegate = RecordingClient()
        SourceLoginSessionRequestClient(delegate, store)
            .execute(SourceHttpRequest(sourceKey, "https://cdn.example.net/image"))
        assertEquals("Bearer secret", delegate.lastRequest?.headers?.get("Authorization"))
    }

    @Test
    fun loginInfoAndHeaderCanBeReadBySeparateRhinoBridgeInstances() {
        val store = InMemorySourceLoginSessionStore
        RhinoSourceLoginExtensions("source-A", store).apply {
            putLoginInfo("""{"username":"reader","token":"abc"}""")
            putLoginHeader("""{"Authorization":"Bearer abc"}""")
        }
        val second = RhinoSourceLoginExtensions("source-A", store)
        assertEquals("reader", second.getLoginInfoMap()["username"])
        assertEquals("Bearer abc", second.getLoginHeaderMap()["Authorization"])
    }

    @Test
    fun clearingSourceRemovesOnlyItsAuthenticationState() {
        val store = InMemorySourceLoginSessionStore
        RhinoSourceLoginExtensions("source-A", store).apply {
            putLoginInfo("""{"token":"a"}""")
            putLoginHeader("""{"Authorization":"Bearer a"}""")
        }
        RhinoSourceLoginExtensions("source-B", store).apply {
            putLoginInfo("""{"token":"b"}""")
            putLoginHeader("""{"Authorization":"Bearer b"}""")
        }

        store.clearSource("source-A")

        assertEquals(null, store.getLoginInfo("source-A"))
        assertEquals(null, store.getLoginHeader("source-A"))
        assertEquals("""{"token":"b"}""", store.getLoginInfo("source-B"))
        assertEquals("""{"Authorization":"Bearer b"}""", store.getLoginHeader("source-B"))
    }
    @Test
    fun rejectsOversizedAggregateLoginInfoBeforePersisting() {
        val store = InMemorySourceLoginSessionStore
        val bridge = RhinoSourceLoginExtensions("source-A", store)
        val entries = (1..5).joinToString(",") { key ->
            "\"key$key\":\"${"x".repeat(7_000)}\""
        }
        assertTrue(runCatching { bridge.putLoginInfo("{$entries}") }.isFailure)
        assertEquals(null, store.getLoginInfo("source-A"))
    }

    @Test
    fun rejectsHeaderLineBreaksAndEmptyHeaderObjects() {
        val bridge = RhinoSourceLoginExtensions("source-A", InMemorySourceLoginSessionStore)
        assertTrue(runCatching {
            bridge.putLoginHeader("""{"Authorization":"Bearer ok\r\nInjected: yes"}""")
        }.isFailure)
        assertTrue(runCatching { bridge.putLoginHeader("{}") }.isFailure)
    }
}
