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
