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
}
