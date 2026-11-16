package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Personal tools hub; keeps utility screens out of the original search home. */
class MyActivity : Activity() {
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "我的"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        root.addView(TextView(this).apply {
            text = "我的"
            textSize = 24f
            setTextColor(ink)
            setPadding(0, 0, 0, dp(18))
        })
        addEntry(root, "书源管理", "导入、启用和整理阅读书源") {
            startActivity(Intent(this, SourceCatalogActivity::class.java))
        }
        addEntry(root, "阅读记录", "继续阅读或管理历史记录") {
            startActivity(Intent(this, ReadingHistoryActivity::class.java))
        }
        addEntry(root, "正文替换规则", "管理 Legado 正文净化规则") {
            startActivity(Intent(this, ReplaceRuleActivity::class.java))
        }
        setContentView(root)
    }

    private fun addEntry(root: LinearLayout, title: String, detail: String, action: () -> Unit) {
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            setBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            addView(TextView(this@MyActivity).apply {
                text = title
                textSize = 16f
                setTextColor(ink)
            })
            addView(TextView(this@MyActivity).apply {
                text = detail
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(5), 0, 0)
            })
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
