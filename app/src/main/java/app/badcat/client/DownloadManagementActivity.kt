package app.badcat.client

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shows downloads created through Android DownloadManager for this application. */
class DownloadManagementActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        root.addView(TextView(this).apply {
            text = "下载管理"
            textSize = 23f
            setTextColor(Color.rgb(45, 52, 62))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    override fun onResume() { super.onResume()
        BadcatTheme.applyTree(this, window.decorView); if (::list.isInitialized) refresh() }

    private fun refresh() {
        list.removeAllViews()
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query())
        cursor.use {
            if (it.count == 0) {
                list.addView(TextView(this).apply {
                    text = "暂无下载任务"
                    textSize = 15f
                    gravity = Gravity.CENTER
                    setTextColor(Color.GRAY)
                    setPadding(0, dp(70), 0, dp(70))
                })
                return
            }
            while (it.moveToNext()) {
                val title = it.string(DownloadManager.COLUMN_TITLE).ifBlank { "未命名下载" }
                val status = when (it.int(DownloadManager.COLUMN_STATUS)) {
                    DownloadManager.STATUS_SUCCESSFUL -> "已完成"
                    DownloadManager.STATUS_FAILED -> "下载失败"
                    DownloadManager.STATUS_RUNNING -> "下载中"
                    DownloadManager.STATUS_PAUSED -> "已暂停"
                    else -> "等待中"
                }
                val reason = it.int(DownloadManager.COLUMN_REASON)
                val detail = if (status == "下载失败") "错误代码：$reason" else status
                list.addView(TextView(this).apply {
                    text = "$title\n$detail"
                    textSize = 15f
                    setTextColor(Color.rgb(48, 55, 66))
                    setPadding(dp(14), dp(16), dp(14), dp(16))
                    setBackgroundColor(Color.WHITE)
                }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }
    }

    private fun Cursor.string(column: String): String =
        getString(getColumnIndexOrThrow(column)).orEmpty()

    private fun Cursor.int(column: String): Int =
        getInt(getColumnIndexOrThrow(column))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
