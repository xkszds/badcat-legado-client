package app.badcat.client.runtime

import android.webkit.CookieManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.LinkedHashSet
import kotlin.coroutines.resume

/**
 * Narrow default-process port around Android's WebView cookie provider.
 * Implementations must invoke the callback with the provider's acceptance result.
 */
internal interface WebCookieManagerPort {
    fun getCookie(url: String): String?
    fun setCookie(url: String, value: String, callback: (Boolean) -> Unit)
    fun flush()
}

internal class AndroidWebCookieManagerPort(
    private val manager: CookieManager = CookieManager.getInstance(),
) : WebCookieManagerPort {
    override fun getCookie(url: String): String? = manager.getCookie(url)
    override fun setCookie(url: String, value: String, callback: (Boolean) -> Unit) {
        manager.setCookie(url, value, callback)
    }
    override fun flush() = manager.flush()
}

internal data class CookieBridgeInvalidationResult(
    val trackedUrlCount: Int,
    val attemptedExpiryCount: Int,
    val remainingNamesByUrl: Map<String, Set<String>>,
) {
    val cleared: Boolean get() = remainingNamesByUrl.values.all { it.isEmpty() }
}

/**
 * Default-process-only bridge between Legado's domain-bucket CookieStore and
 * Android WebView CookieManager. It deliberately does not create a WebView or
 * touch a hidden process. CookieManager has no API to enumerate Set-Cookie
 * attributes, so invalidation is bounded to observed URLs and candidate
 * host/domain/path identities derived from those URLs.
 */
internal class DefaultProcessCookieBridge(
    private val store: SharedPreferencesSourceCookieJar,
    private val webCookies: WebCookieManagerPort,
    private val callbackTimeoutMillis: Long = 5_000L,
    private val maxTrackedUrls: Int = 128,
) {
    private val observedByDomain = LinkedHashMap<String, LinkedHashSet<String>>()

    init {
        require(callbackTimeoutMillis in 100L..30_000L)
        require(maxTrackedUrls in 1..512)
    }

    @Synchronized
    fun observe(url: String) {
        val parsed = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Cookie bridge accepts only valid HTTP(S) URLs")
        val domain = legadoSourceDomain(parsed.toString()) ?: parsed.host
        val urls = observedByDomain.getOrPut(domain) { LinkedHashSet() }
        urls.remove(parsed.toString())
        urls.add(parsed.toString())
        while (urls.size > maxTrackedUrls) urls.remove(urls.first())
        while (observedByDomain.values.sumOf { it.size } > maxTrackedUrls) {
            val oldestDomain = observedByDomain.entries.first()
            oldestDomain.value.remove(oldestDomain.value.first())
            if (oldestDomain.value.isEmpty()) observedByDomain.remove(oldestDomain.key)
        }
    }

    @Synchronized
    fun observedUrls(sourceUrl: String): List<String> {
        val parsed = sourceUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Cookie bridge source URL is invalid")
        val domain = legadoSourceDomain(parsed.toString()) ?: parsed.host
        return observedByDomain[domain]?.toList().orEmpty()
    }

    /** Copy the authoritative Legado name/value bucket into WebView for an observed URL. */
    suspend fun syncToWebView(url: String) {
        observe(url)
        val header = store.getCookie(url)
        parseLegadoCookieMap(header).forEach { (name, value) ->
            setAndAwait(url, "$name=$value; Path=/")
        }
        webCookies.flush()
    }

    /** Import WebView-visible name/value pairs into the authoritative persistent bucket. */
    fun syncFromWebView(url: String) {
        observe(url)
        val header = webCookies.getCookie(url).orEmpty()
        if (header.isNotBlank()) store.mergeLoginCookieHeader(url, header)
    }

    /**
     * Expire only names visible in the authoritative store or at observed URLs.
     * Candidate domains include host suffixes down to eTLD+1; candidate paths
     * include root, every observed prefix and slash-terminated prefix. A readback
     * is mandatory; remaining names produce a typed unsuccessful result.
     */
    suspend fun invalidateDomain(sourceUrl: String): CookieBridgeInvalidationResult {
        val parsedSource = sourceUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Cookie bridge source URL is invalid")
        val root = legadoSourceDomain(parsedSource.toString()) ?: parsedSource.host
        val urls = synchronized(this) { observedByDomain[root]?.toList().orEmpty() }
        val targets = LinkedHashSet<String>().apply {
            add(sourceUrl)
            add("https://$root/")
            addAll(urls)
        }.filter { it.toHttpUrlOrNull() != null }.distinct().take(maxTrackedUrls)

        val namesByUrl = LinkedHashMap<String, Set<String>>()
        targets.forEach { url ->
            val names = LinkedHashSet<String>()
            parseLegadoCookieMap(store.getCookie(url)).keys.forEach(names::add)
            parseLegadoCookieMap(webCookies.getCookie(url).orEmpty()).keys.forEach(names::add)
            namesByUrl[url] = names
        }

        var attempts = 0
        targets.forEach { url ->
            val parsed = requireNotNull(url.toHttpUrlOrNull())
            val names = namesByUrl[url].orEmpty()
            val domains = domainCandidates(parsed.host, root)
            val paths = pathCandidates(parsed.encodedPath)
            names.forEach { name ->
                paths.forEach { path ->
                    setAndAwait(url, "$name=; Max-Age=0; Path=$path")
                    attempts++
                    domains.forEach { domain ->
                        setAndAwait(url, "$name=; Max-Age=0; Path=$path; Domain=$domain")
                        attempts++
                    }
                }
            }
        }
        webCookies.flush()

        // Clear the authoritative domain bucket only after all targeted expiry
        // callbacks have completed; preserve unrelated registrable domains.
        store.removeCookie(sourceUrl)
        val remaining = targets.associateWith { url ->
            parseLegadoCookieMap(webCookies.getCookie(url).orEmpty()).keys
        }
        return CookieBridgeInvalidationResult(targets.size, attempts, remaining)
    }

    private suspend fun setAndAwait(url: String, value: String) {
        withTimeout(callbackTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                try {
                    webCookies.setCookie(url, value) { accepted ->
                        if (continuation.isActive) continuation.resume(accepted)
                    }
                } catch (error: Throwable) {
                    if (continuation.isActive) continuation.cancel(error)
                }
            }
        }.also { accepted ->
            check(accepted) { "WebView CookieManager rejected a scoped cookie update" }
        }
    }

    private fun domainCandidates(host: String, registrableDomain: String): List<String> {
        val normalizedHost = host.lowercase()
        val root = registrableDomain.lowercase()
        require(normalizedHost == root || normalizedHost.endsWith(".$root")) {
            "Registrable domain is not a suffix of the observed host"
        }
        val labels = normalizedHost.split('.')
        val rootCount = root.split('.').size
        return (0..(labels.size - rootCount)).map { labels.drop(it).joinToString(".") }
    }

    private fun pathCandidates(rawPath: String): List<String> {
        val path = rawPath.ifBlank { "/" }
        val result = linkedSetOf("/")
        var prefix = ""
        path.split('/').filter(String::isNotEmpty).forEach { segment ->
            prefix += "/$segment"
            result += prefix
            result += "$prefix/"
        }
        return result.toList()
    }
}
