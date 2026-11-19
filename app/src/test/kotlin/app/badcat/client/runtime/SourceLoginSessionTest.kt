package app.badcat.client.runtime

import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpRequest
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.SourceRequestClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
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

        assertEquals("example.co.uk", legadoSourceDomain(sourceKey))
        assertEquals("example.co.uk", legadoSourceDomain("https://cdn.example.co.uk/cover.jpg"))
        assertEquals("third-party-cdn.net", legadoSourceDomain("https://third-party-cdn.net/cover.jpg"))
        assertEquals("127.0.0.1", legadoSourceDomain("http://127.0.0.1:8080/source.json"))
        assertEquals("[2001:db8::1]", legadoSourceDomain("https://[2001:db8::1]/source.json"))
        assertEquals(null, legadoSourceDomain("not-a-url"))
        assertEquals(null, legadoSourceDomain("file:///tmp/source.json"))
        assertTrue(shouldAttachSourceLoginHeader(sourceKey, "not-a-url"))

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
    fun loginCookieHeaderIsSynchronizedAndRemovedWithLoginHeader() {
        val writes = mutableListOf<Triple<String, String, String>>()
        val removals = mutableListOf<Pair<String, String>>()
        val bridge = RhinoSourceLoginExtensions(
            sourceId = "source-cookie-A",
            store = InMemorySourceLoginSessionStore,
            sourceKey = "https://reader.example/source.json",
            loginCookieWriter = { sourceId, sourceKey, cookie ->
                writes += Triple(sourceId, sourceKey, cookie)
            },
            loginCookieRemover = { sourceId, sourceKey ->
                removals += sourceId to sourceKey
            },
        )
        bridge.putLoginHeader("""{"Authorization":"Bearer token","Cookie":"sid=abc; theme=dark"}""")
        assertEquals(
            listOf(Triple("source-cookie-A", "https://reader.example/source.json", "sid=abc; theme=dark")),
            writes,
        )
        bridge.removeLoginHeader()
        assertEquals(listOf("source-cookie-A" to "https://reader.example/source.json"), removals)
    }

    @Test
    fun loginInfoCipherMatchesPinnedAndroidIdAesRoundTripAndRejectsBadKeyOrCiphertext() {
        val androidId = "0123456789abcdef"
        val plain = """{"username":"reader","token":"secret"}"""
        val encrypted = SourceLoginInfoCipher.encrypt(plain, androidId)
        assertTrue(encrypted != plain)
        assertEquals(plain, SourceLoginInfoCipher.decrypt(encrypted, androidId))
        assertTrue(runCatching { SourceLoginInfoCipher.encrypt(plain, "") }.isFailure)
        assertTrue(runCatching { SourceLoginInfoCipher.decrypt("not-valid-ciphertext", androidId) }.isFailure)
    }

    @Test
    fun legacyPlainLoginInfoMigratesNonDestructivelyAndOnlyOnce() {
        val androidId = "0123456789abcdef"
        val legacy = """{"username":"reader","token":"old"}"""
        var persisted = legacy
        val firstRead = SourceLoginInfoCipher.readStored(persisted, androidId) { persisted = it }
        assertEquals(legacy, firstRead)
        assertTrue(persisted != legacy)
        val encryptedAfterMigration = persisted
        val secondRead = SourceLoginInfoCipher.readStored(persisted, androidId) { persisted = it }
        assertEquals(legacy, secondRead)
        assertEquals(encryptedAfterMigration, persisted)
    }

    @Test
    fun missingAndroidIdPreservesLegacyLoginInfoAndMalformedCiphertextIsNotReturned() {
        val legacy = """{"token":"keep"}"""
        var migrationAttempted = false
        assertEquals(
            legacy,
            SourceLoginInfoCipher.readStored(legacy, "") { migrationAttempted = true },
        )
        assertEquals(false, migrationAttempted)
        assertEquals(
            null,
            SourceLoginInfoCipher.readStored("%%%not-ciphertext%%%", "0123456789abcdef") {},
        )
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
