package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineErrorCode
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceScriptRuntime
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
 * allow-listed java helper (string state, digest, Base64 and URL encoding).
 * It never exposes Java package reflection, Android Context, filesystem,
 * arbitrary network, cookies, WebView or process APIs.
 */
class RhinoSourceScriptRuntime(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val sourceKeyValueStore: SourceScriptKeyValueStore = InMemorySourceScriptKeyValueStore,
    private val sourceLoginSessionStore: SourceLoginSessionStore = InMemorySourceLoginSessionStore,
) : SourceScriptRuntime {
    private val javaExtensions = ConcurrentHashMap<String, RhinoSafeSourceJavaExtensions>()
    private val sourceExtensions = ConcurrentHashMap<String, RhinoSourceLoginExtensions>()

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
                scope["java"] = javaExtensions.computeIfAbsent(source.id) {
                    RhinoSafeSourceJavaExtensions(source.id, sourceKeyValueStore)
                }
                scope["source"] = sourceExtensions.computeIfAbsent(source.id) {
                    RhinoSourceLoginExtensions(source.id, sourceLoginSessionStore)
                }
                val result = try {
                    withTimeout(timeoutMillis) {
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
            sourceKeyValueStore.clearSource(sourceId)
            sourceLoginSessionStore.clearSource(sourceId)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 1_500L
        const val MAX_TIMEOUT_MILLIS = 10_000L
        const val MAX_SCRIPT_CHARS = 128 * 1024
        const val MAX_BINDINGS = 32
        const val MAX_BINDING_VALUE_CHARS = 64 * 1024
        const val MAX_TOTAL_BINDING_CHARS = 128 * 1024
        const val MAX_TOTAL_SCRIPT_CHARS = 256 * 1024
        const val MAX_VARIABLE_KEY_CHARS = 256
        const val MAX_VARIABLE_VALUE_CHARS = 16 * 1024
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
