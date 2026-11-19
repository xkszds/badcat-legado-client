package app.badcat.client.runtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CacheFilePrunerTest {
    @Test
    fun pruningNeverDeletesTheJustWrittenFileWhenTimestampsTie() {
        val directory = Files.createTempDirectory("cache-pruner-tie").toFile()
        try {
            val older = File(directory, "older.cache").apply {
                writeBytes(ByteArray(8))
                setLastModified(1_000L)
            }
            val justWritten = File(directory, "new.cache").apply {
                writeBytes(ByteArray(8))
                setLastModified(1_000L)
            }

            pruneCacheFiles(listOf(justWritten, older), maxBytes = 8, protectedFile = justWritten)

            assertFalse("Old cache entry should be evicted", older.exists())
            assertTrue("Newly written cache entry must survive", justWritten.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun pruningEvictsLeastRecentlyUsedEntriesBeforeTheProtectedFile() {
        val directory = Files.createTempDirectory("cache-pruner-lru").toFile()
        try {
            val oldest = File(directory, "old.cache").apply {
                writeBytes(ByteArray(5))
                setLastModified(1_000L)
            }
            val newer = File(directory, "newer.cache").apply {
                writeBytes(ByteArray(5))
                setLastModified(2_000L)
            }
            val justWritten = File(directory, "current.cache").apply {
                writeBytes(ByteArray(5))
                setLastModified(3_000L)
            }

            pruneCacheFiles(listOf(oldest, newer, justWritten), maxBytes = 10, protectedFile = justWritten)

            assertFalse(oldest.exists())
            assertTrue(newer.exists())
            assertTrue(justWritten.exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
