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

    @Test
    fun removePrefixClearsOnlyMatchingKeysForOneSource() {
        val store = InMemorySourceScriptKeyValueStore
        store.put("prefix-source", "__legado_cache__:a", "cache-a")
        store.put("prefix-source", "__legado_cache__:b", "cache-b")
        store.put("prefix-source", "loginHeader_source", "login")
        store.put("other-prefix-source", "__legado_cache__:a", "other")

        store.removePrefix("prefix-source", "__legado_cache__:")

        assertNull(store.get("prefix-source", "__legado_cache__:a"))
        assertNull(store.get("prefix-source", "__legado_cache__:b"))
        assertEquals("login", store.get("prefix-source", "loginHeader_source"))
        assertEquals("other", store.get("other-prefix-source", "__legado_cache__:a"))
    }
}
