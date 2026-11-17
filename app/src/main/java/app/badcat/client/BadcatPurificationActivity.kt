package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import app.badcat.client.runtime.BadcatReadingEngineFactory

/** Local management for the original Badcat WebView injection toggle and AdMark catalog. */
class BadcatPurificationActivity : Activity() {
    private lateinit var coordinator: app.badcat.client.runtime.BadcatPurificationCoordinator
    private lateinit var root: LinearLayout
    private lateinit var scriptInput: EditText
    private var dark = false
    private val ink get() = if (dark) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (dark) Color.rgb(165, 172, 182) else Color.rgb(125, 135, 148)
    private val page get() = if (dark) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        coordinator = BadcatReadingEngineFactory(this).purificationCoordinator()
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
            setPadding(dp(16), dp(12), dp(16), dp(20))
        }
        root.addView(TextView(this).apply {
            text = "‹    广告过滤"
            textSize = 20f
            setTextColor(ink)
            setPadding(0, dp(8), 0, dp(18))
            setOnClickListener { finish() }
        })
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val enabled = Switch(this).apply {
            text = "启用网页广告过滤"
            textSize = 16f
            setTextColor(ink)
            isChecked = coordinator.webViewAdFilterEnabled()
            setOnCheckedChangeListener { _, checked -> coordinator.setWebViewAdFilterEnabled(checked) }
        }
        body.addView(enabled)
        body.addView(TextView(this).apply {
            text = "仅对 HTTP/HTTPS 网页执行已保存的注入脚本。关闭或未配置脚本时，不会改动网页原有 JavaScript 设置。"
            textSize = 12f; setTextColor(muted); setPadding(0, dp(6), 0, dp(14))
        })
        body.addView(TextView(this).apply {
            text = "网页注入脚本"
            textSize = 15f; setTextColor(ink)
        })
        scriptInput = EditText(this).apply {
            minLines = 5
            gravity = Gravity.TOP
            textSize = 13f
            setText(coordinator.snapshot().webViewInjectionScript.orEmpty())
            hint = "粘贴已确认来源的网页注入脚本"
        }
        body.addView(scriptInput, LinearLayout.LayoutParams(-1, dp(150)))
        body.addView(actionRow("保存脚本") {
            runCatching { coordinator.setWebViewInjectionScript(scriptInput.text.toString().takeIf { it.isNotBlank() }) }
                .onSuccess { toast("注入脚本已保存") }
                .onFailure { toast("保存失败：${it.message}") }
        })
        body.addView(TextView(this).apply {
            text = "广告标记规则"
            textSize = 15f; setTextColor(ink); setPadding(0, dp(20), 0, dp(8))
        })
        body.addView(actionRow("查看 / 删除本地标记") { showMarks() })
        body.addView(actionRow("导入标记 JSON") { importMarks() })
        body.addView(actionRow("导出标记 JSON") { exportMarks() })
        body.addView(TextView(this).apply {
            text = "注意：当前版本保留原版 AdMark 的 host、urlPattern 和 markRules 数据，并按已验证的 URL 匹配规则筛选；DOM 删除规则的执行及网页长按标记入口尚未接通。"
            textSize = 12f; setTextColor(muted); setPadding(0, dp(14), 0, 0)
        })
        scroll.addView(body)
        root.addView(scroll)
        setContentView(root)
    }

    private fun showMarks() {
        val items = coordinator.snapshot().adMarks
        if (items.isEmpty()) {
            BadcatTheme.alertBuilder(this).setTitle("广告标记")
                .setMessage("没有已标识的广告")
                .setPositiveButton("知道了", null).show()
            return
        }
        val labels = items.map { "${it.host}\n${it.urlPattern}（${it.markRules.size} 条规则）" }.toTypedArray()
        BadcatTheme.alertBuilder(this).setTitle("广告标记（点击后删除）")
            .setItems(labels) { _, index ->
                val item = items[index]
                BadcatTheme.alertBuilder(this).setTitle("删除此标记？")
                    .setMessage(item.host)
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        coordinator.removeAdMark(item.host, item.urlPattern)
                        toast("已删除")
                    }.show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun importMarks() {
        val input = EditText(this).apply {
            minLines = 5
            gravity = Gravity.TOP
            hint = "粘贴导出的 AdMark JSON 数组"
        }
        BadcatTheme.alertBuilder(this).setTitle("导入广告标记")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("导入") { _, _ ->
                runCatching { coordinator.importAdMarks(input.text.toString()) }
                    .onSuccess { toast("已导入 $it 条标记") }
                    .onFailure { toast("导入失败：${it.message}") }
            }.show()
    }

    private fun exportMarks() {
        val json = coordinator.exportAdMarks()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Badcat AdMark", json))
        toast("标记 JSON 已复制到剪贴板（${coordinator.snapshot().adMarks.size} 条）")
    }

    private fun actionRow(label: String, action: () -> Unit) = TextView(this).apply {
        text = "$label    ›"
        textSize = 14f
        setTextColor(ink)
        setPadding(dp(4), dp(14), dp(4), dp(14))
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
