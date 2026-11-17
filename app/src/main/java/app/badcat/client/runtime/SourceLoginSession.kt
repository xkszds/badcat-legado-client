package app.badcat.client.runtime

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpRequest
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.SourceRequestClient

/** Per-source persistent authentication state used by Legado's source.* helpers. */
interface SourceLoginSessionStore {
    fun getLoginHeader(sourceId: String): String?
    fun putLoginHeader(sourceId: String, headerJson: String)
    fun removeLoginHeader(sourceId: String)
    fun getLoginInfo(sourceId: String): String?
    fun putLoginInfo(sourceId: String, infoJson: String)
    fun removeLoginInfo(sourceId: String)
    fun clearSource(sourceId: String)
}

object InMemorySourceLoginSessionStore : SourceLoginSessionStore {
    private val headers = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val infos = java.util.concurrent.ConcurrentHashMap<String, String>()
    override fun getLoginHeader(sourceId: String) = headers[sourceId]
    override fun putLoginHeader(sourceId: String, headerJson: String) { headers[sourceId] = headerJson }
    override fun removeLoginHeader(sourceId: String) { headers.remove(sourceId) }
    override fun getLoginInfo(sourceId: String) = infos[sourceId]
    override fun putLoginInfo(sourceId: String, infoJson: String) { infos[sourceId] = infoJson }
    override fun removeLoginInfo(sourceId: String) { infos.remove(sourceId) }
    override fun clearSource(sourceId: String) { headers.remove(sourceId); infos.remove(sourceId) }
}

/** App-private storage. Values may contain authentication secrets and are never logged. */
class SharedPreferencesSourceLoginSessionStore(context: Context) : SourceLoginSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(kind: String, sourceId: String) = "${kind}_${sourceId}"
    override fun getLoginHeader(sourceId: String) = preferences.getString(key("header", sourceId), null)
    override fun putLoginHeader(sourceId: String, headerJson: String) {
        preferences.edit().putString(key("header", sourceId), headerJson).apply()
    }
    override fun removeLoginHeader(sourceId: String) { preferences.edit().remove(key("header", sourceId)).apply() }
    override fun getLoginInfo(sourceId: String) = preferences.getString(key("info", sourceId), null)
    override fun putLoginInfo(sourceId: String, infoJson: String) {
        preferences.edit().putString(key("info", sourceId), infoJson).apply()
    }
    override fun removeLoginInfo(sourceId: String) { preferences.edit().remove(key("info", sourceId)).apply() }
    override fun clearSource(sourceId: String) {
        preferences.edit().remove(key("header", sourceId)).remove(key("info", sourceId)).apply()
    }
    private companion object { const val PREFS = "badcat_legado_source_login_session" }
}

/** Narrow Rhino bridge for the documented source login/session methods. */
class RhinoSourceLoginExtensions(
    private val sourceId: String,
    private val store: SourceLoginSessionStore,
) {
    fun getLoginHeader(): String = store.getLoginHeader(sourceId).orEmpty()
    fun getLoginHeaderMap(): HashMap<String, String> = parseStringMap(getLoginHeader(), "login header")
    fun putLoginHeader(header: String): String {
        val parsed = parseStringMap(header, "login header")
        require(parsed.isNotEmpty()) { "Login header must be a non-empty JSON object" }
        require(parsed.all { (name, value) -> validHeader(name, value) }) {
            "Login header contains an invalid HTTP header name or value"
        }
        val normalized = JsonObject()
        parsed.forEach { (name, value) -> normalized.addProperty(name, value) }
        store.putLoginHeader(sourceId, normalized.toString())
        return normalized.toString()
    }
    fun removeLoginHeader() { store.removeLoginHeader(sourceId) }
    fun getLoginInfo(): String = store.getLoginInfo(sourceId).orEmpty()
    fun getLoginInfoMap(): HashMap<String, String> = parseStringMap(getLoginInfo(), "login info")
    fun putLoginInfo(info: String): String {
        val parsed = parseStringMap(info, "login info")
        val normalized = JsonObject()
        parsed.forEach { (key, value) -> normalized.addProperty(key, value) }
        store.putLoginInfo(sourceId, normalized.toString())
        return normalized.toString()
    }
    fun removeLoginInfo() { store.removeLoginInfo(sourceId) }

    private fun parseStringMap(raw: String, label: String): HashMap<String, String> {
        if (raw.isBlank()) return hashMapOf()
        require(raw.length <= MAX_JSON_CHARS) { "$label exceeds the total size limit" }
        val element = JsonParser.parseString(raw)
        require(element.isJsonObject) { "$label must be a JSON object" }
        val json = element.asJsonObject
        require(json.size() <= MAX_ENTRIES) { "$label contains too many entries" }
        val result = hashMapOf<String, String>()
        var totalChars = 0
        json.entrySet().forEach { (key, value) ->
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                "$label values must be strings"
            }
            val text = value.asString
            require(key.length <= MAX_KEY_CHARS && text.length <= MAX_VALUE_CHARS) {
                "$label entry exceeds the size limit"
            }
            totalChars += key.length + text.length
            require(totalChars <= MAX_TOTAL_ENTRY_CHARS) { "$label exceeds the total entry size limit" }
            result[key] = text
        }
        return result
    }

    private fun validHeader(name: String, value: String): Boolean =
        name.isNotBlank() && name.length <= 256 &&
            name.all { it.isLetterOrDigit() || it in "!#$%&'*+-.^_`|~" } &&
            value.none { it == '\r' || it == '\n' || it.code < 0x20 && it != '\t' || it.code == 0x7f }

    private companion object {
        const val MAX_ENTRIES = 100
        const val MAX_KEY_CHARS = 256
        const val MAX_VALUE_CHARS = 8 * 1024
        const val MAX_JSON_CHARS = 64 * 1024
        const val MAX_TOTAL_ENTRY_CHARS = 32 * 1024
    }
}

/** Adds the source's saved login header to every subsequent request for that source. */
class SourceLoginSessionRequestClient(
    private val delegate: SourceRequestClient,
    private val sessions: SourceLoginSessionStore,
) : SourceRequestClient {
    override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
        val raw = sessions.getLoginHeader(request.sourceId)
        if (raw.isNullOrBlank()) return delegate.execute(request)
        val loginHeaders = runCatching {
            RhinoSourceLoginExtensions(request.sourceId, sessions).getLoginHeaderMap()
        }.getOrDefault(hashMapOf())
        if (loginHeaders.isEmpty()) return delegate.execute(request)
        val merged = LinkedHashMap<String, String>()
        request.headers.forEach { (name, value) -> merged[name] = value }
        loginHeaders.forEach { (name, value) ->
            val existing = merged.keys.firstOrNull { it.equals(name, ignoreCase = true) }
            if (existing != null) merged.remove(existing)
            merged[name] = value
        }
        return delegate.execute(request.copy(headers = merged))
    }
}
