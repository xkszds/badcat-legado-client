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
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Minimal original-style search landing page: centered title and one search bar. */
class MainActivity : Activity() {
    private lateinit var keyword: EditText
    private lateinit var typeLabel: TextView
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
        addView(buildHeader())
        addView(View(this@MainActivity), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        addView(MainBottomNavigation.create(this@MainActivity, "search"))
    }

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(20), dp(20), dp(20), dp(22))
        background = rounded(blue, 0, 0, 0, 22, 22)

        addView(TextView(this@MainActivity).apply {
            text = "坏坏猫"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.035f
            includeFontPadding = false
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))

        addView(buildSearchBar(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
        ).apply { topMargin = dp(15) })
    }

    private fun buildSearchBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(Color.WHITE, 25)
        setPadding(dp(5), 0, dp(5), 0)
        elevation = dp(2).toFloat()

        typeLabel = TextView(this@MainActivity).apply {
            text = "小说 ▾"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(blue)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), 0, dp(12), 0)
            isClickable = true
            isFocusable = true
            contentDescription = "选择搜索类型，当前$contentType"
            setOnClickListener { chooseContentType() }
        }
        addView(typeLabel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        addView(View(this@MainActivity).apply {
            setBackgroundColor(Color.rgb(230, 234, 239))
        }, LinearLayout.LayoutParams(dp(1), dp(24)))

        keyword = EditText(this@MainActivity).apply {
            hint = "搜索书籍"
            textSize = 15f
            setSingleLine(true)
            setTextColor(ink)
            setHintTextColor(muted)
            background = null
            setPadding(dp(11), 0, dp(4), 0)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    openSearch()
                    true
                } else false
            }
        }
        addView(keyword, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        addView(TextView(this@MainActivity).apply {
            text = "⌕"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(blue, 22)
            isClickable = true
            isFocusable = true
            contentDescription = "搜索书籍"
            setOnClickListener { openSearch() }
        }, LinearLayout.LayoutParams(dp(42), dp(42)))
    }

    private fun chooseContentType() {
        val types = arrayOf("小说", "漫画", "综合")
        val selected = types.indexOf(contentType).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("选择搜索类型")
            .setSingleChoiceItems(types, selected) { dialog, which ->
                contentType = types[which]
                typeLabel.text = "$contentType ▾"
                typeLabel.contentDescription = "选择搜索类型，当前$contentType"
                keyword.hint = when (contentType) {
                    "漫画" -> "搜索漫画"
                    "综合" -> "综合搜索"
                    else -> "搜索书籍"
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openSearch() {
        val query = keyword.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            keyword.error = "请输入书名或作者"
            keyword.requestFocus()
            return
        }
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
