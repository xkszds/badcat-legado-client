package app.badcat.client.runtime

import android.content.Context
import android.provider.Settings
import io.legado.app.help.crypto.SymmetricCryptoAndroid
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpRequest
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.SourceRequestClient
import io.legado.app.utils.NetworkUtils

/**
 * Mirrors pinned NetworkUtils.getSubDomainOrNull: compare effective-TLD+1,
 * preserve IP literals and fail open when either URL cannot be parsed.
 */
internal fun legadoSourceDomain(url: String): String? =
    NetworkUtils.getSubDomainOrNull(url)

internal fun shouldAttachSourceLoginHeader(sourceKey: String, targetUrl: String): Boolean {
    val sourceDomain = legadoSourceDomain(sourceKey) ?: return true
    val targetDomain = legadoSourceDomain(targetUrl) ?: return true
    return sourceDomain.equals(targetDomain, ignoreCase = true)
}

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
internal object SourceLoginInfoCipher {
    fun encrypt(plainText: String, androidId: String): String {
        val key = androidId.encodeToByteArray(0, 16)
        require(key.size == 16) { "Android ID must provide the 16-byte Legado AES key" }
        return SymmetricCryptoAndroid("AES", key).encryptBase64(plainText)
    }

    fun decrypt(cipherText: String, androidId: String): String {
        val key = androidId.encodeToByteArray(0, 16)
        require(key.size == 16) { "Android ID must provide the 16-byte Legado AES key" }
        return SymmetricCryptoAndroid("AES", key).decrypt(cipherText).toString(Charsets.UTF_8)
    }

    fun isLegacyPlainText(value: String): Boolean = value.trimStart().startsWith("{")

    fun readStored(
        value: String,
        androidId: String,
        onMigrated: (encryptedValue: String) -> Unit,
    ): String? {
        if (isLegacyPlainText(value)) {
            runCatching { encrypt(value, androidId) }.getOrNull()?.let(onMigrated)
            return value
        }
        return runCatching { decrypt(value, androidId) }.getOrNull()
    }
}

/** App-private storage. Login info follows pinned BaseSource's Android-ID AES format. */
class SharedPreferencesSourceLoginSessionStore(
    context: Context,
    private val androidIdProvider: () -> String = {
        Settings.System.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: "null"
    },
) : SourceLoginSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(kind: String, sourceId: String) = "${kind}_${sourceId}"
    override fun getLoginHeader(sourceId: String) = preferences.getString(key("header", sourceId), null)
    override fun putLoginHeader(sourceId: String, headerJson: String) {
        preferences.edit().putString(key("header", sourceId), headerJson).apply()
    }
    override fun removeLoginHeader(sourceId: String) { preferences.edit().remove(key("header", sourceId)).apply() }
    override fun getLoginInfo(sourceId: String): String? {
        val storageKey = key("info", sourceId)
        val stored = preferences.getString(storageKey, null) ?: return null
        return SourceLoginInfoCipher.readStored(stored, androidIdProvider()) { encrypted ->
            preferences.edit().putString(storageKey, encrypted).apply()
        }
    }
    override fun putLoginInfo(sourceId: String, infoJson: String) {
        val encrypted = SourceLoginInfoCipher.encrypt(infoJson, androidIdProvider())
        preferences.edit().putString(key("info", sourceId), encrypted).apply()
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
    private val sourceKeyValueStore: SourceScriptKeyValueStore = InMemorySourceScriptKeyValueStore,
    private val sourceKey: String = sourceId,
    private val loginCookieWriter: ((sourceId: String, sourceKey: String, cookieHeader: String) -> Unit)? = null,
    private val loginCookieRemover: ((sourceId: String, sourceKey: String) -> Unit)? = null,
) {
    /** Legado source key is the imported book-source URL, represented by this client's source id. */
    fun getKey(): String = sourceKey

    /** Per-source variable shared by all rules of this source; null clears it. */
    fun getVariable(): String = sourceKeyValueStore.get(sourceId, SOURCE_VARIABLE_STORAGE_KEY).orEmpty()

    fun setVariable(variable: String?): String {
        if (variable == null) {
            sourceKeyValueStore.remove(sourceId, SOURCE_VARIABLE_STORAGE_KEY)
            return ""
        }
        require(variable.length <= MAX_SOURCE_VARIABLE_CHARS) { "Source variable exceeds the size limit" }
        sourceKeyValueStore.put(sourceId, SOURCE_VARIABLE_STORAGE_KEY, variable)
        return variable
    }
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
        val cookie = parsed.entries.firstOrNull { it.key.equals("Cookie", ignoreCase = true) }?.value
        if (cookie != null) loginCookieWriter?.invoke(sourceId, sourceKey, cookie)
        store.putLoginHeader(sourceId, normalized.toString())
        return normalized.toString()
    }
    fun removeLoginHeader() {
        store.removeLoginHeader(sourceId)
        loginCookieRemover?.invoke(sourceId, sourceKey)
    }
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
        const val SOURCE_VARIABLE_STORAGE_KEY = "__legado_source_variable__"
        const val MAX_SOURCE_VARIABLE_CHARS = 16 * 1024
        const val MAX_ENTRIES = 100
        const val MAX_KEY_CHARS = 256
        const val MAX_VALUE_CHARS = 8 * 1024
        const val MAX_JSON_CHARS = 64 * 1024
        const val MAX_TOTAL_ENTRY_CHARS = 32 * 1024
    }
}

/** Adds saved login headers only to requests on the source's pinned Legado site. */
class SourceLoginSessionRequestClient(
    private val delegate: SourceRequestClient,
    private val sessions: SourceLoginSessionStore,
) : SourceRequestClient {
    override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
        val raw = sessions.getLoginHeader(request.sourceId)
        if (raw.isNullOrBlank()) return delegate.execute(request)
        if (!shouldAttachSourceLoginHeader(request.sourceId, request.url)) return delegate.execute(request)
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
