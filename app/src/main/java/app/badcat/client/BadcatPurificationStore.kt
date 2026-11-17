package app.badcat.client

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * App-private persistence for the original Badcat WebView-ad-filter toggle,
 * injected script and opaque AdMark records. It is intentionally independent
 * from the Legado replacement-rule catalog.
 */
class BadcatPurificationSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isWebViewAdFilterEnabled(): Boolean =
        preferences.getBoolean(KEY_IS_AD_BLOCK, false)

    fun setWebViewAdFilterEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_IS_AD_BLOCK, enabled).apply()
    }

    fun webViewInjectionScript(): String? =
        preferences.getString(KEY_AD_BLOCK_JS_INJECT, null)
            ?.takeIf { it.isNotBlank() && it.length <= BadcatPurificationUrlPolicy.MAX_SCRIPT_CHARS }

    /** Stores only a bounded script explicitly supplied by an authorized local source. */
    fun setWebViewInjectionScript(script: String?) {
        require(script == null || script.length <= BadcatPurificationUrlPolicy.MAX_SCRIPT_CHARS) {
            "Badcat WebView purification script exceeds the size limit"
        }
        preferences.edit().putString(KEY_AD_BLOCK_JS_INJECT, script).apply()
    }

    fun snapshot(adMarks: List<BadcatAdMarkItem>): BadcatPurificationSnapshot =
        BadcatPurificationSnapshot(
            webViewAdFilterEnabled = isWebViewAdFilterEnabled(),
            webViewInjectionScript = webViewInjectionScript(),
            adMarks = adMarks,
        )

    private companion object {
        const val PREFERENCES_NAME = "badcat_original_purification"
        const val KEY_IS_AD_BLOCK = "key_is_ad_block"
        const val KEY_AD_BLOCK_JS_INJECT = "key_ad_block_js_inject"
    }
}

/**
 * Durable user-owned AdMark records. The original matching strings are stored
 * without reinterpretation; no private issue/report service is contacted.
 */
class BadcatAdMarkCatalog(context: Context) {
    private val file = AtomicFile(
        File(context.applicationContext.filesDir, FILE_NAME),
    )

    @Synchronized
    fun all(): List<BadcatAdMarkItem> = readItems()

    @Synchronized
    fun upsert(item: BadcatAdMarkItem) {
        require(item.isValid()) { "Invalid Badcat AdMark record" }
        val current = readItems().toMutableList()
        val index = current.indexOfFirst {
            it.host.equals(item.host, ignoreCase = true) && it.urlPattern == item.urlPattern
        }
        if (index >= 0) current[index] = item else current += item
        require(current.size <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        writeItems(current)
    }

    @Synchronized
    fun remove(host: String, urlPattern: String): Boolean {
        val current = readItems().toMutableList()
        val removed = current.removeAll {
            it.host.equals(host, ignoreCase = true) && it.urlPattern == urlPattern
        }
        if (removed) writeItems(current)
        return removed
    }

    @Synchronized
    fun replaceAll(items: List<BadcatAdMarkItem>) {
        require(items.size <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        require(items.all(BadcatAdMarkItem::isValid)) { "Badcat AdMark catalog contains invalid records" }
        val unique = items.distinctBy { it.host.lowercase() + "|" + it.urlPattern }
        require(unique.size == items.size) { "Badcat AdMark catalog contains duplicate records" }
        writeItems(items)
    }

    @Synchronized
    fun exportJson(): String = encode(readItems())

    @Synchronized
    fun importJson(json: String): Int {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES) {
            "Badcat AdMark import exceeds the size limit"
        }
        val imported = decode(json)
        val merged = LinkedHashMap<String, BadcatAdMarkItem>()
        readItems().forEach { merged[key(it)] = it }
        imported.forEach { merged[key(it)] = it }
        require(merged.size <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        writeItems(merged.values.toList())
        return imported.size
    }

    private fun readItems(): List<BadcatAdMarkItem> {
        val backup = File(file.baseFile.path + ".bak")
        if (!file.baseFile.exists() && !backup.exists()) return emptyList()
        return file.openRead().use { input ->
            require(file.baseFile.length() <= MAX_IMPORT_BYTES) {
                "Badcat AdMark catalog exceeds the storage limit"
            }
            val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (raw.isBlank()) emptyList() else decode(raw)
        }
    }

    private fun writeItems(items: List<BadcatAdMarkItem>) {
        val bytes = encode(items).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_IMPORT_BYTES) { "Badcat AdMark catalog exceeds the storage limit" }
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    private fun encode(items: List<BadcatAdMarkItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("host", item.host)
                put("urlPattern", item.urlPattern)
                put("markRules", JSONArray(item.markRules))
            })
        }
        return array.toString()
    }

    private fun decode(raw: String): List<BadcatAdMarkItem> {
        require(raw.length <= MAX_IMPORT_BYTES) { "Badcat AdMark JSON exceeds the size limit" }
        val array = JSONArray(raw)
        require(array.length() <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: error("Badcat AdMark item $index is not an object")
                val rules = obj.optJSONArray("markRules") ?: error("Badcat AdMark item $index has no markRules")
                val item = BadcatAdMarkItem(
                    host = obj.optString("host"),
                    urlPattern = obj.optString("urlPattern"),
                    markRules = buildList {
                        for (ruleIndex in 0 until rules.length()) add(rules.optString(ruleIndex))
                    },
                )
                require(item.isValid()) { "Invalid Badcat AdMark item at index $index" }
                add(item)
            }
        }.also { items ->
            require(items.distinctBy(::key).size == items.size) { "Badcat AdMark JSON contains duplicate records" }
        }
    }

    private fun key(item: BadcatAdMarkItem): String =
        item.host.lowercase() + "|" + item.urlPattern

    private companion object {
        const val FILE_NAME = "badcat-ad-mark-catalog.json"
        const val MAX_ITEMS = 5_000
        const val MAX_IMPORT_BYTES = 2 * 1024 * 1024
    }
}
