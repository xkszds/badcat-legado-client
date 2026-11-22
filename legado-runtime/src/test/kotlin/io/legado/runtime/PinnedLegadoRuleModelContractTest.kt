package io.legado.runtime

import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.utils.GSON
import io.legado.app.utils.INITIAL_GSON
import io.legado.app.utils.fromJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * These tests exercise the actual pinned upstream model classes and Gson
 * adapters compiled into :legado-runtime, not host-side shadow DTOs.
 */
class PinnedLegadoRuleModelContractTest {

    @Test
    fun allSixRuleModelsAcceptObjectAndLegacyStringForms() {
        val fixtures = listOf(
            Triple(ExploreRule::class.java, """{"bookList":"div.book","name":"a.name"}""", """{"bookList":"div.book","name":"a.name"}"""),
            Triple(SearchRule::class.java, """{"checkKeyWord":"true","bookList":"div.book"}""", """{"checkKeyWord":"true","bookList":"div.book"}"""),
            Triple(BookInfoRule::class.java, """{"name":"h1","tocUrl":"a@href"}""", """{"name":"h1","tocUrl":"a@href"}"""),
            Triple(TocRule::class.java, """{"chapterList":"li","chapterName":"a@text"}""", """{"chapterList":"li","chapterName":"a@text"}"""),
            Triple(ContentRule::class.java, """{"content":"#content","nextContentUrl":"a@href"}""", """{"content":"#content","nextContentUrl":"a@href"}"""),
            Triple(ReviewRule::class.java, """{"enabled":true,"reviewDetailUrl":"/review"}""", """{"enabled":true,"reviewDetailUrl":"/review"}"""),
        )
        fixtures.forEach { (type, objectJson, stringJson) ->
            val objectValue = GSON.fromJson(objectJson, type)
            val stringValue = GSON.fromJson(GSON.toJson(objectJson), type)
            assertNotNull("object-form parse for ${type.simpleName}", objectValue)
            assertNotNull("string-form parse for ${type.simpleName}", stringValue)
        }
    }

    @Test
    fun ruleModelsKeepPinnedDefaultsAndUnknownFieldsAreIgnoredLikeUpstream() {
        val content = INITIAL_GSON.fromJson(
            """{"content":"#content","futureField":{"v":1}}""",
            ContentRule::class.java,
        )
        assertEquals("#content", content.content)
        assertEquals(null, content.nextContentUrl)
        assertEquals(null, content.maxBatchSize)

        val review = INITIAL_GSON.fromJson("""{"futureField":true}""", ReviewRule::class.java)
        assertEquals(false, review.enabled)
        assertEquals(null, review.reviewDetailUrl)
    }

    @Test
    fun gsonObjectParserUsesPinnedDeserializerForAllRuleTypes() {
        assertNotNull(GSON.fromJsonObject<ExploreRule>("""{"bookList":"li"}""").getOrNull())
        assertNotNull(GSON.fromJsonObject<SearchRule>("""{"bookList":"li"}""").getOrNull())
        assertNotNull(GSON.fromJsonObject<BookInfoRule>("""{"name":"h1"}""").getOrNull())
        assertNotNull(GSON.fromJsonObject<TocRule>("""{"chapterList":"li"}""").getOrNull())
        assertNotNull(GSON.fromJsonObject<ContentRule>("""{"content":"#content"}""").getOrNull())
        assertNotNull(GSON.fromJsonObject<ReviewRule>("""{"enabled":true}""").getOrNull())
    }
}
