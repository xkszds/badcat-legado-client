package io.legado.app.utils

import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import com.google.gson.reflect.TypeToken

/**
 * Contract tests for Gson behavior copied from LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
 *
 * These tests intentionally exercise the actual host Gson instances and rule classes rather
 * than a test-only adapter. Book.ReadConfig is not present in this host source tree and is
 * covered as an explicit follow-up gate, not represented by a locally invented shadow model.
 */
class LegadoGsonCompatibilityTest {

    @Test
    fun allSixRuleConvertersAcceptObjectAndStringEncodedObject() {
        fun encodedObject(json: String): String = GSON.toJson(json)

        assertEquals("li.book", GSON.fromJson("""{"bookList":"li.book"}""", ExploreRule::class.java).bookList)
        assertEquals("li.book", GSON.fromJson(encodedObject("""{"bookList":"li.book"}"""), ExploreRule::class.java).bookList)

        assertEquals("h3.title", GSON.fromJson("""{"name":"h3.title"}""", SearchRule::class.java).name)
        assertEquals("h3.title", GSON.fromJson(encodedObject("""{"name":"h3.title"}"""), SearchRule::class.java).name)

        assertEquals("h1", GSON.fromJson("""{"name":"h1"}""", BookInfoRule::class.java).name)
        assertEquals("h1", GSON.fromJson(encodedObject("""{"name":"h1"}"""), BookInfoRule::class.java).name)

        assertEquals("dd a", GSON.fromJson("""{"chapterList":"dd a"}""", TocRule::class.java).chapterList)
        assertEquals("dd a", GSON.fromJson(encodedObject("""{"chapterList":"dd a"}"""), TocRule::class.java).chapterList)

        assertEquals("article", GSON.fromJson("""{"content":"article"}""", ContentRule::class.java).content)
        assertEquals("article", GSON.fromJson(encodedObject("""{"content":"article"}"""), ContentRule::class.java).content)

        assertEquals(true, GSON.fromJson("""{"enabled":true}""", ReviewRule::class.java).enabled)
        assertEquals(true, GSON.fromJson(encodedObject("""{"enabled":true}"""), ReviewRule::class.java).enabled)
    }

    @Test
    fun ruleDeserializersReturnNullForArrayAndJsonNull() {
        assertNull(GSON.fromJson("[]", ExploreRule::class.java))
        assertNull(GSON.fromJson("null", ExploreRule::class.java))
        assertNull(GSON.fromJson("[]", SearchRule::class.java))
        assertNull(GSON.fromJson("null", BookInfoRule::class.java))
        assertNull(GSON.fromJson("[]", TocRule::class.java))
        assertNull(GSON.fromJson("null", ContentRule::class.java))
        assertNull(GSON.fromJson("[]", ReviewRule::class.java))
    }

    @Test
    fun ruleModelsIgnoreUnknownFieldsButPreserveKnownFields() {
        val decoded = GSON.fromJson(
            """{"bookList":"ul li","futureRuleExtension":{"enabled":true},"name":"a"}""",
            ExploreRule::class.java
        )
        assertEquals("ul li", decoded.bookList)
        assertEquals("a", decoded.name)
        assertTrue(GSON.toJson(decoded).contains("bookList"))
        assertTrue(!GSON.toJson(decoded).contains("futureRuleExtension"))
    }

    @Test
    fun initialGsonUsesPinnedIntegerAndStringCoercion() {
        val mapType = object : TypeToken<Map<String?, Any?>?>() {}.type
        val map = requireNotNull(
            INITIAL_GSON.fromJson<Map<String?, Any?>>(
                """{"whole":3.0,"fraction":3.25,"nested":[2.0,true]}""",
                mapType
            )
        )
        assertEquals(3L, map["whole"])
        assertEquals(3.25, map["fraction"])
        assertEquals(2L, (map["nested"] as List<*>)[0])

        assertEquals("42", INITIAL_GSON.fromJson("42", String::class.java))
        assertNull(INITIAL_GSON.fromJson("{}", Int::class.java))
        assertEquals(7, INITIAL_GSON.fromJson("7.9", Int::class.java))
    }

    @Test
    fun localDateAdapterAcceptsIsoAndLegacyObjectAndWritesIso() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            INITIAL_GSON.fromJson(""""2024-02-29"""", LocalDate::class.java)
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            INITIAL_GSON.fromJson("""{"year":2024,"month":2,"day":29}""", LocalDate::class.java)
        )
        assertNull(INITIAL_GSON.fromJson("""{"year":2023,"month":2,"day":29}""", LocalDate::class.java))
        assertNull(INITIAL_GSON.fromJson("null", LocalDate::class.java))
        assertEquals(""""2024-02-29"""", INITIAL_GSON.toJson(LocalDate.of(2024, 2, 29)))
    }

    @Test
    fun gsonSerializationDisablesHtmlEscapingAndRuleRoundTrips() {
        assertEquals(""""<tag>"""", INITIAL_GSON.toJson("<tag>"))
        val source = ContentRule(content = "div#content", maxBatchSize = 12)
        val encoded = GSON.toJson(source)
        val decoded = GSON.fromJson(encoded, ContentRule::class.java)
        assertEquals(source.content, decoded.content)
        assertEquals(source.maxBatchSize, decoded.maxBatchSize)
    }
}
