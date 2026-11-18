package app.badcat.client.runtime

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class FileChapterContentCacheTest {
    @Test
    fun boundedReaderReturnsEntryAtExactLimit() {
        val bytes = "chapter text".toByteArray(Charsets.UTF_8)

        val result = readBoundedChapterCacheEntry(ByteArrayInputStream(bytes), bytes.size.toLong())

        assertArrayEquals(bytes, result)
    }

    @Test
    fun boundedReaderRejectsEntryAtFirstByteBeyondLimit() {
        val bytes = "chapter text".toByteArray(Charsets.UTF_8)

        val result = readBoundedChapterCacheEntry(ByteArrayInputStream(bytes), (bytes.size - 1).toLong())

        assertNull(result)
    }

    @Test
    fun boundedReaderMakesProgressWhenBulkReadReturnsZero() {
        val bytes = "stream".toByteArray(Charsets.UTF_8)
        val delegate = ByteArrayInputStream(bytes)
        val stalledOnce = object : java.io.InputStream() {
            private var firstBulkRead = true

            override fun read(): Int = delegate.read()

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (firstBulkRead) {
                    firstBulkRead = false
                    return 0
                }
                return delegate.read(buffer, offset, length)
            }
        }

        val result = readBoundedChapterCacheEntry(stalledOnce, bytes.size.toLong())

        assertArrayEquals(bytes, result)
    }

    @Test
    fun boundedReaderAcceptsEmptyEntryWithZeroLimit() {
        val result = readBoundedChapterCacheEntry(ByteArrayInputStream(byteArrayOf()), 0)

        assertArrayEquals(byteArrayOf(), result)
    }
}
