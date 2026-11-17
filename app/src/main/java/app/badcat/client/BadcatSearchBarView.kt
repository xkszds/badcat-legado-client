package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * One shared search control used on both the home page and the results page.
 * The appearance mode is persisted by SettingsActivity; the content type is
 * carried between screens independently.
 */
class BadcatSearchBarView(
    context: Context,
    initialQuery: String = "",
    initialType: String = "小说",
    appearance: String = "integrated",
    dark: Boolean = false,
    private val onSearch: (String, String) -> Unit,
    private val onTypeChanged: (String) -> Unit = { },
    private val searchPageStyle: Boolean = false,
    private val homePageStyle: Boolean = false,
    private val showHomeTypeSelector: Boolean = false,
) : LinearLayout(context) {
    val input = EditText(context)
    val searchAction = TextView(context)
    private val typeLabel = TextView(context)
    private var contentType = initialType.takeIf { it in TYPES } ?: "小说"
    private var homeTapAction: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val compact = appearance == "small"
        val configuredHeight = context.getSharedPreferences("badcat_home_layout", Context.MODE_PRIVATE)
            .getInt("home_search_bar_height_dp", if (compact) 36 else 40)
        val barHeight = configuredHeight.coerceIn(34, 46)
        setPadding(dp(if (compact) 4 else 5), 0, dp(if (compact) 4 else 5), 0)
        background = if (searchPageStyle) searchPageBackground(dark) else searchBackground(dark, dp(if (compact) 22 else 25))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(barHeight))
        elevation = dp(2).toFloat()
        clipToOutline = true

        typeLabel.apply {
            text = "$contentType ▾"
            textSize = if (searchPageStyle) 12f else if (compact) 13f else 14f
            gravity = Gravity.CENTER
            setTextColor(if (searchPageStyle) INK else BLUE)
            typeface = Typeface.DEFAULT_BOLD
            val typePadding = if (searchPageStyle) 6 else if (compact) 9 else 12
            setPadding(dp(typePadding), 0, dp(typePadding), 0)
            isClickable = true
            isFocusable = true
            contentDescription = "选择搜索类型，当前$contentType"
            setOnClickListener { chooseContentType() }
        }
        if (!homePageStyle || showHomeTypeSelector) {
            addView(typeLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
            addView(TextView(context).apply {
                setBackgroundColor(if (dark) Color.rgb(76, 82, 92) else Color.rgb(230, 234, 239))
            }, LayoutParams(dp(1), dp(22)))
        }

        input.apply {
            hint = hintFor(contentType)
            textSize = if (compact) 14f else 15f
            setSingleLine(true)
            setTextColor(if (dark) Color.rgb(240, 242, 245) else INK)
            setHintTextColor(if (dark) Color.rgb(165, 172, 182) else MUTED)
            background = null
            setPadding(dp(10), 0, dp(4), 0)
            setText(initialQuery)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    submit()
                    true
                } else false
            }
        }
        addView(input, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        input.setOnClickListener { homeTapAction?.invoke() }

        searchAction.apply {
            text = when { searchPageStyle -> "搜索"; homePageStyle -> ""; else -> "⌕" }
            textSize = when { searchPageStyle -> 15f; homePageStyle -> 25f; else -> 26f }
            gravity = Gravity.CENTER
            setTextColor(when {
                searchPageStyle -> Color.rgb(225, 111, 48)
                homePageStyle -> Color.rgb(165, 165, 165)
                else -> Color.WHITE
            })
            background = when {
                searchPageStyle || homePageStyle -> null
                else -> rounded(BLUE, dp(22))
            }
            if (homePageStyle) {
                val glyph = BadcatNavigationIcons.create(
                    context, "search", Color.rgb(115, 130, 148)
                ).drawable
                glyph.setBounds(0, 0, dp(24), dp(24))
                setCompoundDrawables(null, null, glyph, null)
                gravity = Gravity.CENTER
            }
            typeface = if (searchPageStyle) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            isClickable = true
            isFocusable = true
            contentDescription = "搜索"
            setOnClickListener { submit() }
        }
        addView(searchAction, LayoutParams(
            when { searchPageStyle -> dp(54); homePageStyle -> dp(42); else -> dp(if (compact) 34 else 36) },
            if (searchPageStyle || homePageStyle) LayoutParams.MATCH_PARENT else dp(if (compact) 34 else 36),
        ))
    }

    /** Makes the home search field navigate to the dedicated search screen. */
    fun setHomeTapToNavigate(action: () -> Unit) {
        homeTapAction = action
        input.isFocusable = false
        input.isFocusableInTouchMode = false
        input.isCursorVisible = false
        input.setOnClickListener { action() }
        searchAction.setOnClickListener { action() }
        setOnClickListener { action() }
    }

    fun submit() = onSearch(input.text?.toString()?.trim().orEmpty(), contentType)

    fun setContentType(value: String) {
        if (value !in TYPES) return
        contentType = value
        typeLabel.text = "$contentType ▾"
        typeLabel.contentDescription = "选择搜索类型，当前$contentType"
        input.hint = hintFor(contentType)
        onTypeChanged(contentType)
    }

    private fun chooseContentType() {
        val selected = TYPES.indexOf(contentType).coerceAtLeast(0)
        val builder = (context as? Activity)?.let { BadcatTheme.alertBuilder(it) } ?: AlertDialog.Builder(context)
        builder.setTitle("选择搜索类型")
            .setSingleChoiceItems(TYPES, selected) { dialog, which ->
                setContentType(TYPES[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun hintFor(type: String) = when (type) {
        "漫画" -> "搜索漫画"
        "综合" -> "综合搜索"
        else -> if (homePageStyle) "请输入书名、作者或关键字" else "搜索书籍"
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun searchPageBackground(dark: Boolean) = GradientDrawable().apply {
        setColor(if (dark) Color.rgb(43, 47, 54) else Color.rgb(245, 245, 246))
        cornerRadius = dp(13).toFloat()
    }

    private fun searchBackground(dark: Boolean, radius: Int) = GradientDrawable().apply {
        setColor(if (dark) Color.rgb(31, 37, 46) else Color.rgb(255, 255, 255))
        cornerRadius = radius.toFloat()
        setStroke(
            dp(2),
            if (dark) Color.rgb(130, 164, 205) else Color.rgb(38, 91, 145),
        )
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val TYPES = arrayOf("小说", "漫画", "综合")
        private val BLUE = Color.rgb(55, 137, 222)
        private val INK = Color.rgb(48, 55, 66)
        private val MUTED = Color.rgb(135, 145, 158)
    }
}
