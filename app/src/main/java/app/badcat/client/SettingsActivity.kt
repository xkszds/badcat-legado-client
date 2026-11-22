package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Settings hub for appearance, search layout and reader controls. */
class SettingsActivity : Activity() {
    private var dark = false
    private val ink get() = if (dark) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (dark) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val blue = Color.rgb(55, 137, 222)
    private val page get() = if (dark) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)
    private val card get() = if (dark) Color.rgb(38, 42, 49) else Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = isDarkMode()
        applyWindowTheme()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(page) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(18), dp(8))
            setBackgroundColor(card)
            addView(TextView(this@SettingsActivity).apply {
                text = "‹"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(ink)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(42), dp(46)))
            addView(TextView(this@SettingsActivity).apply {
                text = "设置"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            }, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(20))
        }
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14)
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        addSetting(group, "布局", "首页搜索栏样式、位置与页面布局") {
            startActivity(Intent(this, LayoutSettingsActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "阅读设置", "音量键翻页、锁屏时间、进度、时间、电量和状态栏") {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "正文替换规则", "管理阅读内容替换规则") {
            startActivity(Intent(this, ReplaceRuleActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "广告过滤", "网页注入开关与本地 AdMark 标记管理") {
            startActivity(Intent(this, BadcatPurificationActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "书源管理", "导入、启用和管理 Legado 书源") {
            startActivity(Intent(this, SourceCatalogActivity::class.java))
        }
        content.addView(group)
        content.addView(TextView(this).apply {
            text = "坏坏猫增强客户端 · 0.1.0-integration"
            textSize = 12f; gravity = Gravity.CENTER; setTextColor(muted); setPadding(0, dp(22), 0, 0)
        })
        root.addView(content)
        setContentView(root)
    }

    private fun isDarkMode() = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
        .getString("appearance_mode", "light") == "dark"

    private fun applyWindowTheme() {
        window.statusBarColor = page
        window.navigationBarColor = page
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (dark) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
        val current = isDarkMode()
        if (current != dark) recreate()
    }

    private fun addSetting(parent: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(13), dp(2), dp(13))
            isClickable = true; isFocusable = true; setOnClickListener { action() }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@SettingsActivity).apply {
                    text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = subtitle; textSize = 12f; setTextColor(muted); setPadding(0, dp(5), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@SettingsActivity).apply {
                text = "›"; textSize = 25f; setTextColor(muted); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(28), dp(40)))
        })
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply { setBackgroundColor(if (dark) Color.rgb(57, 62, 70) else Color.rgb(238, 240, 243)) },
            LinearLayout.LayoutParams(-1, dp(1)))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
