package app.badcat.client.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.badcat.engine.PinnedLegadoRuleAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the final Android runtime class path, not just JVM compilation.
 * The class must be defined in the target app APK and callable on-device.
 */
@RunWith(AndroidJUnit4::class)
class PinnedLegadoRuleAnalyzerPackagingInstrumentedTest {
    @Test
    fun authenticPinnedRuleAnalyzerIsPresentAndCallableFromTargetApk() {
        val targetLoader = InstrumentationRegistry.getInstrumentation()
            .targetContext.classLoader
        val loaded = Class.forName(
            "io.legado.app.model.analyzeRule.RuleAnalyzer",
            true,
            targetLoader,
        )
        assertEquals("io.legado.app.model.analyzeRule.RuleAnalyzer", loaded.name)

        val parsed = PinnedLegadoRuleAnalyzer.split("div.book")
        assertFalse("Pinned Legado RuleAnalyzer must return a selector rule", parsed.rules.isEmpty())
    }
}
