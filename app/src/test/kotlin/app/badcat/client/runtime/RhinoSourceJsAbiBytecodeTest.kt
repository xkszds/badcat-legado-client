package app.badcat.client.runtime

import android.webkit.JavascriptInterface
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpResponse
import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.AssertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Probes the compiled host ABI and invokes the real host bridge through Rhino.
 *
 * This deliberately does not claim upstream bytecode parity: the pinned upstream
 * JsExtensions/BaseSource classes are not part of a successfully compiled source
 * boundary in this repository yet.
 */
class RhinoSourceJsAbiBytecodeTest {

    @Test
    fun compiledHostBridgeHasInspectableJvmDescriptorsAndOverloads() {
        val methods = RhinoSafeSourceJavaExtensions::class.java.declaredMethods.toList()
        fun named(name: String) = methods.filter { it.name == name }

        val getSource = named("getSource").single()
        assertEquals(emptyList<Class<*>>(), getSource.parameterTypes.toList())
        assertEquals(Map::class.java, getSource.returnType)

        val batchContext = named("getBatchContext").single()
        assertEquals(Any::class.java, batchContext.returnType)

        val cacheContent = named("cacheContent").single()
        assertEquals(listOf(Any::class.java, String::class.java), cacheContent.parameterTypes.toList())
        assertEquals(Boolean::class.javaPrimitiveType, cacheContent.returnType)

        // @JvmOverloads must be verified on emitted JVM methods, not inferred from Kotlin source.
        assertEquals(setOf(1, 2), named("ajaxAll").map { it.parameterCount }.toSet())
        assertEquals(setOf(2, 3), named("ajaxTestAll").map { it.parameterCount }.toSet())
        assertEquals(setOf(1, 2, 3), named("get").map { it.parameterCount }.toSet())

        val timeFormat = named("timeFormat")
        assertEquals(2, timeFormat.size)
        assertEquals(
            setOf(Number::class.java, String::class.java),
            timeFormat.map { it.parameterTypes.single() }.toSet(),
        )
        assertTrue(timeFormat.all { it.returnType == String::class.java })

        // These helpers are called through Rhino's direct Java-object binding, not Android WebView.
        assertFalse(named("getSource").single().isAnnotationPresent(JavascriptInterface::class.java))
        assertNotNull(named("timeFormat").first())
    }

    @Test
    fun realRhinoBridgeHandlesNullScalarsObjectsArraysAndNoArgumentMethods() = runBlocking {
        val source = BookSource(
            id = "https://abi-rhino.example/",
            name = "ABI Rhino fixture",
            baseUrl = "https://abi-rhino.example/",
        )
        val requestedUrls = java.util.concurrent.CopyOnWriteArrayList<String>()
        val runtime = RhinoSourceScriptRuntime(
            sourceGet = { requestedSource, baseUrl, url, headers ->
                assertEquals(source.id, requestedSource.id)
                assertEquals(source.baseUrl, baseUrl)
                assertTrue(headers.isEmpty())
                requestedUrls += url
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = "https://abi-rhino.example$url",
                    body = "body:$url",
                )
            },
        )

        val result = runtime.evaluate(
            source,
            """
                var noArg = java.getSource();
                var nullArg = java.parseJsRequestHeaders(null);
                var stringArg = java.parseJsRequestHeaders('{"X-Test":"text"}');
                var objectArg = java.parseJsRequestHeaders({Count: 7, Enabled: true});
                var batch = java.ajaxAll(['/one', '/two'], true);
                [
                    noArg.bookSourceName,
                    Object.keys(nullArg).length,
                    stringArg['X-Test'],
                    objectArg.Count,
                    objectArg.Enabled,
                    batch.length,
                    batch[0].body(),
                    batch[1].body(),
                    java.timeFormat(0).length > 0,
                    java.timeFormat('0').length > 0
                ].join('|');
            """.trimIndent(),
        )

        assertTrue("Rhino invocation failed: $result", result is EngineResult.Success)
        assertEquals(
            "ABI Rhino fixture|0|text|7|true|2|body:/one|body:/two|true|true",
            (result as EngineResult.Success).value,
        )
        assertEquals(setOf("/one", "/two"), requestedUrls.toSet())
        runtime.clearSourceState(source.id)
    }

    @Test
    fun realRhinoBridgePreservesExplicitScriptErrorsInsteadOfReturningFakeSuccess() = runBlocking {
        val source = BookSource(
            id = "https://abi-error.example/",
            name = "ABI error fixture",
            baseUrl = "https://abi-error.example/",
        )
        val runtime = RhinoSourceScriptRuntime()
        val result = runtime.evaluate(
            source,
            """
                try {
                    java.parseJsRequestHeaders('{invalid-json}');
                    'unexpected-success';
                } catch (error) {
                    'threw:' + (error.name || error.javaException.getClass().getSimpleName());
                }
            """.trimIndent(),
        )

        assertTrue("Rhino invocation failed: $result", result is EngineResult.Success)
        assertTrue((result as EngineResult.Success).value.startsWith("threw:"))
        runtime.clearSourceState(source.id)
    }
}
