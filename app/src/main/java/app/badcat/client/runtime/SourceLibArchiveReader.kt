package app.badcat.client.runtime

import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveException
import okio.Buffer
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Minimal streaming reader around the same libarchive API used by the pinned
 * Legado revision. It reads one requested regular-file entry and always frees
 * the native archive handle, including when decoding fails.
 */
internal object SourceLibArchiveReader {
    fun readEntry(input: InputStream, requestedPath: String, maxBytes: Int): ByteArray? {
        require(requestedPath.isNotBlank()) { "Archive entry path must not be blank" }
        val archive = openArchive(input)
        try {
            while (true) {
                val entry = Archive.readNextHeader(archive)
                if (entry == 0L) return null
                val name = ArchiveEntry.pathnameUtf8(entry)
                    ?: ArchiveEntry.pathname(entry)?.toString(Charsets.UTF_8)
                    ?: continue
                if (name != requestedPath) continue

                val output = Buffer()
                val chunk = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    chunk.clear()
                    Archive.readData(archive, chunk)
                    chunk.flip()
                    if (!chunk.hasRemaining()) break
                    val count = chunk.remaining()
                    total += count
                    require(total <= maxBytes) {
                        "Archive entry exceeds the ${maxBytes}-byte expanded limit"
                    }
                    output.write(chunk)
                }
                return output.readByteArray()
            }
        } finally {
            Archive.free(archive)
        }
    }

    private fun openArchive(input: InputStream): Long {
        val archive = Archive.readNew()
        var successful = false
        try {
            Archive.setCharset(archive, Charsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            Archive.readSetCallbackData(archive, null)
            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            Archive.readSetReadCallback<Any?>(archive) { _, _ ->
                buffer.clear()
                val count = try {
                    input.read(buffer.array())
                } catch (error: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.read", error)
                }
                if (count < 0) {
                    null
                } else {
                    buffer.limit(count)
                    buffer
                }
            }
            Archive.readSetSkipCallback<Any?>(archive) { _, _, requested ->
                try {
                    input.skip(requested)
                } catch (error: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.skip", error)
                }
            }
            Archive.readOpen1(archive)
            successful = true
            return archive
        } finally {
            if (!successful) Archive.free(archive)
        }
    }
}
