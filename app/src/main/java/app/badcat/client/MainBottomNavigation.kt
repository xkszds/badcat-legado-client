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
                    addView(BadcatLineIconView(
                        context,
                        key,
                        if (isSelected) active else inactive,
                    ), LinearLayout.LayoutParams(context.dpNav(27), context.dpNav(27)))
                    addView(TextView(context).apply {
                        text = label
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setTextColor(if (isSelected) active else inactive)
                        typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                    })
                    setOnClickListener {
                        if (isSelected) {
                            animate().scaleX(0.90f).scaleY(0.90f).setDuration(75L)
                                .withEndAction {
                                    animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                                }.start()
                            return@setOnClickListener
                        }
                        animate().scaleX(0.90f).scaleY(0.90f).setDuration(70L)
                            .withEndAction {
                                animate().scaleX(1f).scaleY(1f).setDuration(100L)
                                    .withEndAction { navigate(context, key) }.start()
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
