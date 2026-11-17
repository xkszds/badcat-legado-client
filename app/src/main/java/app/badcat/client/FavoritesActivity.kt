package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
data class FavoriteBook(val sourceId: String, val bookUrl: String, val name: String, val folderId: String = FavoriteBooks.DEFAULT_FOLDER_ID, val coverUrl: String? = null, val addedAt: Long = System.currentTimeMillis(), val status: String? = null)

internal fun favoriteBooksInFolder(books: List<FavoriteBook>, folderId: String): List<FavoriteBook> =
    if (folderId == FavoriteBooks.DEFAULT_FOLDER_ID) books else books.filter { it.folderId == folderId }

internal fun favoriteBookCountInFolder(books: List<FavoriteBook>, folderId: String): Int =
    if (folderId == FavoriteBooks.DEFAULT_FOLDER_ID) books.size else books.count { it.folderId == folderId }

internal fun sortFavoriteBooksByLastRead(
    books: List<FavoriteBook>,
    lastReadAt: Map<String, Long>,
    newestFirst: Boolean,
): List<FavoriteBook> {
    val timestamp: (FavoriteBook) -> Long = {
        lastReadAt[it.sourceId + "|" + it.bookUrl] ?: it.addedAt
    }
    return if (newestFirst) books.sortedByDescending(timestamp) else books.sortedBy(timestamp)
}

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
                    item.optString("folderId").ifBlank { DEFAULT_FOLDER_ID }, item.optString("coverUrl").takeIf { it.isNotBlank() }, item.optLong("addedAt", index.toLong()), item.optString("status").takeIf { it.isNotBlank() })
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, sourceId: String, bookUrl: String): Boolean =
        all(context).any { it.sourceId == sourceId && it.bookUrl == bookUrl }

    fun add(context: Context, sourceId: String, bookUrl: String, name: String, coverUrl: String? = null, status: String? = null): Boolean {
        if (sourceId.isBlank() || bookUrl.isBlank() || contains(context, sourceId, bookUrl)) return false
        save(context, all(context).toMutableList().apply { add(0, FavoriteBook(sourceId, bookUrl, name.ifBlank { "未命名书籍" }, coverUrl = coverUrl, status = status)) })
        return true
    }

    fun updateStatus(context: Context, sourceId: String, bookUrl: String, status: String?) {
        if (status.isNullOrBlank()) return
        save(context, all(context).map { if (it.sourceId == sourceId && it.bookUrl == bookUrl) it.copy(status = status) else it })
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

    fun replaceAll(context: Context, books: List<FavoriteBook>, folders: List<FavoriteFolder>) {
        val validFolders = folders.filter { it.id != DEFAULT_FOLDER_ID && it.id.isNotBlank() && it.name.isNotBlank() }
        val folderIds = validFolders.map { it.id }.toSet()
        val normalizedBooks = books.map { book ->
            if (book.folderId == DEFAULT_FOLDER_ID || book.folderId in folderIds) book
            else book.copy(folderId = DEFAULT_FOLDER_ID)
        }
        saveFolders(context, validFolders)
        save(context, normalizedBooks.distinctBy { it.sourceId + "|" + it.bookUrl })
    }

    private fun saveFolders(context: Context, folders: List<FavoriteFolder>) {
        val json = JSONArray()
        folders.forEach { json.put(JSONObject().put("id", it.id).put("name", it.name)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(FOLDER_KEY, json.toString()).apply()
    }

    private fun save(context: Context, books: List<FavoriteBook>) {
        val json = JSONArray()
        books.forEach { json.put(JSONObject().put("sourceId", it.sourceId).put("bookUrl", it.bookUrl).put("name", it.name).put("folderId", it.folderId).put("coverUrl", it.coverUrl.orEmpty()).put("addedAt", it.addedAt).put("status", it.status.orEmpty())) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }
}

class FavoritesActivity : Activity() {
    private lateinit var rootContent: LinearLayout
    private var selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
    private var selectedType = "小说"
    private var showFolderPreview = false
    private var folderMode = false
    private var listMode = false
    private var selectionMode = false
    private val selectedBooks = linkedMapOf<String, FavoriteBook>()
    private lateinit var bottomActions: LinearLayout
    private fun bookKey(book: FavoriteBook) = book.sourceId + "|" + book.bookUrl
    private val blue = Color.rgb(55, 137, 222)
    private var darkMode = false
    private val ink get() = if (darkMode) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (darkMode) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val page get() = if (darkMode) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)
    private val card get() = if (darkMode) Color.rgb(38, 42, 49) else Color.WHITE
    private val selectedFolder get() = if (darkMode) Color.rgb(39, 59, 78) else Color.rgb(231, 242, 253)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        window.statusBarColor = card
        window.navigationBarColor = card
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (darkMode) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(page) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(14), dp(12))
            setBackgroundColor(card)
        }
        lateinit var title: TextView
        title = TextView(this).apply {
            text = selectedType
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setOnLongClickListener {
                val types = arrayOf("小说", "漫画", "其他")
                BadcatTheme.alertBuilder(this@FavoritesActivity).setItems(types) { _, which ->
                    selectedType = types[which]
                    title.text = selectedType
                    selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
                    selectedBooks.clear()
                    selectionMode = false
                    refresh()
                }.show()
                true
            }
        }
        header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val more = TextView(this@FavoritesActivity).apply {
                text = "•••"
                textSize = 17f
                letterSpacing = 0.02f
                gravity = Gravity.CENTER
                setTextColor(ink)
                minWidth = dp(40)
                minHeight = dp(36)
                isClickable = true
                isFocusable = true
                contentDescription = "更多收藏夹选项，长按切换排列方向"
                setOnLongClickListener {
                    text = if (text == "•••") "•\n•\n•" else "•••"
                    requestLayout()
                    true
                }
                setOnClickListener {
                    animate().scaleX(0.88f).scaleY(0.88f).setDuration(65L)
                        .withEndAction {
                            animate().scaleX(1f).scaleY(1f).setDuration(95L).start()
                            showMoreMenu(this)
                        }.start()
                }
            }
            addView(more, LinearLayout.LayoutParams(dp(40), dp(36)))
        }, LinearLayout.LayoutParams(dp(40), dp(36)))
        root.addView(header)
        val scroll = ScrollView(this)
        rootContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(20)) }
        scroll.addView(rootContent)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        bottomActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setBackgroundColor(Color.WHITE); visibility = View.GONE }
        root.addView(bottomActions)
        root.addView(MainBottomNavigation.create(this, "favorites"))
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        darkMode = BadcatTheme.isDark(this)
        if (::rootContent.isInitialized) refresh()
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun refresh() {
        rootContent.removeAllViews()
        val folders = FavoriteBooks.folders(this)
        if (folders.none { it.id == selectedFolderId }) selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
        val allBooks = FavoriteBooks.all(this)
        if (selectedType != "小说") {
            selectedBooks.clear()
            selectionMode = false
            bottomActions.removeAllViews()
            bottomActions.visibility = View.GONE
            rootContent.addView(TextView(this).apply {
                text = "暂无" + selectedType + "收藏"
                textSize = 15f
                setTextColor(muted)
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(100), dp(12), dp(100))
            })
            BadcatTheme.applyTree(this, window.decorView)
            return
        }
        folderMode = getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).getBoolean("folder_mode", false)
        listMode = getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).getBoolean("list_mode", false)
        if (folderMode) {
        sectionTitle("小说文件夹")
        folders.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { folder ->
                row.addView(folderCard(folder, favoriteBookCountInFolder(allBooks, folder.id)),
                    LinearLayout.LayoutParams(0, dp(94), 1f).apply { marginEnd = dp(7); bottomMargin = dp(8) })
            }
            repeat(3 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(94), 1f).apply { marginEnd = dp(7) }) }
            rootContent.addView(row)
        }
        sectionTitle(if (selectedFolderId == FavoriteBooks.DEFAULT_FOLDER_ID) "小说" else folders.first { it.id == selectedFolderId }.name)
        } else { selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID }
        // Reading-history timestamps are the real last-read/update signal.
        // Books without a reading record fall back to their original favorite time.
        val lastReadAt = latestBookReadTimes()
        val newestFirst = getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE)
            .getBoolean("sort_newest_first", true)
        val visible = sortFavoriteBooksByLastRead(
            favoriteBooksInFolder(allBooks, selectedFolderId),
            lastReadAt,
            newestFirst,
        )
        if (visible.isEmpty()) {
            rootContent.addView(TextView(this).apply {
                text = if (allBooks.isEmpty()) "还没有小说\n在小说详情页点击「加入收藏」，小说就会出现在这里。" else "这个文件夹还没有小说"
                textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER
                setPadding(dp(12), dp(38), dp(12), dp(38))
            })
            renderSelectionBar()
            BadcatTheme.applyTree(this, window.decorView)
            return
        }
        if (listMode) visible.forEach { book -> rootContent.addView(bookListRow(book), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) }) }
        else visible.chunked(3).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { book -> row.addView(bookCard(book), LinearLayout.LayoutParams(0, dp(138), 1f).apply { marginEnd = dp(7); bottomMargin = dp(8) }) }
            repeat(3 - group.size) { row.addView(View(this), LinearLayout.LayoutParams(0, dp(138), 1f).apply { marginEnd = dp(7) }) }
            rootContent.addView(row)
        }
        renderSelectionBar()
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun latestBookReadTimes(): Map<String, Long> {
        val raw = getSharedPreferences(ReadingHistoryActivity.PREFS, MODE_PRIVATE)
            .getString(ReadingHistoryActivity.KEY, "[]").orEmpty()
        return runCatching {
            val records = JSONArray(raw)
            buildMap {
                for (index in 0 until records.length()) {
                    val item = records.optJSONObject(index) ?: continue
                    val sourceId = item.optString("sourceId")
                    val bookUrl = item.optString("bookUrl")
                    if (sourceId.isBlank() || bookUrl.isBlank()) continue
                    val updatedAt = item.optLong("updatedAt", 0L)
                    if (updatedAt > 0L) {
                        val key = "$sourceId|$bookUrl"
                        put(key, maxOf(get(key) ?: 0L, updatedAt))
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun folderCard(folder: FavoriteFolder, count: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(if (folder.id == selectedFolderId) selectedFolder else card, 12,
            if (folder.id == selectedFolderId) blue else Color.TRANSPARENT)
        isClickable = true; isFocusable = true
        setOnClickListener { selectedFolderId = folder.id; refresh() }
        setOnLongClickListener { showFolderActions(folder); true }
        addView(TextView(this@FavoritesActivity).apply { text = "▰"; textSize = 24f; setTextColor(blue) })
        addView(TextView(this@FavoritesActivity).apply { text = folder.name; textSize = 12f; maxLines = 1; setTextColor(ink); gravity = Gravity.CENTER })
        addView(TextView(this@FavoritesActivity).apply { text = "$count 本小说"; textSize = 10f; setTextColor(muted) })
        if (showFolderPreview && count > 0) {
            favoriteBooksInFolder(FavoriteBooks.all(this@FavoritesActivity), folder.id).take(2).forEach { book ->
                addView(TextView(this@FavoritesActivity).apply {
                    text = book.name; textSize = 9f; maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(muted); gravity = Gravity.CENTER
                })
            }
        }
    }

    private fun bookCard(book: FavoriteBook): View = LinearLayout(this).apply {
        val isSelected = selectedBooks.containsKey(bookKey(book))
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(if (isSelected) selectedFolder else card, 10,
            if (isSelected) blue else Color.TRANSPARENT)
        setPadding(dp(5), dp(8), dp(5), dp(7))
        isClickable = true; isFocusable = true
        setOnClickListener {
            if (selectionMode) { toggleBookSelection(book); return@setOnClickListener }
            startActivity(Intent(this@FavoritesActivity, SourceBookActivity::class.java).apply {
                putExtra(SourceBookActivity.EXTRA_SOURCE_ID, book.sourceId)
                putExtra(SourceBookActivity.EXTRA_BOOK_URL, book.bookUrl)
                putExtra(SourceBookActivity.EXTRA_BOOK_NAME, book.name)
                putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, book.coverUrl)
                putExtra(SourceBookActivity.EXTRA_BOOK_STATUS, book.status)
            })
        }
        setOnLongClickListener { selectionMode = true; selectedBooks.clear(); toggleBookSelection(book); true }
        addView(BookStatusBadge.cover(this@FavoritesActivity, book.coverUrl, book.status, 72, 94),
            LinearLayout.LayoutParams(dp(72), dp(94)))
        addView(TextView(this@FavoritesActivity).apply { text = book.name; textSize = 12f; maxLines = 2; gravity = Gravity.CENTER; setTextColor(ink) })
        addView(TextView(this@FavoritesActivity).apply { text = book.sourceId; textSize = 9f; maxLines = 1; setTextColor(muted); setPadding(0, dp(4), 0, 0) })
    }


    private fun toggleBookSelection(book: FavoriteBook) {
        val key = bookKey(book)
        if (selectedBooks.containsKey(key)) selectedBooks.remove(key) else selectedBooks[key] = book
        if (selectedBooks.isEmpty()) selectionMode = false
        refresh()
    }

    private fun renderSelectionBar() {
        bottomActions.removeAllViews()
        bottomActions.visibility = if (selectionMode) View.VISIBLE else View.GONE
        if (!selectionMode) return
        val count = selectedBooks.size
        bottomActions.addView(actionText("移到文件夹（" + count + "本）") { showBatchMoveDialog() },
            LinearLayout.LayoutParams(0, dp(48), 1f))
        bottomActions.addView(actionText("删除（" + count + "本）") { deleteSelectedBooks() }.apply { setTextColor(Color.rgb(190, 75, 75)) },
            LinearLayout.LayoutParams(0, dp(48), 1f))
        bottomActions.addView(actionText("取消") { selectedBooks.clear(); selectionMode = false; refresh() },
            LinearLayout.LayoutParams(0, dp(48), 0.6f))
    }

    private fun showBatchMoveDialog() {
        if (selectedBooks.isEmpty()) return

        val folders = FavoriteBooks.folders(this)
        val dialog = Dialog(this)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(16))
            background = GradientDrawable().apply {
                setColor(card)
                cornerRadii = floatArrayOf(
                    dp(20).toFloat(), dp(20).toFloat(),
                    dp(20).toFloat(), dp(20).toFloat(),
                    0f, 0f, 0f, 0f,
                )
            }
        }

        panel.addView(View(this).apply {
            background = rounded(Color.rgb(220, 224, 230), 4)
        }, LinearLayout.LayoutParams(dp(38), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(14)
        })
        panel.addView(TextView(this).apply {
            text = "移动到"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }, LinearLayout.LayoutParams(-1, dp(42)))

        panel.addView(TextView(this).apply {
            text = "＋  新建文件夹"
            textSize = 15f
            setTextColor(blue)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(12), dp(4), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                dialog.dismiss()
                promptFolderName("新建文件夹") { name ->
                    if (!FavoriteBooks.createFolder(this@FavoritesActivity, name)) {
                        toast("文件夹名称无效或已存在")
                    } else {
                        val target = FavoriteBooks.folders(this@FavoritesActivity).last()
                        moveSelectedBooksToFolder(target.id)
                    }
                }
            }
        }, LinearLayout.LayoutParams(-1, dp(48)))

        val folderRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        folders.forEach { folder ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(dp(4), dp(11), dp(4), dp(11))
                addView(TextView(this@FavoritesActivity).apply {
                    text = folder.name
                    textSize = 15f
                    setTextColor(ink)
                    maxLines = 1
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(TextView(this@FavoritesActivity).apply {
                    text = favoriteBookCountInFolder(FavoriteBooks.all(this@FavoritesActivity), folder.id).toString() + "本"
                    textSize = 12f
                    setTextColor(muted)
                })
                setOnClickListener {
                    dialog.dismiss()
                    moveSelectedBooksToFolder(folder.id)
                }
            }
            folderRows.addView(row, LinearLayout.LayoutParams(-1, dp(48)))
        }
        val folderScroll = ScrollView(this).apply {
            isFillViewport = false
            addView(folderRows)
        }
        panel.addView(folderScroll, LinearLayout.LayoutParams(-1, dp(220)))

        panel.addView(TextView(this).apply {
            text = "取消"
            textSize = 15f
            setTextColor(ink)
            gravity = Gravity.CENTER
            background = rounded(if (darkMode) Color.rgb(50, 55, 64) else Color.rgb(245, 247, 250), 10)
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }, LinearLayout.LayoutParams(-1, dp(46)).apply {
            topMargin = dp(12)
        })

        dialog.setContentView(panel)
        dialog.setCancelable(true)
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setDimAmount(0.28f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    private fun moveSelectedBooksToFolder(folderId: String) {
        val moving = selectedBooks.values.toList()
        if (moving.isEmpty()) return
        moving.forEach { FavoriteBooks.moveToFolder(this, it, folderId) }
        selectedFolderId = folderId
        folderMode = true
        getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE)
            .edit().putBoolean("folder_mode", true).apply()
        selectedBooks.clear()
        selectionMode = false
        refresh()
    }

    private fun deleteSelectedBooks() {
        val books = selectedBooks.values.toList()
        if (books.isEmpty()) return
        BadcatTheme.alertBuilder(this).setTitle("删除收藏").setMessage("确定删除选中的 " + books.size + " 本小说吗？")
            .setNegativeButton("取消", null).setPositiveButton("删除") { _, _ ->
                books.forEach { FavoriteBooks.remove(this, it.sourceId, it.bookUrl) }
                selectedBooks.clear(); selectionMode = false; refresh()
            }.show()
    }

    private fun bookListRow(book: FavoriteBook): View = LinearLayout(this).apply {
        val isSelected = selectedBooks.containsKey(bookKey(book))
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = rounded(if (isSelected) selectedFolder else card, 10,
            if (isSelected) blue else Color.TRANSPARENT)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        setOnClickListener { if (selectionMode) toggleBookSelection(book) else startActivity(Intent(this@FavoritesActivity, SourceBookActivity::class.java).apply {
            putExtra(SourceBookActivity.EXTRA_SOURCE_ID, book.sourceId); putExtra(SourceBookActivity.EXTRA_BOOK_URL, book.bookUrl)
            putExtra(SourceBookActivity.EXTRA_BOOK_NAME, book.name); putExtra(SourceBookActivity.EXTRA_BOOK_COVER_URL, book.coverUrl)
            putExtra(SourceBookActivity.EXTRA_BOOK_STATUS, book.status)
        }) }
        setOnLongClickListener { selectionMode = true; selectedBooks.clear(); toggleBookSelection(book); true }
        addView(BookStatusBadge.cover(this@FavoritesActivity, book.coverUrl, book.status, 48, 64),
            LinearLayout.LayoutParams(dp(48), dp(64)))
        addView(TextView(this@FavoritesActivity).apply { text = book.name; textSize = 14f; setTextColor(ink); setPadding(dp(12), 0, 0, 0) },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (selectedBooks.containsKey(bookKey(book))) addView(TextView(this@FavoritesActivity).apply { text = "✓"; textSize = 20f; setTextColor(blue) })
    }

    private fun showFolderActions(folder: FavoriteFolder) {
        val actions = if (folder.id == FavoriteBooks.DEFAULT_FOLDER_ID) arrayOf("打开文件夹") else arrayOf("打开文件夹", "重命名", "删除文件夹")
        BadcatTheme.alertBuilder(this).setTitle(folder.name).setItems(actions) { _, which ->
            when (actions[which]) {
                "打开文件夹" -> { selectedFolderId = folder.id; refresh() }
                "重命名" -> promptFolderName("重命名文件夹", folder.name) { name ->
                    if (!FavoriteBooks.renameFolder(this, folder.id, name)) toast("名称无效或已存在")
                    refresh()
                }
                "删除文件夹" -> BadcatTheme.alertBuilder(this).setTitle("删除文件夹")
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
        BadcatTheme.alertBuilder(this).setTitle(book.name).setItems(options) { _, which ->
            if (which == 0) BadcatTheme.alertBuilder(this).setMessage("确定移出《${book.name}》吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("移出") { _, _ -> FavoriteBooks.remove(this, book.sourceId, book.bookUrl); refresh() }.show()
            else {
                val folders = FavoriteBooks.folders(this)
                BadcatTheme.alertBuilder(this).setTitle("移动到文件夹").setItems(folders.map { it.name }.toTypedArray()) { _, index ->
                    FavoriteBooks.moveToFolder(this, book, folders[index].id)
                    selectedFolderId = folders[index].id
                    refresh()
                }.show()
            }
        }.show()
    }

    private fun showMoreMenu(anchor: View) {
        PopupMenu(BadcatTheme.themedContext(this), anchor).apply {
            menu.add(if (listMode) "宫格模式" else "列表模式")
            menu.add("更新时间排序")
            menu.add(if (folderMode) "关闭小说文件夹" else "开启小说文件夹")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "列表模式", "宫格模式" -> {
                        listMode = item.title.toString() == "列表模式"
                        getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).edit().putBoolean("list_mode", listMode).apply()
                        refresh()
                    }
                    "更新时间排序" -> {
                        val newestFirst = getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).getBoolean("sort_newest_first", true)
                        getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).edit().putBoolean("sort_newest_first", !newestFirst).apply()
                        toast(if (!newestFirst) "已按更新时间倒序排列" else "已按更新时间正序排列")
                        refresh()
                    }
                    "开启小说文件夹", "关闭小说文件夹" -> {
                        folderMode = !folderMode; selectedFolderId = FavoriteBooks.DEFAULT_FOLDER_ID
                        getSharedPreferences("badcat_favorites_ui", MODE_PRIVATE).edit().putBoolean("folder_mode", folderMode).apply()
                        refresh()
                    }
                }
                true
            }
            show()
        }
    }

    private fun promptFolderName(title: String, initial: String = "", onSave: (String) -> Unit) {
        val input = EditText(this).apply { setText(initial); hint = "输入文件夹名称"; setSingleLine(true) }
        BadcatTheme.alertBuilder(this).setTitle(title).setView(input)
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
