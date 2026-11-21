package app.badcat.client.runtime

import app.badcat.engine.LegadoSourceElement
import app.badcat.engine.LegadoSourceElementList
import app.badcat.engine.SourceHttpResponse
import cn.hutool.crypto.symmetric.SymmetricCrypto
import com.google.gson.GsonBuilder
import com.script.ScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.help.crypto.AsymmetricCrypto
import io.legado.app.help.crypto.Sign
import org.htmlunit.corejs.javascript.Scriptable
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Files

/**
 * Emits the real compiled JVM descriptors and Rhino-visible member names for
 * every Badcat source-script scope object and every concrete return wrapper.
 * This is an inventory test, not a parity or safety approval.
 */
class RhinoHostSurfaceInventoryTest {

    @Test
    fun captureCompiledAndRhinoVisibleHostSurface() {
        val sourceId = "https://host-surface-inventory.example/"
        val bridge = RhinoSafeSourceJavaExtensions(sourceId, InMemorySourceScriptKeyValueStore)
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://host-surface-inventory.example/chapter",
            body = "<article><p>inventory</p></article>",
        )
        val tempRoot = Files.createTempDirectory("badcat-rhino-surface").toFile()
        try {
            val file = File(tempRoot, "inventory.txt").apply { writeText("surface") }
            val document = Jsoup.parse("<html><body><p>inventory</p></body></html>")
            val element = LegadoSourceElement("<article><p>inventory</p></article>")
            val elementList = LegadoSourceElementList(listOf(element))
            val httpResponse = RhinoSourceHttpResponse(response)
            val rawResponse = httpResponse.raw()
            val instances = linkedMapOf<String, Any>(
                "scope.java" to bridge,
                "scope.cookie" to RhinoSourceCookieExtensions(),
                "scope.source" to RhinoSourceLoginExtensions(
                    sourceId,
                    InMemorySourceLoginSessionStore,
                    InMemorySourceScriptKeyValueStore,
                ),
                "scope.cache" to RhinoSourceCacheExtensions(
                    sourceId,
                    InMemorySourceScriptKeyValueStore,
                ),
                "replacement.java" to LegadoRegexJsExtensions("inventory"),
                "return.file" to RhinoSourceFileHandle(file, tempRoot),
                "return.httpResponse" to httpResponse,
                "return.rawResponse" to rawResponse,
                "return.rawRequest" to rawResponse.request(),
                "return.url" to RhinoLegadoUrl("https://host-surface-inventory.example/a?b=c"),
                "return.element" to element,
                "return.elementList" to elementList,
                "return.jsoupDocument" to document,
                "return.jsoupElements" to document.select("p"),
                "return.symmetricCrypto" to bridge.createSymmetricCrypto("AES", ByteArray(16) { 1 }),
                "return.asymmetricCrypto" to bridge.createAsymmetricCrypto("RSA"),
                "return.sign" to bridge.createSign("SHA256withRSA"),
            )

            val records = instances.map { (name, value) -> inspect(name, value) }
            val report = mapOf(
                "schemaVersion" to 1,
                "pinnedUpstream" to "LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a",
                "comparisonKind" to "compiled JVM reflection plus actual Rhino wrapper member inventory; not upstream parity",
                "runtimeBindings" to mapOf(
                    "RhinoSourceScriptRuntime" to mapOf(
                        "java" to "RhinoSafeSourceJavaExtensions",
                        "cookie" to "RhinoSourceCookieExtensions",
                        "source" to "RhinoSourceLoginExtensions",
                        "cache" to "RhinoSourceCacheExtensions",
                    ),
                    "RhinoLegadoJsReplacementEvaluator" to mapOf(
                        "java" to "LegadoRegexJsExtensions",
                        "additionalBindings" to listOf("result", "book", "chapter"),
                    ),
                ),
                "objects" to records,
                "limitations" to listOf(
                    "This report is a Debug unit-test runtime inventory and does not establish upstream bytecode parity.",
                    "Rhino-visible names are measured on the current Debug runtime wrapper; Release/R8 and Android WebView exposure are separate.",
                    "External dependency classes are measured as returned objects, but dependency source provenance and upstream descriptor parity require a separate pinned dependency crosswalk.",
                ),
            )
            val workspace = System.getenv("GITHUB_WORKSPACE") ?: System.getProperty("user.dir")
            val output = File(workspace, "build/legado-host-rhino-surface.json")
            output.parentFile?.mkdirs()
            output.writeText(GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n")

            assertEquals(instances.keys, records.map { it["binding"] as String }.toSet())
            assertTrue(records.all { (it["rhinoWrapperType"] as String).isNotBlank() })
            assertTrue(output.isFile && output.length() > 0)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun inspect(binding: String, instance: Any): Map<String, Any?> {
        val type = instance.javaClass
        val declared = type.declaredMethods.filter { Modifier.isPublic(it.modifiers) }
        val public = type.methods.filter { Modifier.isPublic(it.modifiers) }
        val fields = type.fields.filter { Modifier.isPublic(it.modifiers) }
        val constructors = type.declaredConstructors.filter { Modifier.isPublic(it.modifiers) }

        val scope = RhinoScriptEngine.getRuntimeScope(
            ScriptBindings().apply { put("target", instance) },
        )
        val wrapped = scope.get("target", scope) as Scriptable
        val candidateNames = public.map { it.name }.distinct().sorted()
        val candidateJson = GsonBuilder().create().toJson(candidateNames)
        val visibleScript = "($candidateJson).filter(function(name) { return typeof target[name] === 'function'; }).sort().join('\\n')"
        val visibleNames = RhinoScriptEngine.eval(visibleScript, scope)
            ?.toString()
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            .orEmpty()

        return mapOf(
            "binding" to binding,
            "class" to type.name,
            "superclass" to type.superclass?.name,
            "interfaces" to type.interfaces.map { it.name }.sorted(),
            "declaredPublicMethods" to declared.map(::methodRecord).sortedBy { it["descriptor"].toString() },
            "inheritedPublicMethods" to public.map(::methodRecord).sortedBy { it["descriptor"].toString() },
            "publicFields" to fields.map(::fieldRecord).sortedBy { it["name"].toString() },
            "publicConstructors" to constructors.map(::constructorRecord).sortedBy { it["descriptor"].toString() },
            "rhinoPropertyIds" to wrapped.getIds().map { it.toString() }.sorted(),
            "rhinoCallableMethodNames" to visibleNames,
        )
    }

    private fun methodRecord(method: Method): Map<String, Any?> = mapOf(
        "declaringClass" to method.declaringClass.name,
        "name" to method.name,
        "descriptor" to methodDescriptor(method),
        "returnType" to method.returnType.name,
        "parameterTypes" to method.parameterTypes.map { it.name },
        "modifiers" to Modifier.toString(method.modifiers),
        "bridge" to method.isBridge,
        "synthetic" to method.isSynthetic,
        "annotations" to method.annotations.map { it.annotationClass.java.name }.sorted(),
    )

    private fun fieldRecord(field: Field): Map<String, Any?> = mapOf(
        "declaringClass" to field.declaringClass.name,
        "name" to field.name,
        "type" to field.type.name,
        "modifiers" to Modifier.toString(field.modifiers),
    )

    private fun constructorRecord(constructor: Constructor<*>): Map<String, Any?> = mapOf(
        "descriptor" to constructorDescriptor(constructor),
        "modifiers" to Modifier.toString(constructor.modifiers),
        "synthetic" to constructor.isSynthetic,
    )

    private fun methodDescriptor(method: Method): String =
        method.parameterTypes.joinToString(prefix = "(", postfix = ")") { descriptor(it) } +
            descriptor(method.returnType)

    private fun constructorDescriptor(constructor: Constructor<*>): String =
        constructor.parameterTypes.joinToString(prefix = "(", postfix = ")V") { descriptor(it) }

    private fun descriptor(type: Class<*>): String {
        if (type.isArray) return type.name.replace('.', '/')
        return when (type) {
            java.lang.Void.TYPE -> "V"
            java.lang.Boolean.TYPE -> "Z"
            java.lang.Byte.TYPE -> "B"
            java.lang.Character.TYPE -> "C"
            java.lang.Short.TYPE -> "S"
            java.lang.Integer.TYPE -> "I"
            java.lang.Long.TYPE -> "J"
            java.lang.Float.TYPE -> "F"
            java.lang.Double.TYPE -> "D"
            else -> "L${type.name.replace('.', '/')};"
        }
    }
}
