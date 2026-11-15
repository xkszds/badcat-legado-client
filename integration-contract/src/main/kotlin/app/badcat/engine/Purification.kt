package app.badcat.engine

/**
 * Purification domains are intentionally isolated.
 * Chapter text rules must not be applied to HTML, catalog metadata or video payloads.
 */
fun interface BookTextReplacement {
    fun replace(chapterText: String): String
}

/**
 * Suspendable, book-aware replacement hook for durable Legado rule catalogs.
 * The origin is the selected book-source URL; the title is supplied by the
 * caller so title-scoped rules are not guessed from a URL.
 */
data class ReplacedChapterContent(
    val title: String,
    val text: String,
)

data class LegadoReplacementBookContext(
    val bookUrl: String,
    val origin: String,
    val originName: String,
    val name: String,
)

data class LegadoReplacementChapterContext(
    val url: String,
    val title: String,
    val bookUrl: String,
    val index: Int,
    val baseUrl: String,
)

data class LegadoReplacementContext(
    val book: LegadoReplacementBookContext,
    val chapter: LegadoReplacementChapterContext,
)

data class LegadoJsReplacementContext(
    val ruleId: Long,
    val ruleName: String,
    val result: String,
    val timeoutMillisecond: Long,
    val configuredTimeoutMillisecond: Long,
    val book: LegadoReplacementBookContext,
    val chapter: LegadoReplacementChapterContext,
)

fun interface LegadoJsReplacementEvaluator {
    fun evaluate(script: String, context: LegadoJsReplacementContext): String
}

fun interface ScopedBookTextReplacement {
    suspend fun replace(
        chapterTitle: String,
        chapterText: String,
        context: LegadoReplacementContext,
    ): ReplacedChapterContent
}

fun interface PageAdFilter {
    fun filter(html: String): String
}

fun interface CatalogCleaner {
    fun clean(chapters: List<BookChapter>): List<BookChapter>
}

fun interface VideoRuleProcessor {
    fun process(episodePayload: String): String
}

/** Applies each rule only to its matching content domain. */
class PurificationPipeline(
    private val bookTextReplacement: BookTextReplacement,
    private val pageAdFilter: PageAdFilter,
    private val catalogCleaner: CatalogCleaner,
    private val videoRuleProcessor: VideoRuleProcessor,
) {
    fun cleanChapterText(text: String): String = bookTextReplacement.replace(text)
    fun cleanPageHtml(html: String): String = pageAdFilter.filter(html)
    fun cleanCatalog(chapters: List<BookChapter>): List<BookChapter> = catalogCleaner.clean(chapters)
    fun cleanVideoPayload(payload: String): String = videoRuleProcessor.process(payload)
}
