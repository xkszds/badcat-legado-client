package app.badcat.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BadcatPpRuleProtoDecoderTest {

    @Test
    fun decodesVerifiedPpResponseConfigAndAllPpRuleFields() {
        val rule = message(
            varintField(1, 1) +
                stringField(2, "//div[@class='chapter']") +
                stringField(3, "//a[@class='next']") +
                stringField(4, "return page + 1") +
                varintField(5, 1) +
                stringField(6, "//video") +
                stringField(7, "//a[@class='episode']") +
                stringField(8, "return JSON.stringify(images)") +
                stringField(99, "future-rule-field"),
        )
        val config = message(
            stringField(1, "reader.example") +
                bytesField(2, rule) +
                bytesField(3, rule) +
                stringField(4, "wap-to-pc") +
                stringField(5, "pc-to-wap") +
                varintField(6, 3) +
                bytesField(7, rule) +
                bytesField(8, rule) +
                varintField(9, 1),
        )
        val response = message(
            varintField(1, 0) +
                stringField(2, "OK") +
                varintField(3, 7) +
                bytesField(4, config) +
                stringField(5, "script-code") +
                stringField(6, "script-signature") +
                varintField(7, 11) +
                stringField(8, "book-detail-script") +
                stringField(9, "js-lib-script"),
        )

        val decoded = assertIs<EngineResult.Success<BadcatPpResponse>>(
            BadcatPpRuleProtoDecoder.decodeResponse(response),
        ).value

        assertEquals(0, decoded.code)
        assertEquals("OK", decoded.message)
        assertEquals(7, decoded.version)
        assertEquals(11, decoded.ppRuleVersion)
        assertEquals("script-code", decoded.scriptCode)
        assertEquals("script-signature", decoded.scriptSignature)
        assertEquals("book-detail-script", decoded.bookDetailScript)
        assertEquals("js-lib-script", decoded.jsLibScript)

        val item = decoded.items.single()
        assertEquals("reader.example", item.host)
        assertEquals("wap-to-pc", item.wap2pcRule)
        assertEquals("pc-to-wap", item.pc2wapRule)
        assertEquals(3, item.dataTypeValue)
        assertTrue(item.isNotUseReferer)

        val listRule = item.listRule!!
        assertEquals(1, listRule.ruleTypeValue)
        assertEquals(BadcatPpRuleType.XPATH, listRule.ruleType)
        assertEquals("//div[@class='chapter']", listRule.pageRule)
        assertEquals("//a[@class='next']", listRule.nextPageRule)
        assertEquals("return page + 1", listRule.pageJs)
        assertTrue(listRule.chapterIsReverse)
        assertEquals("//video", listRule.videoPathRule)
        assertEquals("//a[@class='episode']", listRule.videoEpisodeRule)
        assertEquals("return JSON.stringify(images)", listRule.pageJsForSingleHtml)
        assertEquals(1, listRule.unknownFields.size)
        assertEquals(item.contentRule?.pageRule, item.bookNameRule?.pageRule)
        assertEquals(item.contentRule?.pageRule, item.authorRule?.pageRule)
    }

    @Test
    fun preservesUnknownTopLevelFieldsAndRejectsMalformedWireData() {
        val response = message(
            stringField(2, "OK") +
                bytesField(4, message(stringField(1, "reader.example"))) +
                stringField(120, "future-response-field"),
        )
        val decoded = assertIs<EngineResult.Success<BadcatPpResponse>>(
            BadcatPpRuleProtoDecoder.decodeResponse(response),
        ).value
        assertEquals(1, decoded.unknownFields.size)

        assertEquals(
            EngineErrorCode.PARSE,
            assertIs<EngineResult.Failure>(
                BadcatPpRuleProtoDecoder.decodeRule(byteArrayOf(0x12, 0x05, 0x41)),
            ).code,
        )
    }

    private fun message(bytes: ByteArray): ByteArray = bytes

    private fun stringField(number: Int, value: String): ByteArray =
        bytesField(number, value.toByteArray(Charsets.UTF_8))

    private fun bytesField(number: Int, value: ByteArray): ByteArray =
        varint(((number shl 3) or 2).toLong()) + varint(value.size.toLong()) + value

    private fun varintField(number: Int, value: Int): ByteArray =
        varint((number shl 3).toLong()) + varint(value.toLong())

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val output = ArrayList<Byte>()
        while ((remaining and -128L) != 0L) {
            output += ((remaining and 0x7fL) or 0x80L).toByte()
            remaining = remaining ushr 7
        }
        output += remaining.toByte()
        return output.toByteArray()
    }
}
