package app.badcat.engine

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Wire-compatible, read-only decoder for the verified Badcat PPRule protobuf
 * subset. Field numbers are taken from the original generated DEX constants;
 * unknown fields are retained as raw wire slices instead of being discarded.
 *
 * This decoder does not execute pageJs or infer nextPageRule pagination semantics.
 */
data class BadcatPpRule(
    val ruleTypeValue: Int = 0,
    val pageRule: String = "",
    val nextPageRule: String = "",
    val pageJs: String = "",
    val chapterIsReverse: Boolean = false,
    val videoPathRule: String = "",
    val videoEpisodeRule: String = "",
    val pageJsForSingleHtml: String = "",
    val unknownFields: List<ByteArray> = emptyList(),
) {
    val ruleType: BadcatPpRuleType
        get() = BadcatPpRuleType.fromWireValue(ruleTypeValue)
}

enum class BadcatPpRuleType(val wireValue: Int) {
    RE(0),
    XPATH(1),
    UNRECOGNIZED(-1);

    companion object {
        fun fromWireValue(value: Int): BadcatPpRuleType =
            entries.firstOrNull { it.wireValue == value } ?: UNRECOGNIZED
    }
}

data class BadcatPpConfigItem(
    val host: String = "",
    val listRule: BadcatPpRule? = null,
    val contentRule: BadcatPpRule? = null,
    val wap2pcRule: String = "",
    val pc2wapRule: String = "",
    val dataTypeValue: Int = 0,
    val bookNameRule: BadcatPpRule? = null,
    val authorRule: BadcatPpRule? = null,
    val isNotUseReferer: Boolean = false,
    val unknownFields: List<ByteArray> = emptyList(),
)

data class BadcatPpResponse(
    val code: Int = 0,
    val message: String = "",
    val version: Int = 0,
    val items: List<BadcatPpConfigItem> = emptyList(),
    val ppRuleVersion: Int = 0,
    val bookDetailScript: String = "",
    val jsLibScript: String = "",
    val scriptCode: String = "",
    val scriptSignature: String = "",
    val unknownFields: List<ByteArray> = emptyList(),
)

/**
 * Bounded protobuf wire reader for the original Badcat PPResponse/PPRule schema.
 * It deliberately preserves opaque script and URL-rule strings; decoding is not
 * permission to execute server-supplied JavaScript.
 */
object BadcatPpRuleProtoDecoder {
    const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
    const val MAX_FIELDS_PER_MESSAGE = 8_192
    const val MAX_ITEMS = 5_000
    const val MAX_STRING_BYTES = 256 * 1024

    fun decodeResponse(bytes: ByteArray): EngineResult<BadcatPpResponse> {
        if (bytes.size > MAX_RESPONSE_BYTES) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPResponse exceeds the configured size limit",
            )
        }
        return try {
            EngineResult.Success(parseResponse(bytes))
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Badcat PPResponse protobuf is malformed",
                error,
            )
        }
    }

    fun decodeRule(bytes: ByteArray): EngineResult<BadcatPpRule> {
        if (bytes.size > MAX_STRING_BYTES * 8) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Badcat PPRule exceeds the configured size limit",
            )
        }
        return try {
            EngineResult.Success(parseRule(bytes))
        } catch (error: Exception) {
            EngineResult.Failure(
                EngineErrorCode.PARSE,
                "Badcat PPRule protobuf is malformed",
                error,
            )
        }
    }

    private fun parseResponse(bytes: ByteArray): BadcatPpResponse {
        var code = 0
        var message = ""
        var version = 0
        var ppRuleVersion = 0
        var bookDetailScript = ""
        var jsLibScript = ""
        var scriptCode = ""
        var scriptSignature = ""
        val items = mutableListOf<BadcatPpConfigItem>()
        val unknown = mutableListOf<ByteArray>()
        for (field in readFields(bytes)) {
            when {
                field.number == 1 && field.wireType == WIRE_VARINT ->
                    code = field.varintValue!!.toInt()
                field.number == 2 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    message = field.stringValue()
                field.number == 3 && field.wireType == WIRE_VARINT ->
                    version = field.varintValue!!.toInt()
                field.number == 4 && field.wireType == WIRE_LENGTH_DELIMITED -> {
                    if (items.size >= MAX_ITEMS) error("Too many Badcat PPConfigItem records")
                    items += parseConfigItem(field.bytesValue!!)
                }
                field.number == 5 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    scriptCode = field.stringValue()
                field.number == 6 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    scriptSignature = field.stringValue()
                field.number == 7 && field.wireType == WIRE_VARINT ->
                    ppRuleVersion = field.varintValue!!.toInt()
                field.number == 8 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    bookDetailScript = field.stringValue()
                field.number == 9 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    jsLibScript = field.stringValue()
                else -> unknown += field.raw
            }
        }
        return BadcatPpResponse(
            code, message, version, items, ppRuleVersion,
            bookDetailScript, jsLibScript, scriptCode, scriptSignature, unknown,
        )
    }

    private fun parseConfigItem(bytes: ByteArray): BadcatPpConfigItem {
        var host = ""
        var listRule: BadcatPpRule? = null
        var contentRule: BadcatPpRule? = null
        var wap2pcRule = ""
        var pc2wapRule = ""
        var dataTypeValue = 0
        var bookNameRule: BadcatPpRule? = null
        var authorRule: BadcatPpRule? = null
        var isNotUseReferer = false
        val unknown = mutableListOf<ByteArray>()
        for (field in readFields(bytes)) {
            when {
                field.number == 1 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    host = field.stringValue()
                field.number == 2 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    listRule = parseRule(field.bytesValue!!)
                field.number == 3 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    contentRule = parseRule(field.bytesValue!!)
                field.number == 4 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    wap2pcRule = field.stringValue()
                field.number == 5 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    pc2wapRule = field.stringValue()
                field.number == 6 && field.wireType == WIRE_VARINT ->
                    dataTypeValue = field.varintValue!!.toInt()
                field.number == 7 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    bookNameRule = parseRule(field.bytesValue!!)
                field.number == 8 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    authorRule = parseRule(field.bytesValue!!)
                field.number == 9 && field.wireType == WIRE_VARINT ->
                    isNotUseReferer = field.varintValue != 0L
                else -> unknown += field.raw
            }
        }
        return BadcatPpConfigItem(
            host, listRule, contentRule, wap2pcRule, pc2wapRule,
            dataTypeValue, bookNameRule, authorRule, isNotUseReferer, unknown,
        )
    }

    private fun parseRule(bytes: ByteArray): BadcatPpRule {
        var ruleTypeValue = 0
        var pageRule = ""
        var nextPageRule = ""
        var pageJs = ""
        var chapterIsReverse = false
        var videoPathRule = ""
        var videoEpisodeRule = ""
        var pageJsForSingleHtml = ""
        val unknown = mutableListOf<ByteArray>()
        for (field in readFields(bytes)) {
            when {
                field.number == 1 && field.wireType == WIRE_VARINT ->
                    ruleTypeValue = field.varintValue!!.toInt()
                field.number == 2 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    pageRule = field.stringValue()
                field.number == 3 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    nextPageRule = field.stringValue()
                field.number == 4 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    pageJs = field.stringValue()
                field.number == 5 && field.wireType == WIRE_VARINT ->
                    chapterIsReverse = field.varintValue != 0L
                field.number == 6 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    videoPathRule = field.stringValue()
                field.number == 7 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    videoEpisodeRule = field.stringValue()
                field.number == 8 && field.wireType == WIRE_LENGTH_DELIMITED ->
                    pageJsForSingleHtml = field.stringValue()
                else -> unknown += field.raw
            }
        }
        return BadcatPpRule(
            ruleTypeValue, pageRule, nextPageRule, pageJs, chapterIsReverse,
            videoPathRule, videoEpisodeRule, pageJsForSingleHtml, unknown,
        )
    }

    private data class WireField(
        val number: Int,
        val wireType: Int,
        val varintValue: Long? = null,
        val bytesValue: ByteArray? = null,
        val raw: ByteArray,
    ) {
        fun stringValue(): String {
            val bytes = bytesValue ?: error("Expected a length-delimited protobuf string")
            require(bytes.size <= MAX_STRING_BYTES) { "Badcat protobuf string exceeds the size limit" }
            return Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }
    }

    private class FieldBudget {
        private var consumed = 0

        fun consume() {
            require(consumed < MAX_FIELDS_PER_MESSAGE) { "Too many protobuf fields, including nested groups" }
            consumed++
        }
    }

    private fun readFields(bytes: ByteArray): List<WireField> {
        val fields = ArrayList<WireField>()
        val budget = FieldBudget()
        var offset = 0
        while (offset < bytes.size) {
            budget.consume()
            val start = offset
            val (tag, afterTag) = readVarint(bytes, offset)
            offset = afterTag
            val numberValue = tag ushr 3
            require(numberValue in 1..MAX_FIELD_NUMBER.toLong()) { "Invalid protobuf field number" }
            val number = numberValue.toInt()
            val wireType = (tag and 7L).toInt()
            val value: WireField = when (wireType) {
                WIRE_VARINT -> {
                    val (v, next) = readVarint(bytes, offset)
                    offset = next
                    WireField(number, wireType, varintValue = v, raw = bytes.copyOfRange(start, offset))
                }
                WIRE_FIXED64 -> {
                    require(bytes.size - offset >= 8) { "Truncated fixed64 protobuf field" }
                    offset += 8
                    WireField(number, wireType, raw = bytes.copyOfRange(start, offset))
                }
                WIRE_LENGTH_DELIMITED -> {
                    val (length, next) = readVarint(bytes, offset)
                    offset = next
                    require(length <= MAX_RESPONSE_BYTES.toLong() && length <= (bytes.size - offset).toLong()) {
                        "Invalid protobuf field length"
                    }
                    val end = offset + length.toInt()
                    val payload = bytes.copyOfRange(offset, end)
                    offset = end
                    WireField(number, wireType, bytesValue = payload, raw = bytes.copyOfRange(start, offset))
                }
                WIRE_FIXED32 -> {
                    require(bytes.size - offset >= 4) { "Truncated fixed32 protobuf field" }
                    offset += 4
                    WireField(number, wireType, raw = bytes.copyOfRange(start, offset))
                }
                WIRE_START_GROUP -> {
                    offset = skipGroup(bytes, offset, number, 1, budget)
                    WireField(number, wireType, raw = bytes.copyOfRange(start, offset))
                }
                else -> error("Unsupported protobuf wire type $wireType")
            }
            fields += value
        }
        return fields
    }

    /**
     * Skips a deprecated protobuf group while retaining its complete raw slice
     * in the caller. Nested groups must close with their matching field number.
     */
    private fun skipGroup(
        bytes: ByteArray,
        start: Int,
        groupNumber: Int,
        depth: Int,
        budget: FieldBudget,
    ): Int {
        require(depth <= MAX_GROUP_DEPTH) { "Protobuf group nesting exceeds the limit" }
        var offset = start
        while (offset < bytes.size) {
            budget.consume()
            val (tag, afterTag) = readVarint(bytes, offset)
            val numberValue = tag ushr 3
            require(numberValue in 1..MAX_FIELD_NUMBER.toLong()) { "Invalid protobuf group field number" }
            val number = numberValue.toInt()
            val wireType = (tag and 7L).toInt()
            offset = afterTag
            when (wireType) {
                WIRE_VARINT -> offset = readVarint(bytes, offset).second
                WIRE_FIXED64 -> {
                    require(bytes.size - offset >= 8) { "Truncated fixed64 protobuf group field" }
                    offset += 8
                }
                WIRE_LENGTH_DELIMITED -> {
                    val (length, afterLength) = readVarint(bytes, offset)
                    offset = afterLength
                    require(length >= 0 && length <= (bytes.size - offset).toLong()) {
                        "Invalid length-delimited protobuf group field"
                    }
                    offset += length.toInt()
                }
                WIRE_START_GROUP -> offset = skipGroup(bytes, offset, number, depth + 1, budget)
                WIRE_END_GROUP -> {
                    require(number == groupNumber) { "Mismatched protobuf end-group field" }
                    return offset
                }
                WIRE_FIXED32 -> {
                    require(bytes.size - offset >= 4) { "Truncated fixed32 protobuf group field" }
                    offset += 4
                }
                else -> error("Unsupported protobuf group wire type $wireType")
            }
        }
        error("Unterminated protobuf group")
    }

    private fun readVarint(bytes: ByteArray, start: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var offset = start
        repeat(10) {
            require(offset < bytes.size) { "Truncated protobuf varint" }
            val value = bytes[offset++].toInt() and 0xff
            if (shift == 63) require(value <= 1) { "Protobuf varint overflow" }
            result = result or ((value and 0x7f).toLong() shl shift)
            if (value and 0x80 == 0) return result to offset
            shift += 7
        }
        error("Malformed protobuf varint")
    }

    private const val WIRE_VARINT = 0
    private const val WIRE_FIXED64 = 1
    private const val WIRE_LENGTH_DELIMITED = 2
    private const val WIRE_START_GROUP = 3
    private const val WIRE_END_GROUP = 4
    private const val WIRE_FIXED32 = 5
    private const val MAX_FIELD_NUMBER = 536_870_911
    private const val MAX_GROUP_DEPTH = 64
}
