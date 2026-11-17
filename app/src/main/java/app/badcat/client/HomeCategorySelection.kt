package app.badcat.client

/** Keeps the home search category usable even when layout preferences hide every tab. */
object HomeCategorySelection {
    private val supported = listOf("小说", "漫画", "综合")

    fun visibleCategories(configured: Set<String>): List<String> {
        val visible = supported.filter { it in configured }
        return visible.ifEmpty { listOf("小说") }
    }

    fun initialSelection(current: String, visible: List<String>): String =
        current.takeIf { it in visible } ?: visible.firstOrNull() ?: "小说"
}
