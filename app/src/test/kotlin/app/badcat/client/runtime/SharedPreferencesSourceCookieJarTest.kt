package app.badcat.client.runtime

import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class SharedPreferencesSourceCookieJarTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val session = InMemorySourceCookieSessionStore()

    @Before
    fun clearFixtureDomains() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        listOf("https://www.reader.example/", "https://www.other.example/").forEach {
            jar.removeCookie(it)
        }
    }

    @Test
    fun sameEffectiveDomainSharesNameValueCookiesAcrossHostsAndPaths() {
        val first = SharedPreferencesSourceCookieJar(context, session)
        val second = SharedPreferencesSourceCookieJar(context, session)

        first.setCookie("https://www.reader.example/private/source.json", "sid=one; theme=dark")
        assertEquals(
            "sid=one; theme=dark",
            second.getCookie("https://cdn.reader.example/public/chapter"),
        )
    }

    @Test
    fun differentEffectiveDomainsRemainIsolated() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        jar.setCookie("https://reader.example/source.json", "sid=reader")
        jar.setCookie("https://other.example/source.json", "sid=other")

        assertEquals("sid=reader", jar.getCookie("https://cdn.reader.example/book"))
        assertEquals("sid=other", jar.getCookie("https://www.other.example/book"))
    }

    @Test
    fun cookieStoreDoesNotApplyRfcPathOrSecureFiltering() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        jar.setCookie("https://reader.example/private/login", "sid=path-independent")
        assertEquals("sid=path-independent", jar.getCookie("http://reader.example/public/book"))
        val loaded = jar.loadForRequest("http://reader.example/public/book".toHttpUrl())
        assertEquals(listOf("sid=path-independent"), loaded.map { "${it.name}=${it.value}" })
    }

    @Test
    fun persistentCookiesSurviveAdapterRecreationButSessionCookiesDoNot() {
        val firstSession = InMemorySourceCookieSessionStore()
        val first = SharedPreferencesSourceCookieJar(context, firstSession)
        val url = "https://www.reader.example/login".toHttpUrl()
        val persistent = Cookie.Builder()
            .name("persistent").value("keep").domain("reader.example").path("/")
            .expiresAt(System.currentTimeMillis() + 60_000).build()
        val sessionCookie = Cookie.Builder()
            .name("session").value("memory").domain("reader.example").path("/").build()

        first.saveFromResponse(url, listOf(persistent, sessionCookie))

        val afterProcessRestart = SharedPreferencesSourceCookieJar(
            context,
            InMemorySourceCookieSessionStore(),
        )
        assertEquals("persistent=keep", afterProcessRestart.getCookie("https://reader.example/next"))
    }

    @Test
    fun sessionCookiesAreSharedByAdaptersInsideOneProcess() {
        val sharedSession = InMemorySourceCookieSessionStore()
        val first = SharedPreferencesSourceCookieJar(context, sharedSession)
        val second = SharedPreferencesSourceCookieJar(context, sharedSession)
        val cookie = Cookie.Builder()
            .name("sid").value("session").domain("reader.example").path("/").build()

        first.saveFromResponse("https://reader.example/".toHttpUrl(), listOf(cookie))

        assertEquals("sid=session", second.getCookie("https://cdn.reader.example/"))
    }

    @Test
    fun persistentAndSessionResponseCookiesAreSavedSeparatelyAndMerged() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        val url = "https://reader.example/".toHttpUrl()
        val persistent = Cookie.Builder()
            .name("theme").value("light").domain("reader.example").path("/")
            .expiresAt(System.currentTimeMillis() + 60_000).build()
        val oldSessionId = Cookie.Builder()
            .name("sid").value("old").domain("reader.example").path("/")
            .expiresAt(System.currentTimeMillis() + 60_000).build()
        val sessionCookie = Cookie.Builder()
            .name("sid").value("abc==").domain("reader.example").path("/").build()

        jar.saveFromResponse(url, listOf(persistent, oldSessionId, sessionCookie))

        assertEquals("theme=light; sid=abc==", jar.getCookie(url.toString()))
    }

    @Test
    fun setReplacesAndReplaceMergesPersistentNames() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        jar.setCookie("https://reader.example/", "sid=old; keep=yes")
        jar.replaceCookie("https://reader.example/", "sid=new; theme=dark")

        assertEquals("sid=new; keep=yes; theme=dark", jar.getCookie("https://reader.example/"))
        jar.setCookie("https://reader.example/", "only=this")
        assertEquals("only=this", jar.getCookie("https://reader.example/"))
    }

    @Test
    fun loginCookieImportMergesAndLogoutRemovesTheWholeDomainBucket() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        jar.setCookie("https://reader.example/", "sid=old; keep=yes")
        jar.mergeLoginCookieHeader("https://www.reader.example/source.json", "sid=login; theme=dark")

        assertEquals("sid=login; keep=yes; theme=dark", jar.getCookie("https://cdn.reader.example/"))
        jar.removeCookie("https://reader.example/source.json")
        assertEquals("", jar.getCookie("https://reader.example/"))
    }

    @Test
    fun removeCookieCanDeleteOneNameOrTheWholeEffectiveDomain() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        jar.setCookie("https://reader.example/", "sid=one; theme=dark")
        jar.removeCookie("https://reader.example/", "sid")
        assertEquals("theme=dark", jar.getCookie("https://reader.example/"))
        jar.removeCookie("https://cdn.reader.example/")
        assertEquals("", jar.getCookie("https://reader.example/"))
    }

    @Test
    fun concurrentSameDomainUpdatesDoNotLoseCookieNames() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        val workers = (0 until 8).map { worker ->
            kotlin.concurrent.thread {
                (0 until 20).forEach { index ->
                    jar.replaceCookie(
                        "https://reader.example/",
                        "worker_${worker}_${index}=value",
                    )
                }
            }
        }
        workers.forEach { it.join() }
        assertEquals(160, parseLegadoCookieMap(jar.getCookie("https://reader.example/")).size)
    }

    @Test
    fun cookieMapParserPreservesEqualsAndIgnoresPairsWithoutValues() {
        assertEquals(
            linkedMapOf("token" to "a=b==", "theme" to "dark"),
            parseLegadoCookieMap("token=a=b==; invalid; theme=dark"),
        )
    }

    @Test
    fun cookieHeaderAndStorageRemainBounded() {
        val jar = SharedPreferencesSourceCookieJar(context, session)
        val cookies = (0 until 300).map { index ->
            Cookie.Builder()
                .name("cookie_$index")
                .value("v".repeat(24))
                .domain("reader.example")
                .path("/")
                .build()
        }
        jar.saveFromResponse("https://reader.example/".toHttpUrl(), cookies)
        assertTrue(jar.getCookie("https://reader.example/").length <= 4_096)
    }
}
