package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegadoSourceElementTest {
    @Test
    fun supportsCommonLegadoJsoupTraversalAndValueHelpers() {
        val document = LegadoSourceElement(
            """
            <section id="catalog">
              <div class="chapter"><input name="token" value="abc-123"><script>window.page = 2;</script><span>Chapter A</span></div>
              <div class="chapter"><span>Chapter B</span></div>
            </section>
            """.trimIndent(),
            "https://reader.example/book/1",
        )
        val first = assertNotNull(document.select(".chapter").first())
        val second = assertNotNull(first.next())

        assertEquals("Chapter A", first.text())
        assertTrue(first.hasAttr("class"))
        assertEquals("abc-123", first.select("input").first()?.`val`())
        assertEquals("window.page = 2;", first.select("script").first()?.data())
        assertEquals("section", first.parent()?.tagName())
        assertEquals("Chapter B", second.text())
        assertEquals("Chapter A", second.prev()?.text())
        assertEquals(1, first.siblings().size)
        assertEquals("section", first.parents().first()?.tagName())
        assertEquals("div", first.children().first()?.parent()?.tagName())
        assertEquals("script", first.children().first()?.next()?.tagName())
    }
}
