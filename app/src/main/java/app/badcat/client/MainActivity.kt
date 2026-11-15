package app.badcat.client

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Temporary build-validation shell.
 *
 * This is not the reconstructed Badcat UI. It exists only to prove the new
 * Android project can package an APK before the original UI integration work.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TextView(this).apply {
            text = """
                Badcat + Legado integration workspace

                Build scaffold: initialized
                Reading engine: adapter pending
                Original Badcat UI: not yet integrated
                APK validation: pending CI build
            """.trimIndent()
            textSize = 16f
            setPadding(32, 48, 32, 32)
        }.also(::setContentView)
    }
}
