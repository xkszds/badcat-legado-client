package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FavoriteFolder(val id: String, val name: String)
data class FavoriteBook(val sourceId: String, val bookUrl: String, val name: String, val folderId: String = FavoriteBooks.DEFAULT_FOLDER_ID, val coverUrl: String? = null)

object FavoriteBooks {
    private const val PREFS = "badcat_favorites"
    private const val KEY = "books"
    private const val FOLDER_KEY = "folders"
    const val DEFAULT_FOLDER_ID = "default"

    fun folders(context: Context): List<FavoriteFolder> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(FOLDER_KEY, "[]") ?: "[]"
        val saved = runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { i ->
                json.optJSONObject(i)?.let { item ->
                    val id = item.optString("id")
                    val name = item.optString("name")
                    if (id.isBlank() || name.isBlank() || id == DEFAULT_FOLDER_ID) null else FavoriteFolder(id, name)
                }
            }
        }.getOrDefault(emptyList())
        return listOf(FavoriteFolder(DEFAULT_FOLDER_ID, "全部小说")) + saved
    }

    fun all(context: Context): List<FavoriteBook> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                val source = item.optString("sourceId")
                val url = item.optString("bookUrl")
                if (source.isBlank() || url.isBlank()) null
                else FavoriteBook(source, url, item.optString("name").ifBlank { "未命名书籍" },
                    item.optString("folderId").ifBlank { DEFAULT_FOLDER_ID }, item.optString("coverUrl").takeIf { it.isNotBlank() })
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, sourceId: String, bookUrl: String): Boolean =
        all(context).any { it.sourceId == sourceId && it.bookUrl == bookUrl }

    fun add(context: Context, sourceId: String, bookUrl: String, name: String, coverUrl: String? = null): Boolean {
        if (sourceId.isBlank() || bookUrl.isBlank() || contains(context, sourceId, bookUrl)) return false
        save(context, all(context).toMutableList().apply { add(0, FavoriteBook(sourceId, bookUrl, name.ifBlank { "未命名书籍" }, coverUrl = coverUrl)) })
        return true
    }

    fun updateCover(context: Context, sourceId: String, bookUrl: String, coverUrl: String?) {
        if (coverUrl.isNullOrBlank()) return
        save(context, all(context).map { if (it.sourceId == sourceId && it.bookUrl == bookUrl) it.copy(coverUrl = coverUrl) else it })
    }

    fun remove(context: Context, sourceId: String, bookUrl: String) {
        save(context, all(context).filterNot { it.sourceId == sourceId && it.bookUrl == bookUrl })
    }

    fun createFolder(context: Context, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || folders(context).any { it.name == clean }) return false
        saveFolders(context, folders(context).drop(1) + FavoriteFolder(UUID.randomUUID().toString(), clean))
        return true
    }

    fun renameFolder(context: Context, id: String, name: String): Boolean {
        val clean = name.trim()
        val old = folders(context)
        if (id == DEFAULT_FOLDER_ID || clean.isBlank() || old.any { it.id != id && it.name == clean } || old.none { it.id == id }) return false
        saveFolders(context, old.drop(1).map { if (it.id == id) it.copy(name = clean) else it })
        return true
    }

    fun deleteFolder(context: Context, id: String) {
        if (id == DEFAULT_FOLDER_ID) return
        save(context, all(context).map { if (it.folderId == id) it.copy(folderId = DEFAULT_FOLDER_ID) else it })
        saveFolders(context, folders(context).filterNot { it.id == id }.drop(1))
    }

    fun moveToFolder(context: Context, book: FavoriteBook, folderId: String) {
        save(context, all(context).map { if (it.sourceId == book.sourceId && it.bookUrl == book.bookUrl) it.copy(folderId = folderId) else it })
    }

    private fun saveFolders(context: Context, folders: List<FavoriteFolder>) {
        val json = JSONArray()
        folders.forEach { json.put(JSONObject().put("id", it.id).put("name", it.name)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(FOLDER_KEY, json.toString()).apply()
    }

    private fun save(context: Context, books: List<FavoriteBook>) {
        val json = JSONArray()
        books.forEach { json.put(JSONObject().put("sourceId", it.sourceId).put("bookUrl", it.bookUrl).put("name", it.name).put("folderId", it.folderId).put("coverUrl", it.coverUrl.orEmpty())) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }
}

class FavoritesActivity : Activity() {
    private lateinit var rootContent: LinearLayout
    private var selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
    private val blue = Color.rgb(55, 137, 222)
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val page = Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(page) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(14), dp(12))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(TextView(this).apply {
            text = "小说"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(actionText("＋ 文件夹") {
            promptFolderName("新建文件夹") { name ->
                if (!FavoriteBooks.createFolder(this, name)) toast("文件夹名称无效或已存在")
                refresh()
            }
        })
        val more = actionText("⋮") {}
        more.setOnClickListener { showMoreMenu(more) }
        header.addView(more)
        root.addView(header)
        val scroll = ScrollView(this)
        rootContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(20)) }
        scroll.addView(rootContent)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        refresh()
    }

    override fun onResume() { super.onResume(); if (::rootContent.isInitialized) refresh() }

    private fun refresh() {
        rootContent.removeAllViews()
        val folders = FavoriteBooks.folders(this)
        if (folders.none { it.id == selectedFolderId }) selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
        val allBooks = FavoriteBooks.all(this)
        sectionTitle("小说文件夹")
        folders.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { folder ->
                row.addView(folderCard(folder, allBooks.count { it.folderId == folder.id }),
                    LinearLayout.LayoutParams(0, dp(94), 1f).apply { marginEnd = dp(7); bottomMargin = dp(8) })
            }
            repeat(3 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(94), 1f).apply { marginEnd = dp(7) }) }
            rootContent.addView(row)
        }
        sectionTitle(if (selectedFolderId == FavoriteBooks.DEFAULT_FOLDER_ID) "小说" else folders.first { it.id == selectedFolderId }.name)
        val visible = allBooks.filter { it.folderId == selectedFolderId }
        if (visible.isEmpty()) {
            rootContent.addView(TextView(this).apply {
                text = if (allBooks.isEmpty()) "还没有小说\n在小说详情页点击「加入收藏」，小说就会出现在这里。" else "这个文件夹还没有小说"
                textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER
                setPadding(dp(12), dp(38), dp(12), dp(38))
            })
            return
        }
        visible.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { book -> row.addView(bookCard(book), LinearLayout.LayoutParams(0, dp(138), 1f).apply {
                marginEnd = dp(7); bottomMargin = dp(8)
            }) }
            repeat(3 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(138), 1f).apply { marginEnd = dp(7) }) }
            rootContent.addView(row)
        }
    }

    private fun folderCard(folder: FavoriteFolder, count: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(if (folder.id == selectedFolderId) Color.rgb(231, 242, 253) else Color.WHITE, 12,
            if (folder.id == selectedFolderId) blue else Color.TRANSPARENT)
        isClickable = true; isFocusable = true
        setOnClickListener { selectedFolderId = folder.id; refresh() }
        setOnLongClickListener { showFolderActions(folder); true }
        addView(TextView(this@FavoritesActivity).apply { text = "▰"; textSize = 24f; setTextColor(blue) })
        addView(TextView(this@FavoritesActivity).apply { text = folder.name; textSize = 12f; maxLines = 1; setTextColor(ink); gravity = Gravity.CENTER })
        addView(TextView(this@FavoritesActivity).apply { text = "$count 本小说"; textSize = 10f; setTextColor(muted) })
    }

    private fun bookCard(book: FavoriteBook): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(Color.WHITE, 10); setPadding(dp(5), dp(8), dp(5), dp(7))
        isClickable = true; isFocusable = true
        setOnClickListener {
            startActivity(Intent(this@FavoritesActivity, SourceBookActivity::class.java).apply {
                putExtra(SourceBookActivity.EXTRA_SOURCE_ID, book.sourceId)
                putExtra(SourceBookActivity.EXTRA_BOOK_URL, book.bookUrl)
                putExtra(SourceBookActivity.EXTRA_BOOK_NAME, book.name)
                putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, book.coverUrl)
            })
        }
        setOnLongClickListener { showBookActions(book); true }
        addView(android.widget.ImageView(this@FavoritesActivity).apply {
            contentDescription = "小说封面"
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(235, 239, 245), 5)
            clipToOutline = true
            BookCoverLoader.load(this, book.coverUrl)
        }, LinearLayout.LayoutParams(dp(72), dp(94)))
        addView(TextView(this@FavoritesActivity).apply { text = book.name; textSize = 12f; maxLines = 2; gravity = Gravity.CENTER; setTextColor(ink) })
        addView(TextView(this@FavoritesActivity).apply { text = book.sourceId; textSize = 9f; maxLines = 1; setTextColor(muted); setPadding(0, dp(4), 0, 0) })
    }

    private fun showFolderActions(folder: FavoriteFolder) {
        val actions = if (folder.id == FavoriteBooks.DEFAULT_FOLDER_ID) arrayOf("打开文件夹") else arrayOf("打开文件夹", "重命名", "删除文件夹")
        AlertDialog.Builder(this).setTitle(folder.name).setItems(actions) { _, which ->
            when (actions[which]) {
                "打开文件夹" -> { selectedFolderId = folder.id; refresh() }
                "重命名" -> promptFolderName("重命名文件夹", folder.name) { name ->
                    if (!FavoriteBooks.renameFolder(this, folder.id, name)) toast("名称无效或已存在")
                    refresh()
                }
                "删除文件夹" -> AlertDialog.Builder(this).setTitle("删除文件夹")
                    .setMessage("删除后，文件夹内的书籍会移动到「全部小说」，不会删除书籍。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        FavoriteBooks.deleteFolder(this, folder.id)
                        selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
                        refresh()
                    }.show()
            }
        }.show()
    }

    private fun showBookActions(book: FavoriteBook) {
        val options = arrayOf("移出收藏", "移动到文件夹")
        AlertDialog.Builder(this).setTitle(book.name).setItems(options) { _, which ->
            if (which == 0) AlertDialog.Builder(this).setMessage("确定移出《${book.name}》吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("移出") { _, _ -> FavoriteBooks.remove(this, book.sourceId, book.bookUrl); refresh() }.show()
            else {
                val folders = FavoriteBooks.folders(this)
                AlertDialog.Builder(this).setTitle("移动到文件夹").setItems(folders.map { it.name }.toTypedArray()) { _, index ->
                    FavoriteBooks.moveToFolder(this, book, folders[index].id)
                    selectedFolderId = folders[index].id
                    refresh()
                }.show()
            }
        }.show()
    }

    private fun showMoreMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("新建文件夹"); menu.add("管理文件夹"); menu.add("返回全部小说")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "新建文件夹" -> promptFolderName("新建文件夹") { name ->
                        if (!FavoriteBooks.createFolder(this@FavoritesActivity, name)) toast("文件夹名称无效或已存在")
                        refresh()
                    }
                    "管理文件夹" -> {
                        val folders = FavoriteBooks.folders(this@FavoritesActivity).drop(1)
                        if (folders.isEmpty()) toast("暂无自定义文件夹")
                        else AlertDialog.Builder(this@FavoritesActivity).setTitle("文件夹管理")
                            .setItems(folders.map { it.name }.toTypedArray()) { _, i -> showFolderActions(folders[i]) }.show()
                    }
                    "返回全部小说" -> { selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID; refresh() }
                }
                true
            }
            show()
        }
    }

    private fun promptFolderName(title: String, initial: String = "", onSave: (String) -> Unit) {
        val input = EditText(this).apply { setText(initial); hint = "输入文件夹名称"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setNegativeButton("取消", null).setPositiveButton("保存") { _, _ -> onSave(input.text.toString()) }.show()
    }

    private fun sectionTitle(value: String) {
        rootContent.addView(TextView(this).apply {
            text = value; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            setPadding(dp(2), dp(5), 0, dp(10))
        })
    }

    private fun actionText(value: String, action: () -> Unit) = TextView(this).apply {
        text = value; textSize = 13f; gravity = Gravity.CENTER; setTextColor(blue)
        setPadding(dp(9), dp(8), dp(7), dp(8)); setOnClickListener { action() }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
