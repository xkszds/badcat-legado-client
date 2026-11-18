package app.badcat.engine

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class OkHttpSourceRequestClientTest {

    @Test
    fun successfulResponseIncludesElapsedCallTime() = runBlocking {
        ServerSocket(0).use { server ->
            server.soTimeout = 5_000
            val worker = thread {
                server.accept().use { socket ->
                    socket.soTimeout = 5_000
                    val reader = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                    while (reader.readLine()?.isNotEmpty() == true) Unit
                    Thread.sleep(25)
                    val payload = "ok".toByteArray(StandardCharsets.UTF_8)
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\\r\\nConnection: close\\r\\n\\r\\n".toByteArray(StandardCharsets.US_ASCII))
                        write(payload)
                        flush()
                    }
                }
            }
            val result = OkHttpSourceRequestClient().execute(
                SourceHttpRequest(
                    sourceId = "call-time-fixture",
                    url = "http://127.0.0.1:${server.localPort}/",
                    useCookieJar = false,
                ),
            )
            worker.join(5_000)
            assertTrue(!worker.isAlive, "HTTP fixture server should finish")
            val response = (result as EngineResult.Success).value
            assertEquals("ok", response.body)
            assertTrue(response.callTime >= 10, "callTime should include the delayed server response")
        }
    }

    @Test
    fun cookieJarStoresAndReturnsMatchingCookies() {
        val jar = MemorySourceCookieJar()
        val url = "https://reader.example/path/page".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("sid")
            .value("abc123")
            .domain("reader.example")
            .path("/")
            .build()

        jar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), jar.loadForRequest(url))
        assertTrue(jar.loadForRequest("https://other.example/".toHttpUrl()).isEmpty())
    }

    @Test
    fun cookieJarKeepsPathAndSecureScope() {
        val jar = MemorySourceCookieJar()
        val secureUrl = "https://reader.example/private/login".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("token")
            .value("secret")
            .domain("reader.example")
            .path("/private")
            .secure()
            .build()

        jar.saveFromResponse(secureUrl, listOf(cookie))

        assertEquals(listOf(cookie), jar.loadForRequest(secureUrl))
        assertTrue(jar.loadForRequest("https://reader.example/public".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("http://reader.example/private".toHttpUrl()).isEmpty())
    }

    @Test
    fun separateSourceJarsDoNotShareCookies() {
        val firstSourceJar = MemorySourceCookieJar()
        val secondSourceJar = MemorySourceCookieJar()
        val url = "https://reader.example/".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("sid")
            .value("source-one")
            .domain("reader.example")
            .build()

        firstSourceJar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), firstSourceJar.loadForRequest(url))
        assertTrue(secondSourceJar.loadForRequest(url).isEmpty())
    }

    @Test
    fun cookieJarBoundsStorageWhenSourceEmitsManyDistinctCookies() {
        val jar = MemorySourceCookieJar()
        val url = "https://reader.example/".toHttpUrl()
        val cookies = (0..3_100).map { index ->
            Cookie.Builder()
                .name("cookie_$index")
                .value("value")
                .domain("reader.example")
                .build()
        }

        jar.saveFromResponse(url, cookies)

        assertTrue(jar.size <= 3_000)
        assertTrue(jar.loadForRequest(url).size <= 3_000)
    }

    @Test
    fun cookieJarLimitRemainsEnforcedAcrossConcurrentResponses() {
        val jar = MemorySourceCookieJar()
        val url = "https://reader.example/".toHttpUrl()
        val workers = (0 until 8).map { worker ->
            Thread {
                val cookies = (0 until 500).map { index ->
                    Cookie.Builder()
                        .name("cookie_${worker}_$index")
                        .value("value")
                        .domain("reader.example")
                        .build()
                }
                jar.saveFromResponse(url, cookies)
            }.apply { start() }
        }

        workers.forEach { it.join() }

        assertTrue(jar.size <= 3_000)
        assertTrue(jar.loadForRequest(url).size <= 3_000)
    }

    @Test
    fun defaultTimeoutsBoundConnectReadAndWholeCall() {
        val client = OkHttpSourceRequestClient().clientFor(
            SourceHttpRequest(sourceId = "default-timeout-fixture", url = "https://reader.example/"),
        )
        assertEquals(20_000, client.connectTimeoutMillis)
        assertEquals(45_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
        assertEquals(60_000, client.callTimeoutMillis)
    }

    @Test
    fun perRequestTimeoutAppliesToConnectReadWriteAndWholeCall() {
        val timeoutMillis = 4_000L
        val client = OkHttpSourceRequestClient().clientFor(
            SourceHttpRequest(
                sourceId = "timeout-fixture",
                url = "https://reader.example/",
                timeoutMillis = timeoutMillis,
            ),
        )

        assertEquals(timeoutMillis.toInt(), client.connectTimeoutMillis)
        assertEquals(timeoutMillis.toInt(), client.readTimeoutMillis)
        assertEquals(timeoutMillis.toInt(), client.writeTimeoutMillis)
        assertEquals(timeoutMillis.toInt(), client.callTimeoutMillis)
    }

    @Test
    fun retriesOnlyTransientResponsesForIdempotentReadMethods() {
        listOf(408, 425, 429, 500, 502, 503, 504).forEach { status ->
            assertTrue(shouldRetryHttpResponse("GET", status), "GET $status should be retryable")
            assertTrue(shouldRetryHttpResponse("HEAD", status), "HEAD $status should be retryable")
        }
        listOf(400, 401, 403, 404, 501).forEach { status ->
            assertTrue(!shouldRetryHttpResponse("GET", status), "GET $status must not be retried")
        }
        assertTrue(!shouldRetryHttpResponse("POST", 503))
        assertTrue(!shouldRetryHttpResponse("PUT", 502))
    }

    @Test
    fun redirectValidatorAllowsSafeRelativeAndCrossHostHttpsRedirects() {
        val from = "https://reader.example/book/1".toHttpUrl()
        assertTrue(isSafeRedirectTarget(from, "../login"))
        assertTrue(isSafeRedirectTarget(from, "https://cdn.example/book/1"))
    }

    @Test
    fun redirectValidatorRejectsCredentialsAndHttpsDowngrades() {
        val from = "https://reader.example/book/1".toHttpUrl()
        assertTrue(!isSafeRedirectTarget(from, "https://user:pass@cdn.example/book/1"))
        assertTrue(!isSafeRedirectTarget(from, "http://reader.example/book/1"))
        assertTrue(!isSafeRedirectTarget(from, "javascript:alert(1)"))
        assertTrue(!isSafeRedirectTarget(from, "file:///etc/passwd"))
    }

    @Test
    fun redirectValidatorAllowsHttpToHttpsUpgrade() {
        val from = "http://reader.example/book/1".toHttpUrl()
        assertTrue(isSafeRedirectTarget(from, "https://reader.example/book/1"))
    }
    @Test
    fun retriesTransientReadAndDnsFailuresButNeverTlsOrWrites() {
        assertTrue(shouldRetryIOException("GET", java.net.SocketTimeoutException("read timed out")))
        assertTrue(shouldRetryIOException("HEAD", java.io.EOFException("unexpected end of stream")))
        assertTrue(shouldRetryIOException("GET", java.net.ProtocolException("unexpected end of stream")))
        assertTrue(shouldRetryIOException("GET", java.net.UnknownHostException("temporary DNS failure")))
        assertTrue(!shouldRetryIOException("GET", javax.net.ssl.SSLHandshakeException("certificate verify failed")))
        assertTrue(!shouldRetryIOException("POST", java.net.SocketTimeoutException("write timed out")))
    }
}
