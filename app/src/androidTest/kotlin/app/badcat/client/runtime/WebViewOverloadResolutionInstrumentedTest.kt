package app.badcat.client.runtime

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Test-only receiver for the real Android JavaBridge. Same method name and arity
 * deliberately expose the platform's overload-selection behavior without doing I/O.
 */
class WebViewOverloadProbe {
    @JavascriptInterface
    fun select(value: String, second: String?): String = "STRING"

    @JavascriptInterface
    fun select(value: String, second: Int): String = "INT"
}

@RunWith(AndroidJUnit4::class)
class WebViewOverloadResolutionInstrumentedTest {

    @Test
    fun recordsRealProviderOverloadBehaviorForStringNumberNullAndUndefined() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val provider = WebView.getCurrentWebViewPackage()
        assertNotNull("A real WebView provider package must be present", provider)

        val observations = mutableListOf<JSONObject>()
        val webViewRef = AtomicReference<WebView>()
        val pageLoaded = CountDownLatch(1)

        try {
            instrumentation.runOnMainSync {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageLoaded.countDown()
                    }
                }
                webView.addJavascriptInterface(WebViewOverloadProbe(), "probe")
                webViewRef.set(webView)
                webView.loadDataWithBaseURL(
                    "https://localhost/",
                    "<!doctype html><html><head><meta charset='utf-8'></head><body>bridge probe</body></html>",
                    "text/html",
                    "UTF-8",
                    null,
                )
            }

            assertTrue("WebView page load callback timed out", pageLoaded.await(15, TimeUnit.SECONDS))
            val cases = listOf(
                "string" to "'7'",
                "number" to "7",
                "null" to "null",
                "undefined" to "undefined",
            )

            for ((caseName, argument) in cases) {
                val callback = AtomicReference<String?>()
                val completed = CountDownLatch(1)
                val expression = """
                    (function() {
                      try {
                        return JSON.stringify({ok:true, value:String(probe.select('x', $argument))});
                      } catch (error) {
                        return JSON.stringify({ok:false, error:String(error)});
                      }
                    })()
                """.trimIndent()

                instrumentation.runOnMainSync {
                    webViewRef.get().evaluateJavascript(expression) { value ->
                        callback.set(value)
                        completed.countDown()
                    }
                }
                assertTrue("evaluateJavascript callback timed out for $caseName", completed.await(10, TimeUnit.SECONDS))
                val callbackValue = callback.get()
                assertNotNull("Missing evaluateJavascript result for $caseName", callbackValue)
                val decoded = JSONTokener(callbackValue).nextValue() as? String
                    ?: throw AssertionError("Unexpected WebView callback envelope for $caseName: $callbackValue")
                val payload = JSONObject(decoded)
                val ok = payload.optBoolean("ok", false)
                val observation = JSONObject()
                    .put("case", caseName)
                    .put("ok", ok)
                    .put("rawCallback", callbackValue)
                if (ok) {
                    val marker = payload.optString("value")
                    assertTrue("Unknown overload marker for $caseName: $marker", marker == "STRING" || marker == "INT")
                    observation.put("selected", marker)
                } else {
                    val error = payload.optString("error")
                    assertTrue("JavaScript exception was empty for $caseName", error.isNotBlank())
                    observation.put("error", error)
                }
                observations += observation
            }
        } finally {
            val providerInfo = provider!!
            val report = JSONObject()
                .put("task", "Runtime-2.3")
                .put("sdkInt", Build.VERSION.SDK_INT)
                .put("device", Build.DEVICE)
                .put("model", Build.MODEL)
                .put("webViewProviderPackage", providerInfo.packageName)
                .put("webViewProviderVersionName", providerInfo.versionName ?: JSONObject.NULL)
                .put("webViewProviderLongVersionCode", providerInfo.longVersionCode)
                .put("observations", org.json.JSONArray(observations))
            val reportBytes = report.toString(2).toByteArray(Charsets.UTF_8)
            context.openFileOutput("runtime-2.3-webview-report.json", Context.MODE_PRIVATE).use {
                it.write(reportBytes)
            }
            exportReportToShellReadablePath(instrumentation, reportBytes)
            instrumentation.runOnMainSync {
                webViewRef.getAndSet(null)?.apply {
                    stopLoading()
                    removeJavascriptInterface("probe")
                    destroy()
                }
            }
        }
    }

    /**
     * Gradle's connectedAndroidTest uninstalls the target and test APKs before
     * returning to the CI script. Export through UiAutomation's shell identity
     * so the report survives that uninstall and can be pulled afterwards.
     */
    private fun exportReportToShellReadablePath(
        instrumentation: android.app.Instrumentation,
        reportBytes: ByteArray,
    ) {
        val shellProbeCommand = "echo BADCAT_UIAUTOMATION_SHELL_PROBE"
        val shellProbeDescriptor = instrumentation.uiAutomation.executeShellCommand(shellProbeCommand)
        val shellProbeOutput = ParcelFileDescriptor.AutoCloseInputStream(shellProbeDescriptor)
            .bufferedReader()
            .use { it.readText() }
        assertTrue(
            "UiAutomation shell stdout probe returned no marker; output: " +
                shellProbeOutput.ifBlank { "<empty stdout>" },
            shellProbeOutput.contains("BADCAT_UIAUTOMATION_SHELL_PROBE"),
        )

        val path = "/data/local/tmp/runtime-2.3-webview-report.json"
        val encoded = Base64.getEncoder().encodeToString(reportBytes)
        val command = "sh -c 'echo $encoded | base64 -d > $path; export_status=\$?; if [ \$export_status -eq 0 ]; then chmod 644 $path; export_status=\$?; fi; if [ \$export_status -eq 0 ]; then echo REPORT_EXPORTED; fi; echo REPORT_EXIT_STATUS=\$export_status; exit \$export_status' 2>&1"
        val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
        val output = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        val diagnosticOutput = output.ifBlank { "<empty stdout/stderr; shell returned no diagnostic output>" }
        assertTrue(
            "Instrumentation shell could not export WebView report; command output: $diagnosticOutput",
            output.contains("REPORT_EXPORTED") && output.contains("REPORT_EXIT_STATUS=0"),
        )
    }
}
