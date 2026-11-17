package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private data class ReaderBookmark(
    val key: String,
    val sourceId: String,
    val bookUrl: String,
    val chapterUrl: String,
    val bookName: String,
    val chapterTitle: String,
    val scrollY: Int,
    val savedAt: Long,
)

class BookmarkActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("badcat_reading_progress", MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val blue = Color.rgb(55, 137, 222)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(247, 249, 252)) }
        root.addView(TextView(this).apply {
            text = "书签管理"; textSize = 22f; setTextColor(ink)
            setPadding(dp(20), dp(20), dp(20), dp(16))
            setBackgroundColor(Color.WHITE)
        })
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(20)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    override fun onResume() { super.onResume(); if (::content.isInitialized) refresh(); BadcatTheme.applyTree(this, window.decorView) }

    private fun readBookmarks(): List<ReaderBookmark> = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith("chapter_bookmark_") || value !is Int) return@mapNotNull null
        val raw = key.removePrefix("chapter_bookmark_")
        val decoded = runCatching {
            String(Base64.decode(raw, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8).split("\n")
        }.getOrNull() ?: return@mapNotNull null
        if (decoded.size != 3 || decoded.any { it.isBlank() }) return@mapNotNull null
        val titleKey = "chapter_bookmark_title_" + raw
        ReaderBookmark(
            key, decoded[0], decoded[1], decoded[2],
            prefs.getString("chapter_bookmark_book_" + raw, null).orEmpty().ifBlank { "未命名小说" },
            prefs.getString(titleKey, null).orEmpty().ifBlank { decoded[2].substringAfterLast('/').take(48).ifBlank { "章节书签" } },
            value.coerceAtLeast(0),
            prefs.getLong("chapter_bookmark_time_" + raw, 0L),
        )
    }.sortedWith(compareByDescending<ReaderBookmark> { it.savedAt }.thenByDescending { it.key })

    private fun refresh() {
        content.removeAllViews()
        val items = readBookmarks()
        if (items.isEmpty()) {
            content.addView(TextView(this).apply {
                text = "还没有书签\n在小说阅读页面点击「保存书签」，保存当前阅读位置。"
                textSize = 14f; gravity = Gravity.CENTER; setTextColor(muted)
                setPadding(dp(18), dp(54), dp(18), dp(54))
            })
            BadcatTheme.applyTree(this, window.decorView)
            return
        }
        content.addView(TextView(this).apply {
            text = "小说书签（${items.size}）"; textSize = 15f; setTextColor(ink)
            setPadding(dp(4), dp(4), 0, dp(10))
        })
        items.forEach { item ->
            content.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(13), dp(15), dp(12))
                setBackgroundColor(Color.WHITE)
                addView(TextView(this@BookmarkActivity).apply {
                    text = item.bookName; textSize = 16f; setTextColor(ink)
                })
                addView(TextView(this@BookmarkActivity).apply {
                    text = item.chapterTitle + " · 阅读位置 " + item.scrollY + " px"
                    textSize = 12f; setTextColor(muted); setPadding(0, dp(5), 0, dp(8))
                })
                val actions = LinearLayout(this@BookmarkActivity).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
                    addView(action("打开书签") {
                        startActivity(Intent(this@BookmarkActivity, SourceChapterActivity::class.java).apply {
                            putExtra(SourceChapterActivity.EXTRA_SOURCE_ID, item.sourceId)
                            putExtra(SourceChapterActivity.EXTRA_BOOK_URL, item.bookUrl)
                            putExtra(SourceChapterActivity.EXTRA_BOOK_NAME, item.bookName)
                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_URL, item.chapterUrl)
                            putExtra(SourceChapterActivity.EXTRA_CHAPTER_TITLE, item.chapterTitle)
                            putExtra(SourceChapterActivity.EXTRA_JUMP_TO_BOOKMARK, true)
                        })
                    })
                    addView(action("删除") {
                        BadcatTheme.alertBuilder(this@BookmarkActivity).setMessage("删除这条书签？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("删除") { _, _ ->
                                val raw = item.key.removePrefix("chapter_bookmark_")
                                prefs.edit().remove(item.key)
                                    .remove("chapter_bookmark_title_" + raw)
                                    .remove("chapter_bookmark_book_" + raw)
                                    .remove("chapter_bookmark_time_" + raw).apply()
                                refresh()
                            }.show()
                    })
                }
                addView(actions)
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
        }
        BadcatTheme.applyTree(this, window.decorView)
    }

    private fun action(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 13f; gravity = Gravity.CENTER; setTextColor(blue)
        setPadding(dp(12), dp(7), dp(8), dp(7)); setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
