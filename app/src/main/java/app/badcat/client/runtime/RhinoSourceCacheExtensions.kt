package app.badcat.client.runtime

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.ConcurrentHashMap

/**
 * Source-isolated Legado cache helpers.
 *
 * cache.put/get/delete use the source's durable key-value store; the
 * putMemory/getFromMemory/deleteMemory family is deliberately process-local.
 */
class RhinoSourceCacheExtensions(
    private val sourceId: String,
    private val store: SourceScriptKeyValueStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val values = ConcurrentHashMap<String, Any>()

    fun getFromMemory(key: String): Any? = values[key]

    fun putMemory(key: String, value: Any?): Any? {
        requireValidKey(key)
        if (value == null) values.remove(key) else {
            require(value.toString().length <= MAX_VALUE_CHARS) { "cache memory value exceeds the size limit" }
            values[key] = value
        }
        return value
    }

    fun deleteMemory(key: String) {
        values.remove(key)
    }

    fun get(key: String): String? {
        requireValidKey(key)
        val raw = store.get(sourceId, storageKey(key)) ?: return null
        val entry = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            ?: run {
                store.remove(sourceId, storageKey(key))
                return null
            }
        val expiresAt = entry.get("expiresAt")?.asLong ?: 0L
        if (expiresAt > 0L && nowMillis() >= expiresAt) {
            store.remove(sourceId, storageKey(key))
            return null
        }
        return entry.get("value")?.takeIf { it.isJsonPrimitive }?.asString
    }

    fun put(key: String, value: String): String = put(key, value, 0L)

    /** saveTime follows Legado's seconds unit; zero means no expiry. */
    fun put(key: String, value: String, saveTime: Number): String {
        requireValidKey(key)
        require(value.length <= MAX_VALUE_CHARS) { "cache value exceeds the size limit" }
        val seconds = saveTime.toLong()
        require(seconds >= 0L && seconds <= MAX_TTL_SECONDS) { "cache saveTime is outside the supported range" }
        val expiresAt = if (seconds == 0L) 0L else nowMillis() + seconds * 1_000L
        val entry = JsonObject().apply {
            addProperty("value", value)
            addProperty("expiresAt", expiresAt)
        }
        store.put(sourceId, storageKey(key), entry.toString())
        return value
    }

    fun delete(key: String) {
        requireValidKey(key)
        store.remove(sourceId, storageKey(key))
    }

    /** Kept as a compatibility alias for callers that used the old in-memory API. */
    fun remove(key: String) = deleteMemory(key)

    fun clear() {
        values.clear()
    }

    private fun storageKey(key: String) = STORAGE_PREFIX + key

    private fun requireValidKey(key: String) {
        require(key.isNotBlank() && key.length <= MAX_KEY_CHARS) { "cache key is blank or too long" }
    }

    private companion object {
        const val STORAGE_PREFIX = "__legado_cache__:"
        const val MAX_KEY_CHARS = 256
        const val MAX_VALUE_CHARS = 64 * 1024
        const val MAX_TTL_SECONDS = 365L * 24L * 60L * 60L
    }
}
