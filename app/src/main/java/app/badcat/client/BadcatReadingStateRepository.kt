package app.badcat.client

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import app.badcat.engine.ReadingProgressStore

/**
 * Single Badcat-owned access boundary for durable reading state.
 *
 * Existing preference names, key encodings and JSON shapes are intentionally retained so
 * extracting storage from Activities does not invalidate user data or change reader behavior.
 * This repository is not an official Legado Book DAO; the compatibility facade will map its
 * typed state to the pinned upstream model without exposing SharedPreferences to the runtime.
 */
class BadcatReadingStateRepository(context: Context) : ReadingProgressStore {
    private val appContext = context.applicationContext
    private val progress: SharedPreferences
        get() = appContext.getSharedPreferences(PROGRESS_PREFS, Context.MODE_PRIVATE)
    private val history: SharedPreferences
        get() = appContext.getSharedPreferences(ReadingHistoryActivity.PREFS, Context.MODE_PRIVATE)

    data class Bookmark(
        val sourceId: String,
        val bookUrl: String,
        val chapterUrl: String,
        val bookName: String,
        val chapterTitle: String,
        val scrollY: Int,
        val savedAt: Long,
    )

    data class HistoryRecord(
        val sourceId: String,
        val bookUrl: String,
        val bookName: String,
        val chapterUrl: String,
        val chapterTitle: String,
        val updatedAt: Long,
    )

    override fun lastChapterUrl(sourceId: String, bookUrl: String): String? =
        progress.getString(lastChapterKey(sourceId, bookUrl), null)

    override fun saveLastChapterUrl(sourceId: String, bookUrl: String, chapterUrl: String) {
        if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) return
        progress.edit().putString(lastChapterKey(sourceId, bookUrl), chapterUrl).apply()
    }

    override fun clearLastChapterUrl(sourceId: String, bookUrl: String) {
        progress.edit().remove(lastChapterKey(sourceId, bookUrl)).apply()
    }

    override fun chapterScrollY(sourceId: String, bookUrl: String, chapterUrl: String): Int =
        progress.getInt(chapterScrollKey(sourceId, bookUrl, chapterUrl), 0).coerceAtLeast(0)

    override fun saveChapterScrollY(sourceId: String, bookUrl: String, chapterUrl: String, scrollY: Int) {
        if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) return
        progress.edit().putInt(chapterScrollKey(sourceId, bookUrl, chapterUrl), scrollY.coerceAtLeast(0)).apply()
    }

    fun bookmarkScrollY(sourceId: String, bookUrl: String, chapterUrl: String): Int? {
        val key = bookmarkKey(sourceId, bookUrl, chapterUrl)
        return if (progress.contains(key)) progress.getInt(key, 0).coerceAtLeast(0) else null
    }

    fun isBookmarked(sourceId: String, bookUrl: String, chapterUrl: String): Boolean =
        progress.contains(bookmarkKey(sourceId, bookUrl, chapterUrl))

    fun bookmarks(sourceId: String? = null, bookUrl: String? = null): List<Bookmark> =
        progress.all.mapNotNull { (key, value) ->
            if (!key.startsWith(BOOKMARK_PREFIX) || value !is Int) return@mapNotNull null
            val encoded = key.removePrefix(BOOKMARK_PREFIX)
            val identity = decodeIdentity(encoded) ?: return@mapNotNull null
            if (sourceId != null && identity[0] != sourceId) return@mapNotNull null
            if (bookUrl != null && identity[1] != bookUrl) return@mapNotNull null
            Bookmark(
                sourceId = identity[0],
                bookUrl = identity[1],
                chapterUrl = identity[2],
                bookName = progress.getString(BOOKMARK_BOOK_PREFIX + encoded, null)
                    .orEmpty().ifBlank { "未命名小说" },
                chapterTitle = progress.getString(BOOKMARK_TITLE_PREFIX + encoded, null)
                    .orEmpty().ifBlank { identity[2].substringAfterLast('/').take(48).ifBlank { "章节书签" } },
                scrollY = value.coerceAtLeast(0),
                savedAt = progress.getLong(BOOKMARK_TIME_PREFIX + encoded, 0L),
            )
        }.sortedWith(compareByDescending<Bookmark> { it.savedAt }.thenByDescending { encodeIdentity(it.sourceId, it.bookUrl, it.chapterUrl) })

    fun saveBookmark(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        bookName: String,
        chapterTitle: String,
        scrollY: Int,
        savedAt: Long = System.currentTimeMillis(),
    ) {
        if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) return
        val encoded = encodeIdentity(sourceId, bookUrl, chapterUrl)
        progress.edit()
            .putInt(BOOKMARK_PREFIX + encoded, scrollY.coerceAtLeast(0))
            .putString(BOOKMARK_TITLE_PREFIX + encoded, chapterTitle.ifBlank { "章节书签" })
            .putString(BOOKMARK_BOOK_PREFIX + encoded, bookName.ifBlank { "未命名小说" })
            .putLong(BOOKMARK_TIME_PREFIX + encoded, savedAt)
            .apply()
    }

    fun removeBookmark(sourceId: String, bookUrl: String, chapterUrl: String) {
        removeBookmarkByEncoded(encodeIdentity(sourceId, bookUrl, chapterUrl))
    }

    fun removeBookmark(bookmark: Bookmark) =
        removeBookmark(bookmark.sourceId, bookmark.bookUrl, bookmark.chapterUrl)

    private fun removeBookmarkByEncoded(encoded: String) {
        progress.edit()
            .remove(BOOKMARK_PREFIX + encoded)
            .remove(BOOKMARK_TITLE_PREFIX + encoded)
            .remove(BOOKMARK_BOOK_PREFIX + encoded)
            .remove(BOOKMARK_TIME_PREFIX + encoded)
            .apply()
    }

    fun clearBookmarks() {
        val edit = progress.edit()
        progress.all.keys.filter { it.startsWith(BOOKMARK_PREFIX) }.forEach { key -> edit.remove(key) }
        edit.apply()
    }

    fun historyRecords(): List<HistoryRecord> {
        val raw = history.getString(ReadingHistoryActivity.KEY, "[]").orEmpty()
        return runCatching {
            val records = JSONArray(raw)
            (0 until records.length()).mapNotNull { index ->
                val item = records.optJSONObject(index) ?: return@mapNotNull null
                val source = item.optString("sourceId")
                val book = item.optString("bookUrl")
                val chapter = item.optString("chapterUrl")
                if (source.isBlank() || book.isBlank() || chapter.isBlank()) return@mapNotNull null
                HistoryRecord(
                    source, book,
                    item.optString("bookName").ifBlank { "未命名书籍" },
                    chapter,
                    item.optString("chapterTitle").ifBlank { "继续阅读" },
                    item.optLong("updatedAt", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveHistory(
        sourceId: String,
        bookUrl: String,
        bookName: String,
        chapterUrl: String,
        chapterTitle: String,
        updatedAt: Long = System.currentTimeMillis(),
    ) {
        if (sourceId.isBlank() || bookUrl.isBlank() || chapterUrl.isBlank()) return
        val old = readHistoryJson()
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optString("sourceId") != sourceId || item.optString("bookUrl") != bookUrl) next.put(item)
        }
        next.put(JSONObject()
            .put("sourceId", sourceId)
            .put("bookUrl", bookUrl)
            .put("bookName", bookName)
            .put("chapterUrl", chapterUrl)
            .put("chapterTitle", chapterTitle)
            .put("updatedAt", updatedAt))
        history.edit().putString(ReadingHistoryActivity.KEY, next.toString()).apply()
    }

    fun removeHistory(sourceId: String, bookUrl: String) {
        val old = readHistoryJson()
        val next = JSONArray()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optString("sourceId") != sourceId || item.optString("bookUrl") != bookUrl) next.put(item)
        }
        history.edit().putString(ReadingHistoryActivity.KEY, next.toString()).apply()
    }

    fun clearHistory() {
        history.edit().putString(ReadingHistoryActivity.KEY, "[]").apply()
    }

    fun latestReadTimes(): Map<String, Long> = historyRecords().fold(mutableMapOf()) { result, item ->
        if (item.updatedAt > 0L) {
            val key = item.sourceId + "|" + item.bookUrl
            result[key] = maxOf(result[key] ?: 0L, item.updatedAt)
        }
        result
    }

    private fun readHistoryJson(): JSONArray =
        runCatching { JSONArray(history.getString(ReadingHistoryActivity.KEY, "[]").orEmpty()) }
            .getOrDefault(JSONArray())

    private fun decodeIdentity(encoded: String): List<String>? = runCatching {
        String(Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
            .split("\n").takeIf { it.size == 3 && it.all(String::isNotBlank) }
    }.getOrNull()

    private fun encodeIdentity(sourceId: String, bookUrl: String, chapterUrl: String): String {
        val raw = "$sourceId\n$bookUrl\n$chapterUrl".toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun lastChapterKey(sourceId: String, bookUrl: String): String {
        val raw = "$sourceId\n$bookUrl".toByteArray(Charsets.UTF_8)
        return LAST_CHAPTER_PREFIX + Base64.encodeToString(raw, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun chapterScrollKey(sourceId: String, bookUrl: String, chapterUrl: String): String =
        CHAPTER_SCROLL_PREFIX + encodeIdentity(sourceId, bookUrl, chapterUrl)

    private fun bookmarkKey(sourceId: String, bookUrl: String, chapterUrl: String): String =
        BOOKMARK_PREFIX + encodeIdentity(sourceId, bookUrl, chapterUrl)

    private companion object {
        const val PROGRESS_PREFS = "badcat_reading_progress"
        const val LAST_CHAPTER_PREFIX = "last_chapter_"
        const val CHAPTER_SCROLL_PREFIX = "chapter_scroll_"
        const val BOOKMARK_PREFIX = "chapter_bookmark_"
        const val BOOKMARK_TITLE_PREFIX = "chapter_bookmark_title_"
        const val BOOKMARK_BOOK_PREFIX = "chapter_bookmark_book_"
        const val BOOKMARK_TIME_PREFIX = "chapter_bookmark_time_"
    }
}
