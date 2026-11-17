package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import app.badcat.engine.LegadoBookSourceCodec
import app.badcat.engine.LegadoLoginUiV2
import app.badcat.client.runtime.LegadoSourceLoginUiV2Service
import app.badcat.client.runtime.RhinoSourceScriptRuntime
import app.badcat.client.runtime.SharedPreferencesSourceLoginSessionStore
import app.badcat.client.runtime.SharedPreferencesSourceCookieJar
import app.badcat.client.runtime.SharedPreferencesSourceScriptKeyValueStore
import app.badcat.client.runtime.BadcatReadingEngineFactory
import kotlinx.coroutines.CancellationException
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Temporary source-catalog utility screen, separate from the original Badcat UI.
 */
class SourceCatalogActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val loginCheckService by lazy {
        val sessions = SharedPreferencesSourceLoginSessionStore(applicationContext)
        LegadoSourceLoginUiV2Service(
            RhinoSourceScriptRuntime(
                sourceKeyValueStore = SharedPreferencesSourceScriptKeyValueStore(applicationContext),
                sourceLoginSessionStore = sessions,
                sourceCookieReader = { sourceId, url ->
                    SharedPreferencesSourceCookieJar(applicationContext, sourceId).getCookie(url)
                },
            ),
        )
    }
    private lateinit var catalog: FileBookSourceCatalog
    private lateinit var listContainer: LinearLayout
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = FileBookSourceCatalog(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
        }
        root.addView(TextView(this).apply {
            text = "书源管理"
            textSize = 24f
        })
        root.addView(actionButton("从网络 URL 导入书源") { promptNetworkImport() })
        root.addView(actionButton("从本地 JSON 文件导入") { chooseImportFile() })
        root.addView(actionButton("导出全部书源 JSON") { chooseExportFile() })
        root.addView(TextView(this).apply {
            text = "书源列表（开关仅控制本应用是否选用该书源）"
            textSize = 14f
            setPadding(0, 20, 0, 8)
        })
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listContainer)
        message = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 0)
        }
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
                    listContainer.removeAllViews()
                    val sources = result.value
                    if (sources.isEmpty()) {
                        listContainer.addView(TextView(this@SourceCatalogActivity).apply {
                            text = "本地暂无书源"
                            textSize = 15f
                        })
                    }
                    sources.forEach { source ->
                        val row = LinearLayout(this@SourceCatalogActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(0, 12, 0, 12)
                        }
                        row.addView(TextView(this@SourceCatalogActivity).apply {
                            text = source.name + "\n" + source.id
                            textSize = 14f
                        })
                        val enabledSwitch = Switch(this@SourceCatalogActivity).apply {
                            text = if (source.enabled) "已启用" else "已停用"
                            isChecked = source.enabled
                            setOnCheckedChangeListener { _, checked ->
                                isEnabled = false
                                scope.launch {
                                    when (val update = catalog.setEnabled(source.id, checked)) {
                                        is app.badcat.engine.EngineResult.Success -> {
                                            message.text = if (checked) "已启用：${source.name}" else "已停用：${source.name}"
                                            refreshCatalog()
                                        }
                                        is app.badcat.engine.EngineResult.Failure -> {
                                            message.text = "状态更新失败：${update.message}"
                                            refreshCatalog()
                                        }
                                    }
                                }
                            }
                        }
                        row.addView(enabledSwitch)
                        if (source.enabled && source.enabledExplore && !source.exploreUrl.isNullOrBlank()) {
                            row.addView(actionButton("书源发现 / 分类探索") {
                                promptExplore(source.id, source.name)
                            })
                        }
                        if (LegadoLoginUiV2.isV2(source.loginUi)) {
                            row.addView(Button(this@SourceCatalogActivity).apply {
                                text = "登录配置（Login UI v2）"
                                setOnClickListener {
                                    startActivity(Intent(this@SourceCatalogActivity, SourceLoginUiV2Activity::class.java).apply {
                                        putExtra(SourceLoginUiV2Activity.EXTRA_SOURCE_ID, source.id)
                                    })
                                }
                            }, LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ))
                        }
                        row.addView(Button(this@SourceCatalogActivity).apply {
                            text = "清除该书源登录信息"
                            setOnClickListener {
                                BadcatTheme.alertBuilder(this@SourceCatalogActivity)
                                    .setTitle("清除登录信息")
                                    .setMessage("将删除该书源保存的登录请求头、登录信息和 HTTP Cookie，不会删除书源本身。确定继续？")
                                    .setNegativeButton("取消", null)
                                    .setPositiveButton("清除") { _, _ ->
                                        SharedPreferencesSourceLoginSessionStore(applicationContext).clearSource(source.id)
                                        SharedPreferencesSourceCookieJar(applicationContext, source.id).clear()
                                        message.text = source.name + "：本机登录信息和 Cookie 已清除"
                                    }
                                    .show()
                            }
                        }, LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ))
                        if (!source.loginCheckJs.isNullOrBlank()) {
                            row.addView(Button(this@SourceCatalogActivity).apply {
                                text = "检查登录状态"
                                setOnClickListener {
                                    isEnabled = false
                                    text = "正在检查…"
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            loginCheckService.checkLogin(source)
                                        }
                                        this@apply.isEnabled = true
                                        this@apply.text = "检查登录状态"
                                        message.text = when (result) {
                                            is app.badcat.engine.EngineResult.Success ->
                                                if (result.value) source.name + "：已登录" else source.name + "：未登录"
                                            is app.badcat.engine.EngineResult.Failure ->
                                                source.name + "：登录检查失败（" + result.code + "）" + result.message
                                        }
                                    }
                                }
                            }, LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ))
                        }
                        listContainer.addView(row)
                    }
                }
                is app.badcat.engine.EngineResult.Failure ->
                    message.text = "读取书源失败：${result.message}"
            }
            window.decorView.post { BadcatTheme.applyTree(this@SourceCatalogActivity, window.decorView) }
        }
    }

    private fun promptExplore(sourceId: String, sourceName: String) {
        val keyword = android.widget.EditText(this).apply {
            hint = "可输入分类关键词，也可留空"
            setSingleLine(true)
            setPadding(28, 18, 28, 18)
        }
        BadcatTheme.alertBuilder(this)
            .setTitle("探索：$sourceName")
            .setMessage("使用该书源的 exploreUrl 和 ruleExplore 发现书籍。")
            .setView(keyword)
            .setNegativeButton("取消", null)
            .setPositiveButton("开始探索") { _, _ ->
                val query = keyword.text?.toString()?.trim().orEmpty()
                message.text = "正在探索：$sourceName"
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        BadcatReadingEngineFactory(applicationContext).create()
                            .explore(sourceId, keyword = query, page = 1)
                    }
                    when (result) {
                        is app.badcat.engine.EngineResult.Success -> {
                            val books = result.value
                            if (books.isEmpty()) {
                                message.text = "$sourceName：没有发现书籍"
                            } else {
                                message.text = "$sourceName：发现 ${books.size} 本书"
                                BadcatTheme.alertBuilder(this@SourceCatalogActivity)
                                    .setTitle("探索结果（${books.size}）")
                                    .setItems(books.map { it.name }.toTypedArray()) { _, index ->
                                        val book = books[index]
                                        startActivity(Intent(this@SourceCatalogActivity, SourceBookActivity::class.java).apply {
                                            putExtra(SourceBookActivity.EXTRA_SOURCE_ID, book.sourceId)
                                            putExtra(SourceBookActivity.EXTRA_BOOK_URL, book.bookUrl)
                                            putExtra(SourceBookActivity.EXTRA_BOOK_NAME, book.name)
                                        })
                                    }
                                    .setNegativeButton("关闭", null)
                                    .show()
                            }
                        }
                        is app.badcat.engine.EngineResult.Failure ->
                            message.text = "$sourceName：探索失败（${result.code}）${result.message}"
                    }
                }
            }
            .show()
    }

    private fun promptNetworkImport() {
        val address = android.widget.EditText(this).apply {
            hint = "https://example.com/book-sources.json"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        BadcatTheme.alertBuilder(this).setTitle("网络导入书源")
            .setMessage("输入公开可访问的 Legado 书源 JSON 地址。支持 HTTP/HTTPS，单次最多读取 5 MiB。")
            .setView(address)
            .setNegativeButton("取消", null)
            .setPositiveButton("开始导入") { _, _ -> importFromNetwork(address.text.toString().trim()) }
            .show()
    }

    private fun importFromNetwork(rawUrl: String) {
        val parsed = runCatching { Uri.parse(rawUrl) }.getOrNull()
        if (parsed == null || parsed.scheme !in setOf("https", "http") ||
            parsed.host.isNullOrBlank() || parsed.userInfo != null || rawUrl.length > 4096) {
            message.text = "网址无效：请输入完整的 HTTP/HTTPS JSON 地址"
            return
        }
        message.text = "正在从网络获取书源…"
        scope.launch {
            val json = try {
                withContext(Dispatchers.IO) {
                    var current = java.net.URL(rawUrl)
                    for (redirect in 0..5) {
                        val connection = (current.openConnection() as java.net.HttpURLConnection).apply {
                            connectTimeout = 12000
                            readTimeout = 18000
                            instanceFollowRedirects = false
                            setRequestProperty("Accept", "application/json, text/plain, */*")
                            setRequestProperty("User-Agent", "BadcatLegadoClient/1.0")
                        }
                        try {
                            val code = connection.responseCode
                            if (code in 300..399) {
                                require(redirect < 5) { "网络重定向次数过多" }
                                val location = connection.getHeaderField("Location") ?: error("重定向缺少目标地址")
                                val next = java.net.URL(current, location)
                                require(next.protocol == "https" || next.protocol == "http") { "重定向协议不受支持" }
                                require(next.userInfo == null) { "重定向地址不能包含账号密码" }
                                current = next
                                continue
                            }
                            require(code in 200..299) { "服务器返回 HTTP $code" }
                            return@withContext connection.inputStream.use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    require(output.size() + count <= LegadoBookSourceCodec.MAX_IMPORT_BYTES) {
                                        "书源数据超过 5 MiB 导入限制"
                                    }
                                    output.write(buffer, 0, count)
                                }
                                LegadoBookSourceCodec.decodeUtf8ToString(output.toByteArray())
                            }
                        } finally {
                            connection.disconnect()
                        }
                    }
                    error("无法取得书源数据")
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message.text = "网络获取失败：${error.message ?: "连接错误"}"
                return@launch
            }
            when (val result = catalog.importJson(json)) {
                is app.badcat.engine.EngineResult.Success -> {
                    message.text = "网络导入完成：处理 ${result.value} 个书源"
                    refreshCatalog()
                }
                is app.badcat.engine.EngineResult.Failure ->
                    message.text = "网络导入失败：${result.message}"
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
            val json = try {
                withContext(Dispatchers.IO) {
                    val input = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("无法打开所选文件")
                    input.use { stream ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= LegadoBookSourceCodec.MAX_IMPORT_BYTES) {
                                "书源文件超过 5 MiB 导入限制"
                            }
                            output.write(buffer, 0, count)
                        }
                        LegadoBookSourceCodec.decodeUtf8ToString(output.toByteArray())
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message.text = "读取书源文件失败：${error.message ?: "未知文件错误"}"
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
                    val saved = try {
                        withContext(Dispatchers.IO) {
                            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                                it.write(result.value)
                            } != null
                        }
                    } catch (error: Exception) {
                        message.text = "导出文件失败：${error.message ?: "未知文件错误"}"
                        return@launch
                    }
                    message.text = if (saved) "书源已导出" else "无法写入目标文件，请检查文件访问权限"
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

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
