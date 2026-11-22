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
    fun encodesUnicodeAndSpacesInRawQueryBeforeResolvingUri() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            "search?q=中文 x&tag=a+b",
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals(
            "https://example.com/search?q=%E4%B8%AD%E6%96%87%20x&tag=a+b",
            value.url,
        )
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
    fun parsesLegacySingleQuotedStaticPostOptionsUsedByPublicBookSources() {
        val result = LegadoUrlResolver.resolve(
            "https://69shuba.cx/",
            "/modules/article/search.php,{'charset':'gbk','body':'searchkey=Douluo&searchtype=all','method':'POST'}",
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("https://69shuba.cx/modules/article/search.php", value.url)
        assertEquals("POST", value.method)
        assertEquals("gbk", value.charset)
        assertEquals("searchkey=Douluo&searchtype=all", value.body)
    }

    @Test
    fun preservesBackslashEscapesInsideLegacySingleQuotedOptions() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            """search, {'method':'POST','body':'pattern=\d+'}""",
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("pattern=\\d+", value.body)
    }

    @Test
    fun acceptsBodyMethodsImplementedByTheHttpTransport() {
        listOf("PUT", "PATCH", "PROPPATCH", "REPORT").forEach { method ->
            val result = LegadoUrlResolver.resolve(
                "https://example.com/",
                """search, {"method":"$method","body":"payload"}""",
            )
            val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
            assertEquals(method, value.method)
            assertEquals("payload", value.body)
        }
    }

    @Test
    fun rejectsMethodsNotImplementedByTheHttpTransport() {
        val failure = assertIs<EngineResult.Failure>(
            LegadoUrlResolver.resolve(
                "https://example.com/",
                """search, {"method":"DELETE"}""",
            ),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsScriptSyntaxInsteadOfSendingItLiterally() {
        val result = LegadoUrlResolver.resolve("https://example.com/", "{{key}}/search")
        val failure = assertIs<EngineResult.Failure>(result)
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsBaseAndResolvedUrlsWithUnsafeSchemesOrCredentials() {
        val invalidBases = listOf(
            "file:///tmp/",
            "https://user:password@example.com/",
            "/relative/base/",
        )
        invalidBases.forEach { base ->
            assertIs<EngineResult.Failure>(
                LegadoUrlResolver.resolve(base, "search"),
            )
        }

        val credentialRedirect = LegadoUrlResolver.resolve(
            "https://example.com/",
            "https://user:password@other.example/search",
        )
        assertIs<EngineResult.Failure>(credentialRedirect)
    }

    @Test
    fun rejectsTransportOptionsThatAreParsedButNotImplemented() {
        val unsupportedOptions = listOf(
            """search, {"dnsIp":"127.0.0.1"}""",
            """search, {"resolveIp":"127.0.0.1"}""",
            """search, {"type":"image"}""",
        )
        unsupportedOptions.forEach { rule ->
            val failure = assertIs<EngineResult.Failure>(
                LegadoUrlResolver.resolve("https://example.com/", rule),
            )
            assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
        }
    }

    @Test
    fun rejectsLegadoEscapeCharsetUntilItsDecoderExists() {
        val failure = assertIs<EngineResult.Failure>(
            LegadoUrlResolver.resolve(
                "https://example.com/",
                """search, {"charset":"escape"}""",
            ),
        )
        assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
    }

    @Test
    fun rejectsRetryCountAboveTransportLimitAtResolutionTime() {
        val failure = assertIs<EngineResult.Failure>(
            LegadoUrlResolver.resolve(
                "https://example.com/",
                """search, {"retry":6}""",
            ),
        )
        assertEquals(EngineErrorCode.INVALID_SOURCE, failure.code)
    }

    @Test
    fun rejectsMalformedOptions() {
        val result = LegadoUrlResolver.resolve("https://example.com/", "search, {bad}")
        assertIs<EngineResult.Failure>(result)
    }

    @Test
    fun rejectsIncompleteInterpolationMarkersInsteadOfSendingThemLiterally() {
        listOf(
            "{{key/search",
            "search/{{key",
        ).forEach { rule ->
            val failure = assertIs<EngineResult.Failure>(
                LegadoUrlResolver.resolve("https://example.com/", rule),
            )
            assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
        }
    }

    @Test
    fun allowsLiteralClosingBracesInsideStaticPostBodies() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            """search, {"method":"POST","body":"literal }}"}""",
        )
        val resolved = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("literal }}", resolved.body)
    }

    @Test
    fun doesNotTreatAnOrdinaryAtJsSubstringAsAnEmbeddedScriptMarker() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            "/catalog/@js-guide",
        )
        val resolved = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("https://example.com/catalog/@js-guide", resolved.url)
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
    fun preservesSupportedScriptOptionsAndRejectsWebViewOptions() {
        val supported = LegadoUrlResolver.resolve(
            "https://example.com/",
            """search, {"js":"return key","bodyJs":"return result"}""",
        )
        val resolved = assertIs<EngineResult.Success<LegadoResolvedUrl>>(supported).value
        assertEquals("return key", resolved.js)
        assertEquals("return result", resolved.bodyJs)

        val unsupported = listOf(
            """search, {"webView":true}""",
            """search, {"webJs":"return result"}""",
        )
        unsupported.forEach { rule ->
            val failure = assertIs<EngineResult.Failure>(
                LegadoUrlResolver.resolve("https://example.com/", rule),
            )
            assertEquals(EngineErrorCode.UNSUPPORTED_RULE, failure.code)
        }
    }
    @Test
    fun parsesUrlAndBodyJavaScriptOptionsForTheGatewayRuntime() {
        val result = LegadoUrlResolver.resolve(
            baseUrl = "https://example.test/",
            ruleUrl = """/search,{"method":"POST","body":"q=cat","js":"return result + '/v2'","bodyJs":"return result.trim()"}""",
        )
        val resolved = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("POST", resolved.method)
        assertEquals("return result + '/v2'", resolved.js)
        assertEquals("return result.trim()", resolved.bodyJs)
    }

    @Test
    fun encodesUnicodeAndRepeatedHashCharactersInsideSourceUrlFragments() {
        val result = LegadoUrlResolver.resolve(
            "https://example.com/",
            "/search#♤#Qyc",
        )
        val value = assertIs<EngineResult.Success<LegadoResolvedUrl>>(result).value
        assertEquals("https://example.com/search#%E2%99%A4%23Qyc", value.url)
    }
    @Test
    fun defaultsToZeroRetriesButHonorsExplicitRetrySettings() {
        val default = assertIs<EngineResult.Success<LegadoResolvedUrl>>(
            LegadoUrlResolver.resolve("https://example.com/", "/search"),
        ).value
        assertEquals(0, default.retryCount)
        val disabled = assertIs<EngineResult.Success<LegadoResolvedUrl>>(
            LegadoUrlResolver.resolve("https://example.com/", """/search,{"retry":0}"""),
        ).value
        assertEquals(0, disabled.retryCount)
        val enabled = assertIs<EngineResult.Success<LegadoResolvedUrl>>(
            LegadoUrlResolver.resolve("https://example.com/", """/search,{"retry":3}"""),
        ).value
        assertEquals(3, enabled.retryCount)
    }
}
