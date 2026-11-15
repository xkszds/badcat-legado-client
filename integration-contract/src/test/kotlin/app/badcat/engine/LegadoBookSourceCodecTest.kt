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
              "header": "{\\"User-Agent\\":\\"fixture\\"}",
              "jsLib": "https://reader.example/lib.js",
              "loginUrl": "https://reader.example/login",
              "exploreUrl": "https://reader.example/explore",
              "searchUrl": "https://reader.example/search?q={{key}}",
              "ruleExplore": {"bookList": "div.explore"},
              "ruleSearch": {"bookList": "div.book", "name": "h3@text"},
              "ruleBookInfo": {"name": "h1@text"},
              "ruleToc": {"chapterList": "a.chapter"},
              "ruleContent": {"content": "#content"},
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
        assertFalse(sources[1].enabled)
        assertEquals(4, sources[1].sourceType)
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
}
