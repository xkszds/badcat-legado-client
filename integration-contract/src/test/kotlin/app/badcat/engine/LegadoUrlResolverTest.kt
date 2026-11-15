package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LegadoUrlResolverTest {

    @Test
    fun resolvesRelativeUrlAndPageList() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/books/",
            "search?page=<1,2,3>",
            page = 2,
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("https://example.com/books/search?page=2", value.url)
    }

    @Test
    fun usesLastPageValueWhenPageExceedsList() {
        val result = LegadoUrlResolver.resolve("https://example.com/", "search/<a,b>", page = 8)
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("https://example.com/search/b", value.url)
    }

    @Test
    fun parsesStaticRequestOptions() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            "api/search, {\"method\":\"POST\",\"headers\":{\"X-Test\":\"yes\"},\"body\":{\"q\":\"cat\"},\"timeout\":5000,\"followRedirects\":false,\"retry\":2}",
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("POST", value.method)
        assertEquals("yes", value.headers["X-Test"])
        assertEquals("{\"q\":\"cat\"}", value.body)
        assertEquals(5000L, value.timeoutMillis)
        assertEquals(false, value.followRedirects)
        assertEquals(2, value.retryCount)
    }

    @Test
    fun rejectsScriptSyntaxInsteadOfSendingItLiterally() {
        val result = LegadoUrlResolver.resolve("https://example.com/", "{{key}}/search")
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsMalformedOptions() {
        val result = LegadoUrlResolver.resolve("https://example.com/", "search, {bad}")
        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun rejectsAtJsSyntaxInsteadOfTreatingItAsAStaticUrl() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            "@js: 'https://example.com/search?q=' + key",
        )
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsJavascriptAndWebViewRequestOptions() {
        val rules = listOf(
            """search, {"js":"return key"}""",
            """search, {"bodyJs":"return key"}""",
            """search, {"webView":true}""",
        )
        rules.forEach { rule ->
            val failure = assertIs<EngineResult.Failure>(
                LegadoUrlResolver.resolve("https://example.com/", rule),
            )
            assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
        }
    }
}
