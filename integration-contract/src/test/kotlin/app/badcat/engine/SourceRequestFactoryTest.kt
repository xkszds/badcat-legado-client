package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SourceRequestFactoryTest {

    private fun source(
        header: String? = null,
        cookieJar: Boolean? = true,
    ) = BookSource(
        id = "https://reader.example/",
        name = "Fixture",
        baseUrl = "https://reader.example/",
        header = header,
        enabledCookieJar = cookieJar,
    )

    @Test
    fun parsesSourceHeadersAndAppliesPerRequestOverrideCaseInsensitively() {
        val result = SourceRequestFactory.create(
            source = source("""{"User-Agent":"LegadoFixture","Accept":"text/html"}"""),
            url = "https://reader.example/search",
            headers = mapOf("user-agent" to "RequestOverride"),
        )

        val request = assertIs<EngineResult.Success<SourceHttpRequest>>(result).value
        assertEquals("RequestOverride", request.headers["user-agent"])
        assertEquals("text/html", request.headers["Accept"])
        assertEquals(2, request.headers.size)
        assertEquals(true, request.useCookieJar)
    }

    @Test
    fun cookieJarCanBeDisabledAndMethodIsNormalized() {
        val result = SourceRequestFactory.create(
            source = source(cookieJar = false),
            url = "https://reader.example/search",
            method = "post",
            body = "q=fixture",
        )

        val request = assertIs<EngineResult.Success<SourceHttpRequest>>(result).value
        assertEquals("POST", request.method)
        assertEquals("q=fixture", request.body)
        assertEquals(false, request.useCookieJar)
    }

    @Test
    fun rejectsMethodsNotSupportedByTheTransportBeforeCreatingRequest() {
        val result = SourceRequestFactory.create(
            source = source(),
            url = "https://reader.example/search",
            method = "DELETE",
        )

        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsNonHttpAndCredentialBearingUrls() {
        listOf(
            "javascript:alert(1)",
            "file:///etc/passwd",
            "https://user:password@reader.example/search",
            "/relative/search",
        ).forEach { url ->
            val result = SourceRequestFactory.create(source(), url)
            val failure = assertIs<EngineResult.Failure>(result)
            assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
        }
    }

    @Test
    fun rejectsHeaderInjectionAndInvalidHeaderNames() {
        val injectedValue = SourceRequestFactory.create(
            source(),
            "https://reader.example/",
            headers = mapOf("X-Test" to "safe\r\nInjected: yes"),
        )
        val invalidName = SourceRequestFactory.create(
            source(),
            "https://reader.example/",
            headers = mapOf("Bad Header" to "value"),
        )

        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(injectedValue).code,
        )
        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(invalidName).code,
        )
    }

    @Test
    fun rejectsOutOfRangeRetryAndTimeoutOptions() {
        val excessiveRetry = SourceRequestFactory.create(
            source(),
            LegadoResolvedUrl(
                url = "https://reader.example/",
                retryCount = 6,
            ),
        )
        val excessiveTimeout = SourceRequestFactory.create(
            source(),
            LegadoResolvedUrl(
                url = "https://reader.example/",
                timeoutMillis = Int.MAX_VALUE.toLong() + 1,
            ),
        )
        val zeroTimeout = SourceRequestFactory.create(
            source(),
            LegadoResolvedUrl(
                url = "https://reader.example/",
                timeoutMillis = 0,
            ),
        )

        listOf(excessiveRetry, excessiveTimeout, zeroTimeout).forEach { result ->
            assertEquals(
                EngineErrorCode.INVALID_SOURCE,
                assertIs<EngineResult.Failure>(result).code,
            )
        }
    }

    @Test
    fun rejectsEscapeCharsetUntilDecoderIsImplemented() {
        val result = SourceRequestFactory.create(
            source(),
            LegadoResolvedUrl(
                url = "https://reader.example/",
                charset = "escape",
            ),
        )

        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }

    @Test
    fun rejectsUnsupportedResponseCharsetBeforeTransport() {
        val result = SourceRequestFactory.create(
            source(),
            LegadoResolvedUrl(
                url = "https://reader.example/",
                charset = "not-a-real-charset",
            ),
        )

        assertEquals(
            EngineErrorCode.INVALID_SOURCE,
            assertIs<EngineResult.Failure>(result).code,
        )
    }

    @Test
    fun rejectsMalformedOrNonStringHeaderValues() {
        val malformed = SourceRequestFactory.create(
            source = source("""{"User-Agent":42}"""),
            url = "https://reader.example/",
        )

        assertIs<EngineResult.Failure>(malformed)
    }

    @Test
    fun rejectsBlankIdentityOrUrl() {
        val result = SourceRequestFactory.create(
            source = source(),
            url = " ",
        )

        assertIs<EngineResult.Failure>(result)
    }
    @Test
    fun getQueryUsesLegadoRfc3986EncodingAfterSourceAndRequestHeadersMerge() {
        val result = SourceRequestFactory.create(
            source(),
            "https://reader.example/search?q=中文 x&tag=a+b",
        )

        val request = assertIs<EngineResult.Success<SourceHttpRequest>>(result).value
        assertEquals(
            "https://reader.example/search?q=%E4%B8%AD%E6%96%87%20x&tag=a+b",
            request.url,
        )
    }

    @Test
    fun postTextBodyUsesLegadoFormEncodingWhenContentTypeIsAbsent() {
        val result = SourceRequestFactory.create(
            source(),
            "https://reader.example/search",
            method = "POST",
            body = "q=中文&tag=a b",
        )

        val request = assertIs<EngineResult.Success<SourceHttpRequest>>(result).value
        assertEquals("q=%E4%B8%AD%E6%96%87&tag=a+b", request.body)
    }

    @Test
    fun postJsonAndExplicitContentTypeBodiesAreNotFormEncoded() {
        val json = SourceRequestFactory.create(
            source(),
            "https://reader.example/search",
            method = "POST",
            body = """{"q":"中文"}""",
        )
        val typed = SourceRequestFactory.create(
            source(header = """{"Content-Type":"application/json"}"""),
            "https://reader.example/search",
            method = "POST",
            body = "q=中文",
        )

        assertEquals(
            """{"q":"中文"}""",
            assertIs<EngineResult.Success<SourceHttpRequest>>(json).value.body,
        )
        assertEquals(
            "q=中文",
            assertIs<EngineResult.Success<SourceHttpRequest>>(typed).value.body,
        )
    }

    @Test
    fun resolvedUrlOptionsArePassedIntoTransportRequest() {
        val resolved = LegadoResolvedUrl(
            url = "https://reader.example/search",
            method = "POST",
            headers = mapOf("X-Source" to "url-option"),
            body = """{"q":"cat"}""",
            timeoutMillis = 8_000,
            followRedirects = false,
            retryCount = 2,
            charset = "UTF-8",
        )

        val result = SourceRequestFactory.create(source(), resolved)
        val request = assertIs<EngineResult.Success<SourceHttpRequest>>(result).value

        assertEquals(resolved.url, request.url)
        assertEquals("POST", request.method)
        assertEquals("url-option", request.headers["X-Source"])
        assertEquals(resolved.body, request.body)
        assertEquals(8_000L, request.timeoutMillis)
        assertEquals(false, request.followRedirects)
        assertEquals(2, request.retryCount)
        assertEquals("UTF-8", request.responseCharset)
    }

}
