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
}
