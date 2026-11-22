package app.badcat.client.runtime

import android.os.Build
import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.app.Instrumentation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Probes the real provider's path-scoped cookie behavior without external I/O.
 * The test intentionally never calls removeAllCookies(), which would affect
 * unrelated domains in the shared emulator profile.
 */
@RunWith(AndroidJUnit4::class)
class WebViewCookieDomainInvalidationInstrumentedTest {

    @Test
    fun rootExpiryClearsHostOnlyRootCookieButLeavesUnobservedPathCookie() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val managerRef = AtomicReference<CookieManager>()
        val host = "badcat-cookie-${System.nanoTime()}.example.com"
        val registrableRootUrl = "https://example.com/"
        val hostRootUrl = "https://$host/"
        val privateUrl = "https://$host/private/book"

        instrumentation.runOnMainSync {
            managerRef.set(CookieManager.getInstance())
            managerRef.get().setAcceptCookie(true)
        }
        val manager = managerRef.get()
        assertNotNull("Android WebView CookieManager must be available", manager)

        try {
            // Host-only cookies isolate path behavior from Domain-cookie identity.
            setCookie(instrumentation, manager, hostRootUrl, "bc_root=one; Path=/")
            setCookie(instrumentation, manager, hostRootUrl, "bc_private=two; Path=/private")

            val before = readCookie(instrumentation, manager, privateUrl)
            assertTrue("Root cookie should be visible", before.contains("bc_root=one"))
            assertTrue("Path-scoped cookie should be visible on its path", before.contains("bc_private=two"))

            expireWithPinnedUpstreamStrategy(
                instrumentation,
                manager,
                hostRootUrl,
                registrableRootUrl,
            )

            val afterRootExpiry = readCookie(instrumentation, manager, privateUrl)
            assertFalse("Host-only root cookie should be expired", afterRootExpiry.contains("bc_root="))
            assertTrue(
                "A path-scoped cookie not visible at either root URL remains; logout must account for observed paths",
                afterRootExpiry.contains("bc_private=two"),
            )

            // Once the matching path is known, the provider can expire it.
            setCookie(
                instrumentation,
                manager,
                privateUrl,
                "bc_private=; Expires=Wed, 31 Dec 2000 23:59:59 GMT; Path=/private",
            )
            assertFalse(
                "Path-scoped cookie should be gone after matching-path expiry",
                readCookie(instrumentation, manager, privateUrl).contains("bc_private="),
            )
        } finally {
            setCookie(instrumentation, manager, hostRootUrl, "bc_root=; Max-Age=0; Path=/")
            setCookie(instrumentation, manager, privateUrl, "bc_private=; Max-Age=0; Path=/private")
        }
    }

    @Test
    fun upstreamExpiryWithoutDomainAttributeLeavesDomainScopedCookie() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val managerRef = AtomicReference<CookieManager>()
        val host = "badcat-cookie-domain-${System.nanoTime()}.example.com"
        val registrableRootUrl = "https://example.com/"
        val hostRootUrl = "https://$host/"
        val cookieName = "bc_domain"

        instrumentation.runOnMainSync {
            managerRef.set(CookieManager.getInstance())
            managerRef.get().setAcceptCookie(true)
        }
        val manager = managerRef.get()
        assertNotNull("Android WebView CookieManager must be available", manager)

        try {
            setCookie(
                instrumentation,
                manager,
                hostRootUrl,
                "$cookieName=three; Domain=example.com; Path=/",
            )
            assertTrue(
                "Domain-scoped cookie should be visible before upstream expiry",
                readCookie(instrumentation, manager, hostRootUrl).contains("$cookieName=three"),
            )

            expireWithPinnedUpstreamStrategy(
                instrumentation,
                manager,
                hostRootUrl,
                registrableRootUrl,
            )

            assertTrue(
                "Pinned upstream expiry omits Domain, so this Domain-scoped cookie must remain visible",
                readCookie(instrumentation, manager, hostRootUrl).contains("$cookieName=three"),
            )
        } finally {
            // Remove only this unique test cookie using its matching domain identity.
            setCookie(
                instrumentation,
                manager,
                hostRootUrl,
                "$cookieName=; Max-Age=0; Path=/; Domain=example.com",
            )
        }
    }

    /**
     * Mirrors CookieManagerExtensions.removeCookie(url) at the pinned Legado SHA:
     * inspect the host URL and registrable-domain URL, then expire each visible
     * name without adding Domain or Path attributes.
     */
    private fun expireWithPinnedUpstreamStrategy(
        instrumentation: Instrumentation,
        manager: CookieManager,
        hostRootUrl: String,
        registrableRootUrl: String,
    ) {
        for (url in listOf(hostRootUrl, registrableRootUrl)) {
            parseNames(readCookie(instrumentation, manager, url)).forEach { name ->
                setCookie(
                    instrumentation,
                    manager,
                    url,
                    "$name=; Expires=Wed, 31 Dec 2000 23:59:59 GMT",
                )
            }
        }
    }

    @Test
    fun trackedDomainAndPathCandidatesClearVisibleCookiesWithoutTouchingOtherDomains() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val managerRef = AtomicReference<CookieManager>()
        val nonce = System.nanoTime().toString()
        val rootDomain = "badcat-$nonce.example.com"
        val intermediateDomain = "branch.$rootDomain"
        val host = "leaf.$intermediateDomain"
        val trackedUrl = "https://$host/private/deep/chapter"
        val unrelatedHost = "outside-$nonce.otherexample.net"
        val unrelatedUrl = "https://$unrelatedHost/private/deep/chapter"

        instrumentation.runOnMainSync {
            managerRef.set(CookieManager.getInstance())
            managerRef.get().setAcceptCookie(true)
        }
        val manager = managerRef.get()
        assertNotNull("Android WebView CookieManager must be available", manager)

        try {
            // One host-only cookie and three Domain-scoped variants exercise
            // host, intermediate suffix, registrable-domain and path identity.
            setCookie(instrumentation, manager, trackedUrl, "bc_track_host=one; Path=/")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_intermediate=two; Domain=$intermediateDomain; Path=/private")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_root=three; Domain=$rootDomain; Path=/private/deep")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_host_domain=four; Domain=$host; Path=/private/deep/chapter")
            setCookie(instrumentation, manager, unrelatedUrl, "bc_unrelated=keep; Domain=otherexample.net; Path=/private")

            val expectedNames = setOf(
                "bc_track_host",
                "bc_track_intermediate",
                "bc_track_root",
                "bc_track_host_domain",
            )
            val before = readCookie(instrumentation, manager, trackedUrl)
            expectedNames.forEach { name ->
                assertTrue("Expected $name to be visible at the tracked URL", before.contains("$name="))
            }
            assertTrue(
                "Unrelated control cookie should be visible before invalidation",
                readCookie(instrumentation, manager, unrelatedUrl).contains("bc_unrelated=keep"),
            )

            expireTrackedCookieVariants(
                instrumentation = instrumentation,
                manager = manager,
                trackedUrls = listOf(trackedUrl),
            )

            val after = readCookie(instrumentation, manager, trackedUrl)
            expectedNames.forEach { name ->
                assertFalse("Tracked cookie $name must be absent after candidate invalidation; remaining=$after", after.contains("$name="))
            }
            assertTrue(
                "Invalidating example.com must not clear a cookie from another registrable domain",
                readCookie(instrumentation, manager, unrelatedUrl).contains("bc_unrelated=keep"),
            )
        } finally {
            setCookie(instrumentation, manager, trackedUrl, "bc_track_host=; Max-Age=0; Path=/")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_intermediate=; Max-Age=0; Path=/private; Domain=$intermediateDomain")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_root=; Max-Age=0; Path=/private/deep; Domain=$rootDomain")
            setCookie(instrumentation, manager, trackedUrl, "bc_track_host_domain=; Max-Age=0; Path=/private/deep/chapter; Domain=$host")
            setCookie(instrumentation, manager, unrelatedUrl, "bc_unrelated=; Max-Age=0; Path=/private; Domain=otherexample.net")
        }
    }

    /**
     * CookieManager exposes only the name/value pairs visible at a URL. Probe
     * the bounded candidate cross-product for each observed URL: host-only and
     * every DNS suffix down to the effective registrable domain, combined with
     * root and every valid path prefix (including slash-terminated variants).
     * Each candidate is a targeted expiry; no global CookieManager clear is used.
     */
    private fun expireTrackedCookieVariants(
        instrumentation: Instrumentation,
        manager: CookieManager,
        trackedUrls: List<String>,
    ) {
        trackedUrls.forEach { url ->
            val visibleNames = parseNames(readCookie(instrumentation, manager, url))
            if (visibleNames.isEmpty()) return@forEach

            val parsed = URL(url)
            val registrableDomain = requireNotNull(legadoSourceDomain(url)) {
                "Tracked URL must have a registrable domain"
            }
            val domains = domainSuffixCandidates(parsed.host, registrableDomain)
            val paths = pathPrefixCandidates(parsed.path)

            visibleNames.forEach { name ->
                paths.forEach { path ->
                    setCookie(
                        instrumentation,
                        manager,
                        url,
                        "$name=; Max-Age=0; Path=$path",
                    )
                    domains.forEach { domain ->
                        setCookie(
                            instrumentation,
                            manager,
                            url,
                            "$name=; Max-Age=0; Path=$path; Domain=$domain",
                        )
                    }
                }
            }
        }
    }

    private fun domainSuffixCandidates(host: String, registrableDomain: String): List<String> {
        val normalizedHost = host.lowercase()
        val normalizedRoot = registrableDomain.lowercase()
        require(normalizedHost == normalizedRoot || normalizedHost.endsWith(".$normalizedRoot")) {
            "Registrable domain must be a suffix of the tracked host"
        }
        val labels = normalizedHost.split('.')
        val rootLabelCount = normalizedRoot.split('.').size
        return (0..(labels.size - rootLabelCount))
            .map { offset -> labels.drop(offset).joinToString(".") }
    }

    private fun pathPrefixCandidates(rawPath: String): List<String> {
        val path = rawPath.ifBlank { "/" }
        val result = linkedSetOf("/")
        var prefix = ""
        path.split('/').filter(String::isNotEmpty).forEach { segment ->
            prefix += "/$segment"
            result += prefix
            result += "$prefix/"
        }
        if (path.startsWith("/") && path != "/") result += path
        return result.toList()
    }

    private fun setCookie(
        instrumentation: Instrumentation,
        manager: CookieManager,
        url: String,
        value: String,
    ) {
        val completed = CountDownLatch(1)
        val accepted = AtomicReference<Boolean?>()
        instrumentation.runOnMainSync {
            manager.setCookie(url, value) { result ->
                accepted.set(result)
                completed.countDown()
            }
        }
        assertTrue("CookieManager.setCookie callback timed out on API ${Build.VERSION.SDK_INT}", completed.await(5, TimeUnit.SECONDS))
        assertTrue("CookieManager rejected a test cookie", accepted.get() == true)
        instrumentation.runOnMainSync { manager.flush() }
    }

    private fun readCookie(
        instrumentation: Instrumentation,
        manager: CookieManager,
        url: String,
    ): String {
        val value = AtomicReference<String?>()
        instrumentation.runOnMainSync { value.set(manager.getCookie(url)) }
        return value.get().orEmpty()
    }

    private fun parseNames(header: String): Set<String> =
        header.split(';').mapNotNull { pair ->
            pair.trim().substringBefore('=').takeIf(String::isNotBlank)
        }.toSet()
}
