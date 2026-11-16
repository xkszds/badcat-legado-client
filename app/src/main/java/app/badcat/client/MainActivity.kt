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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Modern, lightweight home shell.
 *
 * The original Badcat screen is the visual/behavioral reference. This screen
 * deliberately keeps the original four destinations and three shortcut roles,
 * while the search action continues to use the Legado-backed source engine.
 * Legacy implementation details are not copied into the new client.
 */
class MainActivity : Activity() {
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
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(buildHome())
    }

    private fun buildHome(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(page)

        val body = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(buildHeader())
            addView(buildSearchPanel())
            addView(View(this@MainActivity), LinearLayout.LayoutParams(1, 0, 1f))
        }
        addView(body)
        addView(buildBottomNavigation())
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(25))
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
        setPadding(dp(18), dp(12), dp(18), dp(8))

        val typeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val typeLabels = listOf("小说", "漫画", "综合")
        fun refreshTypes() {
            for (index in 0 until typeRow.childCount) {
                val chip = typeRow.getChildAt(index) as TextView
                val selected = chip.text.toString() == contentType
                chip.setTextColor(if (selected) blue else muted)
                chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
        typeLabels.forEach { label ->
            typeRow.addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(22), dp(9), dp(22), dp(9))
                setOnClickListener {
                    contentType = label
                    refreshTypes()
                }
            }, LinearLayout.LayoutParams(0, dp(42), 1f))
        }
        addView(typeRow)
        refreshTypes()

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
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    openSearch()
                    true
                } else false
            }
        }
        searchRow.addView(keyword, LinearLayout.LayoutParams(0, dp(50), 1f))
        searchRow.addView(TextView(this@MainActivity).apply {
            text = "搜索"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(blue, 20)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { openSearch() }
        })
        addView(searchRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(5) })
    }

    private fun buildBottomNavigation(): View = MainBottomNavigation.create(this, "search")

    private fun openSearch() {
        val query = keyword.text?.toString()?.trim().orEmpty()
        startActivity(Intent(this, SourceSearchActivity::class.java).apply {
            putExtra(SourceSearchActivity.EXTRA_INITIAL_QUERY, query)
            putExtra(SourceSearchActivity.EXTRA_CONTENT_TYPE, contentType)
        })
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
