package app.badcat.client

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared three-destination navigation used by the rebuilt main surfaces.
 * The original requested removals are intentional: no leaderboard, lottery,
 * cash shortcut, or Cat Star destination is reintroduced here.
 */
internal object MainBottomNavigation {
    private val active = Color.rgb(55, 137, 222)
    private val inactive = Color.rgb(135, 145, 158)

    fun create(context: Context, selected: String): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = context.dpNav(8).toFloat()
            listOf(
                Triple("搜索", "⌕", "search"),
                Triple("收藏夹", "▤", "favorites"),
                Triple("我的", "♙", "mine"),
            ).forEach { (label, glyph, key) ->
                val isSelected = key == selected
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    isClickable = true
                    isFocusable = true
                    contentDescription = label
                    setOnClickListener {
                        when (key) {
                            "search" -> if (context !is MainActivity) {
                                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                            }
                            "favorites" -> if (context !is FavoritesActivity) {
                                context.startActivity(Intent(context, FavoritesActivity::class.java))
                            }
                            "mine" -> if (context !is MyActivity) {
                                context.startActivity(Intent(context, MyActivity::class.java))
                            }
                        }
                    }
                    addView(TextView(context).apply {
                        text = glyph
                        textSize = 21f
                        gravity = Gravity.CENTER
                        setTextColor(if (isSelected) active else inactive)
                    })
                    addView(TextView(context).apply {
                        text = label
                        textSize = 11f
                        gravity = Gravity.CENTER
                        setTextColor(if (isSelected) active else inactive)
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    })
                }, LinearLayout.LayoutParams(0, context.dpNav(58), 1f))
            }
        }
}

private fun Context.dpNav(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()
