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

}
