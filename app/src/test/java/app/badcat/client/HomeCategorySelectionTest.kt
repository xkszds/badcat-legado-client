package app.badcat.client

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCategorySelectionTest {
    @Test
    fun preservesConfiguredOrderAndFallsBackWhenEveryCategoryIsHidden() {
        assertEquals(listOf("漫画", "综合"), HomeCategorySelection.visibleCategories(setOf("综合", "漫画")))
        assertEquals(listOf("小说"), HomeCategorySelection.visibleCategories(emptySet()))
    }

    @Test
    fun selectedCategoryAlwaysMatchesVisibleTabs() {
        val visible = HomeCategorySelection.visibleCategories(setOf("漫画"))
        assertEquals("漫画", HomeCategorySelection.initialSelection("小说", visible))
        assertEquals("漫画", HomeCategorySelection.initialSelection("漫画", visible))
    }
}
