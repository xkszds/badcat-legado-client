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
