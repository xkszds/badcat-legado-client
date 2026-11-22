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
        assertEquals(Int.MIN_VALUE, decoded.scopeRule.order)
    }

    @Test
    fun missingOrderSortsBeforeExplicitZeroLikePinnedLegadoDefault() {
        val missingOrder = LegadoReplaceRuleCodec.decode(
            """{"id":41,"pattern":"x","replacement":"y"}"""
        ).single()
        val explicitOrder = LegadoReplaceRuleCodec.decode(
            """{"id":42,"pattern":"x","replacement":"y","order":0}"""
        ).single()

        assertEquals(Int.MIN_VALUE, missingOrder.scopeRule.order)
        assertEquals(
            listOf(41L, 42L),
            LegadoReplaceRuleSelector.forContent(
                listOf(explicitOrder.scopeRule, missingOrder.scopeRule),
                bookName = "",
                bookOrigin = "",
            ).map { it.id },
        )
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
    fun validatesRegexSyntaxAndTrailingAlternationLikeLegado() {
        val invalidTrailingPipe = LegadoReplaceRuleCodec.decode(
            """{"id":10,"pattern":"abc|","replacement":"","isRegex":true}"""
        ).single()
        assertFalse(invalidTrailingPipe.isValid())

        val escapedPipe = LegadoReplaceRuleCodec.decode(
            """{"id":11,"pattern":"abc\\\\|","replacement":"","isRegex":true}"""
        ).single()
        assertTrue(escapedPipe.isValid())

        val invalidRegex = LegadoReplaceRuleCodec.decode(
            """{"id":12,"pattern":"(","replacement":"","isRegex":true}"""
        ).single()
        assertFalse(invalidRegex.isValid())

        val literal = LegadoReplaceRuleCodec.decode(
            """{"id":13,"pattern":"[","replacement":"","isRegex":false}"""
        ).single()
        assertTrue(literal.isValid())
    }

    @Test
    fun rejectsOversizedCatalogBeforeEncoding() {
        val largeRecord = LegadoReplaceRuleCodec.decode(
            """{"id":21,"pattern":"x","replacement":"y"}"""
        ).single().copy(
            rawJson = """{"id":21,"pattern":"x","replacement":"${"z".repeat(LegadoReplaceRuleCodec.MAX_IMPORT_BYTES)}"}"""
        )
        assertFailsWith<IllegalArgumentException> {
            LegadoReplaceRuleCodec.encode(listOf(largeRecord))
        }
    }

    @Test
    fun decodesValidUtf8BytesAndRejectsMalformedSequences() {
        val valid = """{"id":31,"pattern":"猫","replacement":"书"}"""
            .toByteArray(Charsets.UTF_8)
        assertEquals("猫", LegadoReplaceRuleCodec.decodeUtf8(valid).single().pattern)

        val malformed = byteArrayOf(
            '{'.code.toByte(), '"'.code.toByte(), 'i'.code.toByte(), 'd'.code.toByte(),
            '"'.code.toByte(), ':'.code.toByte(), '3'.code.toByte(), '2'.code.toByte(),
            ','.code.toByte(), '"'.code.toByte(), 'p'.code.toByte(), 'a'.code.toByte(),
            't'.code.toByte(), 't'.code.toByte(), 'e'.code.toByte(), 'r'.code.toByte(),
            'n'.code.toByte(), '"'.code.toByte(), ':'.code.toByte(), '"'.code.toByte(),
            0xC3.toByte(), 0x28.toByte(), '"'.code.toByte(), '}'.code.toByte(),
        )
        assertFailsWith<java.nio.charset.CharacterCodingException> {
            LegadoReplaceRuleCodec.decodeUtf8(malformed)
        }
    }

    @Test
    fun rejectsOversizedJsonBeforeParsing() {
        val oversized = " ".repeat(LegadoReplaceRuleCodec.MAX_IMPORT_BYTES + 1)
        assertFailsWith<IllegalArgumentException> {
            LegadoReplaceRuleCodec.decode(oversized)
        }
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
