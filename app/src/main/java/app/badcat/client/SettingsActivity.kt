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

/** Settings hub; each row opens an existing settings/customization surface. */
class SettingsActivity : Activity() {
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val blue = Color.rgb(55, 137, 222)
    private val page = Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(18), dp(8))
            setBackgroundColor(Color.WHITE)
            addView(TextView(this@SettingsActivity).apply {
                text = "‹"
                textSize = 32f
                gravity = Gravity.CENTER
                setTextColor(ink)
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(42), dp(46)))
            addView(TextView(this@SettingsActivity).apply {
                text = "设置"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ink)
            }, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(20))
        }
        val group = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 14)
            setPadding(dp(16), dp(4), dp(16), dp(4))
        }
        val currentLayout = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
            .getString("home_layout_mode", "integrated") ?: "integrated"
        addSetting(group, "布局", "主页样式：" + if (currentLayout == "small") "坏坏猫小字" else "整合在一起") {
            showHomeLayoutOptions()
        }
        addDivider(group)
        addSetting(group, "阅读界面自定义", "管理 CSS 方案、子选择器和阅读样式") {
            startActivity(Intent(this, ReaderCssActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "正文替换规则", "管理阅读内容替换规则") {
            startActivity(Intent(this, ReplaceRuleActivity::class.java))
        }
        addDivider(group)
        addSetting(group, "书源管理", "导入、启用和管理 Legado 书源") {
            startActivity(Intent(this, SourceCatalogActivity::class.java))
        }
        content.addView(group)
        content.addView(TextView(this).apply {
            text = "坏坏猫增强客户端 · 0.1.0-integration"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(muted)
            setPadding(0, dp(22), 0, 0)
        })
        root.addView(content)
        setContentView(root)
    }

    private fun showHomeLayoutOptions() {
        val prefs = getSharedPreferences("badcat_home_layout", MODE_PRIVATE)
        val options = arrayOf("坏坏猫小字", "整合在一起")
        val current = prefs.getString("home_layout_mode", "integrated") ?: "integrated"
        val selected = if (current == "small") 0 else 1
        AlertDialog.Builder(this)
            .setTitle("主页布局")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                prefs.edit().putString("home_layout_mode", if (which == 0) "small" else "integrated").apply()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun addSetting(parent: LinearLayout, title: String, subtitle: String, action: () -> Unit) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(13), dp(2), dp(13))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@SettingsActivity).apply {
                    text = title
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ink)
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(muted)
                    setPadding(0, dp(5), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@SettingsActivity).apply {
                text = "›"
                textSize = 25f
                setTextColor(muted)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(dp(28), dp(40)))
        })
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(this).apply {
            setBackgroundColor(Color.rgb(238, 240, 243))
        }, LinearLayout.LayoutParams(-1, dp(1)))
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
