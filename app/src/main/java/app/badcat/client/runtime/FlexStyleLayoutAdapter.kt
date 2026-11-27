package app.badcat.client.runtime

import android.view.View
import com.google.android.flexbox.FlexboxLayout
import io.legado.app.data.entities.rule.FlexChildStyle

/** App-only adapter for the pinned Legado FlexChildStyle View mutation. */
object FlexStyleLayoutAdapter {

    /** Applies the same layout mutations, in the same order, as upstream. */
    @JvmStatic
    fun apply(style: FlexChildStyle, view: View) {
        val lp = view.layoutParams as FlexboxLayout.LayoutParams
        lp.flexGrow = style.layout_flexGrow
        lp.flexShrink = style.layout_flexShrink
        lp.alignSelf = style.alignSelf()
        lp.flexBasisPercent = style.layout_flexBasisPercent
        lp.isWrapBefore = style.layout_wrapBefore
    }
}
