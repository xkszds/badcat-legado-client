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
    val rawJson: String,
) {
    fun toEngineSource(): BookSource = BookSource(
        id = sourceId,
        name = name,
        baseUrl = sourceId,
        enabled = enabled,
        group = group,
        sourceType = sourceType,
        rawJson = rawJson,
    )
}

object LegadoBookSourceCodec {

    fun decode(json: String): List<LegadoBookSource> {
        val root = JsonParser.parseString(json)
        val entries = when {
            root.isJsonArray -> root.asJsonArray.toList()
            root.isJsonObject -> listOf(root)
            else -> throw IllegalArgumentException("Book source JSON must be an object or array")
        }

        return entries.mapIndexed { index, element ->
            if (!element.isJsonObject) {
                throw IllegalArgumentException("Book source at index $index is not an object")
            }
            decodeObject(element.asJsonObject, index)
        }
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
            rawJson = source.toString(),
        )
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
}
