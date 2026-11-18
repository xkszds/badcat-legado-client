package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceSearchPlannerTest {
    @Test
    fun searchModeIncludesEveryEnabledSearchSourceWithoutTwentyFourSourceCap() {
        val sources = (1..40).map { index ->
            BookSource(
                id = "source-$index",
                name = "Source $index",
                baseUrl = "https://source$index.example/",
                searchUrl = "/search?q={{key}}",
                enabled = true,
            )
        } + BookSource(
            id = "disabled",
            name = "Disabled",
            baseUrl = "https://disabled.example/",
            searchUrl = "/search",
            enabled = false,
        )

        val plan = SourceSearchPlanner.plan(sources)

        assertEquals(40, plan.size)
        assertEquals((1..40).map { "source-$it" }, plan.map { it.source.id })
        assertEquals(
            setOf(SourceSearchOperationKind.SEARCH),
            plan.map { it.kind }.toSet(),
        )
    }


    @Test
    fun excludesAudioComicAndOtherSourceTypesFromNovelSearch() {
        val sources = listOf(
            BookSource("novel", "Novel", "https://novel.example/", searchUrl = "/search", sourceType = 0),
            BookSource("audio", "Audio", "https://audio.example/", searchUrl = "/search", sourceType = 1),
            BookSource("comic", "Comic", "https://comic.example/", searchUrl = "/search", sourceType = 2),
            BookSource("other", "Other", "https://other.example/", searchUrl = "/search", sourceType = 3),
        )

        val plan = SourceSearchPlanner.plan(sources)

        assertEquals(listOf("novel"), plan.map { it.source.id })
    }

    @Test
    fun aggregateModeIncludesSearchAndEnabledExploreEntryPoints() {
        val sources = listOf(
            BookSource(
                id = "both",
                name = "Both",
                baseUrl = "https://both.example/",
                searchUrl = "/search",
                exploreUrl = "/explore",
                enabledExplore = true,
            ),
            BookSource(
                id = "search-only",
                name = "Search only",
                baseUrl = "https://search.example/",
                searchUrl = "/search",
            ),
            BookSource(
                id = "explore-only",
                name = "Explore only",
                baseUrl = "https://explore.example/",
                exploreUrl = "/explore",
                enabledExplore = true,
            ),
            BookSource(
                id = "explore-disabled",
                name = "Explore disabled",
                baseUrl = "https://disabled.example/",
                exploreUrl = "/explore",
                enabledExplore = false,
            ),
        )

        val plan = SourceSearchPlanner.plan(sources, includeExplore = true)

        assertEquals(
            listOf(
                "both" to SourceSearchOperationKind.SEARCH,
                "both" to SourceSearchOperationKind.EXPLORE,
                "search-only" to SourceSearchOperationKind.SEARCH,
                "explore-only" to SourceSearchOperationKind.EXPLORE,
            ),
            plan.map { it.source.id to it.kind },
        )
    }

    @Test
    fun duplicateSourceIdsDoNotCreateDuplicateRequests() {
        val source = BookSource(
            id = "same",
            name = "Source",
            baseUrl = "https://same.example/",
            searchUrl = "/search",
        )

        val plan = SourceSearchPlanner.plan(listOf(source, source.copy(name = "duplicate")))

        assertEquals(1, plan.size)
        assertEquals("Source", plan.single().source.name)
    }
}
