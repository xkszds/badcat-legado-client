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

        override fun evaluate(script: String, context: LegadoJsReplacementContext): String =
            runBlocking {
                try {
                    withTimeout(context.timeoutMillisecond.coerceAtLeast(1L)) {
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
                        context.configuredTimeoutMillisecond,
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
