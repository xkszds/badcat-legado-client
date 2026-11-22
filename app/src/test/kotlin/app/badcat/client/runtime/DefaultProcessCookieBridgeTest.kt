package app.badcat.client.runtime

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class DefaultProcessCookieBridgeTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val session = InMemorySourceCookieSessionStore()
    private lateinit var store: SharedPreferencesSourceCookieJar
    private lateinit var web: FakeWebCookieManager
    private lateinit var bridge: DefaultProcessCookieBridge

    @Before
    fun setUp() {
        store = SharedPreferencesSourceCookieJar(context, session)
        listOf("https://reader.example/", "https://outside.otherexample.net/").forEach(store::removeCookie)
        web = FakeWebCookieManager()
        bridge = DefaultProcessCookieBridge(store, web)
    }

    @Test
    fun observesBoundedUrlsAndRejectsInvalidSchemes() {
        repeat(140) { bridge.observe("https://reader.example/path/$it") }
        assertEquals(128, bridge.observedUrls("https://reader.example/").size)
        assertTrue(runCatching { bridge.observe("file:///etc/passwd") }.isFailure)
    }

    @Test
    fun syncToWebViewAndBackUsesDeterministicStorePrecedence() = runBlocking {
        store.setCookie("https://reader.example/login", "sid=authoritative; theme=dark")
        bridge.syncToWebView("https://reader.example/private/book")
        assertTrue(web.getCookie("https://reader.example/private/book").orEmpty().contains("sid=authoritative"))
        web.setCookieHeaderNow("https://reader.example/private/book", "sid=webview; token=from-webview")
        bridge.syncFromWebView("https://reader.example/private/book")
        assertEquals("sid=webview; theme=dark; token=from-webview", store.getCookie("https://reader.example/"))
        assertEquals(linkedMapOf("sid" to "webview", "token" to "from-webview"), session.read("reader.example"))
    }

    @Test
    fun syncFromWebViewKeepsImportedCookiesOutOfPersistentStorage() {
        val url = "https://reader.example/login"
        store.setCookie(url, "sid=persistent; theme=dark")
        web.setCookieHeaderNow(url, "sid=webview; token=from-webview")

        bridge.syncFromWebView(url)

        assertEquals("sid=webview; theme=dark; token=from-webview", store.getCookie(url))
        val afterProcessRestart = SharedPreferencesSourceCookieJar(
            context,
            InMemorySourceCookieSessionStore(),
        )
        assertEquals("sid=persistent; theme=dark", afterProcessRestart.getCookie(url))
    }

    @Test
    fun invalidationExpiresTrackedHostDomainAndPathCandidatesAndPreservesOtherDomain() = runBlocking {
        val tracked = "https://leaf.branch.reader.example/private/deep/chapter"
        val unrelated = "https://outside.otherexample.net/private/book"
        store.setCookie(tracked, "sid=store")
        store.setCookie(unrelated, "unrelated=keep")
        web.setCookieNow(tracked, "bc_host=one; Path=/")
        web.setCookieNow(tracked, "bc_branch=two; Domain=branch.reader.example; Path=/private")
        web.setCookieNow(tracked, "bc_root=three; Domain=reader.example; Path=/private/deep")
        web.setCookieNow(unrelated, "unrelated=keep; Domain=otherexample.net; Path=/private")
        bridge.observe(tracked)

        val result = bridge.invalidateDomain("https://reader.example/source.json")

        assertTrue("Expected a typed successful invalidation result", result is CookieBridgeInvalidationResult.Cleared)
        assertTrue(result.cleared)
        assertTrue(web.getCookie(unrelated).orEmpty().contains("unrelated=keep"))
        assertEquals("", store.getCookie("https://reader.example/"))
    }

    @Test
    fun callbackRejectionFailsClosedAndDoesNotClearAuthoritativeStore() = runBlocking {
        store.setCookie("https://reader.example/", "sid=keep")
        web.rejectWrites = true
        val failed = runCatching { bridge.syncToWebView("https://reader.example/") }.isFailure
        assertTrue(failed)
        assertEquals("sid=keep", store.getCookie("https://reader.example/"))
    }

    @Test
    fun rejectedExpiryReturnsTypedFailureAndPreservesAuthoritativeStore() = runBlocking {
        val url = "https://reader.example/private/book"
        store.setCookie(url, "sid=authoritative")
        web.setCookieNow(url, "sid=webview; Path=/")
        web.rejectExpiryWrites = true

        val result = bridge.invalidateDomain(url)

        assertTrue(result is CookieBridgeInvalidationResult.Failed)
        val failure = result as CookieBridgeInvalidationResult.Failed
        assertEquals(CookieBridgeInvalidationFailureReason.CALLBACK_REJECTED, failure.reason)
        assertTrue(failure.remainingStoreNamesByUrl.values.any { "sid" in it })
        assertEquals("sid=authoritative", store.getCookie(url))
    }

    @Test
    fun acceptedButIneffectiveExpiryReturnsWebViewResidueAndPreservesStore() = runBlocking {
        val url = "https://reader.example/private/book"
        store.setCookie(url, "sid=authoritative")
        web.setCookieNow(url, "sid=webview; Path=/")
        web.ignoreExpiryWrites = true

        val result = bridge.invalidateDomain(url)

        assertTrue(result is CookieBridgeInvalidationResult.Failed)
        val failure = result as CookieBridgeInvalidationResult.Failed
        assertEquals(CookieBridgeInvalidationFailureReason.WEBVIEW_COOKIES_REMAIN, failure.reason)
        assertTrue(failure.remainingWebViewNamesByUrl.values.any { "sid" in it })
        assertTrue(failure.remainingStoreNamesByUrl.values.any { "sid" in it })
        assertEquals("sid=authoritative", store.getCookie(url))
    }

    @Test
    fun expiryCallbackTimeoutReturnsTypedFailureAndPreservesAuthoritativeStore() = runBlocking {
        val url = "https://reader.example/private/book"
        store.setCookie(url, "sid=authoritative")
        web.setCookieNow(url, "sid=webview; Path=/")
        web.neverCallback = true
        val shortTimeoutBridge = DefaultProcessCookieBridge(
            store = store,
            webCookies = web,
            callbackTimeoutMillis = 100L,
        )

        val result = shortTimeoutBridge.invalidateDomain(url)

        assertTrue(result is CookieBridgeInvalidationResult.Failed)
        assertEquals(
            CookieBridgeInvalidationFailureReason.CALLBACK_TIMEOUT,
            (result as CookieBridgeInvalidationResult.Failed).reason,
        )
        assertEquals("sid=authoritative", store.getCookie(url))
    }

    @Test
    fun callerCancellationDuringExpiryIsNotConvertedToInvalidationResult() = runBlocking {
        val url = "https://reader.example/private/book"
        store.setCookie(url, "sid=authoritative")
        web.setCookieNow(url, "sid=webview; Path=/")
        web.neverCallback = true

        val pending = launch { bridge.invalidateDomain(url) }
        delay(25)
        pending.cancelAndJoin()

        assertTrue(pending.isCancelled)
        assertEquals("sid=authoritative", store.getCookie(url))
    }

    private class FakeWebCookieManager : WebCookieManagerPort {
        private val cookies = LinkedHashMap<String, MutableList<Pair<String, String>>>()
        var rejectWrites = false
        var rejectExpiryWrites = false
        var ignoreExpiryWrites = false
        var neverCallback = false

        override fun getCookie(url: String): String? {
            val host = url.toHttpUrl().host
            val path = url.toHttpUrl().encodedPath
            val visible = LinkedHashMap<String, String>()
            cookies.values.flatten().forEach { (identity, raw) ->
                val parts = identity.split("|")
                val domain = parts[0]
                val cookiePath = parts[1]
                val hostOnly = parts[2] == "host"
                if ((host == domain || (!hostOnly && host.endsWith(".$domain"))) &&
                    (path == cookiePath || path.startsWith(cookiePath.trimEnd('/') + "/") || cookiePath == "/")) {
                    val pair = parseLegadoCookieMap(raw.substringBefore(";"))
                    pair.forEach { (name, value) -> visible[name] = value }
                }
            }
            return legadoCookieMapToHeader(visible).ifBlank { null }
        }

        override fun setCookie(url: String, value: String, callback: (Result<Boolean>) -> Unit) {
            if (neverCallback) return
            val isExpiry = value.contains("Max-Age=0", ignoreCase = true)
            val accepted = !rejectWrites && !(isExpiry && rejectExpiryWrites)
            if (accepted && !(isExpiry && ignoreExpiryWrites)) setCookieNow(url, value)
            callback(Result.success(accepted))
        }

        fun setCookieHeaderNow(url: String, header: String) {
            val parsed = url.toHttpUrl()
            val bucket = cookies.getOrPut(parsed.host) { mutableListOf() }
            parseLegadoCookieMap(header).forEach { (name, cookieValue) ->
                val identity = "${parsed.host}|/|host"
                bucket.removeAll { (id, raw) -> id == identity && raw.substringBefore('=').trim() == name }
                bucket += identity to "$name=$cookieValue; Path=/"
            }
        }

        fun setCookieNow(url: String, value: String) {
            val parsed = url.toHttpUrl()
            val first = value.substringBefore(';').trim()
            val name = first.substringBefore('=')
            val cookieValue = first.substringAfter('=', "")
            val attrs = value.split(';').drop(1).map { it.trim() }
            val domainAttr = attrs.firstOrNull { it.startsWith("Domain=", true) }?.substringAfter('=')
            val domain = domainAttr?.trimStart('.') ?: parsed.host
            val path = attrs.firstOrNull { it.startsWith("Path=", true) }?.substringAfter('=') ?: "/"
            val identity = "$domain|$path|${if (domainAttr == null) "host" else "domain"}"
            val bucket = cookies.getOrPut(parsed.host) { mutableListOf() }
            if (attrs.any { it.equals("Max-Age=0", true) }) {
                cookies.values.forEach { list ->
                    list.removeAll { (id, raw) ->
                        id == identity && raw.substringBefore('=').trim() == name
                    }
                }
            } else {
                bucket.removeAll { (id, raw) -> id == identity && raw.substringBefore('=').trim() == name }
                bucket += identity to value
            }
        }

        override fun flush() = Unit
    }
}
