package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Temporary source-catalog utility screen. It is deliberately separate from the
 * original Badcat navigation until the original UI resources are reconstructed.
 */
class SourceCatalogActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var catalog: FileBookSourceCatalog
    private lateinit var summary: TextView
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = FileBookSourceCatalog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
        }
        val title = TextView(this).apply {
            text = "书源管理"
            textSize = 24f
        }
        summary = TextView(this).apply {
            textSize = 15f
            setPadding(0, 20, 0, 20)
        }
        message = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 0)
        }
        root.addView(title)
        root.addView(summary)
        root.addView(actionButton("导入 Legado 书源 JSON") { chooseImportFile() })
        root.addView(actionButton("导出全部书源 JSON") { chooseExportFile() })
        root.addView(message)
        setContentView(ScrollView(this).apply { addView(root) })
        refreshCatalog()
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun refreshCatalog() {
        scope.launch {
            when (val result = catalog.all()) {
                is app.badcat.engine.EngineResult.Success -> {
                    val sources = result.value
                    summary.text = if (sources.isEmpty()) {
                        "本地暂无书源"
                    } else {
                        "已保存 ${sources.size} 个书源\n\n" + sources.joinToString("\n") {
                            (if (it.enabled) "● " else "○ ") + it.name + "  [" + it.id + "]"
                        }
                    }
                }
                is app.badcat.engine.EngineResult.Failure ->
                    summary.text = "读取书源失败：${result.message}"
            }
        }
    }

    private fun chooseImportFile() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            },
            REQUEST_IMPORT,
        )
    }

    private fun chooseExportFile() {
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "legado-book-sources.json")
            },
            REQUEST_EXPORT,
        )
    }

    @Deprecated("Uses the platform document picker for API 23 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_IMPORT -> importFrom(uri)
            REQUEST_EXPORT -> exportTo(uri)
        }
    }

    private fun importFrom(uri: Uri) {
        scope.launch {
            val json = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            }
            if (json == null) {
                message.text = "无法读取所选文件"
                return@launch
            }
            when (val result = catalog.importJson(json)) {
                is app.badcat.engine.EngineResult.Success -> {
                    message.text = "导入完成：处理 ${result.value} 个书源"
                    refreshCatalog()
                }
                is app.badcat.engine.EngineResult.Failure ->
                    message.text = "导入失败：${result.message}"
            }
        }
    }

    private fun exportTo(uri: Uri) {
        scope.launch {
            when (val result = catalog.exportJson()) {
                is app.badcat.engine.EngineResult.Success -> {
                    val saved = withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(result.value)
                        } != null
                    }
                    message.text = if (saved) "书源已导出" else "无法写入目标文件"
                }
                is app.badcat.engine.EngineResult.Failure ->
                    message.text = "导出失败：${result.message}"
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_IMPORT = 4101
        private const val REQUEST_EXPORT = 4102
    }
}
