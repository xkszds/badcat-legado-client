package app.badcat.engine

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * A source descriptor retains the complete upstream JSON document.
 *
 * Legado's BookSource contains many nested rule models and script fields. Keeping
 * the original JSON avoids silently dropping fields this host does not yet model.
 */
data class LegadoBookSource(
    val sourceId: String,
    val name: String,
    val group: String?,
    val sourceType: Int,
    val enabled: Boolean,
    val enabledExplore: Boolean,
    val enabledCookieJar: Boolean?,
    val header: String?,
    val jsLib: String?,
    val loginUrl: String?,
    val exploreUrl: String?,
    val searchUrl: String?,
    val mainJs: String?,
    val ruleExploreJson: String?,
    val ruleSearchJson: String?,
    val ruleBookInfoJson: String?,
    val ruleTocJson: String?,
    val ruleContentJson: String?,
    val rawJson: String,
) {
    fun toEngineSource(): BookSource = BookSource(
        id = sourceId,
        name = name,
        baseUrl = sourceId,
        enabled = enabled,
        group = group,
        sourceType = sourceType,
        enabledExplore = enabledExplore,
        enabledCookieJar = enabledCookieJar,
        header = header,
        jsLib = jsLib,
        loginUrl = loginUrl,
        exploreUrl = exploreUrl,
        searchUrl = searchUrl,
        mainJs = mainJs,
        ruleExploreJson = ruleExploreJson,
        ruleSearchJson = ruleSearchJson,
        ruleBookInfoJson = ruleBookInfoJson,
        ruleTocJson = ruleTocJson,
        ruleContentJson = ruleContentJson,
        rawJson = rawJson,
    )
}

object LegadoBookSourceCodec {

    const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

    /** Decode bounded UTF-8 documents without silently replacing malformed byte sequences. */
    fun decodeUtf8ToString(bytes: ByteArray): String {
        require(bytes.size <= MAX_IMPORT_BYTES) {
            "Book-source JSON exceeds the 5 MiB import limit"
        }
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(bytes))
            .toString()
    }

    fun decode(json: String): List<LegadoBookSource> {
        require(json.length <= MAX_IMPORT_BYTES) {
            "Book-source JSON exceeds the 5 MiB import limit"
        }
        val root = JsonParser.parseString(json)
        val entries = when {
            root.isJsonArray -> root.asJsonArray.toList()
            root.isJsonObject -> listOf(root)
            else -> throw IllegalArgumentException("Book source JSON must be an object or array")
        }

        val decoded = entries.mapIndexed { index, element ->
            if (!element.isJsonObject) {
                throw IllegalArgumentException("Book source at index $index is not an object")
            }
            decodeObject(element.asJsonObject, index)
        }
        val duplicateIds = decoded.groupingBy { it.sourceId }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
        require(duplicateIds.isEmpty()) {
            "Duplicate bookSourceUrl values: ${duplicateIds.joinToString()}"
        }
        return decoded
    }

    fun withEnabled(source: LegadoBookSource, enabled: Boolean): LegadoBookSource {
        val json = JsonParser.parseString(source.rawJson).asJsonObject
        json.addProperty("enabled", enabled)
        return decode(json.toString()).single()
    }

    fun encode(sources: List<LegadoBookSource>): String {
        val array = JsonArray()
        sources.forEach { source ->
            val parsed = JsonParser.parseString(source.rawJson)
            require(parsed.isJsonObject) { "Stored source JSON is not an object: ${source.sourceId}" }
            array.add(parsed)
        }
        return array.toString()
    }

    private fun decodeObject(source: JsonObject, index: Int): LegadoBookSource {
        validateKnownFieldTypes(source, index)
        val sourceId = source.stringOrNull("bookSourceUrl")
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Book source at index $index has no bookSourceUrl")
        val name = source.stringOrNull("bookSourceName")
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Book source '$sourceId' has no bookSourceName")

        return LegadoBookSource(
            sourceId = sourceId,
            name = name,
            group = source.stringOrNull("bookSourceGroup"),
            sourceType = source.intOrDefault("bookSourceType", 0),
            enabled = source.booleanOrDefault("enabled", true),
            enabledExplore = source.booleanOrDefault("enabledExplore", true),
            enabledCookieJar = source.booleanOrDefault("enabledCookieJar", true),
            header = source.stringOrNull("header"),
            jsLib = source.stringOrNull("jsLib"),
            loginUrl = source.stringOrNull("loginUrl"),
            exploreUrl = source.stringOrNull("exploreUrl"),
            searchUrl = source.stringOrNull("searchUrl"),
            mainJs = source.stringOrNull("mainJs"),
            ruleExploreJson = source.objectOrNull("ruleExplore"),
            ruleSearchJson = source.objectOrNull("ruleSearch"),
            ruleBookInfoJson = source.objectOrNull("ruleBookInfo"),
            ruleTocJson = source.objectOrNull("ruleToc"),
            ruleContentJson = source.objectOrNull("ruleContent"),
            rawJson = source.toString(),
        )
    }

    private fun validateKnownFieldTypes(source: JsonObject, index: Int) {
        val stringFields = listOf(
            "bookSourceUrl", "bookSourceName", "bookSourceGroup", "header",
            "jsLib", "loginUrl", "exploreUrl", "searchUrl", "mainJs",
        )
        stringFields.forEach { key ->
            val value = source.get(key) ?: return@forEach
            require(value.isJsonNull || (value.isJsonPrimitive && value.asJsonPrimitive.isString)) {
                "Book source at index $index has a non-string '$key' field"
            }
        }
        listOf("enabled", "enabledExplore", "enabledCookieJar").forEach { key ->
            val value = source.get(key) ?: return@forEach
            require(value.isJsonNull || (value.isJsonPrimitive && value.asJsonPrimitive.isBoolean)) {
                "Book source at index $index has a non-boolean '$key' field"
            }
        }
        source.get("bookSourceType")?.let { value ->
            require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber &&
                runCatching { value.asBigDecimal.intValueExact() }.isSuccess
            ) {
                "Book source at index $index has an invalid 'bookSourceType' field"
            }
        }
        listOf("ruleExplore", "ruleSearch", "ruleBookInfo", "ruleToc", "ruleContent").forEach { key ->
            val value = source.get(key) ?: return@forEach
            require(value.isJsonNull || value.isJsonObject || (
                value.isJsonPrimitive && value.asJsonPrimitive.isString &&
                    runCatching {
                        com.google.gson.JsonParser.parseString(value.asString).isJsonObject
                    }.getOrDefault(false)
                )
            ) {
                "Book source at index $index has an invalid '$key' rule object"
            }
        }
        source.get("ruleContent")?.let { value ->
            if (value.isJsonObject || (value.isJsonPrimitive && value.asJsonPrimitive.isString)) {
                val rule = runCatching {
                    if (value.isJsonObject) value.asJsonObject
                    else JsonParser.parseString(value.asString).asJsonObject
                }.getOrNull() ?: return@let
                rule.get("maxBatchSize")?.let { batchSize ->
                    require(batchSize.isJsonNull || (
                        batchSize.isJsonPrimitive && batchSize.asJsonPrimitive.isNumber &&
                            runCatching { batchSize.asBigDecimal.intValueExact() }.isSuccess
                        )
                    ) {
                        "Book source at index $index has an invalid 'ruleContent.maxBatchSize' field"
                    }
                }
            }
        }
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = get(key) ?: return null
        return if (value.isJsonNull) null else runCatching { value.asString }.getOrNull()
    }

    private fun JsonObject.intOrDefault(key: String, default: Int): Int {
        val value: JsonElement = get(key) ?: return default
        return runCatching { value.asInt }.getOrDefault(default)
    }

    private fun JsonObject.booleanOrDefault(key: String, default: Boolean): Boolean {
        val value: JsonElement = get(key) ?: return default
        return runCatching { value.asBoolean }.getOrDefault(default)
    }

    private fun JsonObject.objectOrNull(key: String): String? {
        val value = get(key) ?: return null
        return when {
            value.isJsonObject -> value.asJsonObject.toString()
            value.isJsonPrimitive && value.asJsonPrimitive.isString ->
                com.google.gson.JsonParser.parseString(value.asString).asJsonObject.toString()
            else -> null
        }
    }
}
