package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WebUrlResolverTest {
    @Test
    fun resolvesRelativeAndProtocolRelativeUrls() {
        assertEquals(
            "https://reader.example/chapter/1",
            WebUrlResolver.resolve("https://reader.example/catalog/index", "../chapter/1"),
        )
        assertEquals(
            "https://cdn.example/cover.jpg",
            WebUrlResolver.resolve("https://reader.example/search", "//cdn.example/cover.jpg"),
        )
    }

    @Test
    fun rejectsExecutableLocalAndCredentialBearingUrls() {
        val base = "https://reader.example/search"
        listOf("javascript:alert(1)", "file:///etc/passwd", "https://user:pass@reader.example/book").forEach {
            assertFailsWith<IllegalArgumentException> { WebUrlResolver.resolve(base, it) }
        }
    }

    @Test
    fun rejectsCredentialBearingOrNonWebBaseUrls() {
        assertFailsWith<IllegalArgumentException> {
            WebUrlResolver.resolve("https://user:pass@reader.example/search", "/book")
        }
        assertFailsWith<IllegalArgumentException> {
            WebUrlResolver.resolve("file:///tmp/search", "/book")
        }
    }
}
