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
              "searchUrl": "https://reader.example/search?q={{key}}",
              "ruleSearch": {"bookList": "div.book", "name": "h3@text"},
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
