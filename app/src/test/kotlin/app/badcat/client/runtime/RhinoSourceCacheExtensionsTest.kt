package app.badcat.client.runtime

import org.junit.Assert.assertNull
import org.junit.Test

class RhinoSourceCacheExtensionsTest {
    @Test
    fun nonFiniteTtlsAreRejectedForDatabaseAndFileCache() {
        val cache = RhinoSourceCacheExtensions(
            "cache-nonfinite-ttl-fixture",
            InMemorySourceScriptKeyValueStore,
            InMemorySourceScriptFileCacheStore,
        )
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { ttl ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                cache.put("db-$ttl", "value", ttl)
            }
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                cache.putFile("file-$ttl", "value", ttl)
            }
        }
    }

    @Test
    fun malformedExpiryMetadataIsDiscardedWithoutThrowing() {
        val sourceId = "cache-corrupt-expiry-fixture"
        val key = "__legado_cache__:entry"
        InMemorySourceScriptKeyValueStore.put(
            sourceId,
            key,
            """{"expiresAt":"not-a-number","value":"payload"}""",
        )
        val cache = RhinoSourceCacheExtensions(sourceId, InMemorySourceScriptKeyValueStore)

        assertNull(cache.get("entry"))
        assertNull(InMemorySourceScriptKeyValueStore.get(sourceId, key))
    }

    @Test
    fun cacheEntryWithoutStringValueIsDiscarded() {
        val sourceId = "cache-corrupt-value-fixture"
        val key = "__legado_cache__:entry"
        InMemorySourceScriptKeyValueStore.put(
            sourceId,
            key,
            """{"expiresAt":0,"value":42}""",
        )
        val cache = RhinoSourceCacheExtensions(sourceId, InMemorySourceScriptKeyValueStore)

        assertNull(cache.get("entry"))
        assertNull(InMemorySourceScriptKeyValueStore.get(sourceId, key))
    }
}
