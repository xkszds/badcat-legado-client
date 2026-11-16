package app.badcat.client

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Honest empty state for the local message center; remote notifications need a service. */
class MessageCenterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        root.addView(TextView(this).apply {
            text = "我的消息"
            textSize = 21f
            setTextColor(Color.rgb(48, 55, 66))
            setPadding(dp(20), dp(20), dp(20), dp(16))
            setBackgroundColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "暂无消息\n\n当前客户端尚未连接消息推送服务。书源更新、系统通知等远程消息不会在此自动出现。"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(135, 145, 158))
            setPadding(dp(28), dp(70), dp(28), dp(70))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
