package app.badcat.engine

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkHttpSourceRequestClientTest {

    @Test
    fun cookieJarStoresAndReturnsMatchingCookies() {
        val jar = MemorySourceCookieJar()
        val url = "https://reader.example/path/page".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("sid")
            .value("abc123")
            .domain("reader.example")
            .path("/")
            .build()

        jar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), jar.loadForRequest(url))
        assertTrue(jar.loadForRequest("https://other.example/".toHttpUrl()).isEmpty())
    }

    @Test
    fun cookieJarKeepsPathAndSecureScope() {
        val jar = MemorySourceCookieJar()
        val secureUrl = "https://reader.example/private/login".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("token")
            .value("secret")
            .domain("reader.example")
            .path("/private")
            .secure()
            .build()

        jar.saveFromResponse(secureUrl, listOf(cookie))

        assertEquals(listOf(cookie), jar.loadForRequest(secureUrl))
        assertTrue(jar.loadForRequest("https://reader.example/public".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("http://reader.example/private".toHttpUrl()).isEmpty())
    }

    @Test
    fun separateSourceJarsDoNotShareCookies() {
        val firstSourceJar = MemorySourceCookieJar()
        val secondSourceJar = MemorySourceCookieJar()
        val url = "https://reader.example/".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("sid")
            .value("source-one")
            .domain("reader.example")
            .build()

        firstSourceJar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), firstSourceJar.loadForRequest(url))
        assertTrue(secondSourceJar.loadForRequest(url).isEmpty())
    }

    @Test
    fun cookieJarBoundsStorageWhenSourceEmitsManyDistinctCookies() {
        val jar = MemorySourceCookieJar()
        val url = "https://reader.example/".toHttpUrl()
        val cookies = (0..3_100).map { index ->
            Cookie.Builder()
                .name("cookie_$index")
                .value("value")
                .domain("reader.example")
                .build()
        }

        jar.saveFromResponse(url, cookies)

        assertTrue(jar.size <= 3_000)
        assertTrue(jar.loadForRequest(url).size <= 3_000)
    }

    @Test
    fun redirectValidatorAllowsSafeRelativeAndCrossHostHttpsRedirects() {
        val from = "https://reader.example/book/1".toHttpUrl()
        assertTrue(isSafeRedirectTarget(from, "../login"))
        assertTrue(isSafeRedirectTarget(from, "https://cdn.example/book/1"))
    }

    @Test
    fun redirectValidatorRejectsCredentialsAndHttpsDowngrades() {
        val from = "https://reader.example/book/1".toHttpUrl()
        assertTrue(!isSafeRedirectTarget(from, "https://user:pass@cdn.example/book/1"))
        assertTrue(!isSafeRedirectTarget(from, "http://reader.example/book/1"))
        assertTrue(!isSafeRedirectTarget(from, "javascript:alert(1)"))
        assertTrue(!isSafeRedirectTarget(from, "file:///etc/passwd"))
    }

    @Test
    fun redirectValidatorAllowsHttpToHttpsUpgrade() {
        val from = "http://reader.example/book/1".toHttpUrl()
        assertTrue(isSafeRedirectTarget(from, "https://reader.example/book/1"))
    }
}
