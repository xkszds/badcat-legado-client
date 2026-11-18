package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.CssTocParser
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class RhinoSourceScriptRuntimeTest {
    @Test
    fun evaluatesSourceJsLibBeforeRuleScript() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://source.example/",
            name = "Fixture source",
            baseUrl = "https://source.example/",
            jsLib = "function fixtureGreeting(name) { return 'hello ' + name; }",
        )

        val result = runtime.evaluate(source, "fixtureGreeting('reader')")

        assertTrue(result is EngineResult.Success)
        assertEquals("hello reader", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun doesNotLeakJsLibFunctionsBetweenSources() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val first = BookSource(
            id = "https://first.example/",
            name = "First source",
            baseUrl = "https://first.example/",
            jsLib = "function sourceOnlyHelper() { return 'first'; }",
        )
        val second = BookSource(
            id = "https://second.example/",
            name = "Second source",
            baseUrl = "https://second.example/",
        )

        val firstResult = runtime.evaluate(first, "sourceOnlyHelper()")
        val secondResult = runtime.evaluate(second, "typeof sourceOnlyHelper")

        assertEquals("first", (firstResult as EngineResult.Success).value)
        assertEquals("undefined", (secondResult as EngineResult.Success).value)
        runtime.clearSourceState(first.id)
        runtime.clearSourceState(second.id)
    }

    @Test
    fun evaluatesUrlScriptWithSafeSourceMetadataAndRequestValues() = runBlocking {
        val source = BookSource(
            id = "https://reader.example/source.json",
            name = "Fixture source",
            baseUrl = "https://reader.example/",
        )
        val evaluator = RhinoEmbeddedUrlScriptEvaluator(source, RhinoSourceScriptRuntime())

        val result = evaluator.evaluateWithResult(
            script = "baseUrl + '|' + sourceName + '|' + sourceId + '|' + key + '|' + page + '|' + result",
            key = "cat",
            page = 3,
            result = "previous",
        )

        assertEquals(
            "https://reader.example/|Fixture source|https://reader.example/source.json|cat|3|previous",
            (result as EngineResult.Success).value,
        )
        RhinoSourceScriptRuntime().clearSourceState(source.id)
    }

    @Test
    fun bindsPageAsNumberLikeLegadoAnalyzeUrl() = runBlocking {
        val source = BookSource(
            id = "https://numeric-page.example/",
            name = "Numeric page binding",
            baseUrl = "https://numeric-page.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = RhinoEmbeddedUrlScriptEvaluator(source, runtime)
            .evaluate("typeof page + ':' + (page + 1)", "", 2)

        assertEquals("number:3", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegacyRegexLiteralsContainingUnescapedClosingBraces() = runBlocking {
        val source = BookSource(
            id = "https://legacy-regex.example/",
            name = "Legacy regex compatibility",
            baseUrl = "https://legacy-regex.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val script = """'abc..}}'.match(/\.*?}}/) ? 'matched' : 'not matched'"""
        val result = runtime.evaluate(source, script)

        assertEquals("matched", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegacyIdentityEscapesInNonAsciiRegexLiterals() = runBlocking {
        val source = BookSource(
            id = "https://legacy-identity-regex.example/",
            name = "Legacy identity regex compatibility",
            baseUrl = "https://legacy-identity-regex.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var japaneseWord = 'いくabc}'.match(/\いく.*?}/)[0];
                var repeatedWord = 'く}}'.match(/\く*?}}/)[0];
                japaneseWord + '|' + repeatedWord;
            """.trimIndent(),
        )

        assertTrue("ECMAScript identity escapes must execute in Rhino: $result", result is EngineResult.Success)
        assertEquals("いくabc}|く}}", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun regexCompatibilityDoesNotRewriteStringsCommentsOrValidQuantifiers() {
        val script = """var text = '}}'; // }}
var re = /a{2,3}}/;
text + re.test('aa}}')"""
        val normalized = RhinoRegexCompatibility.normalize(script)

        assertTrue(normalized.contains("var text = '}}'; // }}"))
        assertTrue(normalized.contains("/a{2,3}\\}/"))
    }

    @Test
    fun urlScriptsCannotAccessJavaRuntime() = runBlocking {
        val source = BookSource(
            id = "https://restricted.example/",
            name = "Restricted source",
            baseUrl = "https://restricted.example/",
        )
        val result = RhinoEmbeddedUrlScriptEvaluator(source, RhinoSourceScriptRuntime())
            .evaluate("Packages.java.lang.Runtime.getRuntime().availableProcessors()", "", 1)

        assertTrue(result is EngineResult.Failure)
        RhinoSourceScriptRuntime().clearSourceState(source.id)
    }

    @Test
    fun exposesOnlyAllowListedSourceJavaHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://safe-java.example/",
            name = "Safe Java source",
            baseUrl = "https://safe-java.example/",
        )
        val stored = runtime.evaluate(
            source,
            "java.put('token', 'source-value'); java.getVariable('token')",
        )
        val digest = runtime.evaluate(source, "java.md5Encode('abc')")
        val encoded = runtime.evaluate(source, "java.urlEncode('a b')")
        val uriEncoded = runtime.evaluate(source, "java.encodeURI('http://m.yimixs.org/##yolo(雪 猫)')")

        assertEquals("source-value", (stored as EngineResult.Success).value)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", (digest as EngineResult.Success).value)
        assertEquals("a+b", (encoded as EngineResult.Success).value)
        assertEquals("http://m.yimixs.org/##yolo(%E9%9B%AA%20%E7%8C%AB)", (uriEncoded as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun exposesJavaScriptUriComponentEncodingAndDecodingSemantics() = runBlocking {
        val source = BookSource(
            id = "https://uri-components.example/",
            name = "URI component helpers",
            baseUrl = "https://uri-components.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "java.encodeURIComponent('a/b?x=雪') + '|' + " +
                "java.decodeURIComponent('a+b%2Fc') + '|' + " +
                "java.decodeURI('https%3A%2F%2Fexample.com%2F%E9%9B%AA')",
        )

        assertEquals(
            "a%2Fb%3Fx%3D%E9%9B%AA|a+b/c|https%3A%2F%2Fexample.com%2F雪",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }

    @Test
    fun safeJavaStorageSurvivesRuntimeRecreationWhenStoreIsDurable() = runBlocking {
        val store = InMemorySourceScriptKeyValueStore
        val source = BookSource(
            id = "https://persistent-safe-java.example/",
            name = "Persistent safe Java source",
            baseUrl = "https://persistent-safe-java.example/",
        )
        val firstRuntime = RhinoSourceScriptRuntime(sourceKeyValueStore = store)
        val secondRuntime = RhinoSourceScriptRuntime(sourceKeyValueStore = store)

        val written = firstRuntime.evaluate(source, "java.putString('refreshToken', 'persisted')")
        val read = secondRuntime.evaluate(source, "java.getVariable('refreshToken')")

        assertEquals("persisted", (written as EngineResult.Success).value)
        assertEquals("persisted", (read as EngineResult.Success).value)
        secondRuntime.clearSourceState(source.id)
    }

    @Test
    fun isolatesSafeJavaStorageBetweenBookSources() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val first = BookSource("https://safe-first.example/", "First", "https://safe-first.example/")
        val second = BookSource("https://safe-second.example/", "Second", "https://safe-second.example/")
        runtime.evaluate(first, "java.putString('session', 'first')")
        val result = runtime.evaluate(second, "java.getVariable('session')")
        assertEquals("", (result as EngineResult.Success).value)
        runtime.clearSourceState(first.id)
        runtime.clearSourceState(second.id)
    }

    @Test
    fun sourceLoginHeaderAndInfoApisPersistAcrossRhinoRuntimeInstances() = runBlocking {
        val sessions = InMemorySourceLoginSessionStore
        val source = BookSource(
            id = "https://login-session.example/",
            name = "Login session source",
            baseUrl = "https://login-session.example/",
        )
        val loginRuntime = RhinoSourceScriptRuntime(sourceLoginSessionStore = sessions)
        val requestRuntime = RhinoSourceScriptRuntime(sourceLoginSessionStore = sessions)

        val write = loginRuntime.evaluate(
            source,
            """source.putLoginInfo('{"username":"reader"}'); source.putLoginHeader('{"Authorization":"Bearer token"}')""",
        )
        val read = requestRuntime.evaluate(
            source,
            "source.getLoginInfoMap().get('username') + '|' + source.getLoginHeaderMap().get('Authorization')",
        )

        assertTrue(write is EngineResult.Success)
        assertEquals("reader|Bearer token", (read as EngineResult.Success).value)
        loginRuntime.clearSourceState(source.id)
        requestRuntime.clearSourceState(source.id)
    }

    @Test
    fun exposesLegadoSourceKeyAndPersistentVariableHelpers() = runBlocking {
        val store = InMemorySourceScriptKeyValueStore
        val source = BookSource(
            id = "https://source-variable.example/source.json",
            name = "Source variable fixture",
            baseUrl = "https://source-variable.example/",
        )
        val firstRuntime = RhinoSourceScriptRuntime(sourceKeyValueStore = store)
        val secondRuntime = RhinoSourceScriptRuntime(sourceKeyValueStore = store)

        val written = firstRuntime.evaluate(
            source,
            "source.setVariable('token-A'); source.getKey() + '|' + source.getVariable()",
        )
        val read = secondRuntime.evaluate(source, "source.getVariable()")
        val cleared = secondRuntime.evaluate(source, "source.setVariable(null); source.getVariable()")

        assertEquals(
            "https://source-variable.example/source.json|token-A",
            (written as EngineResult.Success).value,
        )
        assertEquals("token-A", (read as EngineResult.Success).value)
        assertEquals("", (cleared as EngineResult.Success).value)
        firstRuntime.clearSourceState(source.id)
        secondRuntime.clearSourceState(source.id)
    }

    @Test
    fun sourceVariableIsIsolatedBetweenSourcesAndRejectsOversizedValues() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val first = BookSource("https://source-var-a.example/", "A", "https://source-var-a.example/")
        val second = BookSource("https://source-var-b.example/", "B", "https://source-var-b.example/")

        val write = runtime.evaluate(first, "source.setVariable('private-A')")
        val isolated = runtime.evaluate(second, "source.getVariable()")
        val oversized = runtime.evaluate(first, "source.setVariable('" + "x".repeat(16 * 1024 + 1) + "')")

        assertEquals("private-A", (write as EngineResult.Success).value)
        assertEquals("", (isolated as EngineResult.Success).value)
        assertTrue(oversized is EngineResult.Failure)
        runtime.clearSourceState(first.id)
        runtime.clearSourceState(second.id)
    }

    @Test
    fun exposesLegadoJavaGetPutTimeFormatAndAjaxHelpers() = runBlocking {
        val source = BookSource(
            id = "https://java-compat.example/",
            name = "Java compatibility",
            baseUrl = "https://java-compat.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceAjax = { requestedSource, baseUrl, raw ->
                check(requestedSource.id == source.id)
                check(baseUrl == source.baseUrl)
                "ajax:" + raw
            },
        )
        val result = runtime.evaluate(
            source,
            "java.put('headers', '{\\\"headers\\\":{}}'); java.get('headers') + '|' + java.ajax('https://java-compat.example/api') + '|' + java.ajax(['https://java-compat.example/list']) + '|' + (java.randomUUID().length() === 36)",
        )
        assertEquals(
            "{\"headers\":{}}|ajax:https://java-compat.example/api|ajax:https://java-compat.example/list|true",
            (result as EngineResult.Success).value,
        )
        val formatted = runtime.evaluate(source, "java.timeFormat('0')")
        assertTrue((formatted as EngineResult.Success).value.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
        runtime.clearSourceState(source.id)
    }

    @Test
    fun javaGetWithOneRelativeUrlArgumentStillPerformsHttpGet() = runBlocking {
        val source = BookSource(
            id = "https://java-get-overload.example/",
            name = "Java get overload",
            baseUrl = "https://java-get-overload.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceGet = { requestedSource, baseUrl, url, headers ->
                assertEquals(source.id, requestedSource.id)
                assertEquals(source.baseUrl, baseUrl)
                assertEquals("/chapter/1", url)
                assertTrue(headers.isEmpty())
                app.badcat.engine.SourceHttpResponse(200, "https://java-get-overload.example/chapter/1", body = "chapter body")
            },
        )
        val result = runtime.evaluate(
            source,
            "java.get('/chapter/1').body()",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertEquals("chapter body", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun formatsLegadoHtmlWhileKeepingAbsoluteImageLinks() = runBlocking {
        val source = BookSource(
            id = "https://html-format.example/",
            name = "HTML format",
            baseUrl = "https://html-format.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "java.htmlFormat('<div>Hello&nbsp;<b>world</b></div><p>Second</p><img data-src=\\\"/cover.jpg\\\">', 'https://reader.example/book/1')",
        )
        assertTrue("Legado htmlFormat failed: $result", result is EngineResult.Success)
        val formatted = (result as EngineResult.Success).value
        assertTrue(formatted.contains("Hello world"))
        assertTrue(formatted.contains("Second"))
        assertTrue(formatted.contains("<img src=\"https://reader.example/cover.jpg\">"))
        assertTrue(!formatted.contains("<div") && !formatted.contains("<b"))
        runtime.clearSourceState(source.id)
    }

    @Test
    fun htmlFormatPrefersLazyImageUrlAndDropsUnsafeImageSchemes() = runBlocking {
        val source = BookSource(
            id = "https://html-images.example/",
            name = "HTML image safety",
            baseUrl = "https://html-images.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """java.htmlFormat('<img src="/placeholder.gif" data-src="../real.jpg"><img src="javascript:alert(1)"><img src="file:///etc/passwd">', 'https://reader.example/book/1')""",
        )
        assertTrue("Legado htmlFormat image handling failed: $result", result is EngineResult.Success)
        val formatted = (result as EngineResult.Success).value
        assertTrue(formatted.contains("<img src=\"https://reader.example/real.jpg\">"))
        assertTrue(!formatted.contains("placeholder.gif"))
        assertTrue(!formatted.contains("javascript:") && !formatted.contains("file:"))
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoSourceConcurrencyHelpers() = runBlocking {
        val source = BookSource(
            id = "https://source-lock.example/",
            name = "Source lock",
            baseUrl = "https://source-lock.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var calls = 0;
                java.singleFlight('once', function() { calls++; });
                java.singleFlight('once', function() { calls++; });
                java.lock('mutex', function() { calls += 10; });
                java.lock('mutex', function() { calls += 10; });
                [calls, java.tick('counter'), java.tick('counter'), java.tick('counter')].join('|');
            """.trimIndent(),
        )
        assertTrue("Legado source concurrency helpers failed: $result", result is EngineResult.Success)
        assertEquals("21|0|1|2", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoStringByteAndHexOverloads() = runBlocking {
        val source = BookSource(
            id = "https://encoding-overloads.example/",
            name = "Encoding overloads",
            baseUrl = "https://encoding-overloads.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                [
                    java.bytesToStr(java.strToBytes('你好', 'UTF-8'), 'UTF-8'),
                    java.hexEncodeToString('Legado'),
                    java.hexDecodeToString('4c656761646f'),
                    (function(){ try { java.hexDecodeToString('4c 65'); return 'no-error'; } catch(e) { return 'throws'; } })(),
                    java.base64DecodeToByteArray(null) === null ? 'null' : 'not-null',
                    java.base64DecodeToByteArray('SGVsbG8=')[0]
                ].join('|');
            """.trimIndent(),
        )
        assertTrue("Legado encoding overloads failed: $result", result is EngineResult.Success)
        assertEquals("你好|4c656761646f|Legado|throws|null|72", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoJsUrlPropertiesAndRelativeResolution() = runBlocking {
        val source = BookSource(
            id = "https://js-url.example/",
            name = "JsURL compatibility",
            baseUrl = "https://js-url.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var u = java.toURL('../chapter?id=42&name=%E4%BD%A0%E5%A5%BD', 'https://reader.example/book/index');
                [
                    u.host,
                    u.origin,
                    u.pathname,
                    u.searchParams.get('id'),
                    u.searchParams.get('name'),
                    java.toURL('https://plain.example/a', null).origin
                ].join('|');
            """.trimIndent(),
        )
        assertTrue("Legado JsURL compatibility failed: $result", result is EngineResult.Success)
        assertEquals(
            "reader.example|https://reader.example|/chapter|42|你好|https://plain.example",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoBase64FlagsAndCharsetOverloads() = runBlocking {
        val source = BookSource(
            id = "https://base64-flags.example/",
            name = "Base64 flags",
            baseUrl = "https://base64-flags.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                [
                    java.base64Encode('你好', 10),
                    java.base64Encode('a', 0).replace('\n', '!'),
                    java.base64Decode('5L2g5aW9', 'UTF-8'),
                    java.base64Decode('V29y\n bGQ=', 0),
                    java.bytesToStr(java.base64DecodeToByteArray('SGVsbG8=', 0))
                ].join('|');
            """.trimIndent(),
        )
        assertTrue("Legado Base64 overloads failed: $result", result is EngineResult.Success)
        assertEquals("5L2g5aW9|YQ==!|你好|World|Hello", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoHexAndUtcTimeHelpers() = runBlocking {
        val source = BookSource(
            id = "https://utility-helpers.example/",
            name = "Utility helpers",
            baseUrl = "https://utility-helpers.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "java.hexEncodeToString('AB') + '|' + java.bytesToStr(java.hexDecodeToByteArray('4142')) + '|' + java.timeFormatUTC(0, 'yyyy-MM-dd HH:mm', 28800000) + '|' + java.encodeURI('a b/雪') + '|' + java.encodeURI('a b/雪', 'UTF-8')",
        )
        assertEquals("4142|AB|1970-01-01 08:00|a+b%2F%E9%9B%AA|a+b%2F%E9%9B%AA", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsBoundedLegadoAjaxAllArrayRequests() = runBlocking {
        val source = BookSource(
            id = "https://ajax-all.example/",
            name = "Ajax all",
            baseUrl = "https://ajax-all.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, _, method, url, _, _ ->
                assertEquals("GET", method)
                app.badcat.engine.SourceHttpResponse(200, "https://ajax-all.example$url", body = "body:$url")
            },
        )
        val result = runtime.evaluate(
            source,
            "var responses = java.ajaxAll(['/one', '/two']); responses.length + '|' + responses[0].body() + '|' + responses[1].code() + '|' + responses[1].url()",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertEquals(
            "2|body:/one|200|https://ajax-all.example/two",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }

    @Test
    fun javaGetStringExtractsFromTheCurrentJsonDocument() = runBlocking {
        val source = BookSource(
            id = "https://json-get-string.example/",
            name = "JSON getString",
            baseUrl = "https://json-get-string.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "java.getString('$.update_time') + '|' + java.getString('$.id')",
            mapOf(
                "result" to """{"update_time":123456,"id":"book-7"}""",
                "baseUrl" to source.baseUrl,
            ),
        )
        assertEquals("123456|book-7", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun exposesLegadoCookieGetKeySetReplaceAndRemoveHelpers() = runBlocking {
        val source = BookSource(
            id = "https://cookie-mutation.example/",
            name = "Cookie mutation",
            baseUrl = "https://cookie-mutation.example/",
        )
        val writes = mutableListOf<String>()
        val replacements = mutableListOf<String>()
        val removals = mutableListOf<String>()
        var cookieHeader = "sid=old; token=x=y"
        val runtime = RhinoSourceScriptRuntime(
            sourceCookieReader = { sourceId, url ->
                assertEquals(source.id, sourceId)
                assertEquals("https://cookie-mutation.example/book", url)
                cookieHeader
            },
            sourceCookieWriter = { sourceId, url, value ->
                assertEquals(source.id, sourceId)
                writes += "$url|$value"
                cookieHeader = value
            },
            sourceCookieReplacer = { sourceId, url, value ->
                assertEquals(source.id, sourceId)
                replacements += "$url|$value"
                cookieHeader = value
            },
            sourceCookieRemover = { sourceId, url ->
                assertEquals(source.id, sourceId)
                removals += url
                cookieHeader = ""
            },
        )
        val result = runtime.evaluate(
            source,
            """cookie.setCookie('https://cookie-mutation.example/book', 'sid=new'); cookie.getKey('https://cookie-mutation.example/book', 'sid')""",
        )
        assertEquals("new", (result as EngineResult.Success).value)
        val replaced = runtime.evaluate(
            source,
            """cookie.replaceCookie('https://cookie-mutation.example/book', 'sid=replaced'); cookie.getCookie('https://cookie-mutation.example/book')""",
        )
        assertEquals("sid=replaced", (replaced as EngineResult.Success).value)
        runtime.evaluate(source, """cookie.removeCookie('https://cookie-mutation.example/book')""")
        assertEquals(listOf("https://cookie-mutation.example/book|sid=new"), writes)
        assertEquals(listOf("https://cookie-mutation.example/book|sid=replaced"), replacements)
        assertEquals(listOf("https://cookie-mutation.example/book"), removals)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun exposesSourceScopedLegadoCookieLookupHelpers() = runBlocking {
        val sourceId = "https://cookie-helper.example/"
        val runtime = RhinoSourceScriptRuntime(
            sourceCookieReader = { requestedSource, url ->
                if (requestedSource == sourceId && url.startsWith("https://cookie-helper.example/")) {
                    "sid=abc; token=x=y"
                } else {
                    ""
                }
            },
        )
        val source = BookSource(sourceId, "Cookie helper source", "https://cookie-helper.example/")

        val result = runtime.evaluate(
            source,
            "java.getCookie('https://cookie-helper.example/book') + '|' + " +
                "java.getCookie('https://cookie-helper.example/book', null) + '|' + " +
                "java.getCookie('https://cookie-helper.example/book', 'sid') + '|' + " +
                "java.getCookie('https://cookie-helper.example/book', 'token')",
        )

        assertEquals("sid=abc; token=x=y|sid=abc; token=x=y|abc|x=y", (result as EngineResult.Success).value)
        val otherSource = BookSource(
            "https://other-cookie-helper.example/",
            "Other cookie helper source",
            "https://other-cookie-helper.example/",
        )
        val isolated = runtime.evaluate(
            otherSource,
            "java.getCookie('https://cookie-helper.example/book', null)",
        )
        assertEquals("", (isolated as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
        runtime.clearSourceState(otherSource.id)
    }

    @Test
    fun rejectsOversizedSourceJsLibBeforeEvaluation() = runBlocking {
        val source = BookSource(
            id = "https://oversized.example/",
            name = "Oversized source",
            baseUrl = "https://oversized.example/",
            jsLib = " ".repeat(128 * 1024 + 1),
        )

        val result = RhinoSourceScriptRuntime().evaluate(source, "'should not run'")

        assertTrue(result is EngineResult.Failure)
    }
    @Test
    fun serializesConcurrentEvaluationForOneSource() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://concurrent.example/",
            name = "Concurrent source",
            baseUrl = "https://concurrent.example/",
            jsLib = "const concurrentPrefix = 'ok'; function concurrentValue(n) { return concurrentPrefix + n; }",
        )

        val results = coroutineScope {
            (1..12).map { index ->
                async(Dispatchers.Default) {
                    runtime.evaluate(source, "concurrentValue('$index')")
                }
            }.map { it.await() }
        }

        assertEquals(12, results.size)
        assertTrue(results.all { it is EngineResult.Success })
        runtime.clearSourceState(source.id)
    }

    @Test
    fun initializesLexicalJsLibDeclarationsOnlyOnce() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://lexical.example/",
            name = "Lexical source",
            baseUrl = "https://lexical.example/",
            jsLib = "const fixturePrefix = 'hello '; function lexicalGreeting(name) { return fixturePrefix + name; }",
        )

        val first = runtime.evaluate(source, "lexicalGreeting('one')")
        val second = runtime.evaluate(source, "lexicalGreeting('two')")

        assertEquals("hello one", (first as EngineResult.Success).value)
        assertEquals("hello two", (second as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }
    @Test
    fun clearsSourceGlobalsUnderTheEvaluationLock() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://clear-state.example/",
            name = "Clear state source",
            baseUrl = "https://clear-state.example/",
        )

        val initialized = runtime.evaluate(source, "var temporaryValue = 'kept'; temporaryValue")
        val beforeClear = runtime.evaluate(source, "temporaryValue")
        runtime.clearSourceState(source.id)
        val afterClear = runtime.evaluate(source, "typeof temporaryValue")

        assertEquals("kept", (initialized as EngineResult.Success).value)
        assertEquals("kept", (beforeClear as EngineResult.Success).value)
        assertEquals("undefined", (afterClear as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun resetsLexicalScopeWhenSourceJsLibChanges() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val sourceV1 = BookSource(
            id = "https://edited-source.example/",
            name = "Edited source",
            baseUrl = "https://edited-source.example/",
            jsLib = "const editedPrefix = 'old'; function editedValue() { return editedPrefix; }",
        )
        val sourceV2 = sourceV1.copy(
            jsLib = "const editedPrefix = 'new'; function editedValue() { return editedPrefix; }",
        )

        val beforeEdit = runtime.evaluate(sourceV1, "editedValue()")
        val afterEdit = runtime.evaluate(sourceV2, "editedValue()")

        assertEquals("old", (beforeEdit as EngineResult.Success).value)
        assertEquals("new", (afterEdit as EngineResult.Success).value)
        runtime.clearSourceState(sourceV1.id)
    }


    @Test
    fun initializesMainJsOnceBeforeSourceRules() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://main-js.example/",
            name = "Main JS source",
            baseUrl = "https://main-js.example/",
            mainJs = "var mainJsRuns = 0; mainJsRuns++; function mainJsValue() { return mainJsRuns; }",
        )

        val first = runtime.evaluate(source, "mainJsValue()")
        val second = runtime.evaluate(source, "mainJsValue()")

        assertEquals("1", (first as EngineResult.Success).value)
        assertEquals("1", (second as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun resetsSourceScopeWhenMainJsChanges() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val sourceV1 = BookSource(
            id = "https://main-js-edit.example/",
            name = "Main JS edit source",
            baseUrl = "https://main-js-edit.example/",
            mainJs = "const mainJsLabel = 'old'; function readMainJsLabel() { return mainJsLabel; }",
        )
        val sourceV2 = sourceV1.copy(
            mainJs = "const mainJsLabel = 'new'; function readMainJsLabel() { return mainJsLabel; }",
        )

        val first = runtime.evaluate(sourceV1, "readMainJsLabel()")
        val second = runtime.evaluate(sourceV2, "readMainJsLabel()")

        assertEquals("old", (first as EngineResult.Success).value)
        assertEquals("new", (second as EngineResult.Success).value)
        runtime.clearSourceState(sourceV1.id)
    }
    @Test
    fun persistsSourceVariablesPerBookSourceAndClearsThemWithSourceState() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val first = BookSource(
            id = "https://variables-one.example/",
            name = "Variables one",
            baseUrl = "https://variables-one.example/",
        )
        val second = BookSource(
            id = "https://variables-two.example/",
            name = "Variables two",
            baseUrl = "https://variables-two.example/",
        )

        assertTrue(runtime.putString(first, "session", "one") is EngineResult.Success)
        val firstValue = runtime.getString(first, "session")
        val secondValue = runtime.getString(second, "session")
        assertTrue(firstValue is EngineResult.Success)
        assertTrue(secondValue is EngineResult.Success)
        assertEquals("one", (firstValue as EngineResult.Success<String?>).value)
        assertEquals(null, (secondValue as EngineResult.Success<String?>).value)

        runtime.clearSourceState(first.id)
        val clearedValue = runtime.getString(first, "session")
        assertTrue(clearedValue is EngineResult.Success)
        assertEquals(null, (clearedValue as EngineResult.Success<String?>).value)
    }


    @Test
    fun rejectsOversizedMainJsBeforeEvaluation() = runBlocking {
        val source = BookSource(
            id = "https://oversized-main-js.example/",
            name = "Oversized mainJs source",
            baseUrl = "https://oversized-main-js.example/",
            mainJs = " ".repeat(128 * 1024 + 1),
        )

        val result = RhinoSourceScriptRuntime().evaluate(source, "'must not run'")

        assertTrue(result is EngineResult.Failure)
    }


    @Test
    fun supportsRestrictedJavaImporterAesCbcDecryption() = runBlocking {
        val source = BookSource(
            id = "https://aes-compat.example/",
            name = "AES compatibility",
            baseUrl = "https://aes-compat.example/",
        )
        val key = "0123456789abcdef".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(16) { it.toByte() }
        val plaintext = "Legado AES fixture".toByteArray(Charsets.UTF_8)
        val encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptor.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val payload = iv + encryptor.doFinal(plaintext)
        val encoded = Base64.getEncoder().encodeToString(payload)
        val runtime = RhinoSourceScriptRuntime()
        val script = """
            var javaImport = new JavaImporter();
            javaImport.importPackage(Packages.java.lang, Packages.javax.crypto.spec, Packages.javax.crypto, Packages.java.util);
            with (javaImport) {
                var ivEncData = Base64.getDecoder().decode('$encoded');
                var secret = new SecretKeySpec(String('0123456789abcdef').getBytes(), 'AES');
                var vector = new IvParameterSpec(Arrays.copyOfRange(ivEncData, 0, 16));
                var cipher = Cipher.getInstance('AES/CBC/PKCS5Padding');
                cipher.init(2, secret, vector);
                String(cipher.doFinal(Arrays.copyOfRange(ivEncData, 16, ivEncData.length)));
            }
        """.trimIndent()

        val shimCheck = runtime.evaluate(
            source,
            "typeof Packages + '|' + typeof JavaImporter + '|' + typeof String.prototype.getBytes + '|' + typeof Packages.javax + '|' + typeof Packages.javax.crypto.spec",
        )
        assertTrue("Restricted JavaImporter shim unavailable: $shimCheck", shimCheck is EngineResult.Success)
        assertEquals("object|function|function|object|object", (shimCheck as EngineResult.Success).value)

        val result = runtime.evaluate(source, script)

        assertTrue("Restricted AES script failed: $result", result is EngineResult.Success)
        assertEquals("Legado AES fixture", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun exposesLegadoChineseConversionHelpersToSourceScripts() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://chinese-conversion.example/",
            name = "Chinese conversion source",
            baseUrl = "https://chinese-conversion.example/",
        )

        val result = runtime.evaluate(
            source,
            "java.t2s('繁體中文') + '|' + java.s2t('繁体中文')",
        )

        assertEquals("繁体中文|繁體中文", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun exposesLegadoGetStringListAndGetElementHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://element-helpers.example/",
            name = "Element helpers",
            baseUrl = "https://element-helpers.example/",
        )
        val html = """<ul><li>First</li><li>Second</li></ul>"""
        val result = runtime.evaluate(
            source,
            "java.getStringList('ul li@text').map(function(v) { return String(v); }).join('|') + '|' + java.getElement('ul li.0').text() + '|' + java.getElement('ul li.1').toString()",
            mapOf("result" to html, "baseUrl" to source.baseUrl),
        )

        assertTrue("Legado element helpers failed: $result", result is EngineResult.Success)
        assertEquals("First|Second|First|<li>Second</li>", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun exposesLegadoGetElementsListAndIndexedAccess() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://elements-helpers.example/",
            name = "Element collection helpers",
            baseUrl = "https://elements-helpers.example/",
        )
        val html = """<ul><li>First</li><li>Second</li></ul>"""
        val result = runtime.evaluate(
            source,
            "var items = java.getElements('ul li'); items.size() + '|' + items.get(1).text() + '|' + items.toArray()[0].text() + '|' + items[0].text() + '|' + items.length",
            mapOf("result" to html, "baseUrl" to source.baseUrl),
        )

        assertTrue("Legado getElements failed: $result", result is EngineResult.Success)
        assertEquals("2|Second|First|First|2", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun returnsBoundedJavaScriptAndLegadoElementArraysAsHtmlFragments() = runBlocking {
        val source = BookSource(
            id = "https://array-results.example/",
            name = "Array results",
            baseUrl = "https://array-results.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val html = "<ul><li>First</li><li>Second</li></ul>"
        val elements = runtime.evaluate(
            source,
            "java.getElements('ul li')",
            mapOf("result" to html, "baseUrl" to source.baseUrl),
        )
        val elementHtml = (elements as EngineResult.Success).value
        assertTrue(elementHtml.contains("<li>First</li>"))
        assertTrue(elementHtml.contains("<li>Second</li>"))

        val nativeArray = runtime.evaluate(
            source,
            "['<li>Alpha</li>', '<li>Beta</li>']",
        )
        val arrayHtml = (nativeArray as EngineResult.Success).value
        assertTrue(arrayHtml.contains("<li>Alpha</li>"))
        assertTrue(arrayHtml.contains("<li>Beta</li>"))
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoAesBase64AndSymmetricCryptoHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://legacy-crypto.example/",
            name = "Legacy crypto",
            baseUrl = "https://legacy-crypto.example/",
        )
        val key = "0123456789abcdef".toByteArray(Charsets.UTF_8)
        val iv = "abcdef0123456789".toByteArray(Charsets.UTF_8)
        val plaintext = "Encrypted chapter payload".toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext))
        val script = "java.aesBase64DecodeToString('$encrypted','0123456789abcdef','AES/CBC/PKCS7Padding','abcdef0123456789') + '|' + java.toURL(baseUrl).host"
        val result = runtime.evaluate(source, script, mapOf("baseUrl" to source.baseUrl))

        assertTrue("Legado crypto helper failed: $result", result is EngineResult.Success)
        assertEquals("Encrypted chapter payload|legacy-crypto.example", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoJavaGetHttpOverloadWithHeadersAndResponseAccessors() = runBlocking {
        val captured = mutableMapOf<String, String>()
        val runtime = RhinoSourceScriptRuntime(
            sourceGet = { _, _, url, headers ->
                captured["url"] = url
                captured.putAll(headers)
                app.badcat.engine.SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    headers = mapOf("Location" to listOf("/redirected")),
                    body = "GET response body",
                )
            },
        )
        val source = BookSource(
            id = "https://http-get.example/",
            name = "HTTP GET helper",
            baseUrl = "https://http-get.example/",
        )

        val result = runtime.evaluate(
            source,
            "var response = java.get('/resource', {'User-Agent':'Legado fixture'}); response.body() + '|' + response.code() + '|' + response.headers('Location')",
            mapOf("baseUrl" to source.baseUrl),
        )

        assertTrue("Legado java.get overload failed: $result", result is EngineResult.Success)
        assertEquals("GET response body|200|/redirected", (result as EngineResult.Success).value)
        assertEquals("/resource", captured["url"])
        assertEquals("Legado fixture", captured["User-Agent"])
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoGetStringWithExplicitContent() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://explicit-content.example/",
            name = "Explicit content selector",
            baseUrl = "https://explicit-content.example/",
        )
        val result = runtime.evaluate(
            source,
            "java.getString('a@text', '<a>Explicit fragment</a>')",
        )

        assertTrue("Explicit-content getString failed: $result", result is EngineResult.Success)
        assertEquals("Explicit fragment", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoSymmetricCryptoBase64AndHexEncryptionHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://symmetric-encrypt.example/",
            name = "Symmetric encryption compatibility",
            baseUrl = "https://symmetric-encrypt.example/",
        )
        val result = runtime.evaluate(
            source,
            """
                var crypto = java.createSymmetricCrypto(
                    'AES/CBC/PKCS5Padding',
                    '0123456789abcdef',
                    'abcdef0123456789'
                );
                var base64Cipher = crypto.encryptBase64('Legado crypto roundtrip');
                var hexCipher = crypto.encryptHex('Legado crypto roundtrip');
                crypto.decryptStr(base64Cipher) + '|' +
                    crypto.decryptStr(hexCipher) + '|' +
                    (crypto.decrypt(base64Cipher).length > 0) + '|' +
                    (crypto.decrypt(hexCipher).length > 0);
            """.trimIndent(),
        )

        assertTrue("Legado symmetric encrypt helpers failed: $result", result is EngineResult.Success)
        assertEquals("Unexpected crypto roundtrip result: $result", "Legado crypto roundtrip|Legado crypto roundtrip|true|true", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoToNumChapterNumberNormalization() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://to-num-chapter.example/",
            name = "toNumChapter compatibility",
            baseUrl = "https://to-num-chapter.example/",
        )
        val result = runtime.evaluate(
            source,
            """
                [
                    java.toNumChapter('第12章'),
                    java.toNumChapter('第１２章'),
                    java.toNumChapter('第一千零二十五章'),
                    java.toNumChapter('第两百三十章'),
                    java.toNumChapter('前缀第十二章后缀'),
                    java.toNumChapter('第壹佰贰拾叁章'),
                    java.toNumChapter('第abc章'),
                    java.toNumChapter('序章'),
                    java.toNumChapter(null)
                ].join('|');
            """.trimIndent(),
        )

        assertTrue("Legado toNumChapter failed: $result", result is EngineResult.Success)
        assertEquals("第12章|第12章|第1025章|第230章|第12章|第123章|第-1章|序章|", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoDesCbcDecryptionHelper() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://des-crypto.example/",
            name = "DES compatibility",
            baseUrl = "https://des-crypto.example/",
        )
        val key = "12345678".toByteArray(Charsets.UTF_8)
        val iv = "abcdefgh".toByteArray(Charsets.UTF_8)
        val plaintext = "DES encrypted chapter".toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "DES"), IvParameterSpec(iv))
        val encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext))
        val result = runtime.evaluate(
            source,
            "java.createSymmetricCrypto('DES/CBC/PKCS5Padding','12345678','abcdefgh').decryptStr(java.base64DecodeToByteArray('$encrypted'))",
        )

        assertTrue("Legado DES helper failed: $result", result is EngineResult.Success)
        assertEquals("DES encrypted chapter", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsSourceScopedCacheAndCommonStringHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime(
            sourceAjax = { _, _, url -> if (url == "/helper.js") "({value:'imported'})" else "" },
        )
        val first = BookSource(
            id = "https://cache-one.example/",
            name = "Cache one",
            baseUrl = "https://cache-one.example/",
        )
        val second = first.copy(id = "https://cache-two.example/", baseUrl = "https://cache-two.example/")
        val firstResult = runtime.evaluate(
            first,
            "cache.putMemory('token', {value:'cached'}); cache.getFromMemory('token').value + '|' + java.hexDecodeToString('48656c6c6f') + '|' + java.bytesToStr(java.base64DecodeToByteArray('V29ybGQ=')) + '|' + java.importScript('/helper.js')",
        )
        val secondResult = runtime.evaluate(second, "String(cache.getFromMemory('token'))")

        assertTrue("Source cache/string helpers failed: $firstResult", firstResult is EngineResult.Success)
        assertEquals("cached|Hello|World|({value:'imported'})", (firstResult as EngineResult.Success).value)
        assertTrue(secondResult is EngineResult.Success)
        assertEquals("null", (secondResult as EngineResult.Success).value)
        runtime.clearSourceState(first.id)
        runtime.clearSourceState(second.id)
    }


    @Test
    fun supportsLegadoSelectorHelpersWithExplicitContentAndOneArgumentHttpGet() = runBlocking {
        val runtime = RhinoSourceScriptRuntime(
            sourceGet = { _, _, url, _ ->
                app.badcat.engine.SourceHttpResponse(200, url, body = "one argument GET")
            },
        )
        val source = BookSource(
            id = "https://helper-overloads.example/",
            name = "Helper overloads",
            baseUrl = "https://helper-overloads.example/",
        )
        val result = runtime.evaluate(
            source,
            "java.getStringList('li@text', '<ul><li>A</li><li>B</li></ul>').join('|') + '|' + java.getElement('li.1', '<ul><li>A</li><li>B</li></ul>').text() + '|' + java.getElements('li', '<ul><li>A</li><li>B</li></ul>').get(0).text() + '|' + java.get('/plain').body()",
            mapOf("baseUrl" to source.baseUrl),
        )

        assertTrue("Legado helper overloads failed: $result", result is EngineResult.Success)
        assertEquals("A|B|B|A|one argument GET", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsLegadoJsoupGetResponseSelectionAndParsing() = runBlocking {
        val runtime = RhinoSourceScriptRuntime(
            sourceGet = { _, _, url, headers ->
                assertEquals("X-Test", headers.keys.single())
                app.badcat.engine.SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "<div class='page-search' data-id='book-42'><span>Found</span></div>",
                )
            },
        )
        val source = BookSource(
            id = "https://jsoup-get.example/",
            name = "Jsoup GET response",
            baseUrl = "https://jsoup-get.example/",
        )
        val result = runtime.evaluate(
            source,
            "var res = java.get('/search', {'X-Test':'yes'}); res.select('.page-search').attr('data-id') + '|' + res.parse().select('.page-search').text() + '|' + res.statusCode() + '|' + res.header('Content-Type')",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado Jsoup response helpers failed: $result", result is EngineResult.Success)
        assertEquals("book-42|Found|200|", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }
    @Test
    fun supportsLegadoHeadPostAndConnectHttpHelpers() = runBlocking {
        val seen = mutableListOf<String>()
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, _, method, url, body, headers ->
                seen += "$method|$url|${body.orEmpty()}|${headers["X-Token"].orEmpty()}"
                app.badcat.engine.SourceHttpResponse(200, url, body = if (method == "HEAD") "" else "${method} response")
            },
        )
        val source = BookSource(
            id = "https://http-methods.example/",
            name = "HTTP methods",
            baseUrl = "https://http-methods.example/",
        )
        val result = runtime.evaluate(
            source,
            "java.head('/head', {'X-Token':'h'}).statusCode() + '|' + java.post('/submit', 'payload', {'X-Token':'p'}).body() + '|' + java.connect('/connect').body()",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado HTTP method helpers failed: $result", result is EngineResult.Success)
        assertEquals("200|POST response|GET response", (result as EngineResult.Success).value)
        assertEquals(
            listOf(
                "HEAD|/head||h",
                "POST|/submit|payload|p",
                "GET|/connect||",
            ),
            seen,
        )
        runtime.clearSourceState(source.id)
    }
    @Test
    fun supportsCommonJsoupElementNavigationAndAttributes() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://element-navigation.example/",
            name = "Element navigation",
            baseUrl = "https://element-navigation.example/",
        )
        val html = "<ul><li class='chapter' id='one'><a href='/one'>One</a></li><li class='chapter' id='two'><a href='/two'>Two</a></li></ul>"
        val result = runtime.evaluate(
            source,
            "var items = java.getElements('li.chapter'); items.first().id() + '|' + items.last().selectFirst('a').absUrl('href') + '|' + items.get(0).hasClass('chapter') + '|' + items.get(1).children().get(0).tagName() + '|' + items.get(0).children().size() + '|' + items.first().next().id() + '|' + items.last().prev().id() + '|' + java.getElement('ul').select('li.chapter').first().parent().tagName() + '|' + java.getElement('ul').select('li.chapter').first().siblings().size()",
            mapOf("result" to html, "baseUrl" to source.baseUrl),
        )
        assertTrue("Jsoup element navigation failed: $result", result is EngineResult.Success)
        assertEquals("one|https://element-navigation.example/two|true|a|1|two|one|ul|1", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }
    @Test
    fun exposesTheConfiguredDeviceWebViewUserAgent() = runBlocking {
        val source = BookSource(
            id = "https://webview-ua.example/",
            name = "WebView UA",
            baseUrl = "https://webview-ua.example/",
        )
        val runtime = RhinoSourceScriptRuntime(webViewUserAgent = "Badcat-WebView-UA")
        val result = runtime.evaluate(source, "java.getWebViewUA()")
        assertEquals("Badcat-WebView-UA", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun allowsNormalSourceAjaxLatencyWithinTheBoundedScriptTimeout() = runBlocking {
        val runtime = RhinoSourceScriptRuntime(
            sourceAjax = { _, _, _ ->
                Thread.sleep(1_800)
                "slow but successful response"
            },
        )
        val source = BookSource(
            id = "https://slow-ajax.example/",
            name = "Slow ajax",
            baseUrl = "https://slow-ajax.example/",
        )
        val result = runtime.evaluate(source, "java.ajax('/slow')", mapOf("baseUrl" to source.baseUrl))
        assertTrue("Normal source HTTP latency should not exceed the JS execution budget: $result", result is EngineResult.Success)
        assertEquals("slow but successful response", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun exposesLegadoResponseMessageHeadersAndSafeRawRequestMetadata() = runBlocking {
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, _, _, url, _, _ ->
                app.badcat.engine.SourceHttpResponse(
                    statusCode = 206,
                    finalUrl = "https://response.example/final",
                    headers = mapOf("X-Fixture" to listOf("one", "two")),
                    body = "partial",
                    statusMessage = "Partial Content",
                )
            },
        )
        val source = BookSource(
            id = "https://response.example/",
            name = "Response facade",
            baseUrl = "https://response.example/",
        )
        val result = runtime.evaluate(
            source,
            "var r = java.get('/start'); r.message() + '|' + r.headers().get('X-Fixture')[0] + '|' + r.headers('X-Fixture') + '|' + r.raw().request().url() + '|' + r.raw().code()",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado response metadata failed: $result", result is EngineResult.Success)
        assertEquals(
            "Partial Content|one|one|https://response.example/final|206",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }

    @Test
    fun ajaxAllUsesBoundedParallelRequestsAndPreservesInputOrder() = runBlocking {
        val active = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, _, _, url, _, _ ->
                val now = active.incrementAndGet()
                peak.updateAndGet { previous -> maxOf(previous, now) }
                try {
                    Thread.sleep(120)
                    app.badcat.engine.SourceHttpResponse(
                        statusCode = 200,
                        finalUrl = url,
                        body = url,
                    )
                } finally {
                    active.decrementAndGet()
                }
            },
        )
        val source = BookSource(
            id = "https://ajax-all.example/",
            name = "Ajax all",
            baseUrl = "https://ajax-all.example/",
        )
        val result = runtime.evaluate(
            source,
            "java.ajaxAll(['/one','/two','/three','/four']).map(function(r) { return r.body(); }).join('|')",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado ajaxAll failed: $result", result is EngineResult.Success)
        assertEquals("/one|/two|/three|/four", (result as EngineResult.Success).value)
        assertTrue("ajaxAll should issue requests concurrently; peak=${peak.get()}", peak.get() in 2..4)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoDigestAndHmacHelpersWithKnownVectors() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://digest-helpers.example/",
            name = "Digest helpers",
            baseUrl = "https://digest-helpers.example/",
        )
        val result = runtime.evaluate(
            source,
            "java.digestHex('abc','SHA-256') + '|' + java.digestBase64Str('abc','SHA-256') + '|' + java.HMacHex('The quick brown fox jumps over the lazy dog','HmacSHA256','key') + '|' + java.HMacBase64('The quick brown fox jumps over the lazy dog','HmacSHA256','key')",
        )
        assertTrue("Legado digest/HMAC helpers failed: $result", result is EngineResult.Success)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad|" +
                "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=|" +
                "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8|" +
                "97yD9DBThCSxMpjmqm+xQ+9NWaFJRhdZl0edvC0aPNg=",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }


    @Test
    fun mapsLegadoPersistentCacheAndMemoryCacheToSeparateStores() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://cache-compat.example/",
            name = "Cache compatibility",
            baseUrl = "https://cache-compat.example/",
        )
        val first = runtime.evaluate(
            source,
            "cache.put('durable','persisted',60); cache.putMemory('volatile','memory'); " +
                "cache.get('durable') + '|' + cache.getFromMemory('volatile') + '|' + (cache.get('volatile') == null)",
        )
        assertTrue("Legado cache.put/get compatibility failed: $first", first is EngineResult.Success)
        assertEquals("persisted|memory|true", (first as EngineResult.Success).value)

        val second = runtime.evaluate(
            source,
            "cache.get('durable') + '|' + cache.getFromMemory('volatile')",
        )
        assertTrue(second is EngineResult.Success)
        assertEquals("persisted|memory", (second as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoPersistentFileCacheAcrossEvaluations() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://cache-file-compat.example/",
            name = "Cache file compatibility",
            baseUrl = "https://cache-file-compat.example/",
        )
        val first = runtime.evaluate(
            source,
            "cache.putFile('large-data','file-backed-content',60); cache.getFile('large-data')",
        )
        assertTrue("Legado cache.putFile/getFile failed: $first", first is EngineResult.Success)
        assertEquals("file-backed-content", (first as EngineResult.Success).value)

        val second = runtime.evaluate(source, "cache.getFile('large-data')")
        assertTrue(second is EngineResult.Success)
        assertEquals("file-backed-content", (second as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun resolvesRelativeUrlsThroughLegadoToUrlBaseOverload() = runBlocking {
        val source = BookSource(
            id = "https://to-url-overload.example/",
            name = "toURL base overload",
            baseUrl = "https://to-url-overload.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "var u = java.toURL('../chapter/2?x=1', 'https://reader.example/book/1'); " +
                "u.getProtocol() + '|' + u.getHost() + '|' + u.getPath() + '|' + u.getQuery()",
        )
        assertTrue("Legado java.toURL(url, baseUrl) failed: $result", result is EngineResult.Success)
        assertEquals(
            "https|reader.example|/chapter/2|x=1",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsLegadoGetStringThreeArgumentUrlOverload() = runBlocking {
        val source = BookSource(
            id = "https://get-string-url.example/",
            name = "getString URL overload",
            baseUrl = "https://get-string-url.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, baseUrl, method, url, _, _ ->
                assertEquals(source.baseUrl, baseUrl)
                assertEquals("GET", method)
                assertEquals("/fragment", url)
                app.badcat.engine.SourceHttpResponse(200, "https://get-string-url.example/fragment", body = "<h1>Fetched title</h1><p>Body</p>")
            },
        )
        val result = runtime.evaluate(
            source,
            "java.getString('h1@text', '/fragment', true) + '|' + java.getStringList('p@text', '/fragment', true).join('|')",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado three-argument getString overload failed: $result", result is EngineResult.Success)
        assertEquals("Fetched title|Body", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun setContentBaseUrlUpdatesRelativeElementResolution() = runBlocking {
        val source = BookSource(
            id = "https://set-content-base.example/",
            name = "setContent base URL",
            baseUrl = "https://set-content-base.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            "java.setContent('<a href=\\\"../chapter/2\\\">chapter</a>', 'https://reader.example/book/1'); java.getElement('a').absUrl('href')",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado setContent(content, baseUrl) failed: $result", result is EngineResult.Success)
        assertEquals("https://reader.example/chapter/2", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsSourceScopedLegadoTextDownloadReadAndDelete() = runBlocking {
        val store = InMemorySourceScriptFileCacheStore
        val source = BookSource(
            id = "https://download-file.example/",
            name = "Download file compatibility",
            baseUrl = "https://download-file.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceFileCacheStore = store,
            sourceHttpRequest = { requestedSource, baseUrl, method, url, _, _ ->
                assertEquals(source.id, requestedSource.id)
                assertEquals(source.baseUrl, baseUrl)
                assertEquals("GET", method)
                assertEquals("/chapter.txt", url)
                app.badcat.engine.SourceHttpResponse(
                    200,
                    "https://download-file.example/chapter.txt",
                    body = "downloaded chapter text",
                )
            },
        )
        val result = runtime.evaluate(
            source,
            "var downloadedPath = java.downloadFile('/chapter.txt'); " +
                "var downloadedText = java.readTxtFile(downloadedPath); " +
                "java.deleteFile(downloadedPath); " +
                "downloadedText + '|' + java.readTxtFile(downloadedPath)",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Legado text download/read/delete failed: $result", result is EngineResult.Success)
        assertEquals("downloaded chapter text|", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun downloadFileUsesTheRuntimeInjectedSourceFileStore() = runBlocking {
        val values = mutableMapOf<Pair<String, String>, String>()
        val writes = mutableListOf<Pair<String, String>>()
        val store = object : SourceScriptFileCacheStore {
            override fun get(sourceId: String, key: String): String? = values[sourceId to key]
            override fun put(sourceId: String, key: String, value: String) {
                writes += sourceId to key
                values[sourceId to key] = value
            }
            override fun remove(sourceId: String, key: String) {
                values.remove(sourceId to key)
            }
            override fun clearSource(sourceId: String) {
                values.keys.removeAll { it.first == sourceId }
            }
        }
        val source = BookSource(
            id = "https://injected-file-store.example/",
            name = "Injected file store",
            baseUrl = "https://injected-file-store.example/",
        )
        val runtime = RhinoSourceScriptRuntime(
            sourceFileCacheStore = store,
            sourceHttpRequest = { _, _, _, url, _, _ ->
                assertEquals("/text.txt", url)
                app.badcat.engine.SourceHttpResponse(200, "https://injected-file-store.example/text.txt", body = "from injected store")
            },
        )
        val result = runtime.evaluate(
            source,
            "var p = java.downloadFile('/text.txt'); java.readTxtFile(p)",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Download should use the injected file store: $result", result is EngineResult.Success)
        assertEquals("from injected store", (result as EngineResult.Success).value)
        assertEquals(1, writes.size)
        assertEquals(source.id, writes.single().first)
        assertTrue(writes.single().second.startsWith("__legado_download__:"))
        runtime.clearSourceState(source.id)
        assertTrue(values.isEmpty())
    }

    @Test
    fun setContentBaseUrlIsUsedBySubsequentRelativeHttpRequests() = runBlocking {
        val source = BookSource(
            id = "https://set-content-http-base.example/",
            name = "setContent HTTP base URL",
            baseUrl = "https://set-content-http-base.example/",
        )
        val seen = mutableListOf<Pair<String, String>>()
        val runtime = RhinoSourceScriptRuntime(
            sourceHttpRequest = { _, baseUrl, method, url, _, _ ->
                seen += baseUrl to "$method $url"
                app.badcat.engine.SourceHttpResponse(200, "https://reader.example/chapter", body = "chapter response")
            },
        )
        val result = runtime.evaluate(
            source,
            "java.setContent(\"<a href='../chapter'>chapter</a>\", 'https://reader.example/book/1'); java.get('/chapter').body()",
            mapOf("baseUrl" to source.baseUrl),
        )
        assertTrue("Relative HTTP request should use updated base URL: $result", result is EngineResult.Success)
        assertEquals("chapter response", (result as EngineResult.Success).value)
        assertEquals(listOf("https://reader.example/book/1" to "GET /chapter"), seen)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun rejectsTraversalAndNonIssuedLegadoDownloadPaths() = runBlocking {
        val source = BookSource(
            id = "https://download-path-safety.example/",
            name = "Download path safety",
            baseUrl = "https://download-path-safety.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val traversal = runtime.evaluate(source, "java.readTxtFile('../private.txt')")
        val absolute = runtime.evaluate(source, "java.deleteFile('/data/private.txt')")
        assertTrue("Traversal path must be rejected", traversal is EngineResult.Failure)
        assertTrue("Absolute path must be rejected", absolute is EngineResult.Failure)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun restrictedJavaImporterRejectsNonAllowListedCryptoAndJavaClasses() = runBlocking {
        val source = BookSource(
            id = "https://crypto-boundary.example/",
            name = "Crypto boundary",
            baseUrl = "https://crypto-boundary.example/",
        )
        val runtime = RhinoSourceScriptRuntime()

        val unsupportedCipher = runtime.evaluate(
            source,
            "Packages.javax.crypto.Cipher.getInstance('AES/GCM/NoPadding')",
        )
        val arbitraryJavaClass = runtime.evaluate(
            source,
            "Packages.java.lang.Runtime.getRuntime().availableProcessors()",
        )
        val unlistedPackage = runtime.evaluate(
            source,
            "typeof Packages.java.io",
        )

        assertTrue("Only AES/CBC/PKCS5Padding may be requested", unsupportedCipher is EngineResult.Failure)
        assertTrue("General Java classes must remain unavailable", arbitraryJavaClass is EngineResult.Failure)
        assertEquals("undefined", (unlistedPackage as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun restrictedAesImporterRejectsInvalidKeyAndIvLengths() = runBlocking {
        val source = BookSource(
            id = "https://aes-boundary.example/",
            name = "AES boundary",
            baseUrl = "https://aes-boundary.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val invalidKey = runtime.evaluate(
            source,
            """
                var importer = new JavaImporter();
                importer.importPackage(Packages.javax.crypto.spec, Packages.javax.crypto);
                with (importer) {
                    var key = new SecretKeySpec(java.utf8Encode('short'), 'AES');
                    var iv = new IvParameterSpec(java.utf8Encode('0123456789abcdef'));
                    var cipher = Cipher.getInstance('AES/CBC/PKCS5Padding');
                    cipher.init(2, key, iv);
                    cipher.doFinal(java.utf8Encode('data'));
                }
            """.trimIndent(),
        )
        val invalidIv = runtime.evaluate(
            source,
            """
                var importer = new JavaImporter();
                importer.importPackage(Packages.javax.crypto.spec, Packages.javax.crypto);
                with (importer) {
                    var key = new SecretKeySpec(java.utf8Encode('0123456789abcdef'), 'AES');
                    var iv = new IvParameterSpec(java.utf8Encode('12345678'));
                    var cipher = Cipher.getInstance('AES/CBC/PKCS5Padding');
                    cipher.init(2, key, iv);
                    cipher.doFinal(java.utf8Encode('data'));
                }
            """.trimIndent(),
        )

        assertTrue("AES keys outside 128/192/256 bits must be rejected", invalidKey is EngineResult.Failure)
        assertTrue("AES-CBC IVs other than 16 bytes must be rejected", invalidIv is EngineResult.Failure)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun restrictedAesImporterDecryptsKnownAesCbcPkcs5PaddingVector() = runBlocking {
        val source = BookSource(
            id = "https://aes-known-vector.example/",
            name = "AES known vector",
            baseUrl = "https://aes-known-vector.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var importer = new JavaImporter();
                importer.importPackage(Packages.javax.crypto.spec, Packages.javax.crypto);
                with (importer) {
                    var key = new SecretKeySpec(java.utf8Encode('0123456789abcdef'), 'AES');
                    var iv = new IvParameterSpec(java.utf8Encode('abcdef9876543210'));
                    var cipher = Cipher.getInstance('AES/CBC/PKCS5Padding');
                    cipher.init(Cipher.DECRYPT_MODE, key, iv);
                    cipher.doFinal(java.base64DecodeBytes('22Al9WL3X2jrWxylxjjTcw==')).toString();
                }
            """.trimIndent(),
        )

        assertTrue("AES-CBC known-vector decryption failed: $result", result is EngineResult.Success)
        assertEquals("badcat AES test", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }


    @Test
    fun supportsRegexLiteralsUsedByImportedComicAndKuwoSearchSources() = runBlocking {
        val source = BookSource(
            id = "https://imported-regex-fixtures.example/",
            name = "Imported regex fixtures",
            baseUrl = "https://imported-regex-fixtures.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var mkzhanCover = 'https://img.example/cover.jpg?size=large'
                    .replace(/\.(jpg|JPG).*/g, '.$1!cover-400');
                var kuwoStatus = '30 50'
                    .replace(/30/, '连载')
                    .replace(/50/, '完结');
                var qqPayload = "data: 'payload-123'"
                    .match(/data:\s*'(.*?)'/)[1];
                var kanmanKind = 'action|fantasy'
                    .replace(/[a-zA-Z]|\|/g, '');
                var legacyPattern = '[M comic title}'
                    .match(/\[M.*?}/)[0];
                [mkzhanCover, kuwoStatus, qqPayload, kanmanKind, legacyPattern].join('|');
            """.trimIndent(),
        )

        assertTrue("Imported source regex literals must execute in Rhino: $result", result is EngineResult.Success)
        assertEquals(
            "https://img.example/cover.jpg!cover-400|连载 完结|payload-123||[M comic title}",
            (result as EngineResult.Success).value,
        )
        runtime.clearSourceState(source.id)
    }


    @Test
    fun parsesLegadoTocScriptObjectArrayAndAjaxIntoChapters() = runBlocking {
        val bookUrl = "https://www.bixiange.top/trxs/19494"
        val source = BookSource(
            id = "https://www.bixiange.top/",
            name = "笔仙阁",
            baseUrl = "https://www.bixiange.top/",
            ruleTocJson = """{
                "chapterList":"@js:
                    var root = String(baseUrl).replace(/\\/$/, '');
                    var firstPage = String(java.ajax(root + '/index/1.html'));
                    var totalMatch = firstPage.match(/(?:^|[^0-9])1\\/(\\d+)(?:<|[^0-9])/);
                    var total = totalMatch ? Number(totalMatch[1]) : 0;
                    var chapters = [];
                    for (var i = 1; i <= total; i++) {
                      chapters.push({ title: '第' + i + '节', url: root + '/index/' + i + '.html' });
                    }
                    chapters;",
                "chapterName":"title",
                "chapterUrl":"url"
            }""",
        )
        var requestedAjaxUrl = ""
        val runtime = RhinoSourceScriptRuntime(
            sourceAjax = { requestedSource, baseUrl, raw ->
                assertEquals(source.id, requestedSource.id)
                assertEquals(bookUrl, baseUrl)
                requestedAjaxUrl = raw
                "<div>1/2</div>"
            },
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = bookUrl,
            body = "<html><body>Book detail</body></html>",
        )

        val parsed = CssTocParser().parseWithScripts(source, response, bookUrl, runtime)
        assertTrue("Bixiange JavaScript TOC must parse: $parsed", parsed is EngineResult.Success)
        val page = (parsed as EngineResult.Success).value

        assertEquals("$bookUrl/index/1.html", requestedAjaxUrl)
        assertEquals(listOf("第1节", "第2节"), page.chapters.map { it.title })
        assertEquals(
            listOf("$bookUrl/index/1.html", "$bookUrl/index/2.html"),
            page.chapters.map { it.url },
        )
        runtime.clearSourceState(source.id)
    }



    @Test
    fun supportsJavaScriptRegexWithUnescapedClosingBracesUsedByTemplates() = runBlocking {
        val source = BookSource(
            id = "https://regex-braces.example/",
            name = "Regex brace compatibility",
            baseUrl = "https://regex-braces.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """'a}}'.match(/\.*?}}/)[0];""",
        )

        assertTrue("ECMAScript regex with literal closing braces failed: $result", result is EngineResult.Success)
        assertEquals("}}", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun javaAjaxSupportsSearchFormRegexCaptureUsedByImportedSources() = runBlocking {
        val source = BookSource(
            id = "https://regex-search.example/",
            name = "Search regex compatibility",
            baseUrl = "https://regex-search.example/",
        )
        val requested = AtomicInteger()
        val runtime = RhinoSourceScriptRuntime(
            sourceAjax = { _, _, url ->
                requested.incrementAndGet()
                assertEquals("https://regex-search.example/search?q=%E5%89%91%E6%9D%A5", url)
                """<form id="search" action="/search.php"><input name="q"></form>"""
            },
        )

        val result = runtime.evaluate(
            source,
            """
                var url = 'https://regex-search.example/search?q=%E5%89%91%E6%9D%A5';
                java.ajax(url).match(/search.*action="(.+?)"/)[1];
            """.trimIndent(),
        )

        assertTrue("java.ajax + String.match failed: $result", result is EngineResult.Success)
        assertEquals("/search.php", (result as EngineResult.Success).value)
        assertEquals(1, requested.get())
        runtime.clearSourceState(source.id)
    }

}


class BadcatSourceHttpGatewayFactoryAjaxTest {
    @org.junit.Test
    fun javaAjaxUsesTheSameBoundedSourceHttpTransport() = kotlinx.coroutines.runBlocking {
        val requests = mutableListOf<app.badcat.engine.SourceHttpRequest>()
        val client = object : app.badcat.engine.SourceRequestClient {
            override suspend fun execute(
                request: app.badcat.engine.SourceHttpRequest,
            ): app.badcat.engine.EngineResult<app.badcat.engine.SourceHttpResponse> {
                requests += request
                val body = if (request.url.endsWith("/toc")) "toc body" else "result body"
                return app.badcat.engine.EngineResult.Success(
                    app.badcat.engine.SourceHttpResponse(200, request.url, body = body),
                )
            }
        }
        val source = app.badcat.engine.BookSource(
            id = "https://reader.example/source.json",
            name = "Ajax transport fixture",
            baseUrl = "https://reader.example/",
        )
        val gateway = BadcatSourceHttpGatewayFactory(client).create()

        val result = gateway.execute(
            source,
            "@js:java.ajax('/toc'); 'https://reader.example/result'",
        )

        val response = (result as app.badcat.engine.EngineResult.Success).value
        assertEquals("result body", response.body)
        assertEquals(
            listOf("https://reader.example/toc", "https://reader.example/result"),
            requests.map { it.url },
        )
    }

    @org.junit.Test
    fun javaAjaxHonorsStaticPostRequestOptions() = kotlinx.coroutines.runBlocking {
        val requests = mutableListOf<app.badcat.engine.SourceHttpRequest>()
        val client = object : app.badcat.engine.SourceRequestClient {
            override suspend fun execute(
                request: app.badcat.engine.SourceHttpRequest,
            ): app.badcat.engine.EngineResult<app.badcat.engine.SourceHttpResponse> {
                requests += request
                return app.badcat.engine.EngineResult.Success(
                    app.badcat.engine.SourceHttpResponse(200, request.url, body = "ok"),
                )
            }
        }
        val source = app.badcat.engine.BookSource(
            id = "https://reader.example/source.json",
            name = "Ajax POST fixture",
            baseUrl = "https://reader.example/",
        )
        val gateway = BadcatSourceHttpGatewayFactory(client).create()

        val result = gateway.execute(
            source,
            """@js:java.ajax('/lookup,{"method":"POST","body":"q=cat","retry":0}'); 'https://reader.example/result'""",
        )

        assertTrue(result is app.badcat.engine.EngineResult.Success)
        assertEquals("POST", requests.first().method)
        assertEquals("q=cat", requests.first().body)
        assertEquals("https://reader.example/lookup", requests.first().url)
    }

    @Test
    fun supportsLegadoOrgJsoupParseSelectSiblingAndRemoveFacade() = runBlocking {
        val source = BookSource(
            id = "https://org-jsoup-facade.example/",
            name = "org.jsoup facade",
            baseUrl = "https://org-jsoup-facade.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                var doc = org.jsoup.Jsoup.parse(
                    '<ul><li id="one">First</li><li id="two" class="selected"><a href="/chapter">Second</a></li></ul>',
                    'https://reader.example/catalog'
                );
                var imported = new JavaImporter(Packages.org.jsoup);
                var viaPackages = Packages.org.jsoup.Jsoup.parse('<p>Packages</p>');
                var viaImporter = imported.Jsoup.parse('<p>Importer</p>');
                var items = doc.select('li');
                var first = items.get(0);
                var second = first.nextElementSibling();
                var selectedFirst = doc.selectFirst('li');
                var before = items.size();
                first.remove();
                [
                    before,
                    second.text(),
                    second.attr('id'),
                    doc.select('li').size(),
                    selectedFirst.text(),
                    second.selectFirst('a').absUrl('href'),
                    second.hasClass('selected'),
                    second.parent().tagName(),
                    doc.select('missing').isEmpty(),
                    viaPackages.text(),
                    viaImporter.text()
                ].join('|');
            """.trimIndent(),
        )

        assertTrue("Legado org.jsoup.Jsoup.parse facade failed: $result", result is EngineResult.Success)
        assertEquals("2|Second|two|1|First|https://reader.example/chapter|true|ul|true|Packages|Importer", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

}
