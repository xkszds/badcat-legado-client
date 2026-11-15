package app.badcat.engine

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LegadoBookSourceCodecTest {

    @Test
    fun decodesSingleSourceAndKeepsUnknownRuleFields() {
        val input = """
            {
              "bookSourceUrl": "https://reader.example/",
              "bookSourceName": "Example source",
              "bookSourceGroup": "test",
              "bookSourceType": 0,
              "enabled": true,
              "enabledExplore": false,
              "enabledCookieJar": true,
              "header": "{\"User-Agent\":\"fixture\"}",
              "jsLib": "https://reader.example/lib.js",
              "loginUrl": "https://reader.example/login",
              "exploreUrl": "https://reader.example/explore",
              "searchUrl": "https://reader.example/search?q={{key}}",
              "ruleExplore": {"bookList": "div.explore"},
              "ruleSearch": {"bookList": "div.book", "name": "h3@text"},
              "ruleBookInfo": {"name": "h1@text"},
              "ruleToc": {"chapterList": "a.chapter"},
              "ruleContent": {"content": "#content"},
              "ruleReview": {"enabled": true, "reviewUrl": "/reviews"},
              "mainJs": "function search() { return []; }",
              "futureField": {"keep": true}
            }
        """.trimIndent()

        val source = LegadoBookSourceCodec.decode(input).single()
        assertEquals("https://reader.example/", source.sourceId)
        assertEquals("Example source", source.name)
        assertEquals("test", source.group)
        assertTrue(source.enabled)
        val engineSource = source.toEngineSource()
        assertEquals(source.sourceId, engineSource.id)
        assertEquals(source.name, engineSource.name)
        assertEquals(source.group, engineSource.group)
        assertEquals(source.rawJson, engineSource.rawJson)
        assertFalse(engineSource.enabledExplore)
        assertEquals(true, engineSource.enabledCookieJar)
        assertEquals("https://reader.example/search?q={{key}}", engineSource.searchUrl)
        assertEquals("https://reader.example/explore", engineSource.exploreUrl)
        assertEquals("https://reader.example/login", engineSource.loginUrl)
        assertEquals("https://reader.example/lib.js", engineSource.jsLib)
        assertEquals("function search() { return []; }", engineSource.mainJs)
        assertTrue(engineSource.ruleSearchJson!!.contains("div.book"))
        assertTrue(engineSource.ruleContentJson!!.contains("#content"))
        assertTrue(engineSource.ruleReviewJson!!.contains("/reviews"))

        val exported = JsonParser.parseString(LegadoBookSourceCodec.encode(listOf(source)))
            .asJsonArray.single().asJsonObject
        assertEquals("https://reader.example/search?q={{key}}", exported.get("searchUrl").asString)
        assertEquals("function search() { return []; }", exported.get("mainJs").asString)
        assertTrue(exported.getAsJsonObject("futureField").get("keep").asBoolean)
    }

    @Test
    fun decodesArrayAndUsesUpstreamDefaults() {
        val input = """
            [
              {"bookSourceUrl":"https://a.example","bookSourceName":"A"},
              {"bookSourceUrl":"https://b.example","bookSourceName":"B","enabled":false,"bookSourceType":4}
            ]
        """.trimIndent()

        val sources = LegadoBookSourceCodec.decode(input)
        assertEquals(2, sources.size)
        assertTrue(sources[0].enabled)
        assertEquals(0, sources[0].sourceType)
        assertEquals(true, sources[0].toEngineSource().enabledCookieJar)
        assertFalse(sources[1].enabled)
        assertEquals(4, sources[1].sourceType)
    }

    @Test
    fun changingEnabledFlagPreservesUnknownFieldsAndRules() {
        val original = LegadoBookSourceCodec.decode(
            """{"bookSourceUrl":"https://toggle.example","bookSourceName":"Toggle","enabled":true,"future":{"x":7},"ruleSearch":{"bookList":"main"}}"""
        ).single()

        val disabled = LegadoBookSourceCodec.withEnabled(original, false)
        assertFalse(disabled.enabled)
        val exported = JsonParser.parseString(
            LegadoBookSourceCodec.encode(listOf(disabled))
        ).asJsonArray.single().asJsonObject
        assertFalse(exported.get("enabled").asBoolean)
        assertEquals(7, exported.getAsJsonObject("future").get("x").asInt)
        assertEquals("main", exported.getAsJsonObject("ruleSearch").get("bookList").asString)
    }

    @Test
    fun rejectsMissingIdentityFields() {
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode("""{"bookSourceName":"missing url"}""")
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode("""{"bookSourceUrl":"https://a.example"}""")
        }
    }

    @Test
    fun rejectsMalformedKnownFieldTypesInsteadOfApplyingDefaults() {
        val malformedSources = listOf(
            """{"bookSourceUrl":42,"bookSourceName":"Numeric URL"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad enabled","enabled":"yes"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad type","bookSourceType":"web"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Fractional type","bookSourceType":1.5}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Overflow type","bookSourceType":2147483648}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad header","header":{"User-Agent":"x"}}"""
        )
        malformedSources.forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(json)
            }
        }
    }

    @Test
    fun rejectsMalformedRuleObjectsInsteadOfSilentlyDroppingThem() {
        val malformedSources = listOf(
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad search","ruleSearch":"div.book"}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad content","ruleContent":[]}""",
            """{"bookSourceUrl":"https://bad.example","bookSourceName":"Bad review","ruleReview":42}"""
        )
        malformedSources.forEach { json ->
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(json)
            }
        }
    }

    @Test
    fun decodesLegacyStringEncodedRuleObjects() {
        val rule = JsonParser.parseString(
            """{"bookList":"div.book","name":"h2@text"}"""
        ).asJsonObject
        val sourceJson = JsonParser.parseString(
            """{"bookSourceUrl":"https://legacy.example","bookSourceName":"Legacy"}"""
        ).asJsonObject.apply {
            addProperty("ruleSearch", rule.toString())
        }
        val source = LegadoBookSourceCodec.decode(sourceJson.toString()).single()
        assertTrue(source.ruleSearchJson!!.contains("div.book"))
        assertTrue(source.ruleSearchJson!!.contains("h2@text"))
        val exported = JsonParser.parseString(
            LegadoBookSourceCodec.encode(listOf(source))
        ).asJsonArray.single().asJsonObject
        assertTrue(exported.get("ruleSearch").asString.contains("div.book"))
    }
    @Test
    fun rejectsInvalidContentBatchSizes() {
        val malformedRules = listOf(
            """{"maxBatchSize":1.5}""",
            """{"maxBatchSize":2147483648}""",
            """{"maxBatchSize":"10"}""",
            """{"maxBatchSize":true}"""
        )
        malformedRules.forEach { rule ->
            val source = JsonParser.parseString(
                """{"bookSourceUrl":"https://batch.example","bookSourceName":"Batch","ruleContent":$rule}"""
            )
            assertFailsWith<IllegalArgumentException> {
                LegadoBookSourceCodec.decode(source.toString())
            }
        }
        val legacyStringRule = """{"maxBatchSize":1.5}"""
        val legacySource = JsonParser.parseString(
            """{"bookSourceUrl":"https://batch-legacy.example","bookSourceName":"Batch legacy"}"""
        ).asJsonObject.apply {
            addProperty("ruleContent", legacyStringRule)
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(legacySource.toString())
        }
    }

    @Test
    fun rejectsDuplicateSourceUrlsInsteadOfSilentlyOverwriting() {
        val duplicate = """
            [
              {"bookSourceUrl":"https://same.example","bookSourceName":"First"},
              {"bookSourceUrl":"https://same.example","bookSourceName":"Second"}
            ]
        """.trimIndent()
        assertFailsWith<IllegalArgumentException> {
            LegadoBookSourceCodec.decode(duplicate)
        }
    }
}
