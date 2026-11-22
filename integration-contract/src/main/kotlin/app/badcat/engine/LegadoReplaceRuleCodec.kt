package app.badcat.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * Legado ReplaceRule JSON adapter. Unknown JSON fields are retained verbatim
 * so importing and exporting rules does not discard upstream extensions.
 */
data class LegadoReplaceRuleRecord(
    val scopeRule: LegadoReplaceRuleScope,
    val name: String,
    val group: String?,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean,
    val timeoutMillisecond: Long,
    val rawJson: String,
) {
    fun executableLiteral(): LegadoReplaceRule = LegadoReplaceRule(
        scopeRule = scopeRule,
        pattern = pattern,
        replacement = replacement,
        isRegex = isRegex,
        timeoutMillisecond = validTimeoutMillisecond(),
        name = name,
    )

    /** Legado falls back to 3000 ms for non-positive timeout values. */
    fun validTimeoutMillisecond(): Long =
        timeoutMillisecond.takeIf { it > 0 } ?: LegadoReplaceRuleCodec.DEFAULT_TIMEOUT_MILLISECOND

    /** Mirrors Legado's rule validation without executing the expression. */
    fun isValid(): Boolean {
        if (pattern.isEmpty()) return false
        if (!isRegex) return true
        if (runCatching { java.util.regex.Pattern.compile(pattern) }.isFailure) return false
        return !(pattern.endsWith("|") && !pattern.endsWith("\\|"))
    }
}

object LegadoReplaceRuleCodec {
    const val DEFAULT_TIMEOUT_MILLISECOND = 3000L
    const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

    /** Decode imported UTF-8 strictly; malformed byte sequences must not be silently replaced. */
    fun decodeUtf8(bytes: ByteArray): List<LegadoReplaceRuleRecord> {
        require(bytes.size <= MAX_IMPORT_BYTES) {
            "Replace-rule JSON exceeds the 5 MiB import limit"
        }
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val json = decoder.decode(ByteBuffer.wrap(bytes)).toString()
        return decode(json)
    }

    fun decode(json: String): List<LegadoReplaceRuleRecord> {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES) {
            "Replace-rule JSON exceeds the 5 MiB import limit"
        }
        val root = JsonParser.parseString(json)
        val elements = when {
            root.isJsonArray -> root.asJsonArray.toList()
            root.isJsonObject -> listOf(root)
            else -> throw IllegalArgumentException("Replace-rule JSON must be an object or array")
        }
        val decoded = elements.mapIndexed { index, element ->
            require(element.isJsonObject) { "Replace rule at index $index is not an object" }
            decodeObject(element.asJsonObject, index)
        }
        val duplicateIds = decoded.groupingBy { it.scopeRule.id }.eachCount()
            .filterValues { it > 1 }.keys
        require(duplicateIds.isEmpty()) {
            "Duplicate replace-rule IDs: ${duplicateIds.joinToString()}"
        }
        return decoded
    }

    fun encode(rules: List<LegadoReplaceRuleRecord>): String {
        val array = JsonArray()
        rules.forEach { rule ->
            val element = JsonParser.parseString(rule.rawJson)
            require(element.isJsonObject) { "Stored replace rule ${rule.scopeRule.id} is not an object" }
            array.add(element)
        }
        val encoded = array.toString()
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_IMPORT_BYTES) {
            "Replace-rule catalog exceeds the 5 MiB storage limit"
        }
        return encoded
    }

    fun withEnabled(rule: LegadoReplaceRuleRecord, enabled: Boolean): LegadoReplaceRuleRecord {
        val json = JsonParser.parseString(rule.rawJson).asJsonObject
        json.addProperty("isEnabled", enabled)
        return decode(json.toString()).single()
    }

    private fun decodeObject(json: JsonObject, index: Int): LegadoReplaceRuleRecord {
        val id = json.longOrDefault("id", -1L)
        require(id >= 0L) { "Replace rule at index $index has no valid id" }
        val name = json.stringOrDefault("name", "")
        val group = json.stringOrNull("group")
        val pattern = json.stringOrDefault("pattern", "")
        val replacement = json.stringOrDefault("replacement", "")
        val enabled = json.booleanOrDefault("isEnabled", true)
        val scope = json.stringOrNull("scope")
        val excludeScope = json.stringOrNull("excludeScope")
        val scopeTitle = json.booleanOrDefault("scopeTitle", false)
        val scopeSource = json.booleanOrDefault("scopeSource", false)
        val scopeContent = json.booleanOrDefault("scopeContent", true)
        val isRegex = json.booleanOrDefault("isRegex", true)
        val timeout = json.longOrDefault("timeoutMillisecond", DEFAULT_TIMEOUT_MILLISECOND)
        val order = json.intOrDefault("order", Int.MIN_VALUE)

        return LegadoReplaceRuleRecord(
            scopeRule = LegadoReplaceRuleScope(
                id = id,
                enabled = enabled,
                scope = scope,
                excludeScope = excludeScope,
                scopeTitle = scopeTitle,
                scopeSource = scopeSource,
                scopeContent = scopeContent,
                order = order,
            ),
            name = name,
            group = group,
            pattern = pattern,
            replacement = replacement,
            isRegex = isRegex,
            timeoutMillisecond = timeout,
            rawJson = json.toString(),
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = get(key) ?: return null
        require(value.isJsonNull || (value.isJsonPrimitive && value.asJsonPrimitive.isString)) {
            "Replace-rule field '$key' must be a string"
        }
        return if (value.isJsonNull) null else value.asString
    }

    private fun JsonObject.stringOrDefault(key: String, default: String): String =
        stringOrNull(key) ?: default

    private fun JsonObject.booleanOrDefault(key: String, default: Boolean): Boolean {
        val value: JsonElement = get(key) ?: return default
        require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) {
            "Replace-rule field '$key' must be a boolean"
        }
        return value.asBoolean
    }

    private fun JsonObject.longOrDefault(key: String, default: Long): Long {
        val value: JsonElement = get(key) ?: return default
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
            "Replace-rule field '$key' must be an integer"
        }
        return runCatching { value.asBigDecimal.longValueExact() }.getOrElse {
            throw IllegalArgumentException("Replace-rule field '$key' must be an integer", it)
        }
    }

    private fun JsonObject.intOrDefault(key: String, default: Int): Int {
        val value = longOrDefault(key, default.toLong())
        require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "Replace-rule field 'order' is out of range" }
        return value.toInt()
    }
}
