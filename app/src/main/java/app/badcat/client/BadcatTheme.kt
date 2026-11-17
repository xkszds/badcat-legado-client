package app.badcat.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

/** Shared appearance palette and a conservative tint pass for programmatic secondary screens. */
internal object BadcatTheme {
    data class Palette(
        val page: Int,
        val surface: Int,
        val raised: Int,
        val text: Int,
        val muted: Int,
        val divider: Int,
        val accent: Int,
        val field: Int,
        val fieldStroke: Int,
    )

    fun isDark(context: Context): Boolean =
        context.getSharedPreferences("badcat_appearance", Context.MODE_PRIVATE)
            .getString("appearance_mode", "light") == "dark"

    fun palette(context: Context): Palette = if (isDark(context)) {
        Palette(
            page = Color.rgb(20, 23, 29),
            surface = Color.rgb(29, 33, 41),
            raised = Color.rgb(36, 42, 52),
            text = Color.rgb(239, 242, 247),
            muted = Color.rgb(157, 169, 184),
            divider = Color.rgb(57, 66, 79),
            accent = Color.rgb(100, 174, 255),
            field = Color.rgb(31, 37, 46),
            fieldStroke = Color.rgb(83, 101, 123),
        )
    } else {
        Palette(
            page = Color.rgb(246, 249, 253),
            surface = Color.WHITE,
            raised = Color.rgb(250, 252, 255),
            text = Color.rgb(37, 48, 63),
            muted = Color.rgb(119, 133, 151),
            divider = Color.rgb(226, 233, 241),
            accent = Color.rgb(48, 126, 211),
            field = Color.WHITE,
            fieldStroke = Color.rgb(198, 211, 227),
        )
    }

    fun alertBuilder(activity: Activity): AlertDialog.Builder =
        if (isDark(activity)) {
            AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
        } else {
            AlertDialog.Builder(activity)
        }

    fun applyWindow(activity: Activity) {
        val colors = palette(activity)
        activity.window.statusBarColor = colors.page
        activity.window.navigationBarColor = colors.page
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = if (isDark(activity)) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    /**
     * Recolors only neutral platform surfaces and neutral text. Accent colors,
     * covers, illustrations and custom colored controls are deliberately kept.
     */
    fun applyTree(context: Context, root: View) {
        if (!isDark(context)) {
            (context as? Activity)?.let(::applyWindow)
            return
        }
        val colors = palette(context)
        root.setBackgroundColor(colors.page)
        fun visit(view: View) {
            when (val bg = view.background) {
                is ColorDrawable -> {
                    if (isNeutral(bg.color) && Color.alpha(bg.color) > 0) {
                        view.setBackgroundColor(mappedSurface(view, bg.color, colors))
                    }
                }
                is GradientDrawable -> {
                    val fill = bg.color?.defaultColor
                    if (fill != null && isNeutral(fill) && Color.alpha(fill) > 0) {
                        bg.setColor(mappedSurface(view, fill, colors))
                    }
                }
            }
            if (view is TextView) {
                val original = view.currentTextColor
                if (isNeutral(original) && original != Color.WHITE) {
                    view.setTextColor(
                        when {
                            luminance(original) < 0.18 -> colors.text
                            luminance(original) > 0.72 -> colors.text
                            else -> colors.muted
                        }
                    )
                }
                if (view.hint != null && Build.VERSION.SDK_INT >= 23) {
                    view.setHintTextColor(colors.muted)
                }
            }
            if (view is Button) {
                // Give default neutral buttons a dark surface, but preserve
                // explicitly colored call-to-action backgrounds and white labels.
                val originalTint = view.backgroundTintList?.defaultColor
                if (originalTint == null || isNeutral(originalTint)) {
                    view.backgroundTintList = ColorStateList.valueOf(colors.raised)
                }
                val buttonText = view.currentTextColor
                if (isNeutral(buttonText) && buttonText != Color.WHITE) {
                    view.setTextColor(colors.accent)
                }
            }
            if (view is EditText) {
                view.setTextColor(colors.text)
                view.setHintTextColor(colors.muted)
                view.backgroundTintList = ColorStateList.valueOf(colors.fieldStroke)
            }
            if (view is Spinner) {
                view.backgroundTintList = ColorStateList.valueOf(colors.fieldStroke)
            }
            if (view is CompoundButton) {
                view.buttonTintList = ColorStateList.valueOf(colors.accent)
                if (Build.VERSION.SDK_INT >= 21 && view is android.widget.Switch) {
                    view.thumbTintList = ColorStateList.valueOf(colors.accent)
                    view.trackTintList = ColorStateList.valueOf(
                        if (isDark(context)) Color.rgb(75, 91, 110) else Color.rgb(204, 220, 238)
                    )
                }
            }
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) visit(view.getChildAt(index))
            }
        }
        visit(root)
        applyWindow(context as? Activity ?: return)
    }

    private fun isNeutral(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return maxOf(r, g, b) - minOf(r, g, b) < 32
    }

    private fun mappedSurface(view: View, color: Int, colors: Palette): Int = when {
        isDivider(view) -> colors.divider
        isNearWhite(color) -> colors.surface
        luminance(color) > 0.90 -> colors.raised
        luminance(color) < 0.02 -> colors.page
        luminance(color) < 0.15 -> colors.surface
        else -> colors.page
    }

    private fun isDivider(view: View): Boolean {
        val params = view.layoutParams ?: return false
        return (params.height in 1..2) || (params.width in 1..2)
    }

    private fun isNearWhite(color: Int): Boolean = luminance(color) > 0.985

    private fun luminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
