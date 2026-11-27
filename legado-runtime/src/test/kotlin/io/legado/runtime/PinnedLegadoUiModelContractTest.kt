package io.legado.runtime

import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.data.entities.rule.FlexChildStyle
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.utils.INITIAL_GSON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract checks for the pinned source-visible login/explore UI models. */
class PinnedLegadoUiModelContractTest {

    @Test
    fun rowUiKeepsPinnedFieldsDefaultsAndEquality() {
        val row = INITIAL_GSON.fromJson(
            """{"name":"token","type":"password","action":"submit()","chars":["A","B"],"key":"token","hint":"input","value":"x","options":["A","B"],"countdown":30,"futureField":true}""",
            RowUi::class.java,
        )
        assertEquals("token", row.name)
        assertEquals("password", row.type)
        assertEquals("submit()", row.action)
        assertEquals("token", row.key)
        assertEquals("input", row.hint)
        assertEquals("x", row.value)
        assertEquals(listOf("A", "B"), row.options)
        assertEquals(30, row.countdown)
        assertEquals(RowUi.Type.text, "text")
        assertEquals(RowUi.Type.button, "button")
        assertEquals(RowUi.Type.label, "label")
        assertEquals(RowUi.Type.toggle, "toggle")
        assertEquals(RowUi.Type.select, "select")
        assertNull(row.default)
        assertEquals("text", RowUi().type)
        assertEquals(RowUi(name = "n", type = "text"), RowUi(name = "n", type = "text", chars = arrayOf("ignored")))
        assertEquals(RowUi(name = "n", type = "text").hashCode(), RowUi(name = "n", type = "text", chars = arrayOf("ignored")).hashCode())
        assertNotNull(RowUi.Type.password)
    }

    @Test
    fun exploreKindKeepsPinnedFieldsDefaultsAndEquality() {
        val kind = INITIAL_GSON.fromJson(
            """{"title":"分类","url":"/discover","type":"select","action":"choose()","chars":["A","B"],"default":"A","viewName":"picker","futureField":true}""",
            ExploreKind::class.java,
        )
        assertEquals("分类", kind.title)
        assertEquals("/discover", kind.url)
        assertEquals("select", kind.type)
        assertEquals("choose()", kind.action)
        assertEquals("A", kind.default)
        assertEquals("picker", kind.viewName)
        assertEquals("url", ExploreKind().type)
        assertEquals(ExploreKind.Type.url, "url")
        assertEquals(ExploreKind.Type.text, "text")
        assertEquals(ExploreKind.Type.button, "button")
        assertEquals(ExploreKind.Type.toggle, "toggle")
        assertEquals(FlexChildStyle.defaultStyle, kind.style())
        assertEquals(ExploreKind(title = "A", url = "/a"), ExploreKind(title = "A", url = "/a", chars = arrayOf("ignored")))
        assertEquals(ExploreKind.Type.select, "select")
    }

    @Test
    fun flexChildStyleKeepsAllFieldsAndPinnedAlignSelfMapping() {
        val style = INITIAL_GSON.fromJson(
            """{"layout_flexGrow":2.5,"layout_flexShrink":0.5,"layout_alignSelf":"center","layout_flexBasisPercent":0.4,"layout_wrapBefore":true,"layout_justifySelf":"end","futureField":1}""",
            FlexChildStyle::class.java,
        )
        assertEquals(2.5f, style.layout_flexGrow, 0f)
        assertEquals(0.5f, style.layout_flexShrink, 0f)
        assertEquals("center", style.layout_alignSelf)
        assertEquals(0.4f, style.layout_flexBasisPercent, 0f)
        assertTrue(style.layout_wrapBefore)
        assertEquals("auto", FlexChildStyle.defaultStyle.layout_justifySelf)
        assertEquals("end", style.layout_justifySelf)
        assertEquals(-1, FlexChildStyle().alignSelf())
        assertEquals(0, FlexChildStyle(layout_alignSelf = "flex_start").alignSelf())
        assertEquals(1, FlexChildStyle(layout_alignSelf = "flex_end").alignSelf())
        assertEquals(2, FlexChildStyle(layout_alignSelf = "center").alignSelf())
        assertEquals(3, FlexChildStyle(layout_alignSelf = "baseline").alignSelf())
        assertEquals(4, FlexChildStyle(layout_alignSelf = "stretch").alignSelf())
        assertEquals(-1, FlexChildStyle(layout_alignSelf = "unknown").alignSelf())
    }
}
