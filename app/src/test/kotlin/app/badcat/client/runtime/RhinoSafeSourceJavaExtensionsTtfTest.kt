package app.badcat.client.runtime

import app.badcat.engine.SourceHttpResponse
import io.legado.app.model.analyzeRule.QueryTTF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RhinoSafeSourceJavaExtensionsTtfTest {
    private fun fontBytes(): ByteArray =
        requireNotNull(javaClass.getResourceAsStream("/fonts/number.ttf")).use { it.readBytes() }

    @Test
    fun parsesAndCachesPinnedLegadoTrueTypeFontBytes() {
        val bytes = fontBytes()
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "ttf-byte-fixture",
            store = InMemorySourceScriptKeyValueStore,
        )

        val first = extensions.queryTTF(bytes)
        val second = extensions.queryTTF(bytes.copyOf())

        assertNotNull(first)
        assertSame(first, second)
        assertTrue(requireNotNull(first).getGlyfIdByUnicode('0'.code) > 0)
    }

    @Test
    fun fetchesFontThroughSourceHttpTransportAndReplacesGlyphs() {
        val bytes = fontBytes()
        var requestedUrl: String? = null
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "ttf-http-fixture",
            store = InMemorySourceScriptKeyValueStore,
            httpGet = { url, _ ->
                requestedUrl = url
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "",
                    bodyBytes = bytes,
                )
            },
        )

        val font: QueryTTF? = extensions.queryTTF("https://fonts.example/number.ttf")
        assertEquals("https://fonts.example/number.ttf", requestedUrl)
        assertNotNull(font)
        assertEquals("0123456789", extensions.replaceFont("0123456789", font, font))
    }

    @Test
    fun rejectsOversizedFontBytesBeforeParsing() {
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "ttf-size-fixture",
            store = InMemorySourceScriptKeyValueStore,
        )
        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            extensions.queryTTF(ByteArray(8 * 1024 * 1024 + 1))
        }
        assertTrue(error.message.orEmpty().contains("8 MiB"))
    }
}
