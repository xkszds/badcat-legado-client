package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WebUrlResolverTest {
    @Test
    fun preservesLegadoKeyAndPageTemplatesUntilRequestExecution() {
        assertEquals(
            "https://reader.example/toc?q={{key}}&page={{page}}",
            WebUrlResolver.resolve(
                "https://reader.example/book/1",
                "/toc?q={{key}}&page={{page}}",
            ),
        )
    }
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
    fun resolvesUrlWhilePreservingLegadoJsonOptionsSuffix() {
        assertEquals(
            """https://reader.example/api/book/detail?id=42,{"headers":{"sign":"abc"}}""",
            WebUrlResolver.resolve(
                "https://reader.example/search",
                """/api/book/detail?id=42,{"headers":{"sign":"abc"}}""",
            ),
        )
    }

    @Test
    fun encodesWhitespaceAndAdditionalHashCharactersInFragments() {
        assertEquals(
            "http://www.95shubao.net/#%E2%99%A4%0A%23Qyc",
            WebUrlResolver.resolve("http://www.95shubao.net/", "#%E2%99%A4\n#Qyc"),
        )
    }

    @Test
    fun rejectsBlankUrlReferences() {
        listOf("", "   ").forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                WebUrlResolver.resolve("https://reader.example/book/1", value)
            }
        }
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
    @Test
    fun queryOnlyPaginationLinksKeepTheCurrentPath() {
        assertEquals(
            "https://reader.example/book/1/catalog?page=2",
            WebUrlResolver.resolve(
                "https://reader.example/book/1/catalog?page=1",
                "?page=2",
            ),
        )
        assertEquals(
            "https://reader.example/chapter/1?page=2",
            WebUrlResolver.resolve("https://reader.example/chapter/1", "?page=2"),
        )
    }

}
