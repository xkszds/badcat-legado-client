package app.badcat.engine

/** Filters and optionally reverses display order without mutating chapter identity or URLs. */
object ChapterCatalogQuery {
    fun apply(chapters: List<BookChapter>, query: String, reverse: Boolean = false): List<BookChapter> {
        val keyword = query.trim()
        val matching = if (keyword.isEmpty()) chapters else chapters.filter {
            it.title.contains(keyword, ignoreCase = true)
        }
        return if (reverse) matching.asReversed() else matching
    }

    /** Resolve persisted reading progress against the current TOC, not a stale URL. */
    fun findByUrl(chapters: List<BookChapter>, savedUrl: String?): BookChapter? {
        val url = savedUrl?.takeIf(String::isNotBlank) ?: return null
        return chapters.firstOrNull { it.url == url }
    }
}
