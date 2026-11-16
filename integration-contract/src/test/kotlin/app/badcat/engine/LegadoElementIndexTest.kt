package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LegadoElementIndexTest {

    private val items = listOf("a", "b", "c", "d", "e")

    @Test
    fun supportsLegacySinglePositiveAndNegativeIndexes() {
        val positive = LegadoElementIndex.parse("li.item.1")!!
        val negative = LegadoElementIndex.parse("li.item.-1")!!
        assertEquals(listOf("b"), LegadoElementIndex.apply(items, positive))
        assertEquals(listOf("e"), LegadoElementIndex.apply(items, negative))
    }

    @Test
    fun supportsBracketIndexListsAndRanges() {
        val parsed = LegadoElementIndex.parse("li.item[0, 2:4, -1]")!!
        assertEquals(listOf("a", "c", "d", "e"), LegadoElementIndex.apply(items, parsed))
    }

    @Test
    fun supportsLegacyRangeAndExclusion() {
        val range = LegadoElementIndex.parse("li.item.1:4:2")!!
        val exclusion = LegadoElementIndex.parse("li.item!1:3")!!
        assertEquals(listOf("b", "d"), LegadoElementIndex.apply(items, range))
        assertEquals(listOf("a", "e"), LegadoElementIndex.apply(items, exclusion))
    }

    @Test
    fun supportsReverseRangesAndOutOfRangeClamping() {
        val reverse = LegadoElementIndex.parse("li.item[4:1]")!!
        val clamped = LegadoElementIndex.parse("li.item[0:99]")!!
        assertEquals(listOf("e", "d", "c", "b"), LegadoElementIndex.apply(items, reverse))
        assertEquals(items, LegadoElementIndex.apply(items, clamped))
    }

    @Test
    fun leavesOrdinaryCssClassPseudoClassAndAttributeSelectorsUntouched() {
        assertNull(LegadoElementIndex.parse("div.book"))
        assertNull(LegadoElementIndex.parse("li:first-child"))
        assertNull(LegadoElementIndex.parse("a[href]"))
        assertNull(LegadoElementIndex.parse("div[data-index='1']"))
    }

    @Test
    fun rejectsMalformedIndexSuffixes() {
        assertNull(LegadoElementIndex.parse("li.item[]"))
        assertNull(LegadoElementIndex.parse("li.item[1:2:3:4]"))
        assertNull(LegadoElementIndex.parse("li.item[1:2:0]"))
    }

    @Test
    fun supportsBracketExclusionAndLegadoReverseSentinel() {
        val excluded = LegadoElementIndex.parse("li.item[!0, -1]")!!
        val reversed = LegadoElementIndex.parse("li.item[-1:0]")!!
        assertEquals(listOf("b", "c", "d"), LegadoElementIndex.apply(items, excluded))
        assertEquals(listOf("e", "d", "c", "b", "a"), LegadoElementIndex.apply(items, reversed))
    }

    @Test
    fun appliesLegadoNegativeStepNormalization() {
        val parsed = LegadoElementIndex.parse("li.item[0:4:-1]")!!
        assertEquals(listOf("a", "e"), LegadoElementIndex.apply(items, parsed))
    }

}
