package app.badcat.client.runtime

import android.content.Context
import android.util.Base64
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
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
            decode(preferences.getString(storageKey, null)).forEach { cookie ->
                if (cookie.expiresAt > System.currentTimeMillis()) {
                    cookies[cookieKey(cookie)] = cookie
                }
            }
            persistLocked()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            cookies.forEach { cookie ->
                val key = cookieKey(cookie)
                if (cookie.expiresAt <= now) this.cookies.remove(key)
                else this.cookies[key] = cookie
            }
            removeExpiredLocked(now)
            persistLocked()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        removeExpiredLocked(System.currentTimeMillis())
        persistLocked()
        cookies.values.filter { it.matches(url) }
    }

    private fun removeExpiredLocked(now: Long) {
        cookies.entries.removeAll { it.value.expiresAt <= now }
    }

    private fun persistLocked() {
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
        preferences.edit().putString(storageKey, array.toString()).apply()
    }

    private fun decode(raw: String?): List<Cookie> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
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

    companion object {
        private const val PREFERENCES_NAME = "badcat_legado_source_cookies"
        private val locks = ConcurrentHashMap<String, Any>()
    }
}
