package app.badcat.engine

/** A cached, already parsed and purified chapter payload. */
data class CachedChapterContent(
    val content: ChapterContent,
    val savedAtEpochMillis: Long,
)

/** Stable host cache identity; URL is metadata/navigation, never the chapter key. */
data class ChapterContentCacheKey(
    val sourceId: String,
    val bookUrl: String,
    val chapterIndex: Int,
) {
    init {
        require(sourceId.isNotBlank())
        require(bookUrl.isNotBlank())
        require(chapterIndex >= 0)
    }
}

/** Captured before a network request; stale tokens may not publish cache content. */
data class ChapterContentSaveToken(
    val key: ChapterContentCacheKey,
    val version: Long,
)

/** Host-owned persistence boundary for offline chapter reading. */
interface ChapterContentCache {
    /** Legacy URL-keyed operations are retained for migration/debug compatibility. */
    suspend fun get(sourceId: String, bookUrl: String, chapterUrl: String): CachedChapterContent?
    suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        value: CachedChapterContent,
    )

    /** Every implementation must provide the index-keyed, fenced operations. */
    fun beginSave(sourceId: String, bookUrl: String, chapter: BookChapter): ChapterContentSaveToken
    suspend fun get(sourceId: String, bookUrl: String, chapter: BookChapter): CachedChapterContent?
    fun isCurrent(token: ChapterContentSaveToken): Boolean
    suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapter: BookChapter,
        token: ChapterContentSaveToken,
        value: CachedChapterContent,
    ): Boolean
    suspend fun invalidate(sourceId: String, bookUrl: String, chapterIndex: Int)
}
