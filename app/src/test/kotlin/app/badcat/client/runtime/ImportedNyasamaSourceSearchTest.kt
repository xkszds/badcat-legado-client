package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.CssSearchResultsParser
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoBookSourceCodec
import app.badcat.engine.SourceHttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replay the exact search-rule shapes imported from the user's Nyasama source
 * collection. Live host DNS is unavailable in this runner, so response bodies
 * are deterministic captures matching the source API/document formats.
 */
class ImportedNyasamaSourceSearchTest {
    private val parser = CssSearchResultsParser()
    private val runtime = RhinoSourceScriptRuntime()

    private fun source(json: String): BookSource =
        LegadoBookSourceCodec.decode(json).single().toEngineSource()

    @Test
    fun parsesNyasamaMkzhanJsonSearchRules() = runBlocking {
        val imported = source(
            """{"bookSourceName":"漫客栈","bookSourceUrl":"http://comic.mkzhan.com#♤Haxc","bookSourceType":2,"enabled":true,"enabledExplore":true,"ruleSearch":{"author":"$.author_title","bookList":"$.data.list.*","bookUrl":"https://comic.mkzhan.com/comic/info/?comic_id={$.comic_id}","coverUrl":"$.cover@js:result.replace(/\\.(jpg|JPG).*/g,\".$1!cover-400\")","lastChapter":"$.chapter_title","name":"$.title@put:{comic_id:$.comic_id}"},"searchUrl":"https://comic.mkzhan.com/search/keyword/?keyword={{key}}&page_num={{page}}&page_size=20"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://comic.mkzhan.com/search/keyword/?keyword=%E6%B5%B7%E8%B4%BC%E7%8E%8B&page_num=1&page_size=20",
            contentType = "application/json",
            body = """{"data":{"list":[{"comic_id":12345,"author_title":"尾田","cover":"https://img.example/cover.jpg?size=large","chapter_title":"第1话","title":"海贼王"}]}}""",
        )

        val result = parser.parseWithScripts(imported, response, runtime, key = "海贼王")
        assertTrue("Nyasama 漫客栈 search failed: $result", result is EngineResult.Success)
        val book = (result as EngineResult.Success).value.single()
        assertEquals("海贼王", book.name)
        assertEquals("尾田", book.author)
        assertEquals("https://comic.mkzhan.com/comic/info/?comic_id=12345", book.bookUrl)
        assertEquals("https://img.example/cover.jpg!cover-400", book.coverUrl)
    }

    @Test
    fun parsesNyasamaKuwoJsonSearchRules() = runBlocking {
        val imported = source(
            """{"bookSourceName":"酷我小说","bookSourceUrl":"http://appi.kuwo.cn","bookSourceType":0,"enabled":true,"enabledExplore":true,"ruleSearch":{"author":"$.author_name","bookList":"$.data","bookUrl":"/novels/api/book/{{$.book_id}}","coverUrl":"$.cover_url","intro":"$.intro","kind":"{{$.category_name}},{{$.status}}@js:result.replace(/30/,\"连载\").replace(/50/,\"完结\")","name":"$.title","wordCount":"$.all_words"},"searchUrl":"/novels/api/book/search?keyword={{key}}&pi={{page}}&ps=30"}""",
        )
        val response = SourceHttpResponse(
            200,
            "http://appi.kuwo.cn/novels/api/book/search?keyword=%E9%81%93%E5%90%9B&pi=1&ps=30",
            contentType = "application/json",
            body = """{"data":[{"book_id":6789,"author_name":"作者甲","cover_url":"https://img.example/kuwo.jpg","intro":"简介","category_name":"玄幻","status":30,"title":"道君","all_words":100000}]}""",
        )

        val result = parser.parseWithScripts(imported, response, runtime, key = "道君")
        assertTrue("Nyasama 酷我小说 search failed: $result", result is EngineResult.Success)
        val book = (result as EngineResult.Success).value.single()
        assertEquals("道君", book.name)
        assertEquals("作者甲", book.author)
        assertEquals("http://appi.kuwo.cn/novels/api/book/6789", book.bookUrl)
        assertEquals("https://img.example/kuwo.jpg", book.coverUrl)
    }

    @Test
    fun parsesNyasamaKanmanJsonSearchRulesAndPutVariable() = runBlocking {
        val imported = source(
            """{"bookSourceName":"看漫画","bookSourceUrl":"https://m.kanman.com#Haxc1107","bookSourceType":2,"enabled":true,"enabledExplore":true,"ruleSearch":{"author":"$.cartoon_author_list_name||$.comic_author","bookList":"$.data.data||$.data","bookUrl":"https://m.kanman.com/{{$.comic_id}}/","coverUrl":"https://image.yqmh.com/mh/{{$.comic_id}}.jpg-300x400.webp","intro":"$.cartoon_desc","kind":"$.comic_type##[a-zA-Z]|\\|","lastChapter":"$.latest_cartoon_topic_name||$.last_chapter_name","name":"$.comic_name@put:{bid:$.comic_id}"},"searchUrl":"https://m.kanman.com/api/serachcomic/?product_id=1&productname=kmh&platformname=wap&serachKey={{key}}&topNumber=10"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://m.kanman.com/api/serachcomic/?serachKey=%E6%B5%B7%E8%B4%BC%E7%8E%8B",
            contentType = "application/json",
            body = """{"data":{"data":[{"comic_id":2468,"comic_name":"海贼王","comic_author":"尾田","comic_type":"action|fantasy","cartoon_desc":"冒险漫画","latest_cartoon_topic_name":"第1100话"}]}}""",
        )

        val result = parser.parseWithScripts(imported, response, runtime, key = "海贼王")
        assertTrue("Nyasama 看漫画 search failed: $result", result is EngineResult.Success)
        val book = (result as EngineResult.Success).value.single()
        assertEquals("海贼王", book.name)
        assertEquals("https://m.kanman.com/2468/", book.bookUrl)
        assertEquals("https://image.yqmh.com/mh/2468.jpg-300x400.webp", book.coverUrl)
    }

    @Test
    fun parsesNyasamaTencentComicCssFallbackRules() = runBlocking {
        val imported = source(
            """{"bookSourceName":"腾讯漫画","bookSourceUrl":"https://m.ac.qq.com#Haxc1107","bookSourceType":2,"enabled":true,"enabledExplore":true,"ruleSearch":{"bookList":"id.lst_searchResult@tag.li||id.list_update@tag.li||class.comic-link","bookUrl":"class.comic-link@href","coverUrl":"class.comic-cover@tag.img@src","kind":"class.comic-tag@text","lastChapter":"class.chapter@text||class.comic-update@text##更新","name":"class.comic-title@text"},"searchUrl":"https://m.ac.qq.com/search/result?word={{key}}"}""",
        )
        val response = SourceHttpResponse(
            200,
            "https://m.ac.qq.com/search/result?word=%E6%B5%B7%E8%B4%BC%E7%8E%8B",
            contentType = "text/html",
            body = """<ul id="lst_searchResult"><li><a class="comic-link" href="/comic/88"><img class="comic-cover" src="/cover.jpg"/><span class="comic-title">海贼王</span><span class="comic-tag">热血</span><span class="chapter">第1话</span></a></li></ul>""",
        )

        val result = parser.parseWithScripts(imported, response, runtime, key = "海贼王")
        assertTrue("Nyasama 腾讯漫画 search failed: $result", result is EngineResult.Success)
        val book = (result as EngineResult.Success).value.single()
        assertEquals("海贼王", book.name)
        assertEquals("https://m.ac.qq.com/comic/88", book.bookUrl)
        assertEquals("https://m.ac.qq.com/cover.jpg", book.coverUrl)
    }
}
