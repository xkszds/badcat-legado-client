package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

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
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(16), dp(6))
            setBackgroundColor(Color.WHITE)
        }
        header.addView(BadcatLineIconView(this, "back", Color.rgb(48, 55, 66)).apply {
            contentDescription = "返回"
            isClickable = true
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
        header.addView(LinearLayout(this@ReadingHistoryActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@ReadingHistoryActivity).apply {
                text = "阅读记录"
                textSize = 18f
                setTextColor(Color.rgb(45, 52, 62))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@ReadingHistoryActivity).apply {
                text = "点击作品可从上次阅读章节继续"
                textSize = 11f
                setTextColor(Color.GRAY)
                setPadding(0, dp(3), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "清空"
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.rgb(190, 75, 75))
            setPadding(dp(10), dp(8), dp(4), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { confirmClearHistory() }
        })
        root.addView(header)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refresh()
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun refresh() {
        list.removeAllViews()
        val records = BadcatReadingStateRepository(this).historyRecords()
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
            val name = item.bookName
            val chapterTitle = item.chapterTitle
            list.addView(TextView(this).apply {
                text = "$name\n$chapterTitle\n书源：${item.sourceId}"
                textSize = 15f
                setTextColor(Color.rgb(48, 55, 66))
                setPadding(dp(14), dp(16), dp(14), dp(16))
                setBackgroundColor(Color.WHITE)
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@ReadingHistoryActivity, SourceChapterActivity::class.java).apply {
                        putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, item.sourceId)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_URL, item.bookUrl)
                        putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, name)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, item.chapterUrl)
                        putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, chapterTitle)
                    })
                }
                setOnLongClickListener {
                    BadcatTheme.alertBuilder(this@ReadingHistoryActivity)
                        .setTitle("删除阅读记录")
                        .setMessage("仅删除《$name》的历史记录，不删除书籍或书签。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            BadcatReadingStateRepository(this@ReadingHistoryActivity).removeHistory(item.sourceId, item.bookUrl)
                            refresh()
                        }.show()
                    true
                }
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(9) })
        }
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun confirmClearHistory() {
        val count = BadcatReadingStateRepository(this).historyRecords().size
        if (count == 0) {
            BadcatTheme.alertBuilder(this).setMessage("当前没有阅读记录").setPositiveButton("知道了", null).show()
            return
        }
        BadcatTheme.alertBuilder(this).setTitle("清空阅读记录")
            .setMessage("将删除全部 ${count} 条阅读记录，不会删除收藏、书签或阅读进度。")
            .setNegativeButton("取消", null)
            .setPositiveButton("清空") { _, _ ->
                BadcatReadingStateRepository(this@ReadingHistoryActivity).clearHistory()
                refresh()
            }.show()
    }

    private fun removeRecord(sourceId: String, bookUrl: String) {
        BadcatReadingStateRepository(this).removeHistory(sourceId, bookUrl)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "badcat_reading_history"
        const val KEY = "records"

        fun save(context: android.content.Context, sourceId: String, bookUrl: String, bookName: String, chapterUrl: String, chapterTitle: String) {
            BadcatReadingStateRepository(context).saveHistory(
                sourceId, bookUrl, bookName, chapterUrl, chapterTitle,
            )
        }
    }
}
