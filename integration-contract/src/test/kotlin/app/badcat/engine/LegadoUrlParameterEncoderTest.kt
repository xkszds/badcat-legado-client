package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class LegadoUrlParameterEncoderTest {

    @Test
    fun queryPreservesLegadoSafeCharactersAndValidPercentEscapes() {
        val input = "a-z_~!$&'()*+,-./:;=?@[\\\\]^\u0060{|}%2f"
        assertEquals(input, LegadoUrlParameterEncoder.encodeQuery(input))
    }

    @Test
    fun queryPercentEncodesUtf8AndUnsafeCharactersButLeavesPercentLiteral() {
        assertEquals("q=%E4%B8%AD%20%E6%96%87%25x", LegadoUrlParameterEncoder.encodeQuery("q=中文 x%x"))
    }

    @Test
    fun queryUsesRequestedCharsetWhenEncodingIsNeeded() {
        assertEquals("q=%D6%D0", LegadoUrlParameterEncoder.encodeQuery("q=中", "GBK"))
    }

    @Test
    fun formEncodesEachKeyAndValueAndOnlySplitsAtFirstEquals() {
        assertEquals("q=a+b%26c%3Dd&empty=&flag&tail=", LegadoUrlParameterEncoder.encodeForm("q=a b&c=d&empty=&flag&tail="))
    }

    @Test
    fun formPreservesOnlyValidExistingEscapesWhenCharsetIsImplicit() {
        assertEquals("encoded=%2f&bad=%25XZ&space=a+b", LegadoUrlParameterEncoder.encodeForm("encoded=%2f&bad=%XZ&space=a b"))
    }

    @Test
    fun explicitFormCharsetReencodesExistingPercentEscapes() {
        assertEquals("q=%252F", LegadoUrlParameterEncoder.encodeForm("q=%2F", "UTF-8"))
    }

    @Test
    fun escapeModeUsesLegacyCodeUnitEncodingForQueryAndForm() {
        assertEquals("q=%u4e2d%20a", LegadoUrlParameterEncoder.encodeQuery("q=中 a", "escape"))
        assertEquals("q=%u4e2d%20a", LegadoUrlParameterEncoder.encodeForm("q=中 a", "escape"))
    }

    @Test
    fun formPreservesEmptySegmentsAndRepeatedSeparators() {
        assertEquals("a&&b&", LegadoUrlParameterEncoder.encodeForm("a&&b&"))
    }

    @Test
    fun invalidCharsetIsRejected() {
        assertFails { LegadoUrlParameterEncoder.encodeForm("q=value", "not-a-real-charset") }
    }
}
