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
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Android host adapter for bounded source JavaScript.
 *
 * The host keeps an isolated Rhino global per source and exposes only a small
 * allow-listed java helper (string state, digest, Base64, URL encoding and source-scoped cookie reads).
 * It never exposes Java package reflection, Android Context, filesystem,
 * arbitrary network, cookie mutation, WebView or process APIs.
 */
class RhinoSourceScriptRuntime(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val sourceKeyValueStore: SourceScriptKeyValueStore = InMemorySourceScriptKeyValueStore,
    private val sourceLoginSessionStore: SourceLoginSessionStore = InMemorySourceLoginSessionStore,
    private val sourceCookieReader: (sourceId: String, url: String) -> String = { _, _ -> "" },
    private val sourceCookieRemover: (sourceId: String, url: String) -> Unit = { _, _ -> },
    private val sourceAjax: (source: BookSource, baseUrl: String, urlAndOptions: String) -> String = { _, _, _ -> throw UnsupportedOperationException("java.ajax is not connected to the source HTTP transport") },
    private val sourceGet: (source: BookSource, baseUrl: String, url: String, headers: Map<String, String>) -> SourceHttpResponse = { _, _, _, _ -> throw UnsupportedOperationException("java.get is not connected to the source HTTP transport") },
    private val sourceHttpRequest: (source: BookSource, baseUrl: String, method: String, url: String, body: String?, headers: Map<String, String>) -> SourceHttpResponse = { source, baseUrl, method, url, body, headers ->
        if (method.equals("GET", true) && body == null) sourceGet(source, baseUrl, url, headers)
        else throw UnsupportedOperationException("java.$method is not connected to the source HTTP transport")
    },
) : SourceScriptRuntime {
    private val javaExtensions = ConcurrentHashMap<String, RhinoSafeSourceJavaExtensions>()
    private val sourceExtensions = ConcurrentHashMap<String, RhinoSourceLoginExtensions>()
    private val cacheExtensions = ConcurrentHashMap<String, RhinoSourceCacheExtensions>()

    init {
        require(timeoutMillis in 1L..MAX_TIMEOUT_MILLIS)
    }

    override suspend fun evaluate(
        source: BookSource,
        script: String,
        bindings: Map<String, String>,
    ): EngineResult<String> {
        return sourceLocks.computeIfAbsent(source.id) { Mutex() }.withLock {
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
                bindings.forEach { (name, value) -> scope[name] = value }
                // Common, low-risk Legado helpers are exposed through a narrow
                // source-scoped bridge. This shadows Rhino's package namespace;
                // untrusted rules still cannot reach java.lang or Android APIs.
                var currentRuleContent = bindings["result"].orEmpty()
                val javaBridge = RhinoSafeSourceJavaExtensions(
                    sourceId = source.id,
                    store = sourceKeyValueStore,
                    cookieReader = { url -> sourceCookieReader(source.id, url) },
                    ajaxRequest = { raw ->
                        sourceAjax(
                            source,
                            bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                            raw,
                        )
                    },
                    httpGet = { url, headers ->
                        sourceGet(
                            source,
                            bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                            url,
                            headers,
                        )
                    },
                    httpRequest = { method, url, body, headers ->
                        sourceHttpRequest(
                            source,
                            bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                            method,
                            url,
                            body,
                            headers,
                        )
                    },
                    ruleStringReader = { rule ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = currentRuleContent,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> ""
                                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(rule, parsed.value).orEmpty()
                            }
                        }.getOrDefault("")
                    },
                    ruleStringReaderWithContent = { rule, content ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = content,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> ""
                                is EngineResult.Success -> LegadoRuleExpressionDispatcher.firstScalarValue(rule, parsed.value).orEmpty()
                            }
                        }.getOrDefault("")
                    },
                    ruleStringListReader = { rule ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = currentRuleContent,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> emptyList()
                                is EngineResult.Success -> parsed.value
                            }
                        }.getOrDefault(emptyList())
                    },
                    ruleStringListReaderWithContent = { rule, content ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = content,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                                is EngineResult.Failure -> emptyList()
                                is EngineResult.Success -> parsed.value
                            }
                        }.getOrDefault(emptyList())
                    },
                    ruleElementReader = { rule ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = currentRuleContent,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> null
                                is EngineResult.Success -> parsed.value.firstOrNull()?.let { LegadoSourceElement(it, document.url) }
                            }
                        }.getOrNull()
                    },
                    ruleElementReaderWithContent = { rule, content ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = content,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> null
                                is EngineResult.Success -> parsed.value.firstOrNull()?.let { LegadoSourceElement(it, document.url) }
                            }
                        }.getOrNull()
                    },
                    ruleElementsReader = { rule ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = currentRuleContent,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> LegadoSourceElementList(emptyList())
                                is EngineResult.Success -> LegadoSourceElementList(parsed.value.map { LegadoSourceElement(it, document.url) })
                            }
                        }.getOrDefault(LegadoSourceElementList(emptyList()))
                    },
                    ruleElementsReaderWithContent = { rule, content ->
                        runCatching {
                            val document = SourceDocument(
                                url = bindings["baseUrl"]?.takeIf(String::isNotBlank) ?: source.baseUrl,
                                body = content,
                            )
                            when (val parsed = LegadoRuleExpressionDispatcher.evaluateElements(document, rule)) {
                                is EngineResult.Failure -> LegadoSourceElementList(emptyList())
                                is EngineResult.Success -> LegadoSourceElementList(parsed.value.map { LegadoSourceElement(it, document.url) })
                            }
                        }.getOrDefault(LegadoSourceElementList(emptyList()))
                    },
                    contentWriter = { content -> currentRuleContent = content },
                )
                javaExtensions[source.id] = javaBridge
                scope["java"] = javaBridge
                scope["cookie"] = RhinoSourceCookieExtensions(
                    cookieReader = { url -> sourceCookieReader(source.id, url) },
                    cookieRemover = { url -> sourceCookieRemover(source.id, url) },
                )
                scope["source"] = sourceExtensions.computeIfAbsent(source.id) {
                    RhinoSourceLoginExtensions(source.id, sourceLoginSessionStore, sourceKeyValueStore, source.id)
                }
                scope["cache"] = cacheExtensions.computeIfAbsent(source.id) { RhinoSourceCacheExtensions() }
                val result = try {
                    withTimeout(timeoutMillis) {
                        // A tightly scoped compatibility shim exposes only the crypto classes
                        // required by legacy source rules; it does not restore general Packages access.
                        RhinoScriptEngine.evalSuspend(LEGADO_CRYPTO_COMPAT_JS, scope)
                        // jsLib declarations are installed once per source/library version.
                        // Re-evaluating top-level let/const declarations on every request
                        // would cause duplicate lexical declaration errors.
                        if (jsLibHash != null && sourceJsLibHashes[source.id] == null) {
                            RhinoScriptEngine.evalSuspend(jsLib, scope)
                            sourceJsLibHashes[source.id] = jsLibHash
                        }
                        // Source mainJs initializes helper functions and globals once,
                        // after jsLib and before the first rule for this source.
                        if (mainJsHash != null && sourceMainJsHashes[source.id] == null) {
                            RhinoScriptEngine.evalSuspend(mainJs, scope)
                            sourceMainJsHashes[source.id] = mainJsHash
                        }
                        RhinoScriptEngine.evalSuspend(script, scope)
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
                    else -> EngineResult.Failure(
                        EngineErrorCode.UNSUPPORTED_RULE,
                        "Source JavaScript result must be a primitive value",
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
            javaExtensions.remove(sourceId)
            sourceExtensions.remove(sourceId)
            cacheExtensions.remove(sourceId)?.clear()
            sourceKeyValueStore.clearSource(sourceId)
            sourceLoginSessionStore.clearSource(sourceId)
        }
    }

    private companion object {
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
                }
            };
            JavaImporter = function() {
              this.importPackage = function() {
                for (var i = 0; i < arguments.length; i++) {
                  var pkg = arguments[i];
                  if (pkg) {
                    for (var name in pkg) this[name] = pkg[name];
                  }
                }
              };
            };
            String.prototype.getBytes = function() { return java.utf8Encode(String(this)); };
        """.trimIndent()

        const val GLOBAL_KEY_PREFIX = "badcat-source:"
        val BINDING_NAME = Regex("[A-Za-z_$][A-Za-z0-9_$]{0,63}")
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
