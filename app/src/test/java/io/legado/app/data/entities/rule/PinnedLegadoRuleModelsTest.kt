package io.legado.app.data.entities.rule

import com.google.gson.JsonParser
import io.legado.app.utils.GSON
import io.legado.app.utils.INITIAL_GSON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compatibility tests for rule entities copied from Legado
 * 330e606c4d57e08adc056ff8ab7a4132c784da9.
 *
 * These tests validate the rule-model/JSON layer only. They do not claim that
 * BookSource, AnalyzeRule, AnalyzeUrl, or the complete reading lifecycle is integrated.
 */
class PinnedLegadoRuleModelsTest {

    @Test
    fun searchAndExploreRulesAcceptObjectAndLegacyStringForms() {
        val searchObject = """{"checkKeyWord":"keyword","bookList":"div.book","name":"h2@text"}"""
        val search = GSON.fromJson(searchObject, SearchRule::class.java)
        assertEquals("keyword", search.checkKeyWord)
        assertEquals("div.book", search.bookList)
        assertEquals("h2@text", search.name)
        assertEquals(search, GSON.fromJson(GSON.toJson(searchObject), SearchRule::class.java))

        val exploreObject = """{"bookList":"section.item","author":"span.author@text"}"""
        val explore = GSON.fromJson(exploreObject, ExploreRule::class.java)
        assertEquals("section.item", explore.bookList)
        assertEquals("span.author@text", explore.author)
        assertEquals(explore, GSON.fromJson(GSON.toJson(exploreObject), ExploreRule::class.java))
    }

    @Test
    fun detailTocAndContentRulesPreservePinnedFields() {
        val detailJson = """{"init":"init()","name":"h1@text","tocUrl":"a.toc@href","downloadUrls":"a.down@href"}"""
        val detail = GSON.fromJson(detailJson, BookInfoRule::class.java)
        assertEquals("init()", detail.init)
        assertEquals("h1@text", detail.name)
        assertEquals("a.toc@href", detail.tocUrl)
        assertEquals("a.down@href", detail.downloadUrls)
        assertEquals(detail, GSON.fromJson(GSON.toJson(detailJson), BookInfoRule::class.java))

        val tocJson = """{"preUpdateJs":"before()","chapterList":"a.chapter","chapterName":"a@text","chapterUrl":"a@href","isVip":"@js:true","nextTocUrl":"a.next@href"}"""
        val toc = GSON.fromJson(tocJson, TocRule::class.java)
        assertEquals("before()", toc.preUpdateJs)
        assertEquals("a.chapter", toc.chapterList)
        assertEquals("a@text", toc.chapterName)
        assertEquals("a@href", toc.chapterUrl)
        assertEquals("@js:true", toc.isVip)
        assertEquals("a.next@href", toc.nextTocUrl)
        assertEquals(toc, GSON.fromJson(GSON.toJson(tocJson), TocRule::class.java))

        val contentJson = """{"content":"#content","subContent":"#extra","title":"h1@text","nextContentUrl":"a.next@href","webJs":"render()","sourceRegex":"<main>","replaceRegex":"old##new","imageStyle":"FULL","imageDecode":"decode()","payAction":"buy()","callBackJs":"done()","contentBatch":"batch()","maxBatchSize":50}"""
        val content = GSON.fromJson(contentJson, ContentRule::class.java)
        assertEquals("#content", content.content)
        assertEquals("#extra", content.subContent)
        assertEquals("h1@text", content.title)
        assertEquals("a.next@href", content.nextContentUrl)
        assertEquals("render()", content.webJs)
        assertEquals("<main>", content.sourceRegex)
        assertEquals("old##new", content.replaceRegex)
        assertEquals("FULL", content.imageStyle)
        assertEquals("decode()", content.imageDecode)
        assertEquals("buy()", content.payAction)
        assertEquals("done()", content.callBackJs)
        assertEquals("batch()", content.contentBatch)
        assertEquals(50, content.maxBatchSize)
        assertTrue(content.toString().contains("contentBatch=batch()"))
        assertEquals(content, GSON.fromJson(GSON.toJson(contentJson), ContentRule::class.java))
    }

    @Test
    fun reviewRuleAndNullBehaviorMatchPinnedDeserializer() {
        val reviewJson = """{"reviewUrl":"/reviews","avatarRule":"img@src","contentRule":"p@text","enabled":true,"reviewSummaryUrl":"/summary","summaryListRule":"li","summaryParagraphIndexRule":"i","summaryParagraphDataRule":"p","summaryCountRule":"b@text","reviewDetailUrl":"/detail","reviewDetailNextPageUrl":"/detail?page={{page}}","detailListRule":"article","detailIdRule":"@id","replyListRule":".reply"}"""
        val review = GSON.fromJson(reviewJson, ReviewRule::class.java)
        assertTrue(review.enabled)
        assertEquals("/reviews", review.reviewUrl)
        assertEquals("img@src", review.avatarRule)
        assertEquals("p@text", review.contentRule)
        assertEquals("/summary", review.configuredSummaryUrl())
        assertEquals(review, GSON.fromJson(GSON.toJson(reviewJson), ReviewRule::class.java))

        val disabled = GSON.fromJson(
            """{"reviewSummaryUrl":"/summary","summaryListRule":"li","summaryParagraphIndexRule":"i","summaryCountRule":"b"}""",
            ReviewRule::class.java,
        )
        assertNull(disabled.configuredSummaryUrl())
        assertFalse(disabled.enabled)
        assertNull(GSON.fromJson("null", ReviewRule::class.java))
    }

    @Test
    fun upstreamGsonDefaultsAndStringCoercionAreRetained() {
        val rule = GSON.fromJson("{}", ContentRule::class.java)
        assertNull(rule.content)
        assertNull(rule.maxBatchSize)

        assertEquals("42", INITIAL_GSON.fromJson("42", String::class.java))
        assertEquals(42, INITIAL_GSON.fromJson("42", Int::class.java))
    }

    @Test
    fun exploreKindAndRowUiKeepUpstreamDefaultsAndEqualityContract() {
        val kind = INITIAL_GSON.fromJson(
            """{"title":"分类","type":"select","chars":["A","B"],"default":"A"}""",
            ExploreKind::class.java,
        )
        assertEquals("分类", kind.title)
        assertEquals("select", kind.type)
        assertEquals("A", kind.default)
        assertEquals(FlexChildStyle.defaultStyle, kind.style())

        val row = INITIAL_GSON.fromJson(
            """{"name":"token","type":"password","key":"token","hint":"输入令牌","countdown":30}""",
            RowUi::class.java,
        )
        assertEquals("token", row.name)
        assertEquals("password", row.type)
        assertEquals("token", row.key)
        assertEquals(30, row.countdown)

        val first = ExploreKind(title = "A", type = "url", url = "/a")
        val second = ExploreKind(title = "A", type = "url", url = "/a", chars = arrayOf("x"))
        assertEquals(first, second)
    }

    @Test
    fun legacyRuleStringIsAJsonPrimitiveAndDeserializesThroughUpstreamAdapter() {
        val encoded = GSON.toJson("""{"bookList":"div.book"}""")
        assertTrue(JsonParser.parseString(encoded).isJsonPrimitive)
        assertEquals("div.book", GSON.fromJson(encoded, SearchRule::class.java).bookList)
    }
}
