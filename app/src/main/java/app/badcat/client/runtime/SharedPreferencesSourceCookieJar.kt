package app.badcat.client.runtime

import android.content.Context
import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

internal enum class WebViewCookiePersistencePolicy {
    LEGADO_COMPAT,
    SESSION_ONLY,
}

internal data class SourceWebViewCookieImportMetadata(
    val sourceIdentityHash: String,
    val effectiveDomain: String,
    val policy: WebViewCookiePersistencePolicy,
    val importedAtEpochMillis: Long,
)

/**
 * Session cookies deliberately live only in process memory. The persistent map
 * is the app-private durable projection of Legado's CookieStore domain bucket.
 */
internal interface SourceCookieSessionStore {
    fun read(domain: String): LinkedHashMap<String, String>
    fun write(domain: String, values: Map<String, String>)
    fun clear(domain: String)
}

internal class InMemorySourceCookieSessionStore : SourceCookieSessionStore {
    private val values = ConcurrentHashMap<String, Map<String, String>>()
    override fun read(domain: String) = LinkedHashMap(values[domain].orEmpty())
    override fun write(domain: String, values: Map<String, String>) {
        if (values.isEmpty()) this.values.remove(domain)
        else this.values[domain] = LinkedHashMap(values)
    }
    override fun clear(domain: String) { values.remove(domain) }
}

private object ProcessSourceCookieSessionStore : SourceCookieSessionStore {
    private val delegate = InMemorySourceCookieSessionStore()
    override fun read(domain: String) = delegate.read(domain)
    override fun write(domain: String, values: Map<String, String>) = delegate.write(domain, values)
    override fun clear(domain: String) = delegate.clear(domain)
}

/** Parse Legado CookieStore's name=value map format; attributes are not retained. */
internal fun parseLegadoCookieMap(raw: String): LinkedHashMap<String, String> {
    val result = LinkedHashMap<String, String>()
    raw.split(';').forEach { item ->
        val pair = item.trim()
        val separator = pair.indexOf('=')
        if (separator <= 0) return@forEach
        val name = pair.substring(0, separator).trim { it <= ' ' }
        val value = pair.substring(separator + 1).trim { it <= ' ' }
        // Pinned CookieStore.cookieToMap drops empty values but intentionally
        // preserves the literal "null" value used by some imported sources.
        if (name.isBlank() || name.length > 256 || value.length > 4_096) return@forEach
        if (value.isBlank() && value != "null") return@forEach
        val valid = runCatching {
            Cookie.Builder().name(name).value(value).hostOnlyDomain("cookie.invalid").path("/").build()
        }.isSuccess
        if (valid) result[name] = value
    }
    return result
}

internal fun legadoCookieMapToHeader(values: Map<String, String>): String =
    values.entries.joinToString("; ") { (name, value) -> "$name=$value" }

/**
 * HTTP/JS CookieStore adapter for the pinned Legado semantics:
 * one name/value bucket per effective registrable domain, persistent cookies
 * on disk, session cookies in process memory, and no RFC path/secure filtering.
 *
 * SharedPreferences is a narrow host-storage PoC, not a claim that the upstream
 * Room CookieDao schema or WebView cookie bridge has been ported.
 */
internal class SharedPreferencesSourceCookieJar(
    context: Context,
    private val sessionStore: SourceCookieSessionStore = ProcessSourceCookieSessionStore,
) : CookieJar {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val domain = domainFor(url)
        synchronized(lockFor(domain)) {
            val persistent = readPersistentLocked(domain)
            val session = sessionStore.read(domain)
            val now = System.currentTimeMillis()
            cookies.forEach { cookie ->
                if (!cookie.isWithinLimits()) return@forEach
                if (cookie.expiresAt <= now) {
                    persistent.remove(cookie.name)
                    session.remove(cookie.name)
                } else if (cookie.persistent) {
                    persistent[cookie.name] = cookie.value
                } else {
                    session[cookie.name] = cookie.value
                }
            }
            trim(persistent)
            trim(session)
            writePersistentLocked(domain, persistent)
            sessionStore.write(domain, session)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val values = readBoundedMerged(url.toString())
        return values.mapNotNull { (name, value) ->
            runCatching {
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .hostOnlyDomain(url.host)
                    .path("/")
                    .build()
            }.getOrNull()
        }
    }

    /** Returns the domain-wide name/value Cookie header, independent of URL path. */
    fun getCookie(url: String): String =
        legadoCookieMapToHeader(readBoundedMerged(url))

    /** CookieStore.setCookie replaces the persistent name/value map for this domain. */
    fun setCookie(url: String, cookieHeader: String) {
        val domain = requireDomain(url)
        require(cookieHeader.length <= MAX_COOKIE_HEADER_CHARS) { "Cookie header exceeds the size limit" }
        val parsed = parseLegadoCookieMap(cookieHeader)
        require(parsed.isNotEmpty()) { "Cookie header contains no valid name=value pairs" }
        synchronized(lockFor(domain)) {
            trim(parsed)
            writePersistentLocked(domain, parsed)
        }
    }

    /** CookieStore.replaceCookie merges the supplied names into the persistent map. */
    fun replaceCookie(url: String, cookieHeader: String) {
        mergePersistentCookieHeader(url, cookieHeader)
    }

    /** Login header import merges Cookie pairs by name into the persistent domain bucket. */
    fun mergeLoginCookieHeader(url: String, cookieHeader: String) {
        if (cookieHeader.isBlank()) return
        mergePersistentCookieHeader(url, cookieHeader)
    }

    /**
     * Import WebView-visible name/value pairs. LEGADO_COMPAT follows the pinned
     * CookieStore.setCookie behavior and replaces the persistent domain bucket.
     * SESSION_ONLY merges into process memory without disk writes. WebView does
     * not expose the original Set-Cookie expiry attributes.
     */
    fun importWebViewCookieHeader(
        sourceIdentity: String,
        url: String,
        cookieHeader: String,
        policy: WebViewCookiePersistencePolicy = WebViewCookiePersistencePolicy.LEGADO_COMPAT,
        importedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val identityHash = sourceIdentityHash(sourceIdentity)
        val domain = requireDomain(url)
        require(cookieHeader.isNotBlank()) { "Cookie header must not be blank" }
        require(cookieHeader.length <= MAX_COOKIE_HEADER_CHARS) { "Cookie header exceeds the size limit" }
        require(importedAtEpochMillis >= 0L) { "Import timestamp must not be negative" }
        val imported = parseLegadoCookieMap(cookieHeader)
        require(imported.isNotEmpty()) { "Cookie header contains no valid name=value pairs" }
        synchronized(lockFor(domain)) {
            val session = sessionStore.read(domain)
            when (policy) {
                WebViewCookiePersistencePolicy.LEGADO_COMPAT -> {
                    trim(imported)
                    writePersistentLocked(domain, imported)
                    imported.keys.forEach(session::remove)
                    sessionStore.write(domain, session)
                }
                WebViewCookiePersistencePolicy.SESSION_ONLY -> {
                    val persistent = readPersistentLocked(domain)
                    imported.forEach { (name, value) ->
                        if (persistent[name] == value) session.remove(name)
                        else session[name] = value
                    }
                    trim(session)
                    sessionStore.write(domain, session)
                }
            }
            val metadata = JSONObject()
                .put("sourceIdentityHash", identityHash)
                .put("effectiveDomain", domain)
                .put("policy", policy.name)
                .put("importedAtEpochMillis", importedAtEpochMillis)
            preferences.edit()
                .putString(importMetadataKey(domain, identityHash), metadata.toString())
                .apply()
        }
    }

    /** Read non-secret metadata for one source/domain WebView import. */
    fun webViewCookieImportMetadata(
        sourceIdentity: String,
        url: String,
    ): SourceWebViewCookieImportMetadata? {
        val identityHash = sourceIdentityHash(sourceIdentity)
        val domain = requireDomain(url)
        val raw = preferences.getString(importMetadataKey(domain, identityHash), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            require(json.optString("sourceIdentityHash") == identityHash)
            require(json.optString("effectiveDomain") == domain)
            val policy = WebViewCookiePersistencePolicy.valueOf(json.getString("policy"))
            val importedAt = json.getLong("importedAtEpochMillis")
            require(importedAt >= 0L)
            SourceWebViewCookieImportMetadata(identityHash, domain, policy, importedAt)
        }.getOrNull()
    }

    private fun sourceIdentityHash(sourceIdentity: String): String {
        require(sourceIdentity.isNotBlank() && sourceIdentity == sourceIdentity.trim()) {
            "Source identity must be non-blank and trimmed"
        }
        require(sourceIdentity.length <= MAX_SOURCE_IDENTITY_CHARS) {
            "Source identity exceeds the size limit"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(sourceIdentity.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun importMetadataKey(domain: String, identityHash: String): String =
        "webview_import_${storageKey(domain).removePrefix("domain_")}_$identityHash"

    private fun importMetadataPrefix(domain: String): String =
        "webview_import_${storageKey(domain).removePrefix("domain_")}_"


    private fun mergePersistentCookieHeader(url: String, cookieHeader: String) {
        val domain = requireDomain(url)
        require(cookieHeader.length <= MAX_COOKIE_HEADER_CHARS) { "Cookie header exceeds the size limit" }
        val parsed = parseLegadoCookieMap(cookieHeader)
        require(parsed.isNotEmpty()) { "Cookie header contains no valid name=value pairs" }
        synchronized(lockFor(domain)) {
            val persistent = readPersistentLocked(domain)
            parsed.forEach { (name, value) -> persistent[name] = value }
            trim(persistent)
            writePersistentLocked(domain, persistent)
        }
    }

    /** Remove a complete eTLD+1 bucket, or one name from persistent and session maps. */
    fun removeCookie(url: String, key: String? = null) {
        val domain = domainFor(url.toHttpUrlOrNull() ?: return)
        synchronized(lockFor(domain)) {
            if (key.isNullOrBlank()) {
                val editor = preferences.edit().remove(storageKey(domain))
                preferences.all.keys
                    .filter { it.startsWith(importMetadataPrefix(domain)) }
                    .forEach(editor::remove)
                editor.apply()
                sessionStore.clear(domain)
            } else {
                val persistent = readPersistentLocked(domain)
                persistent.remove(key)
                writePersistentLocked(domain, persistent)
                val session = sessionStore.read(domain)
                session.remove(key)
                sessionStore.write(domain, session)
            }
        }
    }

    /** clearSourceState receives the source URL (BookSource.id), not a partition key. */
    fun clearDomain(sourceKey: String) = removeCookie(sourceKey)

    private fun readBoundedMerged(url: String): LinkedHashMap<String, String> {
        val values = readMerged(url)
        while (legadoCookieMapToHeader(values).length > MAX_COOKIE_HEADER_CHARS && values.isNotEmpty()) {
            values.remove(values.keys.first())
        }
        return values
    }

    private fun readMerged(url: String): LinkedHashMap<String, String> {
        val httpUrl = url.toHttpUrlOrNull() ?: return LinkedHashMap()
        val domain = domainFor(httpUrl)
        return synchronized(lockFor(domain)) {
            val merged = readPersistentLocked(domain)
            sessionStore.read(domain).forEach { (name, value) -> merged[name] = value }
            merged
        }
    }

    private fun readPersistentLocked(domain: String): LinkedHashMap<String, String> {
        val raw = preferences.getString(storageKey(domain), null) ?: return LinkedHashMap()
        if (raw.length > MAX_STORAGE_CHARS) return LinkedHashMap()
        return runCatching {
            val json = JSONObject(raw)
            LinkedHashMap<String, String>().apply {
                json.keys().forEach { name ->
                    val value = json.optString(name, "")
                    if (name.isNotBlank() && name.length <= 256 && value.length <= 4_096) {
                        put(name, value)
                    }
                }
            }
        }.getOrDefault(LinkedHashMap())
    }

    private fun writePersistentLocked(domain: String, values: Map<String, String>) {
        val bounded = LinkedHashMap(values)
        trim(bounded)
        val json = JSONObject()
        bounded.forEach { (name, value) -> json.put(name, value) }
        val encoded = json.toString()
        if (encoded.length > MAX_STORAGE_CHARS) {
            while (bounded.isNotEmpty()) {
                bounded.remove(bounded.keys.first())
                val reduced = JSONObject()
                bounded.forEach { (name, value) -> reduced.put(name, value) }
                if (reduced.toString().length <= MAX_STORAGE_CHARS) {
                    preferences.edit().putString(storageKey(domain), reduced.toString()).apply()
                    return
                }
            }
        }
        preferences.edit().putString(storageKey(domain), encoded).apply()
    }

    private fun requireDomain(url: String): String {
        val parsed = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Cookie URL must be a valid HTTP(S) URL")
        return domainFor(parsed)
    }

    private fun domainFor(url: HttpUrl): String =
        legadoSourceDomain(url.toString()) ?: url.host

    private fun storageKey(domain: String): String = "domain_" + Base64.encodeToString(
        domain.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    private fun Cookie.isWithinLimits(): Boolean =
        name.isNotBlank() && name.length <= 256 && value.length <= 4_096 &&
            runCatching {
                Cookie.Builder().name(name).value(value).hostOnlyDomain("cookie.invalid").path("/").build()
            }.isSuccess

    private fun trim(values: LinkedHashMap<String, String>) {
        while (values.size > MAX_COOKIE_COUNT) values.remove(values.keys.first())
    }

    private fun lockFor(domain: String): Any = locks.computeIfAbsent(domain) { Any() }

    private companion object {
        private const val PREFERENCES_NAME = "badcat_legado_cookie_store"
        private const val MAX_COOKIE_COUNT = 300
        private const val MAX_COOKIE_HEADER_CHARS = 4_096
        private const val MAX_STORAGE_CHARS = 256 * 1024
        private const val MAX_SOURCE_IDENTITY_CHARS = 512
        private val locks = ConcurrentHashMap<String, Any>()
    }
}
