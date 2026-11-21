package app.badcat.client.runtime

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import app.badcat.client.FileBookSourceCatalog
import app.badcat.client.FileReplaceRuleCatalog
import app.badcat.client.BadcatAdMarkCatalog
import app.badcat.client.BadcatPurificationSettingsStore
import app.badcat.engine.BookTextReplacement
import app.badcat.engine.CatalogLegadoBookTextReplacement
import app.badcat.engine.BookSourceSearchUrlResolver
import app.badcat.engine.OkHttpSourceRequestClient
import app.badcat.engine.ReadingEngine
import app.badcat.engine.SourceBookDetailService
import app.badcat.engine.SourceChapterContentService
import app.badcat.engine.SourceExploreService
import app.badcat.engine.SourceReadingEngine
import app.badcat.engine.SourceSearchService
import app.badcat.engine.SourceTocService
import app.badcat.engine.SourceRequestClient
import app.badcat.engine.SourceScriptRuntime
import app.badcat.engine.SourceRequestFactory
import app.badcat.engine.LegadoUrlResolver
import app.badcat.engine.WebUrlResolver
import app.badcat.engine.EngineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Builds the Android host's shared, multi-source reading-engine graph.
 *
 * All four source services share one gateway whose embedded evaluator is
 * selected from the BookSource supplied to each request. The source catalog
 * remains app-private and durable. Legado title/content replacement is part of
 * the chapter pipeline; original Badcat WebView injection and AdMark state are
 * exposed through a separate coordinator created by this same factory.
 */
class BadcatReadingEngineFactory(
    private val context: Context,
    private val requestClient: SourceRequestClient? = null,
    private val scriptRuntime: SourceScriptRuntime? = null,
    private val bookTextReplacement: BookTextReplacement = BookTextReplacement { text -> text },
    private val loginSessionStore: SourceLoginSessionStore = SharedPreferencesSourceLoginSessionStore(context.applicationContext),
) {
    private val catalog = FileBookSourceCatalog(context.applicationContext)
    private val replaceRuleCatalog = FileReplaceRuleCatalog(context.applicationContext)
    private val badcatPurificationSettings = BadcatPurificationSettingsStore(context.applicationContext)
    private val badcatAdMarkCatalog = BadcatAdMarkCatalog(context.applicationContext)
    private val sourceCookieStore by lazy {
        SharedPreferencesSourceCookieJar(context.applicationContext)
    }

    fun create(): ReadingEngine {
        val transport = requestClient ?: OkHttpSourceRequestClient(
            cookieJarProvider = { sourceCookieStore },
        )
        val durableRequestClient = SourceLoginSessionRequestClient(transport, loginSessionStore)
        val activeScriptRuntime: SourceScriptRuntime = scriptRuntime ?: RhinoSourceScriptRuntime(
            webViewUserAgent = android.webkit.WebSettings.getDefaultUserAgent(context.applicationContext),
            sourceKeyValueStore = SharedPreferencesSourceScriptKeyValueStore(context.applicationContext),
            sourceFileCacheStore = FileSourceScriptCacheStore(context.applicationContext),
            sourceFileRoot = context.externalCacheDir ?: context.cacheDir,
            androidIdProvider = {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            },
            toastPresenter = { message, long ->
                val appContext = context.applicationContext
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        appContext,
                        message,
                        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            sourceLoginSessionStore = loginSessionStore,
            sourceCookieReader = { _, url ->
                sourceCookieStore.getCookie(url)
            },
            sourceCookieRemover = { _, url ->
                sourceCookieStore.removeCookie(url)
            },
            sourceCookieWriter = { _, url, cookie ->
                sourceCookieStore.setCookie(url, cookie)
            },
            sourceCookieReplacer = { _, url, cookie ->
                sourceCookieStore.replaceCookie(url, cookie)
            },
            sourceLoginCookieWriter = { _, sourceKey, cookie ->
                sourceCookieStore.mergeLoginCookieHeader(sourceKey, cookie)
            },
            sourceLoginCookieRemover = { _, sourceKey ->
                sourceCookieStore.removeCookie(sourceKey)
            },
            sourceCookieClearer = { sourceId ->
                sourceCookieStore.clearDomain(sourceId)
            },
            sourceCookieKeyRemover = { _, url, key ->
                sourceCookieStore.removeCookie(url, key)
            },
            sourceAjax = { source, baseUrl, urlAndOptions ->
                val resolved = when (val parsed = LegadoUrlResolver.resolve(baseUrl, urlAndOptions)) {
                    is EngineResult.Failure -> throw IllegalArgumentException(parsed.message)
                    is EngineResult.Success -> parsed.value
                }
                val request = when (val created = SourceRequestFactory.create(source, resolved)) {
                    is EngineResult.Failure -> throw IllegalArgumentException(created.message)
                    is EngineResult.Success -> created.value.copy(
                        timeoutMillis = (created.value.timeoutMillis ?: 15_000L).coerceAtMost(30_000L),
                        retryCount = created.value.retryCount.coerceAtMost(3),
                    )
                }
                when (val result = runBlocking(Dispatchers.IO) { durableRequestClient.execute(request) }) {
                    is EngineResult.Failure -> throw IllegalStateException(result.message)
                    is EngineResult.Success -> {
                        require(result.value.statusCode in 200..299) {
                            "java.ajax returned HTTP ${result.value.statusCode}"
                        }
                        require(result.value.body.length <= 8 * 1024 * 1024) {
                            "java.ajax response exceeds the size limit"
                        }
                        result.value.body
                    }
                }
            },
            sourceGet = { source, baseUrl, url, headers ->
                val request = when (val created = SourceRequestFactory.create(
                    source = source,
                    url = WebUrlResolver.resolve(baseUrl, url),
                    headers = headers,
                )) {
                    is EngineResult.Failure -> throw IllegalArgumentException(created.message)
                    is EngineResult.Success -> created.value.copy(
                        timeoutMillis = 15_000L,
                        followRedirects = true,
                        retryCount = maxOf(created.value.retryCount, 1),
                    )
                }
                when (val result = runBlocking(Dispatchers.IO) { durableRequestClient.execute(request) }) {
                    is EngineResult.Failure -> throw IllegalStateException(result.message)
                    is EngineResult.Success -> result.value
                }
            },
            sourceHttpRequest = { source, baseUrl, method, url, body, headers ->
                val request = when (val created = SourceRequestFactory.create(
                    source = source,
                    url = WebUrlResolver.resolve(baseUrl, url),
                    method = method,
                    headers = headers,
                    body = body,
                )) {
                    is EngineResult.Failure -> throw IllegalArgumentException(created.message)
                    is EngineResult.Success -> created.value.copy(
                        // Pinned JsExtensions get/head/post default to 30 seconds.
                        timeoutMillis = 30_000L,
                        followRedirects = false,
                        retryCount = maxOf(created.value.retryCount, if (method.equals("GET", true) || method.equals("HEAD", true)) 1 else 0),
                    )
                }
                when (val result = runBlocking(Dispatchers.IO) { durableRequestClient.execute(request) }) {
                    is EngineResult.Failure -> throw IllegalStateException(result.message)
                    is EngineResult.Success -> result.value
                }
            },
            sourceHttpRequestNoRedirectWithTimeout = { source, baseUrl, method, url, body, headers, callTimeoutMillis ->
                require(callTimeoutMillis in 1L..Int.MAX_VALUE.toLong()) {
                    "HTTP timeout must be between 1ms and ${Int.MAX_VALUE}ms"
                }
                val request = when (val created = SourceRequestFactory.create(
                    source = source,
                    url = WebUrlResolver.resolve(baseUrl, url),
                    method = method,
                    headers = headers,
                    body = body,
                )) {
                    is EngineResult.Failure -> throw IllegalArgumentException(created.message)
                    is EngineResult.Success -> created.value.copy(
                        timeoutMillis = callTimeoutMillis,
                        followRedirects = false,
                        retryCount = maxOf(
                            created.value.retryCount,
                            if (method.equals("GET", true) || method.equals("HEAD", true)) 1 else 0,
                        ),
                    )
                }
                when (val result = runBlocking(Dispatchers.IO) { durableRequestClient.execute(request) }) {
                    is EngineResult.Failure -> throw IllegalStateException(result.message)
                    is EngineResult.Success -> result.value
                }
            },
            sourceHttpRequestWithTimeout = { source, baseUrl, method, url, body, headers, callTimeoutMillis ->
                require(callTimeoutMillis in 1L..Int.MAX_VALUE.toLong()) {
                    "connect callTimeout must be between 1ms and ${Int.MAX_VALUE}ms"
                }
                val request = when (val created = SourceRequestFactory.create(
                    source = source,
                    url = WebUrlResolver.resolve(baseUrl, url),
                    method = method,
                    headers = headers,
                    body = body,
                )) {
                    is EngineResult.Failure -> throw IllegalArgumentException(created.message)
                    is EngineResult.Success -> created.value.copy(
                        timeoutMillis = callTimeoutMillis,
                        followRedirects = true,
                        retryCount = maxOf(
                            created.value.retryCount,
                            if (method.equals("GET", true) || method.equals("HEAD", true)) 1 else 0,
                        ),
                    )
                }
                when (val result = runBlocking(Dispatchers.IO) { durableRequestClient.execute(request) }) {
                    is EngineResult.Failure -> throw IllegalStateException(result.message)
                    is EngineResult.Success -> result.value
                }
            },
        )
        val gateway = BadcatSourceHttpGatewayFactory(
            client = durableRequestClient,
            scriptRuntime = activeScriptRuntime,
        ).create()

        return SourceReadingEngine(
            catalog = catalog,
            searchUrlResolver = BookSourceSearchUrlResolver(),
            searchService = SourceSearchService(gateway, scriptRuntime = activeScriptRuntime),
            exploreService = SourceExploreService(gateway, scriptRuntime = activeScriptRuntime),
            detailService = SourceBookDetailService(gateway, scriptRuntime = activeScriptRuntime),
            tocService = SourceTocService(gateway, scriptRuntime = activeScriptRuntime),
            contentService = SourceChapterContentService(
                http = gateway,
                textReplacement = bookTextReplacement,
                scopedTextReplacement = CatalogLegadoBookTextReplacement(
                    catalog = replaceRuleCatalog,
                    fallback = bookTextReplacement,
                    jsEvaluator = RhinoLegadoJsReplacementEvaluator(),
                ),
                cache = FileChapterContentCache(context.applicationContext),
                scriptRuntime = activeScriptRuntime,
            ),
        )
    }

    /** Exposes the same durable catalog for import/export and enabled-state UI. */
    fun sourceCatalog(): FileBookSourceCatalog = catalog

    /** Exposes durable Legado replacement-rule import/export storage. */
    fun replaceRuleCatalog(): FileReplaceRuleCatalog = replaceRuleCatalog

    /** Shared Badcat WebView purification side; its rule storage stays separate from Legado. */
    fun purificationCoordinator(): BadcatPurificationCoordinator =
        BadcatPurificationCoordinator(
            settings = badcatPurificationSettings,
            adMarks = badcatAdMarkCatalog,
        )

    fun webViewPurificationAdapter(): BadcatWebViewPurificationAdapter =
        BadcatWebViewPurificationAdapter(purificationCoordinator())

}
