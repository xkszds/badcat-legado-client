package app.badcat.client

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverUrlPolicyTest {
    @Test
    fun allowsRelativeAndCrossHostHttpsRedirects() {
        val from = URL("https://reader.example/covers/one.jpg")
        assertEquals(
            URL("https://reader.example/login/cover.jpg"),
            CoverUrlPolicy.resolveRedirect(from, "../login/cover.jpg"),
        )
        assertEquals(
            URL("https://cdn.example/cover.jpg"),
            CoverUrlPolicy.resolveRedirect(from, "https://cdn.example/cover.jpg"),
        )
    }

    @Test
    fun allowsHttpToHttpsUpgradeButRejectsHttpsDowngrade() {
        assertEquals(
            URL("https://reader.example/cover.jpg"),
            CoverUrlPolicy.resolveRedirect(URL("http://reader.example/cover.jpg"), "https://reader.example/cover.jpg"),
        )
        assertNull(
            CoverUrlPolicy.resolveRedirect(URL("https://reader.example/cover.jpg"), "http://cdn.example/cover.jpg"),
        )
    }

    @Test
    fun rejectsUnsafeSchemesCredentialsAndMalformedTargets() {
        val from = URL("https://reader.example/cover.jpg")
        listOf(
            "file:///etc/passwd",
            "javascript:alert(1)",
            "https://user:pass@cdn.example/cover.jpg",
            "",
            "https://",
        ).forEach { assertNull(CoverUrlPolicy.resolveRedirect(from, it)) }
        assertTrue(!CoverUrlPolicy.isSafe(URL("http://user:pass@reader.example/cover.jpg")))
        assertTrue(!CoverUrlPolicy.isSafe(URL("file:///tmp/cover.jpg")))
    }
}
