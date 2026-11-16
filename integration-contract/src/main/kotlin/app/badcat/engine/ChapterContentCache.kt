package app.badcat.engine

/** A cached, already parsed and purified chapter payload. */
data class CachedChapterContent(
    val content: ChapterContent,
    val savedAtEpochMillis: Long,
)

/** Host-owned persistence boundary for offline chapter reading. */
interface ChapterContentCache {
    suspend fun get(sourceId: String, bookUrl: String, chapterUrl: String): CachedChapterContent?
    suspend fun put(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
        value: CachedChapterContent,
    )
}
