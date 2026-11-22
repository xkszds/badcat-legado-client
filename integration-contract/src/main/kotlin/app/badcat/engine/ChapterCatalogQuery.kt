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

    /** Resolve URL navigation only when it identifies exactly one chapter. */
    fun findByUrl(chapters: List<BookChapter>, savedUrl: String?): BookChapter? {
        val url = savedUrl?.takeIf(String::isNotBlank) ?: return null
        val matches = chapters.filter { it.url == url }
        return matches.singleOrNull()
    }

    /** Resolve the persisted chapter index first; URL is a compatibility check, not identity. */
    fun findByIdentity(chapters: List<BookChapter>, index: Int?, url: String?): BookChapter? {
        if (index != null && index >= 0) {
            return chapters.singleOrNull { it.index == index }
                ?.takeIf { url.isNullOrBlank() || it.url == url }
        }
        return findByUrl(chapters, url)
    }
}
