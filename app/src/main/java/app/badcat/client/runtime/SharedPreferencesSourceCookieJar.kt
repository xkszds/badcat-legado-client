package app.badcat.client.runtime

import android.content.Context
import android.util.Base64
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap


internal fun parseLoginCookieHeader(url: HttpUrl, raw: String): List<Cookie> {
    if (raw.isBlank()) return emptyList()
    require(raw.length <= 8 * 1024) { "Login Cookie header exceeds the size limit" }
    return raw.split(';').mapNotNull { part ->
        val pair = part.trim()
        if (pair.isEmpty()) return@mapNotNull null
        val separator = pair.indexOf('=')
        require(separator > 0) { "Login Cookie header contains an invalid pair" }
        val name = pair.substring(0, separator).trim()
        val value = pair.substring(separator + 1).trim()
        require(name.length <= 256 && value.length <= 4096) {
            "Login Cookie entry exceeds the size limit"
        }
        Cookie.Builder().name(name).value(value).path("/").hostOnlyDomain(url.host).build()
    }
}

internal fun sourceCookieBelongsToDomain(cookie: Cookie, sourceDomain: String): Boolean =
    sourceCookieBelongsToDomain(cookie, sourceDomain)

internal fun sourceCookieStorageKey(cookie: Cookie): String =
    "${cookie.domain}|${cookie.path}|${cookie.name}|${cookie.hostOnly}"

/** Build a Cookie request header using OkHttp's full domain/path/secure matching. */
internal fun applicableSourceCookieHeader(url: HttpUrl, candidates: Collection<Cookie>): String =
    candidates.asSequence()
        .filter { it.matches(url) }
        .joinToString("; ") { "${it.name}=${it.value}" }

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
            applicableSourceCookieHeader(httpUrl, cookies.values)
        }
    }

    /** Adds or updates one Set-Cookie value for this source and URL. */
    fun setCookie(url: String, cookieHeader: String) {
        saveCookieHeader(url, cookieHeader, replaceApplicable = false)
    }

    /** Replaces all cookies applicable to this URL with the supplied Set-Cookie value. */
    fun replaceCookie(url: String, cookieHeader: String) {
        saveCookieHeader(url, cookieHeader, replaceApplicable = true)
    }

    private fun saveCookieHeader(url: String, cookieHeader: String, replaceApplicable: Boolean) {
        val httpUrl = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Cookie URL must be a valid HTTP(S) URL")
        require(cookieHeader.isNotBlank() && cookieHeader.length <= MAX_COOKIE_HEADER_CHARS) {
            "Cookie header is blank or exceeds the size limit"
        }
        val parsed = Cookie.parseAll(
            httpUrl,
            Headers.Builder().add("Set-Cookie", cookieHeader).build(),
        ).filter { cookie -> cookie.isWithinStorageLimits() }
        require(parsed.isNotEmpty()) { "Cookie header is invalid for the supplied URL" }
        synchronized(lock) {
            reloadLocked()
            if (replaceApplicable) {
                cookies.entries.removeAll { (_, cookie) -> cookie.matches(httpUrl) }
            }
            val now = System.currentTimeMillis()
            parsed.forEach { cookie ->
                val key = cookieKey(cookie)
                cookies.remove(key)
                if (cookie.expiresAt > now) cookies[key] = cookie
            }
            removeExpiredLocked(now)
            trimToCookieLimitLocked()
            persistLocked()
        }
    }

    /**
     * Imports the raw Cookie request header saved by Legado's BaseSource.putLoginHeader.
     * The source's own registrable domain is replaced; cookies for unrelated CDN domains
     * remain intact. Imported pairs are host-only cookies scoped to the source host.
     */
    fun replaceLoginCookieHeader(url: String, cookieHeader: String) {
        val httpUrl = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Login cookie URL must be a valid HTTP(S) URL")
        require(cookieHeader.length <= MAX_COOKIE_HEADER_CHARS) {
            "Login Cookie header exceeds the size limit"
        }
        val parsed = parseLoginCookieHeader(httpUrl, cookieHeader)
        val sourceDomain = legadoSourceDomain(url) ?: httpUrl.host
        synchronized(lock) {
            reloadLocked()
            cookies.entries.removeAll { (_, cookie) ->
                sourceCookieBelongsToDomain(cookie, sourceDomain)
            }
            parsed.forEach { cookie -> cookies[cookieKey(cookie)] = cookie }
            removeExpiredLocked(System.currentTimeMillis())
            trimToCookieLimitLocked()
            persistLocked()
        }
    }

    /** Mirrors CookieStore.removeCookie(sourceKey) without touching other sources or CDN domains. */
    fun removeForSourceDomain(url: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val sourceDomain = legadoSourceDomain(url) ?: httpUrl.host
        synchronized(lock) {
            reloadLocked()
            cookies.entries.removeAll { (_, cookie) ->
                sourceCookieBelongsToDomain(cookie, sourceDomain)
            }
            persistLocked()
        }
    }

    /** Removes only cookies that would be sent to [url], preserving other host/path cookies. */
    fun removeForUrl(url: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        synchronized(lock) {
            reloadLocked()
            cookies.entries.removeAll { (_, cookie) -> cookie.matches(httpUrl) }
            persistLocked()
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

    private fun cookieKey(cookie: Cookie): String = sourceCookieStorageKey(cookie)

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
        private const val MAX_COOKIE_HEADER_CHARS = 8 * 1024
        private val locks = ConcurrentHashMap<String, Any>()
    }
}
