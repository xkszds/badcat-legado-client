package app.badcat.client.runtime

import org.junit.Assert.assertEquals
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

    @Test
    fun deleteRemovesTheSameKeyFromMemoryKeyValueAndFileStoresOnly() {
        val sourceId = "cache-delete-all-domains-fixture"
        val store = InMemorySourceScriptKeyValueStore
        val files = InMemorySourceScriptFileCacheStore
        val cache = RhinoSourceCacheExtensions(sourceId, store, files)
        val otherSource = RhinoSourceCacheExtensions("cache-delete-other-source", store, files)

        cache.put("shared", "database")
        cache.putFile("shared", "file")
        cache.putMemory("shared", "memory")
        cache.put("keep", "same-source")
        otherSource.put("shared", "other-source")
        otherSource.putFile("shared", "other-file")

        cache.delete("shared")

        assertNull(cache.get("shared"))
        assertNull(cache.getFile("shared"))
        assertNull(cache.getFromMemory("shared"))
        assertEquals("same-source", cache.get("keep"))
        assertEquals("other-source", otherSource.get("shared"))
        assertEquals("other-file", otherSource.getFile("shared"))
    }

    @Test
    fun clearRemovesOnlyThisSourcesCacheNamespaceAcrossAllStores() {
        val sourceId = "cache-clear-source-fixture"
        val store = InMemorySourceScriptKeyValueStore
        val files = InMemorySourceScriptFileCacheStore
        val cache = RhinoSourceCacheExtensions(sourceId, store, files)
        val otherSource = RhinoSourceCacheExtensions("cache-clear-other-source", store, files)

        cache.put("db", "database")
        cache.putFile("file", "file")
        cache.putMemory("memory", "memory")
        store.put(sourceId, "loginHeader_source", "keep source variable")
        otherSource.put("db", "other database")
        otherSource.putFile("file", "other file")

        cache.clear()

        assertNull(cache.get("db"))
        assertNull(cache.getFile("file"))
        assertNull(cache.getFromMemory("memory"))
        assertEquals("keep source variable", store.get(sourceId, "loginHeader_source"))
        assertEquals("other database", otherSource.get("db"))
        assertEquals("other file", otherSource.getFile("file"))
    }

    @Test
    fun deleteMemoryAndRemoveRemainMemoryOnly() {
        val sourceId = "cache-delete-memory-only-fixture"
        val cache = RhinoSourceCacheExtensions(
            sourceId,
            InMemorySourceScriptKeyValueStore,
            InMemorySourceScriptFileCacheStore,
        )
        cache.put("entry", "database")
        cache.putFile("entry", "file")
        cache.putMemory("entry", "memory")

        cache.deleteMemory("entry")
        assertEquals("database", cache.get("entry"))
        assertEquals("file", cache.getFile("entry"))

        cache.putMemory("entry", "memory again")
        cache.remove("entry")
        assertEquals("database", cache.get("entry"))
        assertEquals("file", cache.getFile("entry"))
        assertNull(cache.getFromMemory("entry"))
    }
}
