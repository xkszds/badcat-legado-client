package app.badcat.client.runtime

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashSet
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Narrow default-process port around Android's WebView cookie provider.
 * Implementations must invoke the callback with the provider's acceptance result.
 */
internal interface WebCookieManagerPort {
    fun getCookie(url: String): String?
    fun setCookie(url: String, value: String, callback: (Result<Boolean>) -> Unit)
    fun flush()
}

internal class AndroidWebCookieManagerPort(
    private val manager: CookieManager = CookieManager.getInstance(),
) : WebCookieManagerPort {
    private val mainLooper = Looper.getMainLooper()
    private val mainHandler = Handler(mainLooper)

    override fun getCookie(url: String): String? = manager.getCookie(url)

    override fun setCookie(url: String, value: String, callback: (Result<Boolean>) -> Unit) {
        val operation = Runnable {
            try {
                manager.setCookie(url, value) { accepted ->
                    callback(Result.success(accepted))
                }
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }

        if (Looper.myLooper() == mainLooper) {
            operation.run()
        } else if (!mainHandler.post(operation)) {
            callback(Result.failure(IllegalStateException("WebView CookieManager main Looper is unavailable")))
        }
    }

    override fun flush() = manager.flush()
}

internal enum class CookieBridgeInvalidationFailureReason {
    CALLBACK_REJECTED,
    CALLBACK_TIMEOUT,
    CALLBACK_ERROR,
    WEBVIEW_COOKIES_REMAIN,
    AUTHORITATIVE_STORE_COOKIES_REMAIN,
    AUTHORITATIVE_STORE_CLEAR_ERROR,
}

internal sealed interface CookieBridgeInvalidationResult {
    val trackedUrlCount: Int
    val attemptedExpiryCount: Int
    val cleared: Boolean

    data class Cleared(
        override val trackedUrlCount: Int,
        override val attemptedExpiryCount: Int,
    ) : CookieBridgeInvalidationResult {
        override val cleared: Boolean = true
    }

    data class Failed(
        val reason: CookieBridgeInvalidationFailureReason,
        override val trackedUrlCount: Int,
        override val attemptedExpiryCount: Int,
        val remainingWebViewNamesByUrl: Map<String, Set<String>>,
        val remainingStoreNamesByUrl: Map<String, Set<String>>,
    ) : CookieBridgeInvalidationResult {
        override val cleared: Boolean = false
    }
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
            check(setAndAwait(url, "$name=$value; Path=/") == true) { "WebView CookieManager rejected or timed out a scoped cookie update" }
        }
        webCookies.flush()
    }

    /**
     * Import WebView-visible name/value pairs for an explicit source lifecycle.
     * The default mirrors pinned Legado persistence; SESSION_ONLY is opt-in.
     */
    fun syncFromWebView(
        sourceIdentity: String,
        url: String,
        policy: WebViewCookiePersistencePolicy = WebViewCookiePersistencePolicy.LEGADO_COMPAT,
    ) {
        require(sourceIdentity.isNotBlank() && sourceIdentity == sourceIdentity.trim() &&
            sourceIdentity.length <= 512) { "Cookie import requires a bounded source identity" }
        observe(url)
        val header = webCookies.getCookie(url).orEmpty()
        if (header.isNotBlank()) store.importWebViewCookieHeader(sourceIdentity, url, header, policy)
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
        for (url in targets) {
            val parsed = requireNotNull(url.toHttpUrlOrNull())
            val names = namesByUrl[url].orEmpty()
            val domains = domainCandidates(parsed.host, root)
            val paths = pathCandidates(parsed.encodedPath)
            for (name in names) {
                for (path in paths) {
                    attempts++
                    val failure = awaitExpiry(url, "$name=; Max-Age=0; Path=$path")
                    if (failure != null) return invalidationFailure(failure, targets, attempts)
                    for (domain in domains) {
                        attempts++
                        val domainFailure = awaitExpiry(
                            url,
                            "$name=; Max-Age=0; Path=$path; Domain=$domain",
                        )
                        if (domainFailure != null) {
                            return invalidationFailure(domainFailure, targets, attempts)
                        }
                    }
                }
            }
        }

        try {
            webCookies.flush()
        } catch (error: Exception) {
            return invalidationFailure(
                CookieBridgeInvalidationFailureReason.CALLBACK_ERROR,
                targets,
                attempts,
            )
        }

        // Preserve the authoritative bucket unless every targeted WebView URL
        // has been read back clean after all expiry callbacks have completed.
        val remainingWebView = webViewNamesByUrl(targets)
        if (remainingWebView.values.any { it.isNotEmpty() }) {
            return CookieBridgeInvalidationResult.Failed(
                reason = CookieBridgeInvalidationFailureReason.WEBVIEW_COOKIES_REMAIN,
                trackedUrlCount = targets.size,
                attemptedExpiryCount = attempts,
                remainingWebViewNamesByUrl = remainingWebView,
                remainingStoreNamesByUrl = storeNamesByUrl(targets),
            )
        }

        try {
            store.removeCookie(sourceUrl)
        } catch (error: Exception) {
            return CookieBridgeInvalidationResult.Failed(
                reason = CookieBridgeInvalidationFailureReason.AUTHORITATIVE_STORE_CLEAR_ERROR,
                trackedUrlCount = targets.size,
                attemptedExpiryCount = attempts,
                remainingWebViewNamesByUrl = remainingWebView,
                remainingStoreNamesByUrl = storeNamesByUrl(targets),
            )
        }

        val remainingStore = storeNamesByUrl(targets)
        if (remainingStore.values.any { it.isNotEmpty() }) {
            return CookieBridgeInvalidationResult.Failed(
                reason = CookieBridgeInvalidationFailureReason.AUTHORITATIVE_STORE_COOKIES_REMAIN,
                trackedUrlCount = targets.size,
                attemptedExpiryCount = attempts,
                remainingWebViewNamesByUrl = remainingWebView,
                remainingStoreNamesByUrl = remainingStore,
            )
        }

        return CookieBridgeInvalidationResult.Cleared(
            trackedUrlCount = targets.size,
            attemptedExpiryCount = attempts,
        )
    }

    private suspend fun awaitExpiry(
        url: String,
        value: String,
    ): CookieBridgeInvalidationFailureReason? {
        return try {
            when (setAndAwait(url, value)) {
                null -> CookieBridgeInvalidationFailureReason.CALLBACK_TIMEOUT
                false -> CookieBridgeInvalidationFailureReason.CALLBACK_REJECTED
                true -> null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            CookieBridgeInvalidationFailureReason.CALLBACK_ERROR
        }
    }

    private fun invalidationFailure(
        reason: CookieBridgeInvalidationFailureReason,
        targets: List<String>,
        attempts: Int,
    ): CookieBridgeInvalidationResult.Failed =
        CookieBridgeInvalidationResult.Failed(
            reason = reason,
            trackedUrlCount = targets.size,
            attemptedExpiryCount = attempts,
            remainingWebViewNamesByUrl = webViewNamesByUrl(targets),
            remainingStoreNamesByUrl = storeNamesByUrl(targets),
        )

    private fun webViewNamesByUrl(targets: List<String>): Map<String, Set<String>> =
        targets.associateWith { url ->
            parseLegadoCookieMap(webCookies.getCookie(url).orEmpty()).keys
        }

    private fun storeNamesByUrl(targets: List<String>): Map<String, Set<String>> =
        targets.associateWith { url ->
            parseLegadoCookieMap(store.getCookie(url)).keys
        }

    private suspend fun setAndAwait(url: String, value: String): Boolean? =
        withTimeoutOrNull(callbackTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                try {
                    webCookies.setCookie(url, value) { result ->
                        if (continuation.isActive) {
                            result.fold(
                                onSuccess = { accepted -> continuation.resume(accepted) },
                                onFailure = { error -> continuation.resumeWithException(error) },
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
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
