package app.badcat.client

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Displays only a status explicitly supplied by a book source. */
object BookStatusBadge {
    fun label(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        return when {
            value.contains("完结", ignoreCase = true) -> "完结"
            value.contains("连载", ignoreCase = true) -> "连载"
            value.contains("更新", ignoreCase = true) -> "更新"
            else -> null
        }
    }

    fun cover(context: Context, url: String?, rawStatus: String?, widthDp: Int, heightDp: Int): View =
        FrameLayout(context).apply {
            clipToOutline = true
            val image = ImageView(context).apply {
                contentDescription = "小说封面"
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = GradientDrawable().apply {
                    setColor(Color.rgb(235, 239, 245))
                    cornerRadius = dp(context, 5).toFloat()
                }
            }
            addView(image, FrameLayout.LayoutParams(-1, -1))
            BookCoverLoader.load(image, url)
            label(rawStatus)?.let { value ->
                val color = when (value) {
                    "更新" -> Color.rgb(235, 112, 57)
                    "连载" -> Color.rgb(105, 119, 137)
                    else -> Color.rgb(75, 137, 202)
                }
                val badge = TextView(context).apply {
                    text = value
                    textSize = 9f
                    setTextColor(Color.WHITE)
                    setPadding(dp(context, 5), dp(context, 3), dp(context, 5), dp(context, 3))
                    background = GradientDrawable().apply {
                        setColor(color)
                        cornerRadius = dp(context, 4).toFloat()
                    }
                    elevation = dp(context, 2).toFloat()
                }
                addView(badge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
                    topMargin = dp(context, 4)
                    marginEnd = dp(context, 4)
                })
            }
        }.also {
            it.layoutParams = android.view.ViewGroup.LayoutParams(dp(context, widthDp), dp(context, heightDp))
        }

    private fun dp(context: Context, value: Int) =
        (value * context.resources.displayMetrics.density).toInt()
}
