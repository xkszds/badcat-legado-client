package app.badcat.client

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.content.Intent
import app.badcat.client.runtime.BadcatReadingEngineFactory
import app.badcat.engine.ReadingEngine

/**
 * Temporary build-validation shell.
 *
 * This is not the reconstructed Badcat UI. It exists only to prove the new
 * Android project can package an APK before the original UI integration work.
 */
class MainActivity : Activity() {
    private lateinit var readingEngine: ReadingEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readingEngine = BadcatReadingEngineFactory(applicationContext).create()

        val message = TextView(this).apply {
            text = """
                Badcat + Legado integration workspace

                Build scaffold: initialized
                Reading engine: Android multi-source graph constructed
                Original Badcat UI: not yet integrated
                APK validation: pending CI build
            """.trimIndent()
            textSize = 16f
            setPadding(32, 48, 32, 32)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        root.addView(message)
        root.addView(Button(this).apply {
            text = "书源管理（导入 / 导出）"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SourceCatalogActivity::class.java))
            }
        })
        setContentView(root)
    }
}
