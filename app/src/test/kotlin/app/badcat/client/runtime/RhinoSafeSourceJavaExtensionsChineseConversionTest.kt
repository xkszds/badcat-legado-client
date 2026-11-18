package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun readsDownloadedTextWithRequestedCharsetAndReportsDeleteResult() {
        val fileStore = InMemorySourceScriptFileCacheStore
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "file-helper-test",
            store = InMemorySourceScriptKeyValueStore,
            fileStore = fileStore,
            httpGet = { url, _ ->
                app.badcat.engine.SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "café",
                )
            },
        )

        val path = extensions.downloadFile("https://reader.example/café.txt")
        val utf8 = extensions.readTxtFile(path)
        assertEquals("café", utf8)
        assertEquals(
            String("café".toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1),
            extensions.readTxtFile(path, "ISO-8859-1"),
        )
        assertTrue(extensions.deleteFile(path))
        assertEquals("", extensions.readTxtFile(path))
        assertFalse(extensions.deleteFile(path))
    }

}
