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
    fun legacyHexDownloadAndAutomaticCharsetDetectionPreserveFileSemantics() {
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "https://legacy-file.example/",
            store = InMemorySourceScriptKeyValueStore,
        )
        val gbk = "中文内容".toByteArray(java.nio.charset.Charset.forName("GBK"))
        val hex = gbk.joinToString("") { "%02x".format(it) }

        @Suppress("DEPRECATION")
        val path = extensions.downloadFile(hex, "https://legacy-file.example/font.dat")

        assertTrue(path.endsWith(".dat"))
        assertArrayEquals(gbk, extensions.readFile(path))
        assertEquals("中文内容", extensions.readTxtFile(path))
        assertTrue(extensions.deleteFile(path))
        assertNull(extensions.readFile(path))
    }

    @Test
    fun rarAndSevenZipHelpersPreserveUrlHexAndCharsetContracts() {
        val sourceId = "https://libarchive-fixture.example/"
        val archive = "archive bytes".toByteArray()
        val requests = mutableListOf<String>()
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = sourceId,
            store = InMemorySourceScriptKeyValueStore,
            httpRequest = { _, url, _, _ ->
                requests += url
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "not archive data",
                    bodyBytes = archive,
                )
            },
            archiveEntryReader = { input, path, maxBytes ->
                assertEquals("folder/content.txt", path)
                assertEquals(16 * 1024 * 1024, maxBytes)
                input.readBytes()
            },
        )
        val hex = archive.joinToString("") { "%02x".format(it) }

        assertArrayEquals(archive, extensions.getRarByteArrayContent(hex, "folder/content.txt"))
        assertArrayEquals(
            archive,
            extensions.get7zByteArrayContent(
                "https://libarchive-fixture.example/archive",
                "folder/content.txt",
            ),
        )
        assertEquals(
            "archive bytes",
            extensions.getRarStringContent(hex, "folder/content.txt", "UTF-8"),
        )
        assertEquals(
            "archive bytes",
            extensions.get7zStringContent(hex, "folder/content.txt", "UTF-8"),
        )
        assertEquals(listOf("https://libarchive-fixture.example/archive"), requests)
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
