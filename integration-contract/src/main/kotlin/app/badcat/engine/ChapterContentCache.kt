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
    /** Legacy URL-keyed methods remain for source compatibility only. */
    suspend fun get(sourceId: String, bookUrl: String, chapterUrl: String): CachedChapterContent?
    suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        value: CachedChapterContent,
    )

    /** Identity-safe methods. Implementations should override these for index-keyed storage. */
    fun beginSave(sourceId: String, bookUrl: String, chapter: BookChapter): ChapterContentSaveToken =
        ChapterContentSaveToken(ChapterContentCacheKey(sourceId, bookUrl, chapter.index), 0L)

    suspend fun get(sourceId: String, bookUrl: String, chapter: BookChapter): CachedChapterContent? =
        get(sourceId, bookUrl, chapter.url)

    fun isCurrent(token: ChapterContentSaveToken): Boolean = true

    /** Returns false when the request token is stale and publication is rejected. */
    suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapter: BookChapter,
        token: ChapterContentSaveToken,
        value: CachedChapterContent,
    ): Boolean {
        put(sourceId, bookUrl, chapter.url, value)
        return true
    }

    /** Invalidates cached content and advances the generation for this chapter identity. */
    suspend fun invalidate(sourceId: String, bookUrl: String, chapterIndex: Int) = Unit
}
