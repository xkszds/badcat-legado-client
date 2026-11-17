package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Layout-only preferences. Search presentation is deliberately nested here. */
class LayoutSettingsActivity : Activity() {
    private var dark = false
    private val ink get() = if (dark) Color.rgb(235, 237, 240) else Color.rgb(48, 55, 66)
    private val muted get() = if (dark) Color.rgb(165, 172, 182) else Color.rgb(135, 145, 158)
    private val page get() = if (dark) Color.rgb(25, 28, 33) else Color.rgb(247, 249, 252)
    private val card get() = if (dark) Color.rgb(38, 42, 49) else Color.WHITE
    private lateinit var styleSummary: TextView
    private lateinit var positionSummary: TextView
    private lateinit var heightSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = getSharedPreferences("badcat_appearance", MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"
        window.statusBarColor = page
        window.navigationBarColor = page
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (dark) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(18), dp(8))
            setBackgroundColor(card)
            addView(TextView(this@LayoutSettingsActivity).apply {
                text = "‹"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(ink)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(42), dp(46)))
            addView(TextView(this@LayoutSettingsActivity).apply {
                text = "布局"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            })
        }
        root.addView(header)

        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14)
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        styleSummary = TextView(this).apply { textSize = 12f; setTextColor(muted) }
        positionSummary = TextView(this).apply { textSize = 12f; setTextColor(muted) }
        heightSummary = TextView(this).apply { textSize = 12f; setTextColor(muted) }
        addRow(group, "首页搜索栏样式", styleSummary) { chooseStyle() }
        divider(group)
        addRow(group, "首页搜索栏位置", positionSummary) { choosePosition() }
        divider(group)
        addRow(group, "首页搜索栏高度", heightSummary) { chooseHeight() }

        root.addView(group, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(14), dp(16), dp(14), 0)
        })
        root.addView(TextView(this).apply {
            text = "首页搜索栏点击后会进入书籍搜索页面，并自动定位到唯一的搜索输入框。"
            textSize = 12f; setTextColor(muted); setPadding(dp(18), dp(12), dp(18), 0)
        })
        setContentView(root)
        refreshSummaries()
    }

    private fun addRow(parent: LinearLayout, title: String, summary: TextView, action: () -> Unit) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(14), dp(4), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            addView(TextView(this@LayoutSettingsActivity).apply {
                text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink)
            })
            summary.setPadding(0, dp(5), 0, 0)
            addView(summary)
        })
    }

    private fun chooseStyle() {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        val current = prefs.getString("home_layout_mode", "integrated") ?: "integrated"
        val labels = arrayOf("紧凑型（更矮）", "整合型")
        BadcatTheme.alertBuilder(this).setTitle("首页搜索栏样式")
            .setSingleChoiceItems(labels, if (current == "small") 0 else 1) { dialog, which ->
                prefs.edit().putString("home_layout_mode", if (which == 0) "small" else "integrated").apply()
                dialog.dismiss()
                refreshSummaries()
            }.setNegativeButton("取消", null).show()
    }

    private fun choosePosition() {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        val current = prefs.getInt("home_search_top_spacing_dp", 12)
        val labels = arrayOf("靠上（4dp）", "居中（12dp）", "靠下（24dp）")
        val values = intArrayOf(4, 12, 24)
        BadcatTheme.alertBuilder(this).setTitle("首页搜索栏位置")
            .setSingleChoiceItems(labels, values.indexOf(current).takeIf { it >= 0 } ?: 1) { dialog, which ->
                prefs.edit().putInt("home_search_top_spacing_dp", values[which]).apply()
                dialog.dismiss()
                refreshSummaries()
            }.setNegativeButton("取消", null).show()
    }

    private fun chooseHeight() {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        val current = prefs.getInt("home_search_bar_height_dp", 40)
        val values = intArrayOf(34, 38, 42, 46)
        val labels = arrayOf("更矮（34dp）", "偏矮（38dp）", "标准（42dp）", "稍高（46dp）")
        BadcatTheme.alertBuilder(this).setTitle("首页搜索栏高度")
            .setSingleChoiceItems(labels, values.indexOf(current).takeIf { it >= 0 } ?: 1) { dialog, which ->
                prefs.edit().putInt("home_search_bar_height_dp", values[which]).apply()
                dialog.dismiss()
                refreshSummaries()
            }.setNegativeButton("取消", null).show()
    }

    private fun refreshSummaries() {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        styleSummary.text = if (prefs.getString("home_layout_mode", "integrated") == "small") "紧凑型" else "整合型"
        positionSummary.text = when (prefs.getInt("home_search_top_spacing_dp", 24)) {
            8 -> "靠上（8dp）"
            40 -> "靠下（40dp）"
            else -> "居中（12dp）"
        }
        heightSummary.text = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getInt("home_search_bar_height_dp", 40).coerceIn(34, 46).let { "高度（${it}dp）" }
    }

    private fun divider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(if (dark) Color.rgb(57, 62, 70) else Color.rgb(238, 240, 243))
        }, LinearLayout.LayoutParams(-1, dp(1)))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        val currentDark = BadcatTheme.isDark(this)
        if (currentDark != dark) {
            dark = currentDark
            recreate()
            return
        }
        BadcatTheme.applyTree(this, window.decorView)
    }

}
