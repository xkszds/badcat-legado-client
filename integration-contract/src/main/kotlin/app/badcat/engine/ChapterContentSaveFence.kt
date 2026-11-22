package app.badcat.engine

/**
 * In-process version fence for asynchronous chapter cache writes.
 *
 * The key is the upstream-compatible book URL + chapter index, extended with
 * Badcat's source identity. A replace advances the version before executing
 * the write, so even a failed replacement invalidates older in-flight tokens.
 */
class ChapterContentSaveFence {
    private val versions = HashMap<ChapterContentCacheKey, Long>()

    companion object {
        /**
         * Shared by every on-disk chapter cache instance in this process so an
         * invalidation from one reader engine fences requests started by another.
         *
         * Generations are retained for the process lifetime; evicting one while
         * a request token is in flight could allow a stale write to publish.
         */
        val processWide = ChapterContentSaveFence()
    }

    @Synchronized
    fun token(key: ChapterContentCacheKey): ChapterContentSaveToken =
        ChapterContentSaveToken(key, versions[key] ?: 0L)

    @Synchronized
    fun isCurrent(token: ChapterContentSaveToken): Boolean =
        (versions[token.key] ?: 0L) == token.version

    /** Execute publication atomically only if the request generation is current. */
    @Synchronized
    fun publishIfCurrent(token: ChapterContentSaveToken, publish: () -> Unit): Boolean {
        if ((versions[token.key] ?: 0L) != token.version) return false
        publish()
        return true
    }

    /** Advance generation before replacement; a throwing write still fences old requests. */
    @Synchronized
    fun replace(key: ChapterContentCacheKey, write: () -> Unit) {
        versions[key] = (versions[key] ?: 0L) + 1L
        write()
    }

    @Synchronized
    fun invalidate(key: ChapterContentCacheKey) {
        versions[key] = (versions[key] ?: 0L) + 1L
    }
}
