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
import android.widget.Switch
import android.widget.TextView

/** Persistent controls for the reading screen. */
class ReaderSettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val blue = Color.rgb(55, 137, 222)
    private val page = Color.rgb(247, 249, 252)
    private lateinit var lockSummary: TextView
    private lateinit var progressSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = getSharedPreferences(APPEARANCE_PREFS, MODE_PRIVATE).getString(APPEARANCE_KEY, "light") == "dark"
        val bg = if (dark) Color.rgb(25, 28, 33) else page
        val card = if (dark) Color.rgb(38, 42, 49) else Color.WHITE
        val fg = if (dark) Color.rgb(235, 237, 240) else ink
        val secondary = if (dark) Color.rgb(165, 172, 182) else muted
        window.statusBarColor = bg
        window.navigationBarColor = bg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (dark) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(18), dp(8))
            setBackgroundColor(card)
            addView(TextView(this@ReaderSettingsActivity).apply {
                text = "‹"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(fg)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(42), dp(46)))
            addView(TextView(this@ReaderSettingsActivity).apply {
                text = "阅读设置"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(fg)
            })
        }
        root.addView(header)

        val scroll = android.widget.ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(24))
        }
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14)
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        addSwitch(group, "音量键翻页", "使用音量加减键向前或向后翻动正文", KEY_VOLUME_PAGE, true, fg, secondary)
        divider(group)
        lockSummary = addAction(group, "阅读锁屏时间", lockLabel(), "控制阅读时保持屏幕常亮的时长", fg, secondary) { chooseLockDuration() }
        divider(group)
        addSwitch(group, "展示阅读进度", "显示阅读进度条和进度信息", KEY_SHOW_PROGRESS, true, fg, secondary)
        divider(group)
        progressSummary = addAction(group, "阅读进度显示", progressLabel(), "选择百分比或章节页数", fg, secondary) { chooseProgressMode() }
        divider(group)
        addSwitch(group, "显示时间", "在阅读界面显示当前时间", KEY_SHOW_TIME, true, fg, secondary)
        divider(group)
        addSwitch(group, "显示电量", "在阅读界面显示手机电量", KEY_SHOW_BATTERY, true, fg, secondary)
        divider(group)
        addSwitch(group, "手机状态栏常驻展示", "阅读时保留系统顶部时间、信号和电量状态栏", KEY_STATUS_BAR, true, fg, secondary)
        content.addView(group)

        val css = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 14)
            setPadding(dp(16), dp(4), dp(16), dp(4))
            addView(TextView(this@ReaderSettingsActivity).apply {
                text = "阅读界面自定义"; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(fg); setPadding(0, dp(14), 0, dp(5))
            })
            addView(TextView(this@ReaderSettingsActivity).apply {
                text = "管理 CSS 方案、页面子选择器、备注和预览"
                textSize = 12f; setTextColor(secondary); setPadding(0, 0, 0, dp(12))
            })
            addView(TextView(this@ReaderSettingsActivity).apply {
                text = "打开自定义设置  ›"; textSize = 14f; setTextColor(blue)
                setPadding(0, dp(8), 0, dp(14))
                setOnClickListener { startActivity(Intent(this@ReaderSettingsActivity, ReaderCssActivity::class.java)) }
            })
        }
        content.addView(css, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun addSwitch(parent: LinearLayout, title: String, subtitle: String, key: String, default: Boolean, fg: Int, secondary: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            addView(LinearLayout(this@ReaderSettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@ReaderSettingsActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(fg) })
                addView(TextView(this@ReaderSettingsActivity).apply { text = subtitle; textSize = 12f; setTextColor(secondary); setPadding(0, dp(4), 0, 0) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(Switch(this@ReaderSettingsActivity).apply {
                isChecked = prefs.getBoolean(key, default)
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean(key, checked).apply() }
            })
        }
        parent.addView(row)
    }

    private fun addAction(parent: LinearLayout, title: String, summary: String, subtitle: String, fg: Int, secondary: Int, action: () -> Unit): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, dp(12))
            isClickable = true; isFocusable = true; setOnClickListener { action() }
            addView(TextView(this@ReaderSettingsActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(fg) })
            val detail = TextView(this@ReaderSettingsActivity).apply {
                text = summary; textSize = 13f; setTextColor(blue); setPadding(0, dp(4), 0, 0)
            }
            addView(detail)
            addView(TextView(this@ReaderSettingsActivity).apply { text = subtitle; textSize = 12f; setTextColor(secondary); setPadding(0, dp(3), 0, 0) })
            tag = detail
        }
        parent.addView(row)
        return row.tag as TextView
    }

    private fun chooseLockDuration() {
        val values = intArrayOf(0, 1, 5, 10, 30, 60)
        val labels = arrayOf("跟随系统", "1 分钟", "5 分钟", "10 分钟", "30 分钟", "60 分钟")
        val selected = values.indexOf(prefs.getInt(KEY_LOCK_MINUTES, 0)).coerceAtLeast(0)
        BadcatTheme.alertBuilder(this).setTitle("阅读锁屏时间")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                prefs.edit().putInt(KEY_LOCK_MINUTES, values[which]).apply()
                lockSummary.text = labels[which]
                dialog.dismiss()
            }.setNegativeButton("取消", null).show()
    }

    private fun chooseProgressMode() {
        val labels = arrayOf("百分比", "章节页数")
        val selected = if (prefs.getString(KEY_PROGRESS_MODE, "percent") == "pages") 1 else 0
        BadcatTheme.alertBuilder(this).setTitle("阅读进度显示")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val value = if (which == 1) "pages" else "percent"
                prefs.edit().putString(KEY_PROGRESS_MODE, value).apply()
                progressSummary.text = labels[which]
                dialog.dismiss()
            }.setNegativeButton("取消", null).show()
    }

    private fun lockLabel() = when (prefs.getInt(KEY_LOCK_MINUTES, 0)) {
        1 -> "1 分钟"; 5 -> "5 分钟"; 10 -> "10 分钟"; 30 -> "30 分钟"; 60 -> "60 分钟"; else -> "跟随系统"
    }
    private fun progressLabel() = if (prefs.getString(KEY_PROGRESS_MODE, "percent") == "pages") "章节页数" else "百分比"
    private fun divider(parent: LinearLayout) {
        val dark = getSharedPreferences(APPEARANCE_PREFS, MODE_PRIVATE)
            .getString(APPEARANCE_KEY, "light") == "dark"
        val color = if (dark) Color.rgb(58, 63, 72) else Color.rgb(235, 237, 240)
        parent.addView(View(this).apply { setBackgroundColor(color) }, LinearLayout.LayoutParams(-1, dp(1)))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS = "badcat_reader_ui"
        const val KEY_VOLUME_PAGE = "volume_page_turn"
        const val KEY_LOCK_MINUTES = "reading_lock_minutes"
        const val KEY_SHOW_PROGRESS = "show_reading_progress"
        const val KEY_PROGRESS_MODE = "reading_progress_mode"
        const val KEY_SHOW_TIME = "show_reading_time"
        const val KEY_SHOW_BATTERY = "show_reading_battery"
        const val KEY_STATUS_BAR = "keep_system_status_bar"
        const val APPEARANCE_PREFS = "badcat_appearance"
        const val APPEARANCE_KEY = "appearance_mode"
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
