package app.badcat.engine

/**
 * Platform-neutral boundary between the existing Badcat UI and a reading engine.
 *
 * This contract deliberately contains no Android or Legado implementation details.
 * A production adapter must map these operations to a pinned, reviewed engine version.
 */
interface ReadingEngine {
    suspend fun sources(): EngineResult<List<BookSource>>

    suspend fun search(
        sourceId: String,
        keyword: String,
    ): EngineResult<List<BookCandidate>>

    suspend fun bookDetail(
        sourceId: String,
        bookUrl: String,
    ): EngineResult<BookDetail>

    suspend fun chapters(
        sourceId: String,
        bookUrl: String,
    ): EngineResult<List<BookChapter>>

    suspend fun chapterContent(
        sourceId: String,
        bookUrl: String,
        chapterUrl: String,
    ): EngineResult<ChapterContent>
}

data class BookSource(
    val id: String,
    val name: String,
    val baseUrl: String,
    val enabled: Boolean = true,
    val group: String? = null,
    val sourceType: Int = 0,
    val enabledExplore: Boolean = true,
    val enabledCookieJar: Boolean? = true,
    val header: String? = null,
    val jsLib: String? = null,
    val loginUrl: String? = null,
    val exploreUrl: String? = null,
    val searchUrl: String? = null,
    val mainJs: String? = null,
    /** Raw JSON objects for upstream nested rule models; not executable by this contract. */
    val ruleExploreJson: String? = null,
    val ruleSearchJson: String? = null,
    val ruleBookInfoJson: String? = null,
    val ruleTocJson: String? = null,
    val ruleContentJson: String? = null,
    val ruleReviewJson: String? = null,
    /** Full upstream JSON, including rule fields not yet mapped by the host. */
    val rawJson: String? = null,
)

data class BookCandidate(
    val sourceId: String,
    val name: String,
    val author: String? = null,
    val bookUrl: String,
    val coverUrl: String? = null,
    val latestChapter: String? = null,
    val intro: String? = null,
)

data class BookDetail(
    val sourceId: String,
    val name: String,
    val author: String? = null,
    val bookUrl: String,
    val coverUrl: String? = null,
    val intro: String? = null,
    val latestChapter: String? = null,
    val tocUrl: String? = null,
)

data class BookChapter(
    val index: Int,
    val title: String,
    val url: String,
)

data class ChapterContent(
    val title: String,
    val text: String,
    val nextPageUrl: String? = null,
    val previousChapterUrl: String? = null,
)

/** Typed result keeps engine failures out of UI-specific exception handling. */
sealed interface EngineResult<out T> {
    data class Success<T>(val value: T) : EngineResult<T>
    data class Failure(
        val code: EngineErrorCode,
        val message: String,
        val cause: Throwable? = null,
    ) : EngineResult<Nothing>
}

enum class EngineErrorCode {
    INVALID_SOURCE,
    NETWORK,
    PARSE,
    NOT_FOUND,
    UNSUPPORTED_RULE,
    CANCELLED,
    UNKNOWN,
}
