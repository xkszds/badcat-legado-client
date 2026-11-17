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
        setPadding(dp(if (compact) 4 else 5), 0, dp(if (compact) 4 else 5), 0)
        background = searchBackground(dark, dp(if (compact) 22 else 25))
        elevation = dp(2).toFloat()
        clipToOutline = true

        typeLabel.apply {
            text = "$contentType ▾"
            textSize = if (compact) 13f else 14f
            gravity = Gravity.CENTER
            setTextColor(BLUE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(if (compact) 9 else 12), 0, dp(if (compact) 9 else 12), 0)
            isClickable = true
            isFocusable = true
            contentDescription = "选择搜索类型，当前$contentType"
            setOnClickListener { chooseContentType() }
        }
        addView(typeLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))

        addView(TextView(context).apply {
            setBackgroundColor(if (dark) Color.rgb(76, 82, 92) else Color.rgb(230, 234, 239))
        }, LayoutParams(dp(1), dp(22)))

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
            text = "⌕"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(BLUE, dp(22))
            isClickable = true
            isFocusable = true
            contentDescription = "搜索"
            setOnClickListener { submit() }
        }
        addView(searchAction, LayoutParams(dp(if (compact) 38 else 42), dp(if (compact) 38 else 42)))
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
        else -> "搜索书籍"
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun searchBackground(dark: Boolean, radius: Int) = GradientDrawable().apply {
        setColor(if (dark) Color.rgb(31, 37, 46) else Color.rgb(255, 255, 255))
        cornerRadius = radius.toFloat()
        setStroke(
            dp(1),
            if (dark) Color.rgb(91, 111, 135) else Color.rgb(193, 210, 230),
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
