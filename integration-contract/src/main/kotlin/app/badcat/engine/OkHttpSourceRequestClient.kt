package app.badcat.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
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
    private val connectTimeoutSeconds: Long = 15,
    private val readTimeoutSeconds: Long = 30,
    private val writeTimeoutSeconds: Long = 30,
    private val maxResponseBytes: Long = 8L * 1024 * 1024,
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
                (explicitContentType ?: inferredContentType).toMediaTypeOrNull()
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
                    return response.use { EngineResult.Success(readResponse(it, request.responseCharset)) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (io: IOException) {
                    if (attempt >= request.retryCount) throw io
                    attempt++
                }
            }
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

    private fun clientFor(request: SourceHttpRequest): OkHttpClient {
        val key = request.sourceId + if (request.useCookieJar) "|cookies" else "|no-cookies"
        val baseClient = clients.computeIfAbsent(key) {
            val jar = if (request.useCookieJar) {
                cookieJars.computeIfAbsent(request.sourceId) { MemorySourceCookieJar() }
            } else {
                NoSourceCookies
            }
            OkHttpClient.Builder()
                .cookieJar(jar)
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
        if (request.timeoutMillis == null && request.followRedirects == null) return baseClient
        return baseClient.newBuilder().apply {
            request.timeoutMillis?.let { timeout ->
                readTimeout(timeout, TimeUnit.MILLISECONDS)
                val derivedCallTimeout = maxOf(60_000L, timeout * 2)
                callTimeout(derivedCallTimeout, TimeUnit.MILLISECONDS)
            }
            request.followRedirects?.let {
                followRedirects(it)
                followSslRedirects(it)
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
        )
    }

    companion object {
        private val BODY_REQUIRED_METHODS = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
        private val BODY_FORBIDDEN_METHODS = setOf("GET", "HEAD")
    }
}

/** In-memory cookie storage, scoped to one source by the owning client. */
internal class MemorySourceCookieJar : CookieJar {
    private val cookies = ConcurrentHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        cookies.forEach { cookie ->
            val key = "${cookie.domain}|${cookie.path}|${cookie.name}"
            if (cookie.expiresAt <= now) this.cookies.remove(key)
            else this.cookies[key] = cookie
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        this.cookies.entries.removeIf { it.value.expiresAt <= now }
        return this.cookies.values.filter { it.matches(url) }
    }
}

private object NoSourceCookies : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit
    override fun loadForRequest(url: HttpUrl): List<Cookie> = emptyList()
}
