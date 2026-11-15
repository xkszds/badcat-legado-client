package app.badcat.client

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteBook(val sourceId: String, val bookUrl: String, val name: String)

object FavoriteBooks {
    private const val PREFS = "badcat_favorites"
    private const val KEY = "books"

    fun all(context: Context): List<FavoriteBook> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                val source = item.optString("sourceId")
                val url = item.optString("bookUrl")
                if (source.isBlank() || url.isBlank()) null
                else FavoriteBook(source, url, item.optString("name").ifBlank { "未命名书籍" })
            }
        }.getOrDefault(emptyList())
    }

    fun contains(context: Context, sourceId: String, bookUrl: String): Boolean =
        all(context).any { it.sourceId == sourceId && it.bookUrl == bookUrl }

    fun add(context: Context, sourceId: String, bookUrl: String, name: String): Boolean {
        if (sourceId.isBlank() || bookUrl.isBlank() || contains(context, sourceId, bookUrl)) return false
        save(context, all(context).toMutableList().apply {
            add(0, FavoriteBook(sourceId, bookUrl, name.ifBlank { "未命名书籍" }))
        })
        return true
    }

    fun remove(context: Context, sourceId: String, bookUrl: String) {
        save(context, all(context).filterNot { it.sourceId == sourceId && it.bookUrl == bookUrl })
    }

    private fun save(context: Context, books: List<FavoriteBook>) {
        val json = JSONArray()
        books.forEach { book ->
            json.put(JSONObject().put("sourceId", book.sourceId).put("bookUrl", book.bookUrl).put("name", book.name))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }
}

class FavoritesActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 249, 252))
            setPadding(dp(18), dp(20), dp(18), dp(12))
        }
        root.addView(TextView(this).apply {
            text = "我的收藏"
            textSize = 23f
            setTextColor(Color.rgb(45, 52, 62))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "长按书籍可移出收藏"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(6), 0, dp(14))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val books = FavoriteBooks.all(this)
        if (books.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "书架还是空的\n在书籍详情页点击「加入收藏」，作品就会出现在这里。"
                textSize = 15f
                setTextColor(Color.GRAY)
                setPadding(dp(12), dp(36), dp(12), dp(36))
            })
            return
        }
        books.forEach { book ->
            val row = TextView(this).apply {
                text = book.name + "\n书源：" + book.sourceId
                textSize = 15f
                setTextColor(Color.rgb(48, 55, 66))
                setPadding(dp(14), dp(16), dp(14), dp(16))
                setBackgroundColor(Color.WHITE)
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@FavoritesActivity, SourceBookActivity::class.java).apply {
                        putExtra(SourceBookActivity.EXTRA_SOURCE_ID, book.sourceId)
                        putExtra(SourceBookActivity.EXTRA_BOOK_URL, book.bookUrl)
                        putExtra(SourceBookActivity.EXTRA_BOOK_NAME, book.name)
                    })
                }
                setOnLongClickListener {
                    android.app.AlertDialog.Builder(this@FavoritesActivity)
                        .setTitle("移出收藏")
                        .setMessage("确定移除《${book.name}》吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("移除") { _, _ ->
                            FavoriteBooks.remove(this@FavoritesActivity, book.sourceId, book.bookUrl)
                            Toast.makeText(this@FavoritesActivity, "已移出收藏", Toast.LENGTH_SHORT).show()
                            refresh()
                        }.show()
                    true
                }
            }
            list.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) })
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
