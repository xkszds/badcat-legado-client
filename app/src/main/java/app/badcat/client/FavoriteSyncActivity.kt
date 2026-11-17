package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Local JSON backup/restore. It deliberately does not claim cloud synchronization. */
class FavoriteSyncActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(android.graphics.Color.rgb(247, 249, 252))
        }
        root.addView(TextView(this).apply {
            text = "收藏夹备份与恢复"
            textSize = 21f
            setTextColor(android.graphics.Color.rgb(48, 55, 66))
            setPadding(0, 0, 0, dp(12))
        })
        root.addView(TextView(this).apply {
            text = "当前版本提供本地 JSON 文件备份与恢复。可将文件保存到手机、网盘或其他设备后再导入；不会自动上传云端。恢复会替换本机现有收藏夹和文件夹。"
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(110, 120, 132))
            setPadding(0, 0, 0, dp(18))
        })
        root.addView(Button(this).apply {
            text = "导出收藏夹备份"
            setOnClickListener {
                val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US).format(java.util.Date())
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "badcat-favorites-$stamp.json")
                }, REQUEST_EXPORT)
            }
        })
        root.addView(Button(this).apply {
            text = "从备份恢复收藏夹"
            setOnClickListener {
                BadcatTheme.alertBuilder(this@FavoriteSyncActivity)
                    .setTitle("确认恢复")
                    .setMessage("恢复会替换本机当前收藏夹和文件夹。建议先导出当前备份。是否继续选择备份文件？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("继续") { _, _ ->
                        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/json"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, REQUEST_IMPORT)
                    }.show()
            }
        })
        status = TextView(this).apply {
            text = "小说：${FavoriteBooks.all(this@FavoriteSyncActivity).size} 本 · 文件夹：${(FavoriteBooks.folders(this@FavoriteSyncActivity).size - 1).coerceAtLeast(0)} 个"
            textSize = 13f
            setTextColor(android.graphics.Color.rgb(100, 110, 122))
            setPadding(0, dp(18), 0, 0)
        }
        root.addView(status)
        setContentView(root)
    }

    @Deprecated("Uses the platform document picker for compatibility with the current minimum SDK")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQUEST_EXPORT -> exportBackup(uri)
            REQUEST_IMPORT -> importBackup(uri)
        }
    }

    private fun exportBackup(uri: Uri) {
        val folders = FavoriteBooks.folders(this).drop(1)
        val books = FavoriteBooks.all(this)
        val root = JSONObject().put("format", FORMAT).put("version", 1)
        root.put("folders", JSONArray().apply {
            folders.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
        })
        root.put("books", JSONArray().apply {
            books.forEach { book ->
                put(JSONObject().put("sourceId", book.sourceId).put("bookUrl", book.bookUrl)
                    .put("name", book.name).put("folderId", book.folderId)
                    .put("coverUrl", book.coverUrl.orEmpty()).put("addedAt", book.addedAt))
            }
        })
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(root.toString(2)) }
                ?: error("无法打开目标文件")
        }.onSuccess {
            status.text = "备份已导出：${books.size} 本小说、${folders.size} 个文件夹"
            toast("收藏夹备份成功")
        }.onFailure { status.text = "导出失败：${it.message}" }
    }

    private fun importBackup(uri: Uri) {
        val parsed = runCatching {
            val bytes = contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= MAX_IMPORT_BYTES) { "备份文件超过 4 MiB" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: error("无法读取备份文件")
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            require(json.optString("format") == FORMAT && json.optInt("version") == 1) { "不支持的备份格式或版本" }
            val folderArray = json.optJSONArray("folders") ?: JSONArray()
            val bookArray = json.optJSONArray("books") ?: JSONArray()
            require(folderArray.length() <= MAX_FOLDERS) { "文件夹数量超过限制" }
            require(bookArray.length() <= MAX_BOOKS) { "小说数量超过限制" }
            val folders = (0 until folderArray.length()).map { index ->
                val item = folderArray.getJSONObject(index)
                val id = item.getString("id").trim()
                val name = item.getString("name").trim()
                require(id.isNotBlank() && id != FavoriteBooks.DEFAULT_FOLDER_ID && id.length <= 128) { "文件夹 ID 无效" }
                require(name.isNotBlank() && name.length <= 80) { "文件夹名称无效" }
                FavoriteFolder(id, name)
            }
            require(folders.map { it.id }.distinct().size == folders.size) { "备份中存在重复文件夹 ID" }
            require(folders.map { it.name }.distinct().size == folders.size) { "备份中存在重复文件夹名称" }
            val folderIds = folders.map { it.id }.toSet()
            val books = (0 until bookArray.length()).map { index ->
                val item = bookArray.getJSONObject(index)
                val sourceId = item.getString("sourceId").trim()
                val bookUrl = item.getString("bookUrl").trim()
                val name = item.optString("name", "未命名书籍").take(256)
                val folderId = item.optString("folderId", FavoriteBooks.DEFAULT_FOLDER_ID)
                val cover = item.optString("coverUrl").take(8192).takeIf { it.isNotBlank() }
                require(sourceId.isNotBlank() && sourceId.length <= 512) { "第 ${index + 1} 本小说的书源 ID 无效" }
                require(bookUrl.isNotBlank() && bookUrl.length <= 8192) { "第 ${index + 1} 本小说的地址无效" }
                FavoriteBook(sourceId, bookUrl, name.ifBlank { "未命名书籍" },
                    if (folderId in folderIds) folderId else FavoriteBooks.DEFAULT_FOLDER_ID, cover,
                    item.optLong("addedAt", 0L).coerceAtLeast(0L))
            }.distinctBy { it.sourceId + "|" + it.bookUrl }
            folders to books
        }
        parsed.onSuccess { (folders, books) ->
            FavoriteBooks.replaceAll(this, books, folders)
            status.text = "恢复完成：${books.size} 本小说、${folders.size} 个文件夹"
            toast("收藏夹恢复成功")
        }.onFailure { status.text = "恢复失败：${it.message}" }
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_EXPORT = 5201
        private const val REQUEST_IMPORT = 5202
        private const val MAX_IMPORT_BYTES = 4 * 1024 * 1024
        private const val MAX_FOLDERS = 500
        private const val MAX_BOOKS = 20_000
        private const val FORMAT = "badcat-favorites"
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
