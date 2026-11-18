package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
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
            "java.put('token', 'source-value'); java.get('token')",
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
        val read = secondRuntime.evaluate(source, "java.get('refreshToken')")

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
        val result = runtime.evaluate(second, "java.get('session')")
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
            "java.put('headers', '{\\\"headers\\\":{}}'); java.get('headers') + '|' + java.ajax('https://java-compat.example/api')",
        )
        assertEquals(
            "{\"headers\":{}}|ajax:https://java-compat.example/api",
            (result as EngineResult.Success).value,
        )
        val formatted = runtime.evaluate(source, "java.timeFormat('0')")
        assertTrue((formatted as EngineResult.Success).value.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
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
            "java.getStringList('ul li@text').toArray().map(function(v) { return String(v); }).join('|') + '|' + java.getElement('ul li.0').text() + '|' + java.getElement('ul li.1').toString()",
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
}
