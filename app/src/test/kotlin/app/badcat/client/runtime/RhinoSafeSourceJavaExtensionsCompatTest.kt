package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RhinoSafeSourceJavaExtensionsCompatTest {
    private val extensions = RhinoSafeSourceJavaExtensions(
        sourceId = "https://headers.example/",
        store = InMemorySourceScriptKeyValueStore,
    )

    @Test
    fun parseJsRequestHeadersAcceptsNullAndReturnsEmptyMap() {
        assertEquals(emptyMap<String, String>(), extensions.parseJsRequestHeaders(null))
    }

    @Test
    fun parseJsRequestHeadersAcceptsMapsAndStringifiesScalarValues() {
        assertEquals(
            mapOf("X-Count" to "3", "X-Enabled" to "true"),
            extensions.parseJsRequestHeaders(mapOf("X-Count" to 3, "X-Enabled" to true)),
        )
    }

    @Test
    fun parseJsRequestHeadersAcceptsJsonObjects() {
        assertEquals(
            mapOf("Accept" to "application/json", "X-Page" to "2"),
            extensions.parseJsRequestHeaders("""{"Accept":"application/json","X-Page":2}"""),
        )
    }

    @Test
    fun parseJsRequestHeadersRejectsInvalidJsonAndUnsupportedTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders("[1,2]")
        }
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders("{invalid")
        }
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders("""{"X-Test":null}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders(42)
        }
    }
}
