package app.badcat.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl
import okhttp3.Response
import okhttp3.Interceptor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.EOFException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Concrete asynchronous HTTP transport for imported book sources.
 *
 * Redirects are followed by OkHttp. Cookies are isolated by source ID and can
 * be disabled per request. This implements static headers and HTTP only; it
 * does not evaluate JavaScript URL expressions or implement WebView/DNS overrides.
 */
class OkHttpSourceRequestClient(
    private val connectTimeoutSeconds: Long = 20,
    private val readTimeoutSeconds: Long = 45,
    private val writeTimeoutSeconds: Long = 30,
    private val maxResponseBytes: Long = 8L * 1024 * 1024,
    /** Optional host-owned cookie persistence; jars remain isolated by source ID. */
    private val cookieJarProvider: ((String) -> CookieJar)? = null,
) : SourceRequestClient {

    private val cookieJars = ConcurrentHashMap<String, MemorySourceCookieJar>()
    private val clients = ConcurrentHashMap<String, OkHttpClient>()

    override suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse> {
        if (request.sourceId.isBlank() || request.url.isBlank() ||
            request.retryCount !in 0..5 ||
            request.timeoutMillis?.let { it <= 0 || it > Int.MAX_VALUE } == true
        ) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source identity and request URL must not be blank",
            )
        }
        val requestStartNanos = System.nanoTime()
        return try {
            val client = clientFor(request)
            val builder = Request.Builder().url(request.url)
            request.headers.forEach { (name, value) -> builder.header(name, value) }

            val method = request.method.uppercase()
            val requestBody = request.body?.let { body ->
                val explicitContentType = request.headers.entries
                    .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
                    ?.value
                val inferredContentType = when {
                    body.trimStart().startsWith("{") || body.trimStart().startsWith("[") ->
                        "application/json; charset=utf-8"
                    body.trimStart().startsWith("<?xml", ignoreCase = true) ->
                        "application/xml; charset=utf-8"
                    else -> "application/x-www-form-urlencoded; charset=utf-8"
                }
                val contentType = (explicitContentType ?: inferredContentType).toMediaTypeOrNull()
                body.toRequestBody(contentType)
            }
            val bodyForMethod = when {
                method in BODY_REQUIRED_METHODS -> requestBody
                    ?: ByteArray(0).toRequestBody(null)
                method in BODY_FORBIDDEN_METHODS -> null
                else -> requestBody
            }
            builder.method(method, bodyForMethod)

            var attempt = 0
            while (true) {
                try {
                    val response = awaitResponse(client.newCall(builder.build()))
                    val parsed = response.use { readResponse(it, request.responseCharset) }
                    if (attempt < request.retryCount &&
                        shouldRetryHttpResponse(method, parsed.statusCode)
                    ) {
                        attempt++
                        // Retry only idempotent reads on transient server/rate-limit
                        // responses. Never replay arbitrary POST/PUT source actions.
                        delay((250L shl (attempt - 1).coerceAtMost(3)).coerceAtMost(2_000L))
                        continue
                    }
                    val elapsedMillis = ((System.nanoTime() - requestStartNanos) / 1_000_000L)
                        .coerceAtLeast(0L)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt()
                    return EngineResult.Success(parsed.copy(callTime = elapsedMillis))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (io: IOException) {
                    if (attempt >= request.retryCount || !shouldRetryIOException(method, io)) throw io
                    attempt++
                    delay((250L shl (attempt - 1).coerceAtMost(3)).coerceAtMost(2_000L))
                }
            }
            @Suppress("UNREACHABLE_CODE")
            EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "HTTP retry loop ended unexpectedly",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (invalid: IllegalArgumentException) {
            EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Invalid request for source '${request.sourceId}': ${invalid.message}",
                invalid,
            )
        } catch (failure: Exception) {
            EngineResult.Failure(
                EngineErrorCode.NETWORK,
                "HTTP request failed for source '${request.sourceId}': ${failure.message}",
                failure,
            )
        }
    }

    internal fun clientFor(request: SourceHttpRequest): OkHttpClient {
        val key = request.sourceId + if (request.useCookieJar) "|cookies" else "|no-cookies"
        val baseClient = clients.computeIfAbsent(key) {
            val jar = if (request.useCookieJar) {
                cookieJarProvider?.invoke(request.sourceId)
                    ?: cookieJars.computeIfAbsent(request.sourceId) { MemorySourceCookieJar() }
            } else {
                NoSourceCookies
            }
            OkHttpClient.Builder()
                .cookieJar(jar)
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                // Bound the complete request, including DNS, redirects and body consumption.
                .callTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                // Never downgrade an HTTPS source request to cleartext HTTP.
                .followSslRedirects(false)
                .addNetworkInterceptor(SafeRedirectInterceptor)
                .build()
        }
        if (request.timeoutMillis == null && request.followRedirects == null) return baseClient
        return baseClient.newBuilder().apply {
            request.timeoutMillis?.let { timeout ->
                // Legado's per-request timeout must cover connection setup,
                // request writes, response reads and the complete redirected
                // call. Applying it only to reads allowed slow connects and
                // redirect chains to exceed the source's configured limit.
                connectTimeout(timeout, TimeUnit.MILLISECONDS)
                readTimeout(timeout, TimeUnit.MILLISECONDS)
                writeTimeout(timeout, TimeUnit.MILLISECONDS)
                callTimeout(timeout, TimeUnit.MILLISECONDS)
            }
            request.followRedirects?.let {
                followRedirects(it)
                // The source option controls redirects, but not HTTPS downgrade safety.
                followSslRedirects(false)
            }
        }.build()
    }

    private suspend fun awaitResponse(call: okhttp3.Call): Response =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            })
        }

    private fun readResponse(response: Response, responseCharset: String?): SourceHttpResponse {
        val responseBody = response.body
        val bytes = responseBody?.byteStream()?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxResponseBytes) {
                    throw IOException("Response exceeds configured ${maxResponseBytes}-byte limit")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: ByteArray(0)
        val charset: Charset = responseCharset
            ?.takeUnless { it.equals("escape", ignoreCase = true) }
            ?.let { Charset.forName(it) }
            ?: responseBody?.contentType()?.charset(StandardCharsets.UTF_8)
            ?: StandardCharsets.UTF_8
        val headerMap = response.headers.names().associateWith { name ->
            response.headers.values(name)
        }
        return SourceHttpResponse(
            statusCode = response.code,
            finalUrl = response.request.url.toString(),
            headers = headerMap,
            body = String(bytes, charset),
            contentType = responseBody?.contentType()?.toString(),
            statusMessage = response.message,
            bodyBytes = bytes,
        )
    }

    companion object {
        private val BODY_REQUIRED_METHODS = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
        private val BODY_FORBIDDEN_METHODS = setOf("GET", "HEAD")
    }
}

/** Retry transient connection, read and DNS failures for idempotent reads, but never TLS validation failures. */
internal fun shouldRetryIOException(method: String, error: IOException): Boolean {
    if (!method.equals("GET", ignoreCase = true) && !method.equals("HEAD", ignoreCase = true)) return false
    if (error is SSLException) return false
    return generateSequence(error as Throwable?) { it.cause }.any {
        it is UnknownHostException || it is SocketTimeoutException || it is SocketException || it is EOFException || it is ProtocolException
    }
}

/** Retry only transient HTTP failures for idempotent source reads. */
internal fun shouldRetryHttpResponse(method: String, statusCode: Int): Boolean {
    val idempotentRead = method.equals("GET", ignoreCase = true) ||
        method.equals("HEAD", ignoreCase = true)
    return idempotentRead && statusCode in setOf(408, 425, 429, 500, 502, 503, 504)
}

/** In-memory cookie storage, scoped to one source by the owning client. */
internal class MemorySourceCookieJar : CookieJar {
    private val lock = Any()
    private val cookies = LinkedHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = synchronized(lock) {
        val now = System.currentTimeMillis()
        cookies.forEach { cookie ->
            val key = "${cookie.domain}|${cookie.path}|${cookie.name}"
            if (cookie.expiresAt <= now) this.cookies.remove(key)
            else this.cookies[key] = cookie
        }
        // Serialize insertion and eviction: concurrent responses must not race
        // past the per-source memory limit.
        if (this.cookies.size > MAX_COOKIES) {
            val excess = this.cookies.size - MAX_COOKIES
            this.cookies.entries
                .sortedBy { it.value.expiresAt }
                .take(excess)
                .forEach { this.cookies.remove(it.key) }
        }
    }

    internal val size: Int get() = synchronized(lock) { cookies.size }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        val now = System.currentTimeMillis()
        cookies.entries.removeIf { it.value.expiresAt <= now }
        cookies.values.filter { it.matches(url) }
    }

    private companion object {
        const val MAX_COOKIES = 3_000
    }
}

private object NoSourceCookies : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
    override fun loadForRequest(url: HttpUrl): List<Cookie> = emptyList()
}


/**
 * Validates each redirect before OkHttp follows it. Source URLs may legitimately
 * redirect across hosts, but redirects must remain credential-free HTTP(S), and
 * HTTPS requests must never be downgraded to cleartext HTTP.
 */
internal object SafeRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code !in REDIRECT_CODES) return response
        val location = response.header("Location") ?: return response
        if (!isSafeRedirectTarget(response.request.url, location)) {
            response.close()
            throw IOException("Blocked unsafe or HTTPS-downgrade redirect")
        }
        return response
    }

    private val REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
}

internal fun isSafeRedirectTarget(from: HttpUrl, location: String): Boolean {
    val trimmedLocation = location.trim()
    val explicitScheme = Regex("^([A-Za-z][A-Za-z0-9+.-]*):").find(trimmedLocation)?.groupValues?.get(1)
    if (explicitScheme != null && !explicitScheme.equals("http", true) &&
        !explicitScheme.equals("https", true)
    ) return false
    val target = from.resolve(trimmedLocation) ?: return false
    if (target.scheme != "http" && target.scheme != "https") return false
    if (target.username.isNotEmpty() || target.password.isNotEmpty()) return false
    if (from.scheme == "https" && target.scheme != "https") return false
    return true
}
