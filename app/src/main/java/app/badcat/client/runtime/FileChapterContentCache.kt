package app.badcat.client.runtime

import android.content.Context
import app.badcat.engine.CachedChapterContent
import app.badcat.engine.ChapterContent
import app.badcat.engine.ChapterContentCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * Private, bounded on-disk chapter cache. It stores already parsed and
 * purified text, keyed by source + book + chapter. Expired entries may still
 * be returned by the service as an offline fallback after a network failure.
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
        val file = cacheFile(sourceId, bookUrl, chapterUrl)
        if (!file.isFile || file.length() > MAX_ENTRY_BYTES) return@withContext null
        val cached = runCatching {
            val json = JSONObject(file.readText(Charsets.UTF_8))
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
        if (cached != null) file.setLastModified(System.currentTimeMillis())
        cached
    }

    override suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        value: CachedChapterContent,
    ) = withContext(Dispatchers.IO) {
        if (value.content.text.toByteArray(Charsets.UTF_8).size > MAX_ENTRY_BYTES) return@withContext
        val target = cacheFile(sourceId, bookUrl, chapterUrl)
        val temporary = File(directory, target.name + ".tmp")
        val json = JSONObject().apply {
            put("title", value.content.title)
            put("text", value.content.text)
            put("nextPageUrl", value.content.nextPageUrl)
            put("previousChapterUrl", value.content.previousChapterUrl)
            put("savedAt", value.savedAtEpochMillis)
        }
        runCatching {
            temporary.writeText(json.toString(), Charsets.UTF_8)
            if (!temporary.renameTo(target)) {
                target.delete()
                check(temporary.renameTo(target)) { "Unable to commit chapter cache file" }
            }
            prune()
        }.also { if (it.isFailure) temporary.delete() }
    }

    private fun cacheFile(sourceId: String, bookUrl: String, chapterUrl: String): File {
        val raw = "$sourceId\n$bookUrl\n$chapterUrl".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(directory, "$digest.json")
    }

    private fun prune() {
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "json" } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private companion object {
        const val CACHE_DIRECTORY = "legado_chapter_cache"
        const val MAX_ENTRY_BYTES = 4L * 1024 * 1024
        const val MAX_CACHE_BYTES = 64L * 1024 * 1024
    }
}
