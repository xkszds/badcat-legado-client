package app.badcat.client.runtime

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SharedPreferencesSourceCookieJarTest {
    private fun cookie(
        name: String,
        value: String,
        domain: String = "reader.example",
        path: String = "/",
        hostOnly: Boolean = true,
        secure: Boolean = false,
    ): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(System.currentTimeMillis() + 60_000)
        if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
        if (secure) builder.secure()
        return builder.build()
    }

    @Test
    fun cookieLookupHonorsHostPathAndSecureScope() {
        val candidates = listOf(
            cookie("root", "one"),
            cookie("book", "two", path = "/book"),
            cookie("secure", "three", secure = true),
            cookie("other", "four", domain = "other.example"),
        )

        assertEquals(
            "root=one; book=two; secure=three",
            applicableSourceCookieHeader("https://reader.example/book/12".toHttpUrl(), candidates),
        )
        assertEquals(
            "root=one; book=two",
            applicableSourceCookieHeader("http://reader.example/book/12".toHttpUrl(), candidates),
        )
        assertEquals(
            "root=one; secure=three",
            applicableSourceCookieHeader("https://reader.example/other".toHttpUrl(), candidates),
        )
    }

    @Test
    fun domainCookiesMayMatchSubdomainsButHostOnlyCookiesMayNot() {
        val candidates = listOf(
            cookie("host", "only", hostOnly = true),
            cookie("domain", "wide", hostOnly = false),
        )

        assertEquals(
            "domain=wide",
            applicableSourceCookieHeader("https://cdn.reader.example/book".toHttpUrl(), candidates),
        )
        assertEquals(
            "host=only; domain=wide",
            applicableSourceCookieHeader("https://reader.example/book".toHttpUrl(), candidates),
        )
    }

    @Test
    fun hostOnlyAndDomainCookiesWithSameNameHaveDifferentStorageKeys() {
        val hostOnly = cookie("session", "host", hostOnly = true)
        val domain = cookie("session", "domain", hostOnly = false)
        assertNotEquals(sourceCookieStorageKey(hostOnly), sourceCookieStorageKey(domain))
        assertEquals(
            listOf("domain"),
            listOf(hostOnly, domain).filter { it.matches("https://sub.reader.example".toHttpUrl()) }
                .map { it.value },
        )
    }


}
