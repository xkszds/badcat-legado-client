package app.badcat.client.runtime

import android.system.OsConstants.S_ISREG

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
    private const val MAX_ARCHIVE_HEADERS = 10_000
    private const val MAX_HEADERS_PER_OUTPUT_ENTRY = 8L

    fun readEntry(input: InputStream, requestedPath: String, maxBytes: Int): ByteArray? {
        require(requestedPath.isNotBlank()) { "Archive entry path must not be blank" }
        require(maxBytes > 0) { "Archive entry limit must be positive" }
        val archive = openArchive(input)
        var headersRead = 0
        try {
            while (true) {
                val entry = Archive.readNextHeader(archive)
                if (entry == 0L) return null
                headersRead++
                require(headersRead <= MAX_ARCHIVE_HEADERS) { "Archive contains too many entries" }
                val name = ArchiveEntry.pathnameUtf8(entry)
                    ?: ArchiveEntry.pathname(entry)?.toString(Charsets.UTF_8)
                    ?: continue
                if (!S_ISREG(ArchiveEntry.stat(entry).stMode) || name != requestedPath) continue

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

    fun extractEntries(
        input: InputStream,
        maxEntryBytes: Int,
        maxTotalBytes: Int,
        maxEntries: Int,
    ): Map<String, ByteArray> {
        require(maxEntryBytes > 0 && maxTotalBytes >= maxEntryBytes && maxEntries > 0)
        val archive = openArchive(input)
        val extracted = linkedMapOf<String, ByteArray>()
        var total = 0L
        var headersRead = 0
        val maxHeaders = (maxEntries.toLong() * MAX_HEADERS_PER_OUTPUT_ENTRY)
            .coerceAtMost(MAX_ARCHIVE_HEADERS.toLong()).toInt()
        try {
            while (true) {
                val entry = Archive.readNextHeader(archive)
                if (entry == 0L) break
                headersRead++
                require(headersRead <= maxHeaders) { "Archive contains too many entries" }
                val name = ArchiveEntry.pathnameUtf8(entry)
                    ?: ArchiveEntry.pathname(entry)?.toString(Charsets.UTF_8)
                    ?: continue
                if (!S_ISREG(ArchiveEntry.stat(entry).stMode)) continue
                require(extracted.size < maxEntries) { "Archive contains too many files" }
                val output = Buffer()
                val chunk = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                var entrySize = 0
                while (true) {
                    chunk.clear()
                    Archive.readData(archive, chunk)
                    chunk.flip()
                    if (!chunk.hasRemaining()) break
                    val count = chunk.remaining()
                    entrySize += count
                    total += count
                    require(entrySize <= maxEntryBytes) {
                        "Archive entry exceeds the ${maxEntryBytes}-byte expanded limit"
                    }
                    require(total <= maxTotalBytes) {
                        "Archive exceeds the ${maxTotalBytes}-byte total expanded limit"
                    }
                    output.write(chunk)
                }
                extracted[name] = output.readByteArray()
            }
        } finally {
            Archive.free(archive)
        }
        return extracted
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
                    var bytesRead: Int
                    do {
                        bytesRead = input.read(buffer.array())
                    } while (bytesRead == 0)
                    bytesRead
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
