package app.badcat.client.runtime

/** Storage boundary for the allow-listed Legado java.getString/putString helpers. */
interface SourceScriptKeyValueStore {
    fun get(sourceId: String, key: String): String?
    fun put(sourceId: String, key: String, value: String)
    fun remove(sourceId: String, key: String) { put(sourceId, key, "") }
    fun clearSource(sourceId: String)
}

/** Process-local fallback for JVM tests and non-Android hosts. */
object InMemorySourceScriptKeyValueStore : SourceScriptKeyValueStore {
    private val values = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun get(sourceId: String, key: String): String? = values[storageKey(sourceId, key)]

    override fun put(sourceId: String, key: String, value: String) {
        values[storageKey(sourceId, key)] = value
    }

    override fun remove(sourceId: String, key: String) { values.remove(storageKey(sourceId, key)) }

    override fun clearSource(sourceId: String) {
        val prefix = sourcePrefix(sourceId)
        values.keys.removeAll { it.startsWith(prefix) }
    }

    private fun sourcePrefix(sourceId: String) = "${sourceId.length}:$sourceId:"
    private fun storageKey(sourceId: String, key: String) = sourcePrefix(sourceId) + "${key.length}:$key"
}
