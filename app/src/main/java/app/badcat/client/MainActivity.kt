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
 * Search landing screen rebuilt to follow the original Badcat composition:
 * blue title/search header, content-type tabs, and the original three bottom
 * destinations (Search, Favorites, Mine). Removed promotional shortcuts stay
 * absent; search remains connected to the source-backed result screen.
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
            addView(View(this@MainActivity), LinearLayout.LayoutParams(1, 0, 1f))
        }
        addView(body)
        addView(buildBottomNavigation())
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(22), dp(22), dp(22), dp(22))
        background = rounded(blue, 0, 0, 0, 22, 22)
        addView(TextView(this@MainActivity).apply {
            text = "坏坏猫"
            textSize = 36f
            gravity = Gravity.START
            setTextColor(Color.WHITE)
            typeface = Typeface.create("casual", Typeface.NORMAL)
            letterSpacing = 0.06f
            includeFontPadding = false
        })
        addView(TextView(this@MainActivity).apply {
            text = "发现好书，开始阅读"
            textSize = 14f
            gravity = Gravity.START
            setTextColor(Color.WHITE)
            alpha = 0.9f
            setPadding(0, dp(5), 0, dp(10))
        })
        addView(buildSearchPanel())
    }

    private fun buildSearchPanel(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(4), 0, 0)

        val typeRow = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        val typeLabels = listOf("小说", "漫画", "综合")
        fun refreshTypes() {
            for (index in 0 until typeRow.childCount) {
                val chip = typeRow.getChildAt(index) as TextView
                val selected = chip.text.toString() == contentType
                chip.setTextColor(if (selected) Color.WHITE else Color.WHITE)
                chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                chip.alpha = if (selected) 1f else 0.72f
            }
        }
        typeLabels.forEach { label ->
            typeRow.addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(8), dp(22), dp(8))
                setOnClickListener {
                    contentType = label
                    refreshTypes()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)))
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
            hint = "请输入书名、作者或关键字"
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
            setTextColor(blue)
            background = rounded(Color.rgb(238, 245, 255), 20)
            setPadding(dp(16), dp(10), dp(16), dp(10))
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
