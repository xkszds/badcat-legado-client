package app.badcat.client.runtime

import android.content.Context
import android.util.Base64

/** App-private durable storage for safe source-script key/value state. */
class SharedPreferencesSourceScriptKeyValueStore(context: Context) : SourceScriptKeyValueStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun get(sourceId: String, key: String): String? =
        preferences.getString(storageKey(sourceId, key), null)

    override fun put(sourceId: String, key: String, value: String) {
        preferences.edit().putString(storageKey(sourceId, key), value).apply()
    }

    override fun remove(sourceId: String, key: String) {
        preferences.edit().remove(storageKey(sourceId, key)).apply()
    }

    override fun clearSource(sourceId: String) {
        val prefix = sourcePrefix(sourceId)
        preferences.edit().apply {
            preferences.all.keys.filter { it.startsWith(prefix) }.forEach(::remove)
        }.apply()
    }

    private fun storageKey(sourceId: String, key: String): String =
        sourcePrefix(sourceId) + encode(key)

    private fun sourcePrefix(sourceId: String): String =
        "source_" + encode(sourceId) + "_"

    private fun encode(value: String): String =
        Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

    private companion object {
        const val PREFERENCES_NAME = "badcat_legado_script_storage"
    }
}
