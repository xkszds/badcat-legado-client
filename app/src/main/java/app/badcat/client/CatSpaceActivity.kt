package app.badcat.client

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Reserved destination for the original "喵星" area.
 * No undocumented network endpoints or fake community content are fabricated.
 */
class CatSpaceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(247, 249, 252))
            addView(TextView(this@CatSpaceActivity).apply {
                text = "喵星"
                textSize = 24f
                setTextColor(Color.rgb(48, 55, 66))
                gravity = Gravity.CENTER
            })
            addView(TextView(this@CatSpaceActivity).apply {
                text = "该区域的原版服务协议尚未完成核验，暂不展示虚构内容。"
                textSize = 14f
                setTextColor(Color.rgb(135, 145, 158))
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 0)
            })
        })
    }

    override fun onResume() {
        super.onResume()
        BadcatTheme.applyTree(this, window.decorView)
    }

}
