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
                        write("HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
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
    fun explicitCookieHeaderMergesWithCookieStoreOnEveryRedirectExchange() = runBlocking {
        ServerSocket(0).use { server ->
            server.soTimeout = 5_000
            val receivedCookies = java.util.concurrent.CopyOnWriteArrayList<String>()
            val worker = thread {
                repeat(2) { index ->
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val reader = socket.getInputStream().bufferedReader(StandardCharsets.UTF_8)
                        var cookie = ""
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Cookie:", ignoreCase = true)) {
                                cookie = line.substringAfter(':').trim()
                            }
                        }
                        receivedCookies += cookie
                        val output = socket.getOutputStream()
                        if (index == 0) {
                            output.write(
                                ("HTTP/1.1 302 Found\r\n" +
                                    "Location: /final\r\n" +
                                    "Set-Cookie: sid=jar; Path=/\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n\r\n")
                                    .toByteArray(StandardCharsets.US_ASCII),
                            )
                        } else {
                            val payload = "ok".toByteArray(StandardCharsets.UTF_8)
                            output.write(
                                ("HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\r\n" +
                                    "Connection: close\r\n\r\n")
                                    .toByteArray(StandardCharsets.US_ASCII),
                            )
                            output.write(payload)
                        }
                        output.flush()
                    }
                }
            }
            val jar = MemorySourceCookieJar()
            val result = OkHttpSourceRequestClient(
                cookieJarProvider = { jar },
            ).execute(
                SourceHttpRequest(
                    sourceId = "http://127.0.0.1/source.json",
                    url = "http://127.0.0.1:${server.localPort}/start",
                    headers = mapOf("Cookie" to "sid=explicit; theme=dark"),
                ),
            )
            worker.join(5_000)
            assertTrue(!worker.isAlive, "redirect fixture server should finish")
            assertEquals("ok", (result as EngineResult.Success).value.body)
            assertEquals(
                listOf("sid=explicit; theme=dark", "sid=jar; theme=dark"),
                receivedCookies,
            )
        }
    }

    @Test
    fun rejectsOversizedUrlsWhenTransportIsCalledDirectly() = runBlocking {
        val result = OkHttpSourceRequestClient().execute(
            SourceHttpRequest(
                sourceId = "oversized-url-fixture",
                url = "https://example.com/" + "a".repeat(16 * 1024),
                useCookieJar = false,
            ),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, (result as EngineResult.Failure).code)
    }

    @Test
    fun rejectsDirectTransportUrlsOutsideCredentialFreeHttpAndHttps() = runBlocking {
        val client = OkHttpSourceRequestClient()
        listOf(
            "file:///etc/passwd",
            "javascript:alert(1)",
            "http://user:pass@127.0.0.1/",
            "https://@127.0.0.1/",
        ).forEach { url ->
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = "unsafe-url-fixture",
                    url = url,
                    useCookieJar = false,
                ),
            )
            assertEquals(EngineErrorCode.INVALID_SOURCE, (result as EngineResult.Failure).code, url)
        }
    }

    @Test
    fun rejectsDirectTransportHeadersOutsideConfiguredLimits() = runBlocking {
        val client = OkHttpSourceRequestClient()
        val invalidHeaders = listOf(
            (0..100).associate { "X-$it" to "v" },
            mapOf("X-Large" to "v".repeat(8 * 1024 + 1)),
            mapOf("X-Test" to "safe\r\nInjected: yes"),
        )
        invalidHeaders.forEach { headers ->
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = "direct-header-fixture",
                    url = "http://127.0.0.1:1/",
                    headers = headers,
                    useCookieJar = false,
                ),
            )
            assertEquals(EngineErrorCode.INVALID_SOURCE, (result as EngineResult.Failure).code)
        }
    }

    @Test
    fun rejectsUnsupportedMethodsWhenTransportIsCalledDirectly() = runBlocking {
        val result = OkHttpSourceRequestClient().execute(
            SourceHttpRequest(
                sourceId = "unsupported-method-fixture",
                url = "https://example.com/",
                method = "DELETE",
                useCookieJar = false,
            ),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, (result as EngineResult.Failure).code)
    }

    @Test
    fun rejectsRequestBodiesThatExceedTheConfiguredByteLimit() = runBlocking {
        val result = OkHttpSourceRequestClient(maxRequestBytes = 4).execute(
            SourceHttpRequest(
                sourceId = "request-size-fixture",
                url = "http://127.0.0.1:1/",
                method = "POST",
                body = "ééé",
                useCookieJar = false,
            ),
        )

        val failure = result as EngineResult.Failure
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun utf8EncodedLengthCountsSupplementaryAndUnpairedSurrogates() {
        assertEquals(1L, utf8EncodedLength("a"))
        assertEquals(2L, utf8EncodedLength("é"))
        assertEquals(3L, utf8EncodedLength("中"))
        assertEquals(4L, utf8EncodedLength("😀"))
        assertEquals(1L, utf8EncodedLength("\uD800"))
    }

    @Test
    fun rejectsUtf8RequestBodiesByEncodedByteSizeBeforeBuildingRequestBody() = runBlocking {
        val result = OkHttpSourceRequestClient(maxRequestBytes = 4).execute(
            SourceHttpRequest(
                sourceId = "utf8-request-size-fixture",
                url = "http://127.0.0.1:1/",
                method = "POST",
                body = "ééé",
                useCookieJar = false,
            ),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, (result as EngineResult.Failure).code)
    }

    @Test
    fun rejectsBodiesForMethodsThatDoNotSupportRequestBodies() = runBlocking {
        listOf("GET", "HEAD").forEach { method ->
            val result = OkHttpSourceRequestClient().execute(
                SourceHttpRequest(
                    sourceId = "body-forbidden-$method",
                    url = "http://127.0.0.1:1/",
                    method = method,
                    body = "unexpected",
                    useCookieJar = false,
                ),
            )
            assertEquals(EngineErrorCode.INVALID_SOURCE, (result as EngineResult.Failure).code)
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
