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
    fun ajaxUrlCollectionMaterializationStopsAtConfiguredLimit() {
        var consumed = 0
        val unbounded = Iterable {
            generateSequence { consumed++; "https://example/$consumed" }.iterator()
        }
        assertThrows(IllegalArgumentException::class.java) {
            boundedAjaxUrlValues(unbounded, maxUrls = 20, maxJsonChars = 1024)
        }
        assertEquals(21, consumed)
    }

    @Test
    fun ajaxUrlCollectionsBoundIndividualAndAggregateUrlLengths() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedAjaxUrlValues(listOf("x".repeat(16 * 1024 + 1)), 20, 64 * 1024)
        }
        assertThrows(IllegalArgumentException::class.java) {
            boundedAjaxUrlValues(listOf("x".repeat(16 * 1024), "y".repeat(16 * 1024),
                "z".repeat(16 * 1024), "w".repeat(16 * 1024), "v"), 20, 64 * 1024)
        }
    }

    @Test
    fun ajaxUrlJsonInputIsBoundedBeforeParsing() {
        assertThrows(IllegalArgumentException::class.java) {
            boundedAjaxUrlValues(" ".repeat(1025), maxUrls = 20, maxJsonChars = 1024)
        }
        assertEquals(
            listOf("https://one.example/", "https://two.example/"),
            boundedAjaxUrlValues("""["https://one.example/","https://two.example/"]""", 20, 1024),
        )
    }

    @Test
    fun legacyAjaxRejectsOversizedUrlBeforeCallingTransport() {
        var called = false
        val guarded = RhinoSafeSourceJavaExtensions(
            sourceId = "ajax-limit-fixture",
            store = InMemorySourceScriptKeyValueStore,
            ajaxRequest = { called = true; "unexpected" },
        )
        val result = guarded.ajax("https://example.com/" + "a".repeat(16 * 1024))
        assertEquals(false, called)
        org.junit.Assert.assertTrue(result.contains("per-URL character limit"))
    }

    @Test
    fun legacyHttpGetRejectsOversizedUrlBeforeCallingTransport() {
        var called = false
        val guarded = RhinoSafeSourceJavaExtensions(
            sourceId = "get-limit-fixture",
            store = InMemorySourceScriptKeyValueStore,
            httpGet = { _, _ -> called = true; throw AssertionError("must not call transport") },
        )
        assertThrows(IllegalArgumentException::class.java) {
            guarded.get("https://example.com/" + "a".repeat(16 * 1024))
        }
        assertEquals(false, called)
    }

    @Test
    fun connectRejectsOversizedUrlBeforeCallingTransport() {
        var called = false
        val guarded = RhinoSafeSourceJavaExtensions(
            sourceId = "connect-limit-fixture",
            store = InMemorySourceScriptKeyValueStore,
            httpRequestWithTimeout = { _, _, _, _, _ ->
                called = true
                throw AssertionError("must not call transport")
            },
        )
        val response = guarded.connect("https://example.com/" + "a".repeat(16 * 1024))
        assertEquals(false, called)
        org.junit.Assert.assertTrue(response.body.contains("16 KiB character limit"))
    }

    @Test
    fun downloadFileRejectsOversizedUrlBeforeCallingTransport() {
        var called = false
        val guarded = RhinoSafeSourceJavaExtensions(
            sourceId = "download-limit-fixture",
            store = InMemorySourceScriptKeyValueStore,
            httpGet = { _, _ -> called = true; throw AssertionError("must not call transport") },
        )
        assertThrows(IllegalArgumentException::class.java) {
            guarded.downloadFile("https://example.com/" + "a".repeat(16 * 1024))
        }
        assertEquals(false, called)
    }

    @Test
    fun sourceConcurrencyKeysDoNotAliasWhenIdsAndNamesContainAtSigns() {
        val first = RhinoSafeSourceJavaExtensions(
            sourceId = "a@b",
            store = InMemorySourceScriptKeyValueStore,
        )
        val second = RhinoSafeSourceJavaExtensions(
            sourceId = "a",
            store = InMemorySourceScriptKeyValueStore,
        )

        assertEquals(0, first.tick("c"))
        assertEquals(0, second.tick("b@c"))
        assertEquals(1, first.tick("c"))
        assertEquals(1, second.tick("b@c"))
    }

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
    fun parseJsRequestHeadersBoundsCountAndValueSizes() {
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders((0..100).associate { "X-$it" to "v" })
        }
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders(mapOf("X-Large" to "v".repeat(8 * 1024 + 1)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            extensions.parseJsRequestHeaders(" ".repeat(32 * 1024 + 1))
        }
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
