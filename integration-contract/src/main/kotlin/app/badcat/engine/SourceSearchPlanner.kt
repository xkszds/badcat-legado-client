package app.badcat.engine

/** One concrete Legado entry point participating in an aggregate search. */
enum class SourceSearchOperationKind {
    SEARCH,
    EXPLORE,
}

data class SourceSearchOperation(
    val source: BookSource,
    val kind: SourceSearchOperationKind,
)

/**
 * Builds an untruncated search plan from enabled sources.
 *
 * Aggregate mode includes both searchUrl and enabled exploreUrl entry points.
 * A source may contribute both; callers de-duplicate returned books by source ID
 * and book URL. This planner deliberately does not reinterpret non-Legado PPRule
 * comic/video sources as book sources.
 */
object SourceSearchPlanner {
    fun plan(
        sources: List<BookSource>,
        includeExplore: Boolean = false,
    ): List<SourceSearchOperation> = buildList {
        sources.asSequence()
            .filter { it.enabled }
            .distinctBy { it.id }
            .forEach { source ->
                if (!source.searchUrl.isNullOrBlank()) {
                    add(SourceSearchOperation(source, SourceSearchOperationKind.SEARCH))
                }
                if (includeExplore && source.enabledExplore && !source.exploreUrl.isNullOrBlank()) {
                    add(SourceSearchOperation(source, SourceSearchOperationKind.EXPLORE))
                }
            }
    }
}
