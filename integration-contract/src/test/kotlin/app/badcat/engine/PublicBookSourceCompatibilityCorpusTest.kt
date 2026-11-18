package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Rule-shape regression corpus taken from public Legado source repositories.
 * HTML/JSON bodies are deterministic fixtures: these tests validate Badcat's
 * Kotlin parser/runtime path independently of third-party site availability.
 *
 * Source references:
 * - https://github.com/speauty/legado.sources/tree/main/sources/active
 * - https://github.com/cjj200011222/legado-booksource
 */
class PublicBookSourceCompatibilityCorpusTest {

    private fun source(
        id: String,
        name: String,
        search: String? = null,
        detail: String? = null,
        toc: String? = null,
        content: String? = null,
    ) = BookSource(
        id = id,
        name = name,
        baseUrl = id,
        ruleSearchJson = search,
        ruleBookInfoJson = detail,
        ruleTocJson = toc,
        ruleContentJson = content,
    )

    @Test
    fun bixiangePublicCssSearchRulesParseChainedBookListAndFields() {
        // Public source rule: class.list@tag.ul@tag.li
        val bookSource = source(
            id = "https://www.bixiange.top/",
            name = "笔仙阁",
            search = """{
                "bookList":"class.list@tag.ul@tag.li",
                "name":"class.title@tag.a@text",
                "bookUrl":"class.title@tag.a@href",
                "author":"class.tips@tag.span.0@text"
            }""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://www.bixiange.top/search",
            body = """
                <div class="list"><ul>
                  <li><span class="title"><a href="/book/1">测试小说甲</a></span><span class="tips"><span>作者：甲</span></span></li>
                  <li><span class="title"><a href="/book/2">测试小说乙</a></span><span class="tips"><span>作者：乙</span></span></li>
                </ul></div>
            """.trimIndent(),
        )

        val books = assertIs<EngineResult.Success<List<BookCandidate>>>(
            CssSearchResultsParser().parse(bookSource, response),
        ).value
        assertEquals(listOf("测试小说甲", "测试小说乙"), books.map { it.name })
        assertEquals(
            listOf("https://www.bixiange.top/book/1", "https://www.bixiange.top/book/2"),
            books.map { it.bookUrl },
        )
    }

    @Test
    fun aixiashuPublicTocRulesParseChainedSelectorsAndColonIndexExclusions() {
        // Public source rule: id.list@tag.dl@tag.dd!0:1:2:3:4:5:6:7:8
        val bookSource = source(
            id = "http://www.aixiashu.la/",
            name = "爱下书小说网",
            toc = """{
                "chapterList":"id.list@tag.dl@tag.dd!0:1:2:3:4:5:6:7:8",
                "chapterName":"a@text",
                "chapterUrl":"a@href"
            }""",
        )
        val rows = (0..9).joinToString("") { index ->
            """<dd><a href="/chapter/$index">章节$index</a></dd>"""
        }
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "http://www.aixiashu.la/book/1",
            body = """<div id="list"><dl>$rows</dl></div>""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(bookSource, response, "http://www.aixiashu.la/book/1"),
        ).value
        assertEquals(listOf("章节9"), page.chapters.map { it.title })
        assertEquals(listOf("http://www.aixiashu.la/chapter/9"), page.chapters.map { it.url })
    }

    @Test
    fun xuanhuangePublicContentRuleExtractsTextNodes() {
        // Public source rule: id.content@textNodes
        val bookSource = source(
            id = "http://www.xuanhuange.info/",
            name = "玄幻阁小说网",
            content = """{"content":"id.content@textNodes"}""",
        )
        val chapter = BookChapter(0, "正文", "http://www.xuanhuange.info/read/1")
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = chapter.url,
            body = """<div id="content">第一段<p>嵌套段落</p>第二段</div>""",
        )

        val parsed = assertIs<EngineResult.Success<ChapterContent>>(
            CssChapterContentParser().parse(bookSource, response, chapter),
        ).value
        assertEquals("第一段\n第二段", parsed.text)
    }

    @Test
    fun shuhaigePublicJsonPathTocRulesResolveChapterFields() {
        // Public source rule: $.data.list + bare JSON object fields.
        val bookSource = source(
            id = "https://book.feius.live",
            name = "书海阁",
            toc = """{
                "chapterList":"$.data.list",
                "chapterName":"indexName",
                "chapterUrl":"$.id"
            }""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://book.feius.live/book/queryIndexList",
            contentType = "application/json",
            body = """{"data":{"list":[
                {"id":"101","indexName":"第一章"},
                {"id":"102","indexName":"第二章"}
            ]}}""",
        )

        val page = assertIs<EngineResult.Success<ParsedTocPage>>(
            CssTocParser().parse(bookSource, response, "https://book.feius.live/book/7"),
        ).value
        assertEquals(listOf("第一章", "第二章"), page.chapters.map { it.title })
        assertEquals(
            listOf("https://book.feius.live/101", "https://book.feius.live/102"),
            page.chapters.map { it.url },
        )
    }

    @Test
    fun detailRulesFromPublicCssSourcesResolveChainedSelectors() {
        val bookSource = source(
            id = "http://www.aixiashu.la/",
            name = "爱下书小说网",
            detail = """{
                "name":"id.info@tag.h1@text",
                "author":"id.info@tag.p.0@text"
            }""",
        )
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "http://www.aixiashu.la/book/1",
            body = """<div id="info"><h1>测试书名</h1><p>作者：测试作者</p></div>""",
        )
        val detail = assertIs<EngineResult.Success<BookDetail>>(
            CssBookInfoParser().parse(bookSource, response, response.finalUrl),
        ).value
        assertEquals("测试书名", detail.name)
        assertEquals("作者：测试作者", detail.author)
    }
}
