package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

/** Reading history backed by the same per-book progress store as the reader. */
class ReadingHistoryActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        root.addView(TextView(this).apply {
            text = "阅读记录"
            textSize = 23f
            setTextColor(Color.rgb(45, 52, 62))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "点击作品可从上次阅读章节继续"
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
        BadcatTheme.applyTree(this, window.decorView)
        if (::list.isInitialized) refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "[]").orEmpty()
        val records = runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                json.optJSONObject(index)?.let { item ->
                    val source = item.optString("sourceId")
                    val bookUrl = item.optString("bookUrl")
                    val chapterUrl = item.optString("chapterUrl")
                    if (source.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) null else item
                }
            }
        }.getOrDefault(emptyList())
        if (records.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "还没有阅读记录\n打开一本书并阅读章节后，记录会自动出现在这里。"
                textSize = 15f
                setTextColor(Color.GRAY)
                setPadding(dp(12), dp(36), dp(12), dp(36))
            })
            return
        }
        records.forEach { item ->
            val name = item.optString("bookName").ifBlank { "未命名书籍" }
            val chapterTitle = item.optString("chapterTitle").ifBlank { "继续阅读" }
            list.addView(TextView(this).apply {
                text = "$name\n$chapterTitle\n书源：${item.optString("sourceId")}"
                textSize = 15f
                setTextColor(Color.rgb(48, 55, 66))
                setPadding(dp(14), dp(16), dp(14), dp(16))
                setBackgroundColor(Color.WHITE)
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@ReadingHistoryActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, item.optString("sourceId"))
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, item.optString("bookUrl"))
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, name)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, item.optString("chapterUrl"))
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapterTitle)
                    })
                }
                setOnLongClickListener {
                    android.app.AlertDialog.Builder(this@ReadingHistoryActivity)
                        .setTitle("删除阅读记录")
                        .setMessage("仅删除《$name》的历史记录，不删除书籍或书签。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            removeRecord(item.optString("sourceId"), item.optString("bookUrl"))
                            refresh()
                        }.show()
                    true
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) })
        }
    }

    private fun removeRecord(sourceId: String, bookUrl: String) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val old = runCatching { JSONArray(prefs.getString(KEY, "[]").orEmpty()) }.getOrDefault(JSONArray())
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optString("sourceId") != sourceId || item.optString("bookUrl") != bookUrl) next.put(item)
        }
        prefs.edit().putString(KEY, next.toString()).apply()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "badcat_reading_history"
        const val KEY = "records"

        fun save(context: android.content.Context, sourceId: String, bookUrl: String, bookName: String, chapterUrl: String, chapterTitle: String) {
            if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) return
            val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            val old = runCatching { JSONArray(prefs.getString(KEY, "[]").orEmpty()) }.getOrDefault(JSONArray())
            val next = JSONArray()
            for (i in 0 until old.length()) {
                val item = old.optJSONObject(i) ?: continue
                if (item.optString("sourceId") != sourceId || item.optString("bookUrl") != bookUrl) next.put(item)
            }
            next.put(JSONObject()
                .put("sourceId", sourceId)
                .put("bookUrl", bookUrl)
                .put("bookName", bookName)
                .put("chapterUrl", chapterUrl)
                .put("chapterTitle", chapterTitle)
                .put("updatedAt", System.currentTimeMillis()))
            prefs.edit().putString(KEY, next.toString()).apply()
        }
    }
}
