package app.badcat.client.runtime

import android.content.Context
import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * App-private durable cookie storage, isolated by Legado source URL.
 *
 * Cookies are stored in the application's private SharedPreferences (the app
 * disables Android backup). Domain, path, expiry, host-only, secure and
 * HttpOnly attributes are retained so restored cookies keep their scope.
 */
class SharedPreferencesSourceCookieJar(
    context: Context,
    sourceId: String,
) : CookieJar {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val storageKey = "source_" + Base64.encodeToString(
        sourceId.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
    private val lock = locks.computeIfAbsent(storageKey) { Any() }
    private val cookies = LinkedHashMap<String, Cookie>()

    init {
        synchronized(lock) {
            reloadLocked()
            removeExpiredLocked(System.currentTimeMillis())
            persistLocked()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            // A second engine instance (for example, search and reader screens)
            // may own another jar for this source. Reload under the shared key lock
            // before merging so an older in-memory snapshot cannot erase its cookies.
            reloadLocked()
            val now = System.currentTimeMillis()
            cookies.forEach { cookie ->
                if (!cookie.isWithinStorageLimits()) return@forEach
                val key = cookieKey(cookie)
                this.cookies.remove(key)
                if (cookie.expiresAt <= now) this.cookies.remove(key)
                else this.cookies[key] = cookie
            }
            removeExpiredLocked(now)
            trimToCookieLimitLocked()
            persistLocked()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        // Keep independently-created clients for the same source in sync.
        reloadLocked()
        removeExpiredLocked(System.currentTimeMillis())
        persistLocked()
        cookies.values.filter { it.matches(url) }
    }

    /**
     * Returns only cookies applicable to [url], using OkHttp's domain/path/secure
     * matching. This mirrors Legado's java.getCookie(url) helper without exposing
     * cookies belonging to another book source.
     */
    fun getCookie(url: String): String {
        val httpUrl = url.toHttpUrlOrNull() ?: return ""
        return synchronized(lock) {
            reloadLocked()
            removeExpiredLocked(System.currentTimeMillis())
            persistLocked()
            cookies.values.asSequence()
                .filter { it.matches(httpUrl) }
                .joinToString("; ") { "${it.name}=${it.value}" }
        }
    }

    /** Clears this source's persisted cookies across all jar instances. */
    fun clear() {
        synchronized(lock) {
            cookies.clear()
            preferences.edit().remove(storageKey).apply()
        }
    }

    private fun reloadLocked() {
        cookies.clear()
        decode(preferences.getString(storageKey, null)).forEach { cookie ->
            if (cookie.expiresAt > System.currentTimeMillis()) {
                cookies[cookieKey(cookie)] = cookie
            }
        }
    }

    private fun removeExpiredLocked(now: Long) {
        cookies.entries.removeAll { it.value.expiresAt <= now }
    }

    private fun persistLocked() {
        trimToCookieLimitLocked()
        var encoded = encodeCookies()
        while (encoded.length > MAX_STORAGE_CHARS && cookies.isNotEmpty()) {
            cookies.remove(cookies.keys.first())
            encoded = encodeCookies()
        }
        preferences.edit().putString(storageKey, encoded.takeIf(String::isNotEmpty) ?: "[]").apply()
    }

    private fun encodeCookies(): String {
        val array = JSONArray()
        cookies.values.forEach { cookie ->
            array.put(JSONObject().apply {
                put("name", cookie.name)
                put("value", cookie.value)
                put("domain", cookie.domain)
                put("path", cookie.path)
                put("expiresAt", cookie.expiresAt)
                put("hostOnly", cookie.hostOnly)
                put("secure", cookie.secure)
                put("httpOnly", cookie.httpOnly)
            })
        }
        return array.toString()
    }

    private fun trimToCookieLimitLocked() {
        while (cookies.size > MAX_COOKIE_COUNT) cookies.remove(cookies.keys.first())
    }

    private fun decode(raw: String?): List<Cookie> {
        if (raw.isNullOrBlank() || raw.length > MAX_STORAGE_CHARS) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                val firstIndex = (array.length() - MAX_COOKIE_COUNT).coerceAtLeast(0)
                for (index in firstIndex until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name")
                    val value = item.optString("value")
                    val domain = item.optString("domain")
                    val path = item.optString("path", "/")
                    val expiresAt = item.optLong("expiresAt", 0L)
                    if (name.isBlank() || domain.isBlank() || expiresAt <= System.currentTimeMillis()) continue
                    val builder = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .expiresAt(expiresAt)
                        .path(path)
                    if (item.optBoolean("hostOnly", false)) builder.hostOnlyDomain(domain)
                    else builder.domain(domain)
                    if (item.optBoolean("secure", false)) builder.secure()
                    if (item.optBoolean("httpOnly", false)) builder.httpOnly()
                    runCatching { builder.build() }.getOrNull()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun cookieKey(cookie: Cookie): String =
        "${cookie.domain}|${cookie.path}|${cookie.name}"

    private fun Cookie.isWithinStorageLimits(): Boolean =
        name.isNotBlank() && name.length <= MAX_COOKIE_NAME_CHARS &&
            value.length <= MAX_COOKIE_VALUE_CHARS &&
            domain.length <= MAX_COOKIE_DOMAIN_CHARS &&
            path.length <= MAX_COOKIE_PATH_CHARS

    companion object {
        private const val PREFERENCES_NAME = "badcat_legado_source_cookies"
        private const val MAX_COOKIE_COUNT = 300
        private const val MAX_COOKIE_NAME_CHARS = 256
        private const val MAX_COOKIE_VALUE_CHARS = 4_096
        private const val MAX_COOKIE_DOMAIN_CHARS = 253
        private const val MAX_COOKIE_PATH_CHARS = 2_048
        private const val MAX_STORAGE_CHARS = 256 * 1024
        private val locks = ConcurrentHashMap<String, Any>()
    }
}
