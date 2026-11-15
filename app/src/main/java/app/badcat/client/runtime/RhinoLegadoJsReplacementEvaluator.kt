package app.badcat.client.runtime

import android.util.Log
import app.badcat.engine.LegadoJsReplacementContext
import app.badcat.engine.LegadoJsReplacementEvaluator
import app.badcat.engine.LegadoJsReplacementSession
import app.badcat.engine.LegadoRegexTimeoutException
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Bounded Rhino adapter for the Legado replacement @js: form.
 *
 * It exposes only the current match, immutable book/chapter snapshots and a
 * small stateful java helper. It deliberately does not expose Android Context,
 * HTTP, cookies, files, databases or WebView objects.
 */
class RhinoLegadoJsReplacementEvaluator : LegadoJsReplacementEvaluator {
    override fun createSession(ruleName: String): LegadoJsReplacementSession =
        Session(ruleName)

    private class Session(
        private val ruleName: String,
    ) : LegadoJsReplacementSession {
        private val javaExtensions = LegadoRegexJsExtensions(ruleName)

        override fun evaluate(script: String, context: LegadoJsReplacementContext): String {
            require(script.isNotBlank()) { "Legado @js replacement script must not be blank" }
            require(script.length <= MAX_SCRIPT_CHARS) { "Legado @js replacement script exceeds size limit" }
            require(ruleName.length <= MAX_RULE_NAME_CHARS) { "Legado replacement rule name exceeds size limit" }
            val values = listOf(
                context.result,
                context.chapter.url,
                context.chapter.title,
                context.chapter.bookUrl,
                context.chapter.baseUrl,
                context.book.bookUrl,
                context.book.origin,
                context.book.originName,
                context.book.name,
            )
            require(values.all { it.length <= MAX_BINDING_VALUE_CHARS }) {
                "Legado @js replacement context value exceeds size limit"
            }
            require(values.sumOf { it.length } <= MAX_TOTAL_BINDING_CHARS) {
                "Legado @js replacement context exceeds total size limit"
            }
            val timeoutMillis = context.timeoutMillisecond.coerceIn(1L, MAX_TIMEOUT_MILLIS)
            return runBlocking {
                try {
                    withTimeout(timeoutMillis) {
                        withContext(Dispatchers.Default) {
                            val bindings = ScriptBindings().apply {
                                this["result"] = context.result
                                this["chapter"] = context.chapter
                                this["book"] = context.book
                                this["java"] = javaExtensions
                            }
                            RhinoScriptEngine.eval(
                                script,
                                bindings,
                                currentCoroutineContext(),
                            ).toString()
                        }
                    }
                } catch (error: TimeoutCancellationException) {
                    throw LegadoRegexTimeoutException(
                        context.ruleId,
                        timeoutMillis,
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    throw UnsupportedOperationException(
                        "Legado @js replacement rule '" + ruleName + "' failed: " +
                            (error.message ?: error.javaClass.simpleName),
                        error,
                    )
                }
            }
        }

        private companion object {
            const val MAX_SCRIPT_CHARS = 128 * 1024
            const val MAX_RULE_NAME_CHARS = 256
            const val MAX_BINDING_VALUE_CHARS = 64 * 1024
            const val MAX_TOTAL_BINDING_CHARS = 128 * 1024
            const val MAX_TIMEOUT_MILLIS = 10_000L
        }
    }
}

/** Small, per-rule helper compatible with common Legado replacement scripts. */
class LegadoRegexJsExtensions(
    private val ruleName: String,
) {
    private val variables = ConcurrentHashMap<String, String>()

    fun log(message: Any?): Any? {
        Log.d("BadcatReplaceRule", ruleName + ": " + message)
        return message
    }

    fun logType(value: Any?) {
        log(value?.javaClass?.name ?: "null")
    }

    fun get(key: String): String = variables[key].orEmpty()

    fun put(key: String, value: String): String {
        variables[key] = value
        return value
    }

    fun md5Encode(value: String): String = digest("MD5", value)

    fun md5Encode16(value: String): String = md5Encode(value).substring(8, 24)

    fun digestHex(data: String, algorithm: String): String = digest(algorithm, data)

    fun digestBase64Str(data: String, algorithm: String): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance(algorithm).digest(data.toByteArray(Charsets.UTF_8)))

    fun HMacHex(data: String, algorithm: String, key: String): String =
        hmac(algorithm, data, key).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun HMacBase64(data: String, algorithm: String, key: String): String =
        Base64.getEncoder().encodeToString(hmac(algorithm, data, key))

    private fun hmac(algorithm: String, data: String, key: String): ByteArray =
        Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm))
            doFinal(data.toByteArray(Charsets.UTF_8))
        }

    private fun digest(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
