package app.badcat.client.runtime

import android.webkit.WebView
import app.badcat.client.BadcatAdMarkCatalog
import app.badcat.client.BadcatAdMarkItem
import app.badcat.client.BadcatPurificationSettingsStore
import app.badcat.client.BadcatPurificationSnapshot
import app.badcat.client.BadcatPurificationUrlPolicy

/**
 * Shared coordinator for the two distinct purification domains:
 * Legado text rules stay in FileReplaceRuleCatalog and are executed by the
 * chapter pipeline; Badcat WebView injection and AdMark data stay here.
 */
class BadcatPurificationCoordinator(
    private val settings: BadcatPurificationSettingsStore,
    private val adMarks: BadcatAdMarkCatalog,
) {
    fun snapshot(): BadcatPurificationSnapshot =
        settings.snapshot(adMarks.all())

    fun setWebViewAdFilterEnabled(enabled: Boolean) =
        settings.setWebViewAdFilterEnabled(enabled)

    fun webViewAdFilterEnabled(): Boolean =
        settings.isWebViewAdFilterEnabled()

    fun setWebViewInjectionScript(script: String?) =
        settings.setWebViewInjectionScript(script)

    fun adMarksForUrl(url: String): List<BadcatAdMarkItem> =
        BadcatPurificationUrlPolicy.marksForUrl(url, adMarks.all())

    fun upsertAdMark(item: BadcatAdMarkItem) = adMarks.upsert(item)

    fun removeAdMark(host: String, urlPattern: String): Boolean =
        adMarks.remove(host, urlPattern)

    fun exportAdMarks(): String = adMarks.exportJson()

    fun importAdMarks(json: String): Int = adMarks.importJson(json)
}

/**
 * WebView adapter for the verified Badcat injection preference path.
 *
 * A missing/disabled script leaves the page's existing JavaScript setting
 * untouched. If injection requires JavaScript, its previous setting is
 * restored after evaluateJavascript completes.
 */
class BadcatWebViewPurificationAdapter(
    private val coordinator: BadcatPurificationCoordinator,
) {
    fun onPageFinished(webView: WebView, pageUrl: String) {
        val script = BadcatPurificationUrlPolicy.injectionForPage(coordinator.snapshot(), pageUrl)
            ?: return
        val wasJavaScriptEnabled = webView.settings.javaScriptEnabled
        if (!wasJavaScriptEnabled) webView.settings.javaScriptEnabled = true
        webView.evaluateJavascript(script) {
            if (!wasJavaScriptEnabled) {
                runCatching { webView.settings.javaScriptEnabled = false }
            }
        }
    }
}
