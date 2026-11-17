package app.badcat.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookStatusBadgeTest {
    @Test
    fun mapsOnlyExplicitKnownStatusLabels() {
        assertEquals("更新", BookStatusBadge.label("最近更新"))
        assertEquals("连载", BookStatusBadge.label("连载中"))
        assertEquals("完结", BookStatusBadge.label("已完结"))
        assertNull(BookStatusBadge.label("暂无"))
        assertNull(BookStatusBadge.label(null))
        assertNull(BookStatusBadge.label("第100章"))
    }
}
