package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceScriptKeyValueStoreTest {
    @Test
    fun inMemoryStoreSeparatesSourceAndKeyValuesContainingNewlines() {
        val store = InMemorySourceScriptKeyValueStore
        store.put("source", "key\nother", "first")
        store.put("source\nkey", "other", "second")

        assertEquals("first", store.get("source", "key\nother"))
        assertEquals("second", store.get("source\nkey", "other"))

        store.clearSource("source")
        assertNull(store.get("source", "key\nother"))
        assertEquals("second", store.get("source\nkey", "other"))
    }
}
