package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Personal center: only routes backed by implemented features are shown. */
class MyActivity : Activity() {
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val blue = Color.rgb(55, 137, 222)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(profileHeader())
        content.addView(sectionCard())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun profileHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(24), dp(20), dp(24))
        setBackgroundColor(Color.WHITE)
        addView(TextView(this@MyActivity).apply {
            text = "我的"; textSize = 22f; setTextColor(ink)
        })
        val identity = LinearLayout(this@MyActivity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(4))
        }
        identity.addView(TextView(this@MyActivity).apply {
            text = "🐱"; textSize = 38f; gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(Color.rgb(245, 247, 250))
            }
        }, LinearLayout.LayoutParams(dp(66), dp(66)))
        identity.addView(LinearLayout(this@MyActivity).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0)
            addView(TextView(this@MyActivity).apply {
                text = "阅读中心"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            })
            addView(TextView(this@MyActivity).apply {
                text = "管理阅读记录、小说书签和书源"
                textSize = 12f; setTextColor(muted); setPadding(0, dp(7), 0, 0)
            })
        })
        addView(identity)
    }

    private fun sectionCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(14).toFloat()
        }
        val params = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(14), dp(14), dp(14), dp(14))
        }
        addView(TextView(this@MyActivity).apply {
            text = "常用功能"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink); setPadding(0, 0, 0, dp(18))
        })
        val entries = listOf(
            Entry("◷", "阅读记录", "继续阅读或管理历史记录") {
                startActivity(Intent(this@MyActivity, ReadingHistoryActivity::class.java))
            },
            Entry("▣", "书签管理", "查看和打开小说阅读书签") {
                startActivity(Intent(this@MyActivity, BookmarkActivity::class.java))
            },
            Entry("◇", "书源管理", "导入、启用和整理阅读书源") {
                startActivity(Intent(this@MyActivity, SourceCatalogActivity::class.java))
            },
            Entry("≋", "正文替换规则", "管理 Legado 正文替换规则") {
                startActivity(Intent(this@MyActivity, ReplaceRuleActivity::class.java))
            },
        )
        entries.chunked(2).forEach { pair ->
            val row = LinearLayout(this@MyActivity).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { entry ->
                row.addView(LinearLayout(this@MyActivity).apply {
                    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                    setPadding(dp(5), dp(10), dp(5), dp(15))
                    isClickable = true; isFocusable = true
                    setOnClickListener { entry.action() }
                    addView(TextView(this@MyActivity).apply {
                        text = entry.icon; textSize = 26f; gravity = Gravity.CENTER
                        setTextColor(blue)
                    }, LinearLayout.LayoutParams(dp(54), dp(54)))
                    addView(TextView(this@MyActivity).apply {
                        text = entry.title; textSize = 14f; gravity = Gravity.CENTER
                        setTextColor(ink)
                    })
                }, LinearLayout.LayoutParams(0, -2, 1f))
            }
            if (pair.size == 1) row.addView(View(this@MyActivity), LinearLayout.LayoutParams(0, 1, 1f))
            addView(row)
        }
        layoutParams = params
    }

    private data class Entry(val icon: String, val title: String, val detail: String, val action: () -> Unit)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
