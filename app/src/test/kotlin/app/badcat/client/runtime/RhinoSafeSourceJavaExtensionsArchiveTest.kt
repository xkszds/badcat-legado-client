package app.badcat.client.runtime

import app.badcat.engine.SourceHttpResponse
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
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
    fun unzipCreatesSourcePrivateFilesAndRejectsTraversalEntries() {
        val archive = zipOf(
            "chapter.txt" to "第一章".toByteArray(Charsets.UTF_8),
            "readme.txt" to "目录说明".toByteArray(Charsets.UTF_8),
            "../outside.txt" to "must not escape".toByteArray(),
            "nested/part.txt" to "nested".toByteArray(),
        )
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "https://extract-fixture.example/",
            store = InMemorySourceScriptKeyValueStore,
            httpRequest = { _, url, _, _ ->
                SourceHttpResponse(
                    statusCode = 200,
                    finalUrl = url,
                    body = "",
                    bodyBytes = archive,
                )
            },
        )
        val downloaded = extensions.downloadFile("https://extract-fixture.example/book.zip")
        val directory = extensions.unzipFile(downloaded)

        assertTrue(directory.startsWith("ArchiveTemp/"))
        assertEquals("第一章", extensions.readTxtFile("$directory/chapter.txt"))
        assertEquals("nested", extensions.readTxtFile("$directory/nested/part.txt"))
        assertEquals("第一章\n目录说明", extensions.getTxtInFolder(directory))
        assertTrue(runCatching { extensions.readFile("$directory/../outside.txt") }.exceptionOrNull() is IllegalArgumentException)
        assertNull(extensions.readFile("$directory/chapter.txt"))
        assertNull(extensions.readFile("$directory/nested/part.txt"))

        val secondDownload = extensions.downloadFile("https://extract-fixture.example/book.zip")
        val secondDirectory = extensions.unzipFile(secondDownload)
        assertTrue(extensions.deleteFile(secondDirectory))
        assertFalse(extensions.deleteFile(secondDirectory))
        assertNull(extensions.readFile("$secondDirectory/nested/part.txt"))
    }

    @Test
    fun rarAndSevenZipExtractionUseBoundedVirtualFileAdapter() {
        val archive = "compressed archive".toByteArray()
        val extracted = linkedMapOf(
            "chapter.txt" to "RAR/7z chapter".toByteArray(),
            "../escape.txt" to "blocked".toByteArray(),
        )
        var extractorCalls = 0
        val extensions = RhinoSafeSourceJavaExtensions(
            sourceId = "https://virtual-archive.example/",
            store = InMemorySourceScriptKeyValueStore,
            httpRequest = { _, url, _, _ ->
                SourceHttpResponse(statusCode = 200, finalUrl = url, body = "", bodyBytes = archive)
            },
            archiveExtractor = { bytes, maxEntry, maxTotal, maxEntries ->
                extractorCalls++
                assertArrayEquals(archive, bytes)
                assertEquals(16 * 1024 * 1024, maxEntry)
                assertEquals(64 * 1024 * 1024, maxTotal)
                assertEquals(1000, maxEntries)
                extracted
            },
        )
        val downloaded = extensions.downloadFile("https://virtual-archive.example/book.rar")
        val rarFolder = extensions.unrarFile(downloaded)
        assertEquals("RAR/7z chapter", extensions.readTxtFile("$rarFolder/chapter.txt"))
        assertTrue(runCatching { extensions.readFile("$rarFolder/../escape.txt") }.exceptionOrNull() is IllegalArgumentException)

        val sevenZipFolder = extensions.un7zFile(downloaded)
        assertEquals("RAR/7z chapter", extensions.readTxtFile("$sevenZipFolder/chapter.txt"))
        assertEquals(2, extractorCalls)
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

    private fun zipOf(name: String, content: ByteArray): ByteArray =
        zipOf(name to content)

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
