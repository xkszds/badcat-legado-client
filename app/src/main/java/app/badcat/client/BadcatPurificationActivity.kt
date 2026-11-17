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
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(16))
            addView(BadcatLineIconView(this@BadcatPurificationActivity, "back", ink).apply {
                contentDescription = "返回"
                isClickable = true
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(40), dp(44)))
            addView(TextView(this@BadcatPurificationActivity).apply {
                text = "广告过滤"
                textSize = 20f
                setTextColor(ink)
            })
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
        body.addView(sectionCard("过滤开关", "控制网页阅读场景中的广告过滤注入。") {
            addView(enabled)
        })
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
        body.addView(sectionCard("网页注入配置", "仅保存你确认来源的脚本；注入内容会在受支持的网页阅读场景中使用。") {
            addView(scriptInput, LinearLayout.LayoutParams(-1, dp(150)))
        })
        body.addView(actionRow("保存脚本") {
            runCatching { coordinator.setWebViewInjectionScript(scriptInput.text.toString().takeIf { it.isNotBlank() }) }
                .onSuccess { toast("注入脚本已保存") }
                .onFailure { toast("保存失败：${it.message}") }
        })
        body.addView(TextView(this).apply {
            text = "广告标记规则"
            textSize = 15f; setTextColor(ink); setPadding(0, dp(20), 0, dp(8))
        })
        body.addView(sectionCard("AdMark 本地标记", "管理已保存的站点标记数据。") {
            addView(actionRow("手动添加站点标记") { addMark() })
            addView(actionRow("查看 / 删除本地标记") { showMarks() })
            addView(actionRow("从文件导入标记 JSON") { chooseImportMarksFile() })
            addView(actionRow("粘贴 JSON 导入") { importMarks() })
            addView(actionRow("导出标记 JSON 到文件") { chooseExportMarksFile() })
            addView(actionRow("复制标记 JSON") { exportMarks() })
        })
        body.addView(TextView(this).apply {
            text = "注意：当前版本保留原版 AdMark 的 host、urlPattern 和 markRules 数据，并按已验证的 URL 匹配规则筛选；DOM 删除规则的执行及网页长按标记入口尚未接通。"
            textSize = 12f; setTextColor(muted); setPadding(0, dp(14), 0, 0)
        })
        scroll.addView(body)
        root.addView(scroll)
        setContentView(root)
    }

    private fun addMark() {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(6), dp(4), 0)
        }
        val host = EditText(this).apply {
            hint = "站点主机名，例如 reader.example"
            setSingleLine(true)
            textSize = 14f
        }
        val pattern = EditText(this).apply {
            hint = "URL 匹配数字数组，例如 [12,34,56]"
            setSingleLine(true)
            textSize = 14f
        }
        val rules = EditText(this).apply {
            hint = "markRules，每行一条；保留原始规则值"
            minLines = 3
            gravity = Gravity.TOP
            textSize = 13f
        }
        fields.addView(host)
        fields.addView(pattern)
        fields.addView(rules, LinearLayout.LayoutParams(-1, dp(104)))
        BadcatTheme.alertBuilder(this)
            .setTitle("添加 AdMark 站点标记")
            .setMessage("这里只保存 host、urlPattern 和 markRules 原始数据；当前版本尚未执行网页 DOM 删除规则。")
            .setView(fields)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val item = BadcatAdMarkItem(
                    host = host.text.toString().trim().removePrefix("https://").removePrefix("http://").substringBefore('/'),
                    urlPattern = pattern.text.toString().trim(),
                    markRules = rules.text.toString().lineSequence().map(String::trim).filter(String::isNotEmpty).toList(),
                )
                if (!item.isValid()) {
                    toast("标记格式无效：请检查主机名、数字数组和规则内容")
                } else {
                    runCatching { coordinator.upsertAdMark(item) }
                        .onSuccess { toast("已保存站点标记") }
                        .onFailure { toast("保存失败：${it.message}") }
                }
            }
            .show()
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

    private fun chooseImportMarksFile() {
        startActivityForResult(
            android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "application/json"
            },
            REQUEST_IMPORT_MARKS,
        )
    }

    private fun chooseExportMarksFile() {
        startActivityForResult(
            android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_TITLE, "badcat-ad-marks.json")
            },
            REQUEST_EXPORT_MARKS,
        )
    }

    @Deprecated("Uses the platform document picker for API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_IMPORT_MARKS -> {
                val json = runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= BadcatAdMarkJsonCodec.MAX_JSON_BYTES) {
                                "标记文件超过 2 MiB 限制"
                            }
                            output.write(buffer, 0, count)
                        }
                        Charsets.UTF_8.newDecoder()
                            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                            .decode(java.nio.ByteBuffer.wrap(output.toByteArray()))
                            .toString()
                    } ?: error("无法打开所选文件")
                }
                if (json.isFailure) {
                    toast("读取失败：${json.exceptionOrNull()?.message}")
                } else {
                    runCatching { coordinator.importAdMarks(json.getOrThrow()) }
                        .onSuccess { toast("已从文件导入 $it 条标记") }
                        .onFailure { toast("导入失败：${it.message}") }
                }
            }
            REQUEST_EXPORT_MARKS -> {
                val json = runCatching { coordinator.exportAdMarks() }
                if (json.isFailure) {
                    toast("导出失败：${json.exceptionOrNull()?.message}")
                    return
                }
                runCatching {
                    contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(json.getOrThrow().toByteArray(Charsets.UTF_8))
                        it.flush()
                    } ?: error("无法写入所选文件")
                }.onSuccess { toast("AdMark JSON 已导出到文件") }
                    .onFailure { toast("导出失败：${it.message}") }
            }
        }
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

    private fun sectionCard(title: String, subtitle: String, content: LinearLayout.() -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (dark) Color.rgb(38,42,49) else Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            addView(TextView(this@BadcatPurificationActivity).apply {
                text = title; textSize = 15f; setTextColor(ink)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@BadcatPurificationActivity).apply {
                text = subtitle; textSize = 12f; setTextColor(muted)
                setPadding(0, dp(4), 0, dp(8))
            })
            content()
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

    private companion object {
        const val REQUEST_IMPORT_MARKS = 731
        const val REQUEST_EXPORT_MARKS = 732
    }

    override fun onResume() {
        super.onResume()
        val currentDark = BadcatTheme.isDark(this)
        if (currentDark != dark) {
            dark = currentDark
            recreate()
            return
        }
        BadcatTheme.applyTree(this, window.decorView)
    }

}
