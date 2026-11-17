package app.badcat.client

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoReplaceRuleCodec
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

/** Development-only import/export and inspection screen for Legado replacement rules. */
class ReplaceRuleActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var catalog: FileReplaceRuleCatalog
    private lateinit var jsonInput: EditText
    private lateinit var status: TextView
    private lateinit var ruleList: LinearLayout
    private lateinit var importButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = FileReplaceRuleCatalog(applicationContext)

        jsonInput = EditText(this).apply {
            hint = "粘贴 Legado 替换规则 JSON（单条对象或数组）"
            minLines = 5
            maxLines = 12
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        status = TextView(this).apply { text = "正在读取本地替换规则…" }
        ruleList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        importButton = Button(this).apply {
            text = "导入并合并规则"
            setOnClickListener { importRules() }
        }
        val exportButton = Button(this).apply {
            text = "导出全部规则到上方文本框"
            setOnClickListener { exportRules() }
        }
        val pasteButton = Button(this).apply {
            text = "从剪贴板粘贴 JSON"
            setOnClickListener { pasteClipboard() }
        }
        val copyButton = Button(this).apply {
            text = "复制全部规则 JSON"
            setOnClickListener { copyExportToClipboard() }
        }
        val importFileButton = Button(this).apply {
            text = "从文件导入 JSON"
            setOnClickListener { openImportDocument() }
        }
        val exportFileButton = Button(this).apply {
            text = "导出为 JSON 文件"
            setOnClickListener { createExportDocument() }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(TextView(this@ReplaceRuleActivity).apply {
                text = "Legado 替换规则（开发验证）"
                textSize = 20f
            })
            addView(jsonInput, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
            addView(importButton)
            addView(exportButton)
            addView(pasteButton)
            addView(copyButton)
            addView(importFileButton)
            addView(exportFileButton)
            addView(status)
            addView(ruleList)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        refreshRules()
    }

    private fun openImportDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT_JSON)
    }

    private fun createExportDocument() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "legado-replace-rules.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT_JSON)
    }

    @Deprecated("Uses the platform document picker for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_IMPORT_JSON -> importDocument(uri)
            REQUEST_EXPORT_JSON -> exportDocument(uri)
        }
    }

    private fun importDocument(uri: Uri) {
        importButton.isEnabled = false
        status.text = "正在读取 JSON 文件…"
        worker.execute {
            val result = try {
                val bytes = contentResolver.openInputStream(uri)?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_IMPORT_BYTES) { "规则文件不能超过 5 MB" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } ?: error("无法打开所选文件")
                runBlocking { catalog.importJson(LegadoReplaceRuleCodec.decodeUtf8(bytes).let { rules -> LegadoReplaceRuleCodec.encode(rules) }) }
            } catch (error: Exception) {
                EngineResult.Failure(
                    app.badcat.engine.EngineErrorCode.INVALID_SOURCE,
                    error.message ?: "读取规则文件失败",
                    error,
                )
            }
            mainHandler.post {
                importButton.isEnabled = true
                when (result) {
                    is EngineResult.Success -> status.text = "从文件成功导入/更新 " + result.value + " 条规则"
                    is EngineResult.Failure -> status.text = "文件导入失败（" + result.code + "）：" + result.message
                }
                refreshRules()
            }
        }
    }

    private fun exportDocument(uri: Uri) {
        status.text = "正在写入 JSON 文件…"
        worker.execute {
            val result = runBlocking { catalog.exportJson() }
            val message = when (result) {
                is EngineResult.Failure -> "文件导出失败：" + result.message
                is EngineResult.Success -> try {
                    contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(result.value)
                    } ?: error("无法写入所选位置")
                    "已导出 " + result.value.length + " 个字符到 JSON 文件"
                } catch (error: Exception) {
                    "文件导出失败：" + (error.message ?: "写入错误")
                }
            }
            mainHandler.post { status.text = message }
        }
    }
    private fun pasteClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            status.text = "剪贴板没有可粘贴的内容"
            return
        }
        jsonInput.setText(clip.getItemAt(0).coerceToText(this))
        status.text = "已粘贴剪贴板内容，请确认后点击导入"
    }

    private fun copyExportToClipboard() {
        status.text = "正在准备导出…"
        worker.execute {
            val result = runBlocking { catalog.exportJson() }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Legado replacement rules", result.value)
                        )
                        status.text = "已复制 ${result.value.length} 个字符到剪贴板"
                    }
                    is EngineResult.Failure -> status.text = "复制失败：${result.message}"
                }
            }
        }
    }

    private fun importRules() {
        val json = jsonInput.text.toString()
        if (json.isBlank()) {
            status.text = "请先粘贴替换规则 JSON"
            return
        }
        importButton.isEnabled = false
        status.text = "正在校验并导入规则…"
        worker.execute {
            val result = runBlocking { catalog.importJson(json) }
            mainHandler.post {
                importButton.isEnabled = true
                when (result) {
                    is EngineResult.Success -> status.text = "成功导入/更新 ${result.value} 条规则"
                    is EngineResult.Failure -> status.text = "导入失败（${result.code}）：${result.message}"
                }
                refreshRules()
            }
        }
    }

    private fun exportRules() {
        status.text = "正在导出…"
        worker.execute {
            val result = runBlocking { catalog.exportJson() }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> {
                        jsonInput.setText(result.value)
                        status.text = "已导出 ${result.value.length} 个字符；可复制文本框内容"
                    }
                    is EngineResult.Failure -> status.text = "导出失败：${result.message}"
                }
            }
        }
    }

    private fun refreshRules() {
        worker.execute {
            val result = runBlocking { catalog.all() }
            mainHandler.post {
                ruleList.removeAllViews()
                when (result) {
                    is EngineResult.Failure -> status.text = "读取规则失败：${result.message}"
                    is EngineResult.Success -> {
                        val rules = result.value
                        if (rules.isEmpty()) {
                            ruleList.addView(TextView(this).apply { text = "暂无规则" })
                        } else {
                            rules.forEach { rule ->
                                val row = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(0, 12, 0, 12)
                                }
                                row.addView(TextView(this).apply {
                                    text = buildString {
                                        append(if (rule.scopeRule.enabled) "启用" else "停用")
                                        append(" · #").append(rule.scopeRule.id)
                                        append(" · ").append(rule.name.ifBlank { "未命名规则" })
                                        append("\n").append(if (rule.isRegex) "正则" else "字面")
                                        append(" · 顺序 ").append(rule.scopeRule.order)
                                        append(" · 超时 ").append(rule.validTimeoutMillisecond()).append("ms")
                                        append("\n").append(rule.pattern)
                                        append(" → ").append(rule.replacement)
                                    }
                                    textSize = 13f
                                })
                                row.addView(Button(this).apply {
                                    text = if (rule.scopeRule.enabled) "停用此规则" else "启用此规则"
                                    setOnClickListener { toggleRule(rule.scopeRule.id, !rule.scopeRule.enabled) }
                                })
                                ruleList.addView(row)
                            }
                        }
                        if (rules.isNotEmpty()) status.text = "本地共 ${rules.size} 条规则"
                    }
                }
            }
        }
    }

    private fun toggleRule(ruleId: Long, enabled: Boolean) {
        worker.execute {
            val result = runBlocking { catalog.setEnabled(ruleId, enabled) }
            mainHandler.post {
                when (result) {
                    is EngineResult.Success -> status.text = "规则 #$ruleId 已${if (enabled) "启用" else "停用"}"
                    is EngineResult.Failure -> status.text = "更新失败：${result.message}"
                }
                refreshRules()
            }
        }
    }

    companion object {
        private const val REQUEST_IMPORT_JSON = 4101
        private const val REQUEST_EXPORT_JSON = 4102
        private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
