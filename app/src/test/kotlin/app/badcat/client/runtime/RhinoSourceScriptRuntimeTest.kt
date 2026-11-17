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
            "java.putString('token', 'source-value'); java.getString('token')",
        )
        val digest = runtime.evaluate(source, "java.md5Encode('abc')")
        val encoded = runtime.evaluate(source, "java.urlEncode('a b')")

        assertEquals("source-value", (stored as EngineResult.Success).value)
        assertEquals("900150983cd24fb0d6963f7d28e17f72", (digest as EngineResult.Success).value)
        assertEquals("a+b", (encoded as EngineResult.Success).value)
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
        val read = secondRuntime.evaluate(source, "java.getString('refreshToken')")

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
        val result = runtime.evaluate(second, "java.getString('session')")
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
                "java.getCookie('https://cookie-helper.example/book', 'sid') + '|' + " +
                "java.getCookie('https://cookie-helper.example/book', 'token')",
        )

        assertEquals("sid=abc; token=x=y|abc|x=y", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
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

}
