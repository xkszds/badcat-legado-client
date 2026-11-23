package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceScriptRuntime
import app.badcat.engine.LegadoRuleExpressionDispatcher
import app.badcat.engine.LegadoSourceElement
import app.badcat.engine.LegadoSourceElementList
import app.badcat.engine.SourceDocument
import app.badcat.engine.SourceHttpResponse
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import com.script.rhino.RhinoContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import org.htmlunit.corejs.javascript.Context
import org.htmlunit.corejs.javascript.NativeJSON
import org.htmlunit.corejs.javascript.Scriptable
import org.htmlunit.corejs.javascript.ScriptableObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android host adapter for bounded source JavaScript.
 *
 * The host keeps an isolated Rhino global per source and exposes only a small
 * allow-listed java helper. Network calls are routed through the source
 * transport; filesystem access is limited to a canonical-path-confined cache
 * root and a restricted Rhino file-handle facade. Java package reflection,
 * Android Context, WebView and process APIs remain unavailable.
 */

/** Preserve the current AnalyzeRule keyword/page when Java selector helpers re-enter the rule parser. */
internal fun sourceRuleDocument(url: String, body: String, key: String, page: Int): SourceDocument =
    SourceDocument(url = url, body = body, key = key, page = page)

/** Bound iterable and array materialization before converting script results to HTML/JSON. */
internal fun boundedScriptResultValues(value: Any?, maxItems: Int): List<Any?>? {
    require(maxItems >= 0)
    val values = when (value) {
        // Some pinned Rhino collection facades implement both Scriptable and
        // Iterable. Preserve Legado's iterable semantics before generic JS arrays.
        is Iterable<*> -> value.take(maxItems + 1).toList()
        is Array<*> -> {
            require(value.size <= maxItems) { "Source JavaScript array result exceeds the item limit" }
            value.asList()
        }
        is org.htmlunit.corejs.javascript.Scriptable -> {
            val length = (org.htmlunit.corejs.javascript.ScriptableObject.getProperty(value, "length") as? Number)
                ?.toInt() ?: return null
            require(length in 0..maxItems) { "Source JavaScript array result exceeds the item limit" }
            (0 until length).map { index ->
                org.htmlunit.corejs.javascript.ScriptableObject.getProperty(value, index.toString())
            }
        }
        else -> return null
    }
    require(values.size <= maxItems) { "Source JavaScript array result exceeds the item limit" }
    return values
}


/** Create a non-symlink source directory directly beneath the canonical app cache root. */
internal fun ensureSourcePhysicalCacheDirectory(root: java.io.File, sourceId: String): java.io.File {
    val canonicalRoot = root.canonicalFile
    require(canonicalRoot.isDirectory || canonicalRoot.mkdirs()) {
        "Unable to create the app source cache root"
    }
    val candidate = sourcePhysicalCacheDirectory(canonicalRoot, sourceId)
    if (java.nio.file.Files.isSymbolicLink(candidate.toPath())) {
        throw SecurityException("Source cache directory must not be a symbolic link")
    }
    require(candidate.isDirectory || candidate.mkdir()) {
        "Unable to create the source cache directory"
    }
    val canonicalCandidate = candidate.canonicalFile
    require(canonicalCandidate.parentFile == canonicalRoot && canonicalCandidate.isDirectory) {
        "Source cache directory escaped the app cache root"
    }
    return canonicalCandidate
}

/** Separate the physical source file namespace from the shared app cache directory. */
internal fun sourcePhysicalCacheDirectory(root: java.io.File, sourceId: String): java.io.File {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(sourceId.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    return java.io.File(root, "legado_source_$digest")
}


/** Materialize the complete imported BookSource JSON as a JS-friendly object graph. */
private fun sourceDescriptor(source: BookSource): Map<String, Any?> {
    val values = linkedMapOf<String, Any?>()
    source.rawJson?.let { raw ->
        runCatching {
            com.google.gson.JsonParser.parseString(raw).asJsonObject.entrySet().forEach { (key, value) ->
                values[key] = value.toSourceDescriptorValue()
            }
        }
    }
    val hasImportedJson = values.isNotEmpty()
    // Imported source JSON is the authoritative complete BookSource object.
    // Only identity and enabled state are overlaid from the runtime model;
    // null/default model fields must not erase fields retained in rawJson.
    values["bookSourceUrl"] = source.id
    values["bookSourceName"] = source.name
    values["enabled"] = source.enabled
    if (!hasImportedJson) {
        values["bookSourceGroup"] = source.group
        values["bookSourceType"] = source.sourceType
        values["enabledExplore"] = source.enabledExplore
        values["enabledCookieJar"] = source.enabledCookieJar
        values["header"] = source.header
        values["jsLib"] = source.jsLib
        values["loginUrl"] = source.loginUrl
        values["loginUi"] = source.loginUi
        values["loginCheckJs"] = source.loginCheckJs
        values["coverDecodeJs"] = source.coverDecodeJs
        values["bookUrlPattern"] = source.bookUrlPattern
        values["bookSourceComment"] = source.bookSourceComment
        values["variableComment"] = source.variableComment
        values["exploreUrl"] = source.exploreUrl
        values["exploreScreen"] = source.exploreScreen
        values["lastUpdateTime"] = source.lastUpdateTime
        values["respondTime"] = source.respondTime
        values["weight"] = source.weight
        values["searchUrl"] = source.searchUrl
        values["ruleExplore"] = source.ruleExploreJson?.let(::parseSourceRuleObject)
        values["ruleSearch"] = source.ruleSearchJson?.let(::parseSourceRuleObject)
        values["ruleBookInfo"] = source.ruleBookInfoJson?.let(::parseSourceRuleObject)
        values["ruleToc"] = source.ruleTocJson?.let(::parseSourceRuleObject)
        values["ruleContent"] = source.ruleContentJson?.let(::parseSourceRuleObject)
        values["ruleReview"] = source.ruleReviewJson?.let(::parseSourceRuleObject)
        values["eventListener"] = source.eventListener
        values["customButton"] = source.customButton
        values["mainJs"] = source.mainJs
        values["concurrentRate"] = source.concurrentRate
    }
    return values
}

private fun parseSourceRuleObject(json: String): Any? =
    runCatching {
        com.google.gson.JsonParser.parseString(json).asJsonObject.toSourceDescriptorValue()
    }.getOrNull()

private fun JsonElement.toSourceDescriptorValue(): Any? = when {
    isJsonNull || this is JsonNull -> null
    isJsonObject -> asJsonObject.entrySet().associateTo(linkedMapOf()) { (key, value) ->
        key to value.toSourceDescriptorValue()
    }
    isJsonArray -> asJsonArray.map { it.toSourceDescriptorValue() }
    isJsonPrimitive && asJsonPrimitive.isBoolean -> asBoolean
    isJsonPrimitive && asJsonPrimitive.isNumber -> asNumber.toString().toLongOrNull()
        ?: asNumber.toString().toDouble()
    isJsonPrimitive -> asString
    else -> null
}

class RhinoSourceScriptRuntime(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val sourceKeyValueStore: SourceScriptKeyValueStore = InMemorySourceScriptKeyValueStore,
    private val sourceFileCacheStore: SourceScriptFileCacheStore = InMemorySourceScriptFileCacheStore,
    private val sourceFileRoot: java.io.File? = null,
    private val sourceLoginSessionStore: SourceLoginSessionStore = InMemorySourceLoginSessionStore,
    private val sourceCookieReader: (sourceId: String, url: String) -> String = { _, _ -> "" },
    private val sourceCookieRemover: (sourceId: String, url: String) -> Unit = { _, _ -> },
    private val sourceCookieWriter: (sourceId: String, url: String, cookie: String) -> Unit = { _, _, _ ->
        throw UnsupportedOperationException("cookie.setCookie is not connected to the source cookie jar")
    },
    private val sourceCookieReplacer: (sourceId: String, url: String, cookie: String) -> Unit = { _, _, _ ->
        throw UnsupportedOperationException("cookie.replaceCookie is not connected to the source cookie jar")
    },
    private val sourceAjax: (source: BookSource, baseUrl: String, urlAndOptions: String) -> String = { _, _, _ -> throw UnsupportedOperationException("java.ajax is not connected to the source HTTP transport") },
    private val sourceGet: (source: BookSource, baseUrl: String, url: String, headers: Map<String, String>) -> SourceHttpResponse = { _, _, _, _ -> throw UnsupportedOperationException("java.get is not connected to the source HTTP transport") },
    private val sourceHttpRequest: (source: BookSource, baseUrl: String, method: String, url: String, body: String?, headers: Map<String, String>) -> SourceHttpResponse = { source, baseUrl, method, url, body, headers ->
        if (method.equals("GET", true) && body == null) sourceGet(source, baseUrl, url, headers)
        else throw UnsupportedOperationException("java.$method is not connected to the source HTTP transport")
    },
    private val sourceHttpRequestWithTimeout: (source: BookSource, baseUrl: String, method: String, url: String, body: String?, headers: Map<String, String>, callTimeoutMillis: Long) -> SourceHttpResponse = { source, baseUrl, method, url, body, headers, timeout ->
        require(timeout > 0) { "connect callTimeout must be positive" }
        throw UnsupportedOperationException("Per-call timeout transport is not connected (requested ${timeout}ms)")
    },
    private val sourceHttpRequestNoRedirectWithTimeout: (source: BookSource, baseUrl: String, method: String, url: String, body: String?, headers: Map<String, String>, callTimeoutMillis: Long) -> SourceHttpResponse = { source, baseUrl, method, url, body, headers, timeout ->
        require(timeout > 0) { "HTTP timeout must be positive" }
        throw UnsupportedOperationException("Per-call no-redirect timeout transport is not connected (requested ${timeout}ms)")
    },
    private val webViewUserAgent: String = RhinoSafeSourceJavaExtensions.DEFAULT_WEBVIEW_USER_AGENT,
    private val androidIdProvider: () -> String = { throw UnsupportedOperationException("Android ID provider is not connected") },
    private val toastPresenter: (String, Boolean) -> Unit = { _, _ -> throw UnsupportedOperationException("Toast presenter is not connected") },
    private val sourceRateLimit: (BookSource, Boolean) -> Unit = { source, skip -> LegadoConcurrentRateLimiter.acquire(source.id, source.concurrentRate, skip) },
    private val sourceLoginCookieWriter: (sourceId: String, sourceKey: String, cookie: String) -> Unit = { _, _, _ ->
        throw UnsupportedOperationException("source login Cookie is not connected to the source cookie jar")
    },
    private val sourceLoginCookieRemover: (sourceId: String, sourceKey: String) -> Unit = { _, _ -> },
    private val sourceCookieClearer: (sourceId: String) -> Unit = { _ -> },
    private val sourceCookieKeyRemover: (sourceId: String, url: String, key: String) -> Unit = { _, _, _ ->
        throw UnsupportedOperationException("cookie.removeCookie(url, key) is not connected to the source cookie jar")
    },
) : SourceScriptRuntime {
    private val javaExtensions = ConcurrentHashMap<String, RhinoSafeSourceJavaExtensions>()
    private val sourceExtensions = ConcurrentHashMap<String, RhinoSourceLoginExtensions>()
    private val cacheExtensions = ConcurrentHashMap<String, RhinoSourceCacheExtensions>()

    init {
        require(timeoutMillis in 1L..MAX_TIMEOUT_MILLIS)
        synchronized(runtimeInstances) { runtimeInstances.add(this) }
    }

    override suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String>,
    ): EngineResult<String> = withContext(Dispatchers.IO) {
        sourceLocks.computeIfAbsent(source.id) { Mutex() }.withLock {
            try {
                require(script.isNotBlank()) { "Source JavaScript must not be blank" }
                require(script.length <= MAX_SCRIPT_CHARS) { "Source JavaScript exceeds the size limit" }
                require(bindings.size <= MAX_BINDINGS) { "Too many source JavaScript bindings" }
                require(bindings.keys.all(BINDING_NAME::matches)) { "Invalid source JavaScript binding name" }
                require(bindings.values.all { it.length <= MAX_BINDING_VALUE_CHARS }) {
                    "Source JavaScript binding exceeds the size limit"
                }
                require(bindings.values.sumOf { it.length } <= MAX_TOTAL_BINDING_CHARS) {
                    "Source JavaScript bindings exceed the total size limit"
                }

                val jsLib = source.jsLib.orEmpty()
                val mainJs = source.mainJs.orEmpty()
                require(jsLib.length <= MAX_SCRIPT_CHARS) { "Source JavaScript library exceeds the size limit" }
                require(mainJs.length <= MAX_SCRIPT_CHARS) { "Source mainJs exceeds the size limit" }
                require(script.length + jsLib.length + mainJs.length <= MAX_TOTAL_SCRIPT_CHARS) {
                    "Source JavaScript, library and mainJs exceed the combined size limit"
                }
                val jsLibHash = jsLib.takeIf(String::isNotBlank)?.let(::scriptHash)
                val mainJsHash = mainJs.takeIf(String::isNotBlank)?.let(::scriptHash)
                val previousJsLibHash = sourceJsLibHashes[source.id]
                val previousMainJsHash = sourceMainJsHashes[source.id]
                if ((previousJsLibHash != null && previousJsLibHash != jsLibHash) ||
                    (previousMainJsHash != null && previousMainJsHash != mainJsHash)
                ) {
                    // A source may be edited in place. Reset its old global scope so
                    // top-level let/const declarations from the previous library do not collide.
                    sourceScopes.remove(source.id)
                    sourceGlobals.remove(source.id)
                    ScriptBindings.removeSharedGlobalStates(GLOBAL_KEY_PREFIX + source.id)
                    sourceJsLibHashes.remove(source.id)
                    sourceMainJsHashes.remove(source.id)
                }

                // Keep one VarScope per source. A shared globalThis alone is not
                // sufficient: Rhino stores top-level let/const declarations in the
                // lexical environment attached to the VarScope, not on its prototype.
                val scope = sourceScopes.computeIfAbsent(source.id) {
                    ScriptBindings().apply {
                        chainTo(
                            RhinoScriptEngine.newStandardTopLevel(),
                            sourceGlobals.computeIfAbsent(source.id) {
                                ScriptBindings.getSharedGlobalStateHandle(GLOBAL_KEY_PREFIX + it)
                            },
                        )
                    }
                }
                bindings.forEach { (name, value) ->
                    // Legado AnalyzeUrl binds page as an integer, not a string.
                    // Keep arithmetic such as page + 1 numeric in embedded URL scripts.
                    scope[name] = if (name == "page") value.toIntOrNull() ?: value else value
                }
                // Common, low-risk Legado helpers are exposed through a narrow
                // source-scoped bridge. This shadows Rhino's package namespace;
                // untrusted rules still cannot reach java.lang or Android APIs.
                var currentRuleContent = bindings["result"].orEmpty()
                var currentRuleBaseUrl = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl
                val currentRuleKey = bindings["key"].orEmpty()
                val currentRulePage = bindings["page"]?.toIntOrNull() ?: 1
                val javaBridge = RhinoSafeSourceJavaExtensions(
                    sourceId = source.id,
                    sourceTag = source.name,
                    sourceDescriptor = sourceDescriptor(source),
                    rateLimit = { skip -> sourceRateLimit(source, skip) },
                    store = sourceKeyValueStore,
                    androidIdProvider = androidIdProvider,
                    toastPresenter = toastPresenter,
                    cookieReader = { url -> sourceCookieReader(source.id, url) },
                    ajaxRequest = { raw ->
                        sourceAjax(
                            source,
                            currentRuleBaseUrl,
                            raw,
                        )
                    },
                    httpGet = { url, headers ->
                        sourceGet(
                            source,
                            currentRuleBaseUrl,
                            url,
                            headers,
                        )
                    },
                    httpRequest = { method, url, body, headers ->
                        sourceHttpRequest(
                            source,
                            currentRuleBaseUrl,
                            method,
                            url,
                            body,
                            headers,
                        )
                    },
                    httpRequestWithTimeout = { method, url, body, headers, timeout ->
                        if (timeout == null) {
                            require(method.equals("GET", true) && body == null) {
                                "connect without an explicit timeout supports GET only"
                            }
                            sourceGet(source, currentRuleBaseUrl, url, headers)
                        } else {
                            sourceHttpRequestWithTimeout(source, currentRuleBaseUrl, method, url, body, headers, timeout)
                        }
                    },
                    httpRequestNoRedirectWithTimeout = { method, url, body, headers, timeout ->
                        if (timeout == null) {
                            sourceHttpRequest(source, currentRuleBaseUrl, method, url, body, headers)
                        } else {
                            sourceHttpRequestNoRedirectWithTimeout(
                                source, currentRuleBaseUrl, method, url, body, headers, timeout
                            )
                        }
                    },
                    ruleStringReader = { rule ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = currentRuleContent,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> ""
                                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(rule, parsed.value).orEmpty()
                            }
                        }.getOrDefault("")
                    },
                    ruleStringReaderWithContent = { rule, content ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = content,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> ""
                                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(rule, parsed.value).orEmpty()
                            }
                        }.getOrDefault("")
                    },
                    ruleStringListReader = { rule ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = currentRuleContent,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> emptyList()
                                is EngineResult.Success -> parsed.value
                            }
                        }.getOrDefault(emptyList())
                    },
                    ruleStringListReaderWithContent = { rule, content ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = content,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> emptyList()
                                is EngineResult.Success -> parsed.value
                            }
                        }.getOrDefault(emptyList())
                    },
                    ruleElementReader = { rule ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = currentRuleContent,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> null
                                is EngineResult.Success -> LegadoSourceElement.fromSelection(parsed.value, document.url).firstOrNull()
                            }
                        }.getOrNull()
                    },
                    ruleElementReaderWithContent = { rule, content ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = content,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> null
                                is EngineResult.Success -> LegadoSourceElement.fromSelection(parsed.value, document.url).firstOrNull()
                            }
                        }.getOrNull()
                    },
                    ruleElementsReader = { rule ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = currentRuleContent,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> LegadoSourceElementList(emptyList())
                                is EngineResult.Success -> LegadoSourceElementList(LegadoSourceElement.fromSelection(parsed.value, document.url))
                            }
                        }.getOrDefault(LegadoSourceElementList(emptyList()))
                    },
                    ruleElementsReaderWithContent = { rule, content ->
                        runCatching {
                            val document = sourceRuleDocument(
                                url = currentRuleBaseUrl,
                                body = content,
                                key = currentRuleKey,
                                page = currentRulePage,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> LegadoSourceElementList(emptyList())
                                is EngineResult.Success -> LegadoSourceElementList(LegadoSourceElement.fromSelection(parsed.value, document.url))
                            }
                        }.getOrDefault(LegadoSourceElementList(emptyList()))
                    },
                    contentWriter = { content -> currentRuleContent = content },
                    baseUrlWriter = { baseUrl -> if (!baseUrl.isNullOrBlank()) currentRuleBaseUrl = baseUrl },
                    fileStore = sourceFileCacheStore,
                    fileRoot = sourceFileRoot?.let { ensureSourcePhysicalCacheDirectory(it, source.id) },
                    webViewUserAgent = webViewUserAgent,
                    functionInvoker = { action -> RhinoScriptEngine.callSourceFunction(action, scope) },
                )
                javaExtensions[source.id] = javaBridge
                scope["java"] = javaBridge
                scope["cookie"] = RhinoSourceCookieExtensions(
                    cookieReader = { url -> sourceCookieReader(source.id, url) },
                    cookieRemover = { url -> sourceCookieRemover(source.id, url) },
                    cookieWriter = { url, value -> sourceCookieWriter(source.id, url, value) },
                    cookieReplacer = { url, value -> sourceCookieReplacer(source.id, url, value) },
                    cookieKeyRemover = { url, key -> sourceCookieKeyRemover(source.id, url, key) },
                )
                scope["source"] = sourceExtensions.computeIfAbsent(source.id) {
                    RhinoSourceLoginExtensions(
                        source.id,
                        sourceLoginSessionStore,
                        sourceKeyValueStore,
                        source.id,
                        sourceLoginCookieWriter,
                        sourceLoginCookieRemover,
                    )
                }
                scope["cache"] = cacheExtensions.computeIfAbsent(source.id) { RhinoSourceCacheExtensions(source.id, sourceKeyValueStore, sourceFileCacheStore) }
                val result = try {
                    withTimeout(timeoutMillis) {
                        // A tightly scoped compatibility shim exposes only the crypto classes
                        // required by legacy source rules; it does not restore general Packages access.
                        RhinoScriptEngine.evalSuspend(LEGADO_CRYPTO_COMPAT_JS, scope)
                        // jsLib declarations are installed once per source/library version.
                        // Re-evaluating top-level let/const declarations on every request
                        // would cause duplicate lexical declaration errors.
                        if (jsLibHash != null && sourceJsLibHashes[source.id] == null) {
                            RhinoScriptEngine.evalSuspend(RhinoRegexCompatibility.normalize(jsLib), scope)
                            sourceJsLibHashes[source.id] = jsLibHash
                        }
                        // Source mainJs initializes helper functions and globals once,
                        // after jsLib and before the first rule for this source.
                        if (mainJsHash != null && sourceMainJsHashes[source.id] == null) {
                            RhinoScriptEngine.evalSuspend(RhinoRegexCompatibility.normalize(mainJs), scope)
                            sourceMainJsHashes[source.id] = mainJsHash
                        }
                        val evaluated = RhinoScriptEngine.evalSuspend(
                            RhinoRegexCompatibility.normalize(script),
                            scope,
                        )
                        if (evaluated is Scriptable && evaluated.className != "Array") {
                            stringifyScriptableResult(evaluated)
                                ?: scriptableObjectToJson(evaluated, 0).toString()
                        } else {
                            evaluated
                        }
                    }
                } finally {
                    // Request bindings must not leak into the persistent source scope.
                    bindings.keys.forEach(scope::delete)
                }
                when (result) {
                    null -> EngineResult.Success("")
                    is String -> EngineResult.Success(result)
                    is CharSequence -> EngineResult.Success(result.toString())
                    is Number, is Boolean -> EngineResult.Success(result.toString())
                    else -> arrayResultToHtml(result)?.let { EngineResult.Success(it) }
                        ?: EngineResult.Failure(
                            EngineErrorCode.UNSUPPORTED_RULE,
                            "Source JavaScript result must be a primitive value or a bounded array",
                        )
                }
            } catch (error: TimeoutCancellationException) {
                EngineResult.Failure(
                    EngineErrorCode.RULE_TIMEOUT,
                    "Source JavaScript exceeded the execution time limit",
                    error,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalArgumentException) {
                EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    error.message ?: "Unsupported JavaScript input or result",
                    error,
                )
            } catch (error: Exception) {
                EngineResult.Failure(
                    EngineErrorCode.PARSE,
                    error.message ?: "Source JavaScript evaluation failed",
                    error,
                )
            }
        }
    }

    /**
     * Match pinned JsSourceEngine.normalizeJsResult for root Scriptable objects.
     * Keep JSON.stringify inside the same source lock and timeout as the script
     * evaluation, because source-defined toJSON/getters may execute JavaScript.
     */
    private suspend fun stringifyScriptableResult(value: Scriptable): String? {
        val topScope = value.parentScope?.let { ScriptableObject.getTopLevelScope(it) }
            ?: return null
        val cx = Context.enter() as RhinoContext
        val previousCoroutineContext = cx.coroutineContext
        val previousAllowScriptRun = cx.allowScriptRun
        cx.coroutineContext = currentCoroutineContext()
        cx.allowScriptRun = true
        cx.recursiveCount++
        try {
            cx.checkRecursive()
            val raw = try {
                NativeJSON.stringify(cx, topScope, value, null, null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                throw IllegalArgumentException(
                    "JavaScript object result JSON.stringify failed: ${error.message}",
                    error,
                )
            }
            return RhinoScriptEngine.unwrapReturnValue(raw) as? String
        } finally {
            cx.recursiveCount--
            cx.allowScriptRun = previousAllowScriptRun
            cx.coroutineContext = previousCoroutineContext
            Context.exit()
        }
    }

    override suspend fun getString(source: BookSource, key: String): EngineResult<String?> {
        if (key.isBlank() || key.length > MAX_VARIABLE_KEY_CHARS) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Source variable key is blank or too long")
        }
        return sourceLocks.computeIfAbsent(source.id) { Mutex() }.withLock {
            EngineResult.Success(sourceKeyValueStore.get(source.id, key))
        }
    }

    override suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit> {
        if (key.isBlank() || key.length > MAX_VARIABLE_KEY_CHARS || value.length > MAX_VARIABLE_VALUE_CHARS) {
            return EngineResult.Failure(EngineErrorCode.INVALID_SOURCE, "Source variable key or value exceeds the configured limit")
        }
        return sourceLocks.computeIfAbsent(source.id) { Mutex() }.withLock {
            sourceKeyValueStore.put(source.id, key, value)
            EngineResult.Success(Unit)
        }
    }

    /**
     * Drop source-level globals after a source is replaced or removed.
     *
     * This uses the same per-source mutex as evaluate(), so a source cannot
     * recreate or mutate its global scope while that scope is being cleared.
     */
    suspend fun clearSourceState(sourceId: String) {
        if (sourceId.isBlank()) return
        sourceLocks.computeIfAbsent(sourceId) { Mutex() }.withLock {
            sourceScopes.remove(sourceId)
            sourceGlobals.remove(sourceId)
            sourceJsLibHashes.remove(sourceId)
            sourceMainJsHashes.remove(sourceId)
            ScriptBindings.removeSharedGlobalStates(GLOBAL_KEY_PREFIX + sourceId)

            // A source can be evaluated by several independently-created engine
            // factories. Clear every live runtime's instance-owned bridge/cache
            // state while holding the same process-wide source mutex.
            val failures = mutableListOf<Exception>()
            liveRuntimeInstances().forEach { runtime ->
                fun clear(action: () -> Unit) {
                    try {
                        action()
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        failures += error
                    }
                }
                clear { runtime.javaExtensions.remove(sourceId) }
                clear { runtime.sourceExtensions.remove(sourceId) }
                clear { runtime.cacheExtensions.remove(sourceId)?.clear() }
                clear { runtime.sourceKeyValueStore.clearSource(sourceId) }
                clear { runtime.sourceFileCacheStore.clearSource(sourceId) }
                clear { runtime.clearPhysicalSourceCache(sourceId) }
                clear { runtime.sourceLoginSessionStore.clearSource(sourceId) }
                clear { runtime.sourceCookieClearer(sourceId) }
            }
            if (failures.isNotEmpty()) {
                throw IllegalStateException(
                    "Unable to fully clear runtime state for source '$sourceId' (${failures.size} cleanup failure(s))",
                    failures.first(),
                ).also { aggregate ->
                    failures.drop(1).forEach(aggregate::addSuppressed)
                }
            }
        }
    }

    private fun clearPhysicalSourceCache(sourceId: String) {
        val root = sourceFileRoot ?: return
        val target = sourcePhysicalCacheDirectory(root, sourceId)
        runCatching {
            if (java.nio.file.Files.isSymbolicLink(target.toPath())) {
                java.nio.file.Files.deleteIfExists(target.toPath())
            } else {
                val canonicalRoot = root.canonicalFile
                val canonicalTarget = target.canonicalFile
                if (canonicalTarget.parentFile == canonicalRoot) target.deleteRecursively()
            }
        }
    }

    private companion object {
        fun arrayResultToHtml(value: Any?): String? {
            val values = boundedScriptResultValues(value, MAX_SCRIPT_RESULT_ITEMS) ?: return null
            // Legado TOC rules commonly return [{title, url}, ...] from
            // @js chapterList scripts. Preserve those objects as JSON so the
            // downstream JSONPath field rules can read title/url; calling
            // NativeObject.toString() here would collapse every row to
            // "[object Object]" and silently produce an empty chapter list.
            if (values.isNotEmpty() && values.all { item ->
                    item is org.htmlunit.corejs.javascript.Scriptable &&
                        (org.htmlunit.corejs.javascript.ScriptableObject.getProperty(item, "length") as? Number) == null
                }
            ) {
                val array = JsonArray()
                values.forEach { item -> array.add(scriptableObjectToJson(item as org.htmlunit.corejs.javascript.Scriptable, 0)) }
                return array.toString()
            }
            return values.joinToString("\n") { item ->
                when (item) {
                    null -> ""
                    is String, is CharSequence, is Number, is Boolean -> item.toString()
                    else -> item.toString()
                }
            }
        }

        private fun scriptableObjectToJson(
            value: org.htmlunit.corejs.javascript.Scriptable,
            depth: Int,
        ): JsonObject {
            require(depth <= MAX_SCRIPT_RESULT_JSON_DEPTH) { "Source JavaScript object result is too deeply nested" }
            val ids = value.ids
            require(ids.size <= MAX_SCRIPT_RESULT_OBJECT_PROPERTIES) {
                "Source JavaScript object result has too many properties"
            }
            return JsonObject().apply {
                ids.forEach { rawKey ->
                    val key = rawKey.toString()
                    val item = org.htmlunit.corejs.javascript.ScriptableObject.getProperty(value, key)
                    add(key, scriptableValueToJson(item, depth + 1))
                }
            }
        }

        private fun scriptableValueToJson(value: Any?, depth: Int): JsonElement {
            require(depth <= MAX_SCRIPT_RESULT_JSON_DEPTH) { "Source JavaScript object result is too deeply nested" }
            return when (value) {
                null -> JsonNull.INSTANCE
                is String, is CharSequence -> JsonPrimitive(value.toString())
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is org.htmlunit.corejs.javascript.Scriptable -> {
                    val length = (org.htmlunit.corejs.javascript.ScriptableObject.getProperty(value, "length") as? Number)?.toInt()
                    if (length != null) {
                        require(length in 0..MAX_SCRIPT_RESULT_ITEMS) { "Source JavaScript array property exceeds the item limit" }
                        JsonArray().apply {
                            for (index in 0 until length) {
                                add(scriptableValueToJson(
                                    org.htmlunit.corejs.javascript.ScriptableObject.getProperty(value, index.toString()),
                                    depth + 1,
                                ))
                            }
                        }
                    } else {
                        scriptableObjectToJson(value, depth + 1)
                    }
                }
                else -> JsonPrimitive(value.toString())
            }
        }

        const val MAX_SCRIPT_RESULT_ITEMS = 1_000
        const val MAX_SCRIPT_RESULT_OBJECT_PROPERTIES = 256
        const val MAX_SCRIPT_RESULT_JSON_DEPTH = 8
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
        const val MAX_TIMEOUT_MILLIS = 30_000L
        const val MAX_SCRIPT_CHARS = 128 * 1024
        const val MAX_BINDINGS = 32
        const val MAX_BINDING_VALUE_CHARS = 64 * 1024
        const val MAX_TOTAL_BINDING_CHARS = 128 * 1024
        const val MAX_TOTAL_SCRIPT_CHARS = 256 * 1024
        const val MAX_VARIABLE_KEY_CHARS = 256
        const val MAX_VARIABLE_VALUE_CHARS = 16 * 1024
        private val LEGADO_CRYPTO_COMPAT_JS = """
            Packages = {
              java: {
                lang: {},
                util: {
                  Base64: {
                    getDecoder: function() {
                      return { decode: function(value) { return java.base64DecodeBytes(String(value)); } };
                    },
                    getEncoder: function() {
                      return { encodeToString: function(value) { return java.base64Encode(java.utf8Decode(value)); } };
                    }
                  },
                  Arrays: {
                    copyOfRange: function(value, from, to) { return java.copyByteRange(value, from, to); }
                  }
                }
              },
              javax: {
                  crypto: {
                    spec: {
                      SecretKeySpec: function(bytes, algorithm) {
                        if (String(algorithm) !== 'AES') throw new Error('Only AES SecretKeySpec is supported');
                        this.bytes = bytes;
                        this.algorithm = 'AES';
                      },
                      IvParameterSpec: function(bytes) {
                        this.bytes = bytes;
                      }
                    },
                    Cipher: {
                      ENCRYPT_MODE: 1,
                      DECRYPT_MODE: 2,
                      getInstance: function(transformation) {
                        if (String(transformation) !== 'AES/CBC/PKCS5Padding') {
                          throw new Error('Only AES/CBC/PKCS5Padding is supported');
                        }
                        return {
                          mode: 0,
                          key: null,
                          iv: null,
                          init: function(mode, key, iv) {
                            if (mode !== 1 && mode !== 2) throw new Error('Unsupported AES operation');
                            if (!key || key.algorithm !== 'AES' || !iv) throw new Error('Invalid AES key or IV');
                            this.mode = mode;
                            this.key = key;
                            this.iv = iv;
                          },
                          doFinal: function(data) {
                            if (!this.key || !this.iv) throw new Error('AES cipher is not initialized');
                            var output = java.aesCrypt(data, this.key.bytes, this.iv.bytes, this.mode);
                            return { toString: function() { return java.utf8Decode(output); } };
                          }
                        };
                      }
                    }
                  }
                },
              org: {
                jsoup: {
                  Jsoup: {
                    parse: function(html, baseUri) {
                      return arguments.length > 1
                        ? java.parseHtml(String(html), String(baseUri))
                        : java.parseHtml(String(html));
                    }
                  }
                }
              }
            };
            // Narrow compatibility surface for public Legado sources that call
            // org.jsoup.Jsoup.parse(result). Parsing is delegated to the pinned
            // JSoup dependency; arbitrary Java package access remains blocked.
            org = Packages.org;
            JavaImporter = function() {
              this.importPackage = function() {
                for (var i = 0; i < arguments.length; i++) {
                  var pkg = arguments[i];
                  if (pkg) {
                    for (var name in pkg) this[name] = pkg[name];
                  }
                }
              };
              // Match JavaImporter(Packages.foo) constructor semantics as well
              // as its later importPackage(...) method.
              this.importPackage.apply(this, arguments);
            };
            String.prototype.getBytes = function() { return java.utf8Encode(String(this)); };
        """.trimIndent()

        const val GLOBAL_KEY_PREFIX = "badcat-source:"
        val BINDING_NAME = Regex("[A-Za-z_$][A-Za-z0-9_$]{0,63}")
        private val runtimeInstances = Collections.synchronizedSet(
            Collections.newSetFromMap(WeakHashMap<RhinoSourceScriptRuntime, Boolean>()),
        )

        private fun liveRuntimeInstances(): List<RhinoSourceScriptRuntime> =
            synchronized(runtimeInstances) { runtimeInstances.toList() }

        val sourceScopes = ConcurrentHashMap<String, ScriptBindings>()
        val sourceGlobals = ConcurrentHashMap<String, com.script.SharedGlobalStateHandle>()
        val sourceLocks = ConcurrentHashMap<String, Mutex>()
        val sourceJsLibHashes = ConcurrentHashMap<String, String>()
        val sourceMainJsHashes = ConcurrentHashMap<String, String>()

        fun scriptHash(script: String): String =
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(script.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte -> "%02x".format(byte) }
    }
}
