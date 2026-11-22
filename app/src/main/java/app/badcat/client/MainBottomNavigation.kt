package app.badcat.client

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared three-destination navigation. Each icon is drawn into the same optical
 * viewport and every destination has a visible ripple + scale feedback.
 */
internal object MainBottomNavigation {
    private val active = Color.rgb(55, 137, 222)
    private val inactive = Color.rgb(135, 145, 158)

    fun create(context: Context, selected: String): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val dark = context.getSharedPreferences("badcat_appearance", Context.MODE_PRIVATE)
                .getString("appearance_mode", "light") == "dark"
            setBackgroundColor(if (dark) Color.rgb(38, 42, 49) else Color.WHITE)
            elevation = context.dpNav(8).toFloat()
            listOf(
                "搜索" to "search",
                "收藏夹" to "favorites",
                "我的" to "mine",
            ).forEach { (label, key) ->
                val isSelected = key == selected
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    contentDescription = label
                    background = RippleDrawable(
                        ColorStateList.valueOf(if (dark) Color.rgb(75, 91, 110) else Color.rgb(220, 232, 246)),
                        null,
                        null,
                    )
                    // Use the three bitmap glyphs extracted from the original Badcat
                    // navigation artwork, not the approximate hand-drawn line substitutes.
                    val iconView = BadcatNavigationIcons.create(
                        context,
                        key,
                        if (isSelected) active else inactive,
                    )
                    addView(iconView, LinearLayout.LayoutParams(context.dpNav(30), context.dpNav(30)))
                    addView(TextView(context).apply {
                        text = label
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setTextColor(if (isSelected) active else inactive)
                        typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                    })
                    setOnClickListener {
                        // Animate the original glyph in place; do not replace or morph the icon.
                        iconView.animate().cancel()
                        iconView.rotation = 0f
                        iconView.scaleX = 1f
                        iconView.scaleY = 1f
                        iconView.animate().rotationBy(12f).scaleX(1.16f).scaleY(1.16f)
                            .setDuration(115L).withEndAction {
                                iconView.animate().rotationBy(-12f).scaleX(1f).scaleY(1f)
                                    .setDuration(155L).withEndAction {
                                        if (!isSelected) navigate(context, key)
                                    }.start()
                            }.start()
                    }
                }
                addView(item, LinearLayout.LayoutParams(0, context.dpNav(64), 1f))
            }
        }

    private fun navigate(context: Context, key: String) {
        val intent = when (key) {
            "search" -> if (context !is MainActivity) Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) else null
            "favorites" -> if (context !is FavoritesActivity) Intent(context, FavoritesActivity::class.java) else null
            "mine" -> if (context !is MyActivity) Intent(context, MyActivity::class.java) else null
            else -> null
        }
        if (intent != null) {
            context.startActivity(intent)
            if (context is android.app.Activity) context.overridePendingTransition(0, 0)
        }
    }
}

private fun Context.dpNav(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()
