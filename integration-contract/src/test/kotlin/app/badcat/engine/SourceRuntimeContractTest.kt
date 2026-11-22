package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SourceRuntimeContractTest {

    @Test
    fun requestDefaultsMatchCookieJarEnabledSourceBehavior() {
        val request = SourceHttpRequest(
            sourceId = "https://reader.example/",
            url = "https://reader.example/search?q=cat",
        )

        assertEquals("GET", request.method)
        assertTrue(request.useCookieJar)
        assertTrue(request.headers.isEmpty())
        assertNull(request.body)
    }

    @Test
    fun requestCanRepresentPostHeadersAndDisabledCookieJar() {
        val request = SourceHttpRequest(
            sourceId = "source-1",
            url = "https://reader.example/search",
            method = "POST",
            headers = mapOf("User-Agent" to "fixture", "X-Test" to "1"),
            body = "keyword=cat",
            useCookieJar = false,
        )

        assertEquals("POST", request.method)
        assertEquals("fixture", request.headers["User-Agent"])
        assertEquals("keyword=cat", request.body)
        assertEquals(false, request.useCookieJar)
    }

    @Test
    fun unsupportedScriptRuntimeFailsClosedInsteadOfReturningScriptText() = runBlocking {
        val source = BookSource(
            id = "source-1",
            name = "Fixture",
            baseUrl = "https://reader.example/",
        )
        val result = UnsupportedSourceScriptRuntime.evaluate(
            source = source,
            script = "return key",
            bindings = mapOf("key" to "cat"),
        )

        val failure = result as EngineResult.Failure
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun ruleStagesRemainExplicitlySeparated() {
        assertEquals(
            listOf("EXPLORE", "SEARCH", "BOOK_INFO", "TOC", "CONTENT", "REVIEW"),
            SourceRuleStage.entries.map { it.name },
        )
    }
}
