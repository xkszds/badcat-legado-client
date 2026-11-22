package app.badcat.client.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class UpstreamLegadoCssRuleAdapterTest {
    private val parser = UpstreamLegadoCssRuleAdapter()

    @Test
    fun evaluatesTheReportedTextNodesRuleWithThePinnedUpstreamParser() {
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

        assertEquals(
            listOf("第五段"),
            parser.getStringList(html, "https://example.com/book", "@.w_txt li.4@textNodes"),
        )
    }

    @Test
    fun preservesOriginalLegacyIndexOrderingAndExclusion() {
        val html = """
            <ul><li>zero</li><li>one</li><li>two</li><li>three</li><li>four</li></ul>
        """.trimIndent()

        assertEquals(
            listOf("three", "one", "four"),
            parser.getStringList(html, "https://example.com", "tag.ul@tag.li.3:1:4@text"),
        )
        assertEquals(
            listOf("zero", "two", "four"),
            parser.getStringList(html, "https://example.com", "tag.ul@tag.li!1:3@text"),
        )
    }

    @Test
    fun supportsChainedSelectorRootsAndPerElementExtraction() {
        val html = """
            <div class="list"><ul><li><a href="/a">Alpha</a></li><li><a href="/b">Beta</a></li></ul></div>
        """.trimIndent()

        assertEquals(
            listOf("Alpha", "Beta"),
            parser.getStringList(html, "https://example.com", "class.list@tag.ul@tag.li@tag.a@text"),
        )
        assertEquals(
            listOf("<li><a href=\"/a\">Alpha</a></li>", "<li><a href=\"/b\">Beta</a></li>"),
            parser.getElementHtmlList(html, "https://example.com", "class.list@tag.ul@tag.li"),
        )
    }

    @Test
    fun handlesFallbackAndInterleaveOperatorsUsingUpstreamRuleAnalyzer() {
        val html = """
            <div class="a"><i>A1</i><i>A2</i></div>
            <div class="b"><i>B1</i><i>B2</i></div>
        """.trimIndent()

        assertEquals(
            listOf("A1", "A2"),
            parser.getStringList(html, "https://example.com", ".missing@text||.a i@text"),
        )
        assertEquals(
            listOf("A1", "B1", "A2", "B2"),
            parser.getStringList(html, "https://example.com", ".a i@text%%.b i@text"),
        )
    }
}
