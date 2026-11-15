package app.badcat.client

import android.app.Activity
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
            addView(status)
            addView(ruleList)
        }
        setContentView(ScrollView(this).apply { addView(root) })
        refreshRules()
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

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
