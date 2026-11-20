package app.badcat.client.runtime

import android.webkit.JavascriptInterface
import com.script.rhino.RhinoScriptEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Public test-only receiver shared by the Rhino and future WebView overload probes.
 * Both overloads deliberately have the same name and arity.
 */
class RhinoOverloadProbe {
    @JavascriptInterface
    fun select(value: String, second: String): String = "STRING"

    @JavascriptInterface
    fun select(value: String, second: Int): String = "INT"
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class RhinoOverloadResolutionTest {

    @Test
    fun selectsStringOverloadForJavascriptStringArgument() {
        assertEquals("STRING", evaluate("probe.select('x', '7')"))
    }

    @Test
    fun selectsIntOverloadForJavascriptNumberArgument() {
        assertEquals("INT", evaluate("probe.select('x', 7)"))
    }

    @Test
    fun selectsStringOverloadForJavascriptNullArgument() {
        assertEquals("STRING", evaluate("probe.select('x', null)"))
    }

    @Test
    fun selectsStringOverloadForJavascriptUndefinedArgument() {
        assertEquals("STRING", evaluate("probe.select('x', undefined)"))
    }

    private fun evaluate(expression: String): String {
        val result = RhinoScriptEngine.eval(expression) {
            put("probe", RhinoOverloadProbe())
        }
        return result as? String
            ?: error("Expected a String overload marker, got ${result?.javaClass?.name}: $result")
    }
}
