package app.badcat.client.runtime

import app.badcat.engine.SourceHttpResponse
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RhinoSafeSourceJavaExtensionsArchiveTest {
    @Test
    fun readsZipEntryFromOriginalDownloadedBytesAndSupportsHexInput() {
        val archive = zipOf("folder/chapter.txt", "章节内容".toByteArray(Charsets.UTF_8))
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "https://zip-fixture.example/",
            store = InMemorySourceScriptKeyValueStore,
            httpRequest = { _, url, _, _ ->
                assertEquals("https://zip-fixture.example/book.zip", url)
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "not the archive",
                    bodyBytes = archive,
                )
            },
        )

        val fromUrl = extensions.getZipByteArrayContent(
            "https://zip-fixture.example/book.zip",
            "folder/chapter.txt",
        )
        val fromHex = extensions.getZipByteArrayContent(
            archive.joinToString("") { "%02x".format(it) },
            "folder/chapter.txt",
        )

        assertArrayEquals("章节内容".toByteArray(Charsets.UTF_8), fromUrl)
        assertArrayEquals(fromUrl, fromHex)
        assertEquals(
            "章节内容",
            extensions.getZipStringContent(
                archive.joinToString("") { "%02x".format(it) },
                "folder/chapter.txt",
                "UTF-8",
            ),
        )
        assertNull(extensions.getZipByteArrayContent(
            archive.joinToString("") { "%02x".format(it) },
            "missing.txt",
        ))
    }

    @Test
    fun zipEntryExpansionIsBounded() {
        val archive = zipOf("large.bin", ByteArray(16 * 1024 * 1024 + 1))
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "https://zip-limit.example/",
            store = InMemorySourceScriptKeyValueStore,
        )
        val error = runCatching {
            extensions.getZipByteArrayContent(
                archive.joinToString("") { "%02x".format(it) },
                "large.bin",
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun zipOf(name: String, content: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(content)
            zip.closeEntry()
        }
        return output.toByteArray()
    }
}
