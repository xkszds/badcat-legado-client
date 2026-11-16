package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
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

        val results = kotlinx.coroutines.coroutineScope {
            (1..12).map { index ->
                kotlinx.coroutines.async(kotlinx.coroutines.Dispatchers.Default) {
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
}
