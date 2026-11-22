package app.badcat.engine

/**
 * Platform-neutral bridge for Badcat-owned reader progress.
 *
 * The implementation owns persistence and legacy migration. Legado-facing code may consume
 * this contract, but must not depend on Android SharedPreferences, Activities or Badcat DAOs.
 * Chapter URLs remain source-scoped because the current Badcat reader restores by URL; a
 * compatibility adapter must resolve the URL to the pinned Legado chapter index when needed.
 */
interface ReadingProgressStore {
    fun lastChapterUrl(sourceId: String, bookUrl: String): String?

    fun saveLastChapterUrl(sourceId: String, bookUrl: String, chapterUrl: String)

    fun clearLastChapterUrl(sourceId: String, bookUrl: String)

    fun chapterScrollY(sourceId: String, bookUrl: String, chapterUrl: String): Int

    fun saveChapterScrollY(sourceId: String, bookUrl: String, chapterUrl: String, scrollY: Int)
}
