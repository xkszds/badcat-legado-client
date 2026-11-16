package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Personal hub with the requested bookmark entry and no welfare section. */
class MyActivity : Activity() {
    private val ink = Color.rgb(48, 52, 60)
    private val muted = Color.rgb(145, 151, 160)
    private val blue = Color.rgb(75, 143, 226)
    private val page = Color.rgb(247, 248, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(page) }
        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(22))
            setBackgroundColor(Color.WHITE)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(label("我的", 18f, ink), LinearLayout.LayoutParams(0, dp(42), 1f))
        top.addView(label("☼", 26f, blue).apply { setOnClickListener { toast("主题设置尚未接入") } })
        top.addView(label("⚙", 25f, ink).apply { setPadding(dp(22), 0, 0, 0); setOnClickListener { toast("个人设置尚未接入") } })
        profile.addView(top)
        val identity = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        identity.addView(TextView(this).apply {
            text = "🐱"; textSize = 42f; gravity = Gravity.CENTER
            background = rounded(Color.rgb(245, 246, 248), 48)
        }, LinearLayout.LayoutParams(dp(76), dp(76)))
        identity.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(label(
                getSharedPreferences("badcat_profile", MODE_PRIVATE).getString("nickname", "坏坏猫用户") ?: "坏坏猫用户",
                21f, ink, true
            ).apply {
                isClickable = true
                setOnClickListener { editNickname(this) }
                contentDescription = "点击修改昵称"
            })
            addView(label("阅读时长：—    获得的收藏数：${FavoriteBooks.all(this@MyActivity).size}", 12f, muted).apply { setPadding(0, dp(9), 0, 0) })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        profile.addView(identity)
        root.addView(profile)

        val scroll = android.widget.ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(16), dp(14), dp(24)) }
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 16)
            setPadding(dp(16), dp(15), dp(16), dp(16))
        }
        tools.addView(label("常用功能", 18f, ink, true).apply { setPadding(0, 0, 0, dp(12)) })
        val entries = listOf(
            "我的消息" to "messages",
            "历史记录" to "history",
            "书签管理" to "bookmarks",
            "下载管理" to "downloads",
            "意见反馈" to "feedback",
            "同步收藏夹" to "sync",
            "分享坏坏猫" to "share",
            "书源管理" to "sources",
            "正文替换规则" to "replace"
        )
        entries.chunked(4).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { (title, key) ->
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(10), dp(3), dp(10))
                    isClickable = true
                    isFocusable = true
                    addView(label(when (key) {
                        "history" -> "◷"; "bookmarks" -> "◇"; "sources" -> "▤"
                        "replace" -> "≋"; "sync" -> "☁"; "downloads" -> "↓"
                        "feedback" -> "▣"; "share" -> "↗"; else -> "···"
                    }, 25f, blue))
                    addView(label(title, 12f, ink).apply { setPadding(0, dp(5), 0, 0) })
                    setOnClickListener { openTool(key) }
                }
                row.addView(cell, LinearLayout.LayoutParams(0, dp(78), 1f))
            }
            repeat(4 - group.size) { row.addView(android.view.View(this), LinearLayout.LayoutParams(0, dp(78), 1f)) }
            tools.addView(row)
        }
        body.addView(tools)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(MainBottomNavigation.create(this, "mine"))
        setContentView(root)
    }

    private fun editNickname(target: TextView) {
        val input = android.widget.EditText(this).apply {
            setText(target.text)
            hint = "输入昵称"
            setSingleLine(true)
            setSelection(text.length)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("修改昵称")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val value = input.text.toString().trim().take(24)
                if (value.isNotBlank()) {
                    getSharedPreferences("badcat_profile", MODE_PRIVATE).edit()
                        .putString("nickname", value).apply()
                    target.text = value
                } else {
                    toast("昵称不能为空")
                }
            }
            .show()
    }

    private fun openTool(key: String) {
        when (key) {
            "history" -> startActivity(Intent(this, ReadingHistoryActivity::class.java))
            "bookmarks" -> startActivity(Intent(this, BookmarkActivity::class.java))
            "sources" -> startActivity(Intent(this, SourceCatalogActivity::class.java))
            "replace" -> startActivity(Intent(this, ReplaceRuleActivity::class.java))
            "share" -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "坏坏猫阅读客户端")
            }, "分享"))
            else -> toast("该功能尚未接入")
        }
    }

    private fun toast(message: String) = android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
