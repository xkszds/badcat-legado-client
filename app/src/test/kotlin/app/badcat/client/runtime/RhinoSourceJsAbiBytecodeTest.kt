package app.badcat.client.runtime

import android.webkit.JavascriptInterface
import app.badcat.engine.EngineResult
import app.badcat.engine.SourceHttpResponse
import app.badcat.engine.BookSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // Verify emitted JVM overloads and compiler-generated default-argument bridges.
        assertEquals(setOf(1, 2), named("ajaxAll").map { it.parameterCount }.toSet())
        assertEquals(setOf(2, 3), named("ajaxTestAll").map { it.parameterCount }.toSet())
        assertTrue(named("ajaxAll\$default").single().isSynthetic)
        assertTrue(named("ajaxTestAll\$default").single().isSynthetic)
        assertEquals(setOf(1, 2, 3), named("get").map { it.parameterCount }.toSet())

        val timeFormat = named("timeFormat")
        assertEquals(2, timeFormat.size)
        assertEquals(
            setOf(Number::class.java, String::class.java),
            timeFormat.map { it.parameterTypes.single() }.toSet(),
        )
        assertTrue(timeFormat.all { it.returnType == String::class.java })

        // These helpers are called through Rhino's direct Java-object binding, not Android WebView.
        assertFalse(getSource.isAnnotationPresent(JavascriptInterface::class.java))
        assertFalse(timeFormat.any { it.isAnnotationPresent(JavascriptInterface::class.java) })
        assertTrue(named("aesDecodeToString").single().isAnnotationPresent(JavascriptInterface::class.java))
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
                var batchDefault = java.ajaxAll(['/default-argument']);
                var batch = java.ajaxAll(['/one', '/two'], true);
                [
                    noArg.get('bookSourceName'),
                    nullArg.size(),
                    batchDefault.length,
                    batchDefault[0].body(),
                    stringArg.get('X-Test'),
                    objectArg.get('Count'),
                    objectArg.get('Enabled'),
                    batch.length,
                    batch[0].body(),
                    batch[1].body(),
                    java.timeFormat(0).length() > 0,
                    java.timeFormat('0').length() > 0
                ].join('|');
            """.trimIndent(),
        )

        assertTrue("Rhino invocation failed: $result", result is EngineResult.Success)
        assertEquals(
            "ABI Rhino fixture|0|1|body:/default-argument|text|7.0|true|2|body:/one|body:/two|true|true",
            (result as EngineResult.Success).value,
        )
        assertEquals(setOf("/default-argument", "/one", "/two"), requestedUrls.toSet())
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
                    java.cacheContent(null, 'fixture');
                    'unexpected-success';
                } catch (error) {
                    'threw:' + error.name + ':' + error.message;
                }
            """.trimIndent(),
        )

        assertTrue("Rhino invocation failed: $result", result is EngineResult.Success)
        assertTrue((result as EngineResult.Success).value.startsWith("threw:JavaException:"))
        runtime.clearSourceState(source.id)
    }
}
