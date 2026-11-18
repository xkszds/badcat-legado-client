package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpstreamLegadoCssRuleDispatcherTest {
    @Test
    fun productionDispatcherUsesPinnedUpstreamLegacyIndexAndTextNodesSemantics() {
        val html = """
            <html><body>
              <div class="w_txt">
                <li>第一段<strong>强调</strong>尾部</li>
                <li>第二段</li>
                <li>第三段</li>
                <li>第四段</li>
                <li>第五段</li>
              </div>
            </body></html>
        """.trimIndent()
        val document = SourceDocument("https://example.com/book", html, "text/html")

        val scalar = LegadoRuleExpressionDispatcher.evaluate(document, "@.w_txt li.4@textNodes")
        assertIs<EngineResult.Success<List<String>>>(scalar)
        assertEquals(listOf("第五段"), scalar.value)

        val collection = LegadoRuleExpressionDispatcher.evaluateElements(document, "@.w_txt li.4")
        assertIs<EngineResult.Success<List<String>>>(collection)
        assertEquals(1, collection.value.size)
        assertEquals("第五段", org.jsoup.Jsoup.parseBodyFragment(collection.value.single()).text())
    }

    @Test
    fun productionDispatcherUsesUpstreamChainedSelectors() {
        val html = """
            <div class="list"><ul><li><a href="/a">Alpha</a></li><li><a href="/b">Beta</a></li></ul></div>
        """.trimIndent()
        val document = SourceDocument("https://example.com", html, "text/html")

        val result = LegadoRuleExpressionDispatcher.evaluate(
            document,
            "class.list@tag.ul@tag.li@tag.a@text",
        )
        assertIs<EngineResult.Success<List<String>>>(result)
        assertEquals(listOf("Alpha", "Beta"), result.value)
    }
}
