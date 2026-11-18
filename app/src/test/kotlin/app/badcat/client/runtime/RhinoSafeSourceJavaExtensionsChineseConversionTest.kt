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
    @Test
    fun exposesImportedSourceTagAndReadOnlyMetadataToScripts() {
        val source = RhinoSafeSourceJavaExtensions(
            sourceId = "https://reader.example/source.json",
            store = InMemorySourceScriptKeyValueStore,
            sourceTag = "Example source",
            sourceDescriptor = mapOf(
                "bookSourceUrl" to "https://reader.example/source.json",
                "bookSourceName" to "Example source",
                "enabled" to true,
                "concurrentRate" to "2/500",
            ),
        )

        assertEquals("Example source", source.getTag())
        assertEquals(
            mapOf(
                "bookSourceUrl" to "https://reader.example/source.json",
                "bookSourceName" to "Example source",
                "enabled" to true,
                "concurrentRate" to "2/500",
            ),
            source.getSource(),
        )
    }

}
