package app.badcat.client.runtime

import org.junit.Assert.assertNull
import org.junit.Test

class RhinoSourceCacheExtensionsTest {
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
