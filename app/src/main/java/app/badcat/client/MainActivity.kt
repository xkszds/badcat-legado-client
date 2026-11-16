package app.badcat.client

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.badcat.client.runtime.BadcatReadingEngineFactory
import app.badcat.engine.ReadingEngine

/**
 * First-pass reconstruction of the Badcat search-home structure.
 *
 * Geometry and colors are based on the supplied reference descriptions;
 * exact pixel parity still requires decoded original resources and device screenshots.
 */
class MainActivity : Activity() {
    private lateinit var readingEngine: ReadingEngine
    private lateinit var keyword: EditText
    private var contentType = "小说"

    private val blue = Color.rgb(55, 137, 222)
    private val ink = Color.rgb(48, 55, 66)
    private val muted = Color.rgb(135, 145, 158)
    private val page = Color.rgb(247, 249, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = blue
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        readingEngine = BadcatReadingEngineFactory(applicationContext).create()
        setContentView(buildHome())
    }

    private fun buildHome(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(page)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        content.addView(buildHeader())
        content.addView(buildSearchPanel())
        content.addView(buildShortcuts())
        val spacer = View(this)
        content.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(content)
        root.addView(buildBottomNavigation())
        return root
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(28))
        background = rounded(blue, 24, 0, 0, 24, 24)
        addView(TextView(this@MainActivity).apply {
            text = "坏坏猫"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(this@MainActivity).apply {
            text = "发现好书，开始阅读"
            textSize = 13f
            setTextColor(Color.WHITE)
            alpha = 0.9f
            setPadding(0, dp(5), 0, 0)
        })
    }

    private fun buildSearchPanel(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(8))
        val typeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("小说", "漫画", "全部").forEach { label ->
            val chip = TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(22), dp(9), dp(22), dp(9))
                setOnClickListener {
                    contentType = label
                    for (i in 0 until typeRow.childCount) {
                        val child = typeRow.getChildAt(i) as TextView
                        child.setTextColor(if (child.text == contentType) blue else muted)
                        child.typeface = if (child.text == contentType) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    }
                }
                setTextColor(if (label == contentType) blue else muted)
                typeface = if (label == contentType) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            typeRow.addView(chip, LinearLayout.LayoutParams(0, dp(42), 1f))
        }
        addView(typeRow)
        val searchRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.WHITE, 24)
            elevation = dp(2).toFloat()
            setPadding(dp(14), 0, dp(6), 0)
        }
        keyword = EditText(this@MainActivity).apply {
            hint = "输入书名或作者"
            textSize = 15f
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
            background = null
            setPadding(0, dp(12), 0, dp(12))
        }
        searchRow.addView(keyword, LinearLayout.LayoutParams(0, dp(50), 1f))
        val go = TextView(this@MainActivity).apply {
            text = "搜索"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(blue, 20)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener { openSearch() }
        }
        searchRow.addView(go)
        addView(searchRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(8) })
    }

    private fun buildShortcuts(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(8))
        addView(TextView(this@MainActivity).apply {
            text = "快捷入口"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setPadding(0, 0, 0, dp(12))
        })
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val shortcuts = listOf(
            Triple("书源管理", "导入与整理", { openSourceCatalog() }),
            Triple("搜索书籍", "多源检索", { openSearch() }),
            Triple("阅读记录", "继续上次阅读", { startActivity(Intent(this@MainActivity, ReadingHistoryActivity::class.java)) }),
            Triple("替换规则", "Legado正文规则", { startActivity(Intent(this@MainActivity, ReplaceRuleActivity::class.java)) }),
        )
        shortcuts.forEach { (title, subtitle, action) ->
            val card = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(Color.WHITE, 14)
                setPadding(dp(5), dp(14), dp(5), dp(14))
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
                addView(TextView(this@MainActivity).apply {
                    text = title
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(ink)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@MainActivity).apply {
                    text = subtitle
                    textSize = 10f
                    setTextColor(muted)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(5), 0, 0)
                })
            }
            row.addView(card, LinearLayout.LayoutParams(0, dp(82), 1f).apply {
                marginEnd = dp(7)
            })
        }
        addView(row)
    }

    private fun buildBottomNavigation(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.WHITE)
        elevation = dp(8).toFloat()
        listOf("搜索", "收藏", "猫咪空间", "我的").forEachIndexed { index, label ->
            val item = TextView(this@MainActivity).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(13), dp(2), dp(13))
                setTextColor(if (index == 0) blue else muted)
                typeface = if (index == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setOnClickListener {
                    when (index) {
                        0 -> Unit
                        1 -> startActivity(Intent(this@MainActivity, FavoritesActivity::class.java))
                        2 -> Toast.makeText(this@MainActivity, "猫咪空间入口待接入", Toast.LENGTH_SHORT).show()
                        3 -> openSourceCatalog()
                    }
                }
            }
            addView(item, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
    }

    private fun openSearch() {
        val query = keyword.text?.toString()?.trim().orEmpty()
        startActivity(Intent(this, SourceSearchActivity::class.java).apply {
            putExtra(SourceSearchActivity.EXTRA_INITIAL_QUERY, query)
            putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
        })
    }

    private fun openSourceCatalog() {
        startActivity(Intent(this, SourceCatalogActivity::class.java))
    }

    private fun rounded(
        color: Int,
        radius: Int,
        tl: Int = radius,
        tr: Int = radius,
        br: Int = radius,
        bl: Int = radius,
    ): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(
            dp(tl).toFloat(), dp(tl).toFloat(), dp(tr).toFloat(), dp(tr).toFloat(),
            dp(br).toFloat(), dp(br).toFloat(), dp(bl).toFloat(), dp(bl).toFloat()
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
