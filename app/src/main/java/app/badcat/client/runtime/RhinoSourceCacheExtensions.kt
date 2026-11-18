package app.badcat.client.runtime

import java.util.concurrent.ConcurrentHashMap

/**
 * Source-isolated in-memory cache matching the common Legado cache helpers.
 * Values stay in the source's Rhino host and are discarded with its scope.
 */
class RhinoSourceCacheExtensions {
    private val values = ConcurrentHashMap<String, Any>()

    fun getFromMemory(key: String): Any? = values[key]
    fun putMemory(key: String, value: Any?): Any? {
        if (key.isBlank() || key.length > MAX_KEY_CHARS) return null
        if (value == null) values.remove(key) else values[key] = value
        return value
    }
    fun get(key: String): Any? = getFromMemory(key)
    fun put(key: String, value: Any?): Any? = putMemory(key, value)
    fun delete(key: String) { values.remove(key) }
    fun remove(key: String) { values.remove(key) }
    fun clear() { values.clear() }

    private companion object { const val MAX_KEY_CHARS = 256 }
}
