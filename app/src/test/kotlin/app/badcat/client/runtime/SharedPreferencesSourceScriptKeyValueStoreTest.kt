package app.badcat.client.runtime

import android.content.Context
import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35], manifest = Config.NONE)
class SharedPreferencesSourceScriptKeyValueStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val preferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Before
    fun clearPreferences() {
        preferences.edit().clear().commit()
    }

    @Test
    fun removePrefixDeletesOnlyCacheKeysForTheSelectedSource() {
        val store = SharedPreferencesSourceScriptKeyValueStore(context)
        val sourceId = "prefix-source"
        val otherSourceId = "other-prefix-source"

        store.put(sourceId, "__legado_cache__:entry-a", "cache-a")
        store.put(sourceId, "__legado_cache__:entry-b", "cache-b")
        store.put(sourceId, "loginHeader_source", "login-header")
        store.put(sourceId, "sourceVariable", "variable")
        store.put(sourceId, "__legado_cacheX__:not-a-cache-entry", "near-match")
        store.put(otherSourceId, "__legado_cache__:entry-a", "other-source")

        store.removePrefix(sourceId, "__legado_cache__:")

        assertNull(store.get(sourceId, "__legado_cache__:entry-a"))
        assertNull(store.get(sourceId, "__legado_cache__:entry-b"))
        assertEquals("login-header", store.get(sourceId, "loginHeader_source"))
        assertEquals("variable", store.get(sourceId, "sourceVariable"))
        assertEquals("near-match", store.get(sourceId, "__legado_cacheX__:not-a-cache-entry"))
        assertEquals("other-source", store.get(otherSourceId, "__legado_cache__:entry-a"))
    }

    @Test
    fun removePrefixLeavesMalformedAndUnrelatedKeysWithinSourceNamespaceUntouched() {
        val sourceId = "malformed-prefix-source"
        val store = SharedPreferencesSourceScriptKeyValueStore(context)
        val rawPrefix = sourcePrefix(sourceId)
        val unrelatedKey = rawPrefix + encode("ordinary-key")
        val malformedKey = rawPrefix + "!!!"

        preferences.edit()
            .putString(unrelatedKey, "unrelated")
            .putString(malformedKey, "malformed")
            .commit()

        store.removePrefix(sourceId, "__legado_cache__:")

        assertEquals("unrelated", preferences.getString(unrelatedKey, null))
        assertEquals("malformed", preferences.getString(malformedKey, null))
    }

    private fun sourcePrefix(sourceId: String): String = "source_" + encode(sourceId) + "_"

    private fun encode(value: String): String =
        Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )

    private companion object {
        const val PREFERENCES_NAME = "badcat_legado_script_storage"
    }
}
