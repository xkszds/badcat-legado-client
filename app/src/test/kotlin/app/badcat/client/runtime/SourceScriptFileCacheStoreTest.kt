package app.badcat.client.runtime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceScriptFileCacheStoreTest {
    @Test
    fun inMemoryStorePreservesArbitraryBytesAndReturnsDefensiveCopies() {
        val store = InMemorySourceScriptFileCacheStore
        val sourceId = "https://binary-cache.example/"
        val key = "font.bin"
        val original = byteArrayOf(0, 0xff.toByte(), 0x80.toByte(), 0xc3.toByte())

        store.putBytes(sourceId, key, original)
        original[0] = 42
        val firstRead = store.getBytes(sourceId, key)
        assertArrayEquals(byteArrayOf(0, 0xff.toByte(), 0x80.toByte(), 0xc3.toByte()), firstRead)

        firstRead!![1] = 0
        assertArrayEquals(byteArrayOf(0, 0xff.toByte(), 0x80.toByte(), 0xc3.toByte()), store.getBytes(sourceId, key))
        store.remove(sourceId, key)
        assertNull(store.getBytes(sourceId, key))
    }

    @Test
    fun clearingOneSourceDoesNotRemoveAnotherSourcesBytes() {
        val store = InMemorySourceScriptFileCacheStore
        store.putBytes("source-a", "shared", byteArrayOf(1))
        store.putBytes("source-b", "shared", byteArrayOf(2))

        store.clearSource("source-a")

        assertNull(store.getBytes("source-a", "shared"))
        assertArrayEquals(byteArrayOf(2), store.getBytes("source-b", "shared"))
    }
    @Test
    fun inMemoryStoreSeparatesSourceAndKeyValuesContainingNewlines() {
        val store = InMemorySourceScriptFileCacheStore
        store.put("source", "key\\nother", "first")
        store.put("source\\nkey", "other", "second")

        assertEquals("first", store.get("source", "key\\nother"))
        assertEquals("second", store.get("source\\nkey", "other"))

        store.clearSource("source")
        assertNull(store.get("source", "key\\nother"))
        assertEquals("second", store.get("source\\nkey", "other"))
    }

}
