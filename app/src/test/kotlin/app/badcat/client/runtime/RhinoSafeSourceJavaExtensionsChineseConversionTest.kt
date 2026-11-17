package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class RhinoSafeSourceJavaExtensionsChineseConversionTest {
    private val extensions = RhinoSafeSourceJavaExtensions(
        sourceId = "conversion-test",
        store = InMemorySourceScriptKeyValueStore,
    )

    @Test
    fun convertsTraditionalAndSimplifiedChineseLikeLegado() {
        assertEquals("繁体中文", extensions.t2s("繁體中文"))
        assertEquals("繁體中文", extensions.s2t("繁体中文"))
    }

    @Test
    fun traditionalToSimplifiedHonorsLegadoExcludedTerms() {
        assertEquals("多士", extensions.t2s("多士"))
    }
}
