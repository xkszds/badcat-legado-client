package app.badcat.client.runtime

import android.content.Context
import android.util.AtomicFile
import app.badcat.engine.CachedChapterContent
import app.badcat.engine.ChapterContent
import app.badcat.engine.ChapterContentCache
import app.badcat.engine.BookChapter
import app.badcat.engine.ChapterContentCacheKey
import app.badcat.engine.ChapterContentSaveToken
import app.badcat.engine.ChapterContentSaveFence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

private val chapterCacheMutex = Mutex()
private val chapterCacheSaveFence = ChapterContentSaveFence()

/**
 * Private, bounded on-disk chapter cache. It stores already parsed and
 * purified text, keyed by source + book + chapter. Expired entries may still
 * be returned by the service as an offline fallback after a network failure.
 *
 * All cache instances share one process-wide lock: reading, replacing and
 * pruning entries cannot race with another reader-engine instance. AtomicFile
 * keeps a previously committed chapter intact if the process dies mid-write.
 */
class FileChapterContentCache(context: Context) : ChapterContentCache {
    private val directory = File(context.applicationContext.filesDir, CACHE_DIRECTORY).apply {
        if (!exists()) mkdirs()
    }

    override suspend fun get(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
    ): CachedChapterContent? = withContext(Dispatchers.IO) {
        chapterCacheMutex.withLock {
            val file = AtomicFile(cacheFile(sourceId, bookUrl, chapterUrl))
            // openRead() must run before checking baseFile: AtomicFile restores
            // its .bak after an interrupted commit, when the base file is absent.
            val cached = runCatching {
                val encoded = file.openRead().use { readBoundedChapterCacheEntry(it, MAX_ENTRY_BYTES) } ?: return@runCatching null
                val json = JSONObject(String(encoded, Charsets.UTF_8))
                CachedChapterContent(
                    content = ChapterContent(
                        title = json.optString("title"),
                        text = json.getString("text"),
                        nextPageUrl = json.optString("nextPageUrl").takeIf(String::isNotBlank),
                        previousChapterUrl = json.optString("previousChapterUrl").takeIf(String::isNotBlank),
                    ),
                    savedAtEpochMillis = json.getLong("savedAt"),
                )
            }.getOrNull()
            if (cached != null) file.baseFile.setLastModified(System.currentTimeMillis())
            cached
        }
    }

    override suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        value: CachedChapterContent,
    ) = withContext(Dispatchers.IO) {
        chapterCacheMutex.withLock {
            val target = AtomicFile(cacheFile(sourceId, bookUrl, chapterUrl))
            val json = JSONObject().apply {
                put("title", value.content.title)
                put("text", value.content.text)
                put("nextPageUrl", value.content.nextPageUrl)
                put("previousChapterUrl", value.content.previousChapterUrl)
                put("savedAt", value.savedAtEpochMillis)
            }
            val encoded = json.toString().toByteArray(Charsets.UTF_8)
            if (encoded.size > MAX_ENTRY_BYTES) return@withLock
            val output = target.startWrite()
            try {
                output.write(encoded)
                output.flush()
                target.finishWrite(output)
            } catch (error: Exception) {
                target.failWrite(output)
                throw error
            }
            // Pruning is best-effort and must never roll back a committed chapter.
            runCatching { prune(target.baseFile) }
            Unit
        }
    }

    override fun beginSave(
        sourceId: String,
        bookUrl: String,
        chapter: BookChapter,
    ): ChapterContentSaveToken = chapterCacheSaveFence.token(
        ChapterContentCacheKey(sourceId, bookUrl, chapter.index),
    )

    override fun isCurrent(token: ChapterContentSaveToken): Boolean =
        chapterCacheSaveFence.isCurrent(token)

    override suspend fun get(
        sourceId: String,
        bookUrl: String,
        chapter: BookChapter,
    ): CachedChapterContent? = withContext(Dispatchers.IO) {
        chapterCacheMutex.withLock {
            val file = AtomicFile(cacheFile(sourceId, bookUrl, chapter.index))
            val cached = runCatching {
                val encoded = file.openRead().use { readBoundedChapterCacheEntry(it, MAX_ENTRY_BYTES) }
                    ?: return@runCatching null
                val json = JSONObject(String(encoded, Charsets.UTF_8))
                if (json.optString("chapterUrl") != chapter.url) return@runCatching null
                CachedChapterContent(
                    content = ChapterContent(
                        title = json.optString("title"),
                        text = json.getString("text"),
                        nextPageUrl = json.optString("nextPageUrl").takeIf(String::isNotBlank),
                        previousChapterUrl = json.optString("previousChapterUrl").takeIf(String::isNotBlank),
                    ),
                    savedAtEpochMillis = json.getLong("savedAt"),
                )
            }.getOrNull()
            if (cached != null) file.baseFile.setLastModified(System.currentTimeMillis())
            cached
        }
    }

    override suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapter: BookChapter,
        token: ChapterContentSaveToken,
        value: CachedChapterContent,
    ): Boolean = withContext(Dispatchers.IO) {
        if (token.key != ChapterContentCacheKey(sourceId, bookUrl, chapter.index)) return@withContext false
        chapterCacheMutex.withLock {
            chapterCacheSaveFence.publishIfCurrent(token) {
                val target = AtomicFile(cacheFile(sourceId, bookUrl, chapter.index))
                val json = JSONObject().apply {
                    put("chapterUrl", chapter.url)
                    put("title", value.content.title)
                    put("text", value.content.text)
                    put("nextPageUrl", value.content.nextPageUrl)
                    put("previousChapterUrl", value.content.previousChapterUrl)
                    put("savedAt", value.savedAtEpochMillis)
                }
                val encoded = json.toString().toByteArray(Charsets.UTF_8)
                require(encoded.size <= MAX_ENTRY_BYTES) { "Chapter cache entry exceeds size limit" }
                val output = target.startWrite()
                try {
                    output.write(encoded)
                    output.flush()
                    target.finishWrite(output)
                } catch (error: Exception) {
                    target.failWrite(output)
                    throw error
                }
                runCatching { prune(target.baseFile) }
            }
        }
    }

    override suspend fun invalidate(sourceId: String, bookUrl: String, chapterIndex: Int) =
        withContext(Dispatchers.IO) {
            val key = ChapterContentCacheKey(sourceId, bookUrl, chapterIndex)
            chapterCacheMutex.withLock {
                chapterCacheSaveFence.invalidate(key)
                AtomicFile(cacheFile(sourceId, bookUrl, chapterIndex)).delete()
            }
            Unit
        }

    private fun cacheFile(sourceId: String, bookUrl: String, chapterUrl: String): File {
        return File(directory, "${chapterCacheDigest(sourceId, bookUrl, chapterUrl)}.json")
    }

    private fun cacheFile(sourceId: String, bookUrl: String, chapterIndex: Int): File =
        File(directory, "${chapterCacheDigest(sourceId, bookUrl, chapterIndex)}.json")

    /** Evict least-recently-read entries until the shared cache is within budget. */
    private fun prune(protectedFile: File) {
        val files = directory.listFiles()?.filter { it.extension == "json" } ?: return
        pruneCacheFiles(files, MAX_CACHE_BYTES, protectedFile)
    }

    private companion object {
        const val CACHE_DIRECTORY = "legado_chapter_cache"
        const val MAX_ENTRY_BYTES = 4L * 1024 * 1024
        const val MAX_CACHE_BYTES = 64L * 1024 * 1024
    }
}


/** Length-prefix each identity field so embedded separators cannot alias another chapter key. */
internal fun chapterCacheDigest(sourceId: String, bookUrl: String, chapterIndex: Int): String =
    chapterCacheDigestIdentity(sourceId, bookUrl, "index:$chapterIndex")

internal fun chapterCacheDigest(sourceId: String, bookUrl: String, chapterUrl: String): String =
    chapterCacheDigestIdentity(sourceId, bookUrl, "url:$chapterUrl")

private fun chapterCacheDigestIdentity(sourceId: String, bookUrl: String, chapterIdentity: String): String {
    val raw = buildString {
        listOf(sourceId, bookUrl, chapterIdentity).forEach { value ->
            append(value.length).append(':').append(value)
        }
    }.toByteArray(Charsets.UTF_8)
    return MessageDigest.getInstance("SHA-256").digest(raw)
        .joinToString("") { byte -> "%02x".format(byte) }
}

/** Read at most one byte beyond the limit so corrupted entries stay bounded. */
internal fun readBoundedChapterCacheEntry(
    input: java.io.InputStream,
    maxBytes: Long,
): ByteArray? {
    require(maxBytes >= 0)
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val allowed = (maxBytes - total + 1).coerceAtMost(buffer.size.toLong()).toInt()
        val count = input.read(buffer, 0, allowed)
        if (count < 0) break
        if (count == 0) {
            // A non-conforming or non-blocking stream may make no progress on
            // a bulk read. Fall back to one byte so this bounded loop cannot spin.
            val next = input.read()
            if (next < 0) break
            total++
            if (total > maxBytes) return null
            output.write(next)
            continue
        }
        total += count
        if (total > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
