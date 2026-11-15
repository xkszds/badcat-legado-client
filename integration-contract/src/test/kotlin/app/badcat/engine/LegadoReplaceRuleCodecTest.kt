package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegadoReplaceRuleCodecTest {

    @Test
    fun decodesLegadoFieldsAndPreservesUnknownJsonOnRoundTrip() {
        val json = """[
            {
              "id": 7,
              "name": "cleanup",
              "group": "default",
              "pattern": "foo",
              "replacement": "bar",
              "scope": "Book%",
              "excludeScope": "Book X",
              "scopeTitle": true,
              "scopeSource": false,
              "scopeContent": true,
              "isEnabled": true,
              "isRegex": false,
              "timeoutMillisecond": 1250,
              "order": 4,
              "futureLegadoField": {"keep": true}
            }
        ]"""

        val decoded = LegadoReplaceRuleCodec.decode(json).single()
        assertEquals(7L, decoded.scopeRule.id)
        assertEquals("cleanup", decoded.name)
        assertEquals("default", decoded.group)
        assertEquals("Book%", decoded.scopeRule.scope)
        assertEquals("Book X", decoded.scopeRule.excludeScope)
        assertTrue(decoded.scopeRule.scopeTitle)
        assertFalse(decoded.scopeRule.scopeSource)
        assertTrue(decoded.scopeRule.scopeContent)
        assertTrue(decoded.scopeRule.enabled)
        assertFalse(decoded.isRegex)
        assertEquals(1250L, decoded.validTimeoutMillisecond())

        val exported = LegadoReplaceRuleCodec.encode(listOf(decoded))
        assertTrue(exported.contains("futureLegadoField"))
        assertTrue(exported.contains("\"keep\":true"))
    }

    @Test
    fun appliesLegadoDefaultsAndTimeoutFallback() {
        val decoded = LegadoReplaceRuleCodec.decode(
            """{"id":1,"pattern":"x","replacement":"y","timeoutMillisecond":0}"""
        ).single()

        assertTrue(decoded.scopeRule.enabled)
        assertTrue(decoded.scopeRule.scopeContent)
        assertFalse(decoded.scopeRule.scopeTitle)
        assertFalse(decoded.scopeRule.scopeSource)
        assertTrue(decoded.isRegex)
        assertEquals(3000L, decoded.validTimeoutMillisecond())
    }

    @Test
    fun updatesEnabledFlagWithoutDroppingUnknownFields() {
        val decoded = LegadoReplaceRuleCodec.decode(
            """{"id":2,"pattern":"x","replacement":"y","extension":"retained"}"""
        ).single()

        val updated = LegadoReplaceRuleCodec.withEnabled(decoded, false)
        assertFalse(updated.scopeRule.enabled)
        assertTrue(LegadoReplaceRuleCodec.encode(listOf(updated)).contains("retained"))
    }

    @Test
    fun rejectsDuplicateIdsAndInvalidKnownFieldTypes() {
        assertFailsWith<IllegalArgumentException> {
            LegadoReplaceRuleCodec.decode("""[{"id":1},{"id":1}]""")
        }
        assertFailsWith<IllegalArgumentException> {
            LegadoReplaceRuleCodec.decode("""{"id":1,"isEnabled":"yes"}""")
        }
    }
}
