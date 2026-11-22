package app.badcat.engine

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Validated data contract for Legado Login UI v2.
 * Parsing is deliberately separate from script execution and authentication.
 */
object LegadoLoginUiV2 {
    const val VERSION = 2
    const val MAX_ROWS = 64
    const val MAX_OPTIONS = 128
    const val MAX_TEXT_CHARS = 2_048

    enum class RowType { TEXT, PASSWORD, LABEL, SELECT, TOGGLE, BUTTON }

    /**
     * Host invocation metadata for declarative Login UI v2.
     *
     * This is not the upstream Book/BookChapter object binding and does not
     * enable the legacy java/browser/long-press login extension surface.
     */
    data class InvocationContext(
        val sourceId: String,
        val bookType: Int? = null,
        val bookUrl: String? = null,
        val chapterUrl: String? = null,
    ) {
        fun validationError(expectedSourceId: String): String? {
            if (sourceId.isBlank() || sourceId.length > MAX_CONTEXT_CHARS) return "invalid source ID"
            if (sourceId != expectedSourceId) return "source ID does not match the loaded source"
            if (bookType != null &&
                (bookType < 0 || (bookType and KNOWN_BOOK_TYPE_MASK.inv()) != 0)
            ) return "bookType contains unknown flags"
            if (bookUrl != null && (bookUrl.isBlank() || bookUrl.length > MAX_CONTEXT_CHARS)) {
                return "invalid book identity"
            }
            if (chapterUrl != null && (chapterUrl.isBlank() || chapterUrl.length > MAX_CONTEXT_CHARS)) {
                return "invalid chapter identity"
            }
            if (chapterUrl != null && bookUrl == null) return "chapter identity requires a book identity"
            return null
        }

        /** Namespaced additive host metadata; never serialized as fake upstream models. */
        fun toJson(): String = JsonObject().apply {
            addProperty("sourceId", sourceId)
            bookType?.let { addProperty("bookType", it) }
            bookUrl?.let { addProperty("bookUrl", it) }
            chapterUrl?.let { addProperty("chapterUrl", it) }
        }.toString()
    }

    const val MAX_CONTEXT_CHARS = 4_096
    /** Pinned BookType flags: video, text, updateError, audio, image, webFile, local, archive, notShelf. */
    const val KNOWN_BOOK_TYPE_MASK = 0x7FC


    data class Row(
        val name: String,
        val type: RowType,
        val key: String? = null,
        val action: String? = null,
        val hint: String? = null,
        val value: String? = null,
        val options: List<String> = emptyList(),
        val countdown: Int? = null,
    )

    data class ActionResult(
        val stateJson: String? = null,
        val errors: Map<String, String> = emptyMap(),
        val loginJson: String? = null,
        val close: Boolean = false,
        val unknownKeys: Set<String> = emptySet(),
    )

    fun isV2(loginUi: String?): Boolean =
        parseObject(loginUi)?.intOrNull("version") == VERSION

    fun parseRows(renderJson: String?): List<Row>? {
        val root = parseObject(renderJson) ?: return null
        val array = root.get("rows")?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        if (array.size() !in 1..MAX_ROWS) return null
        val rows = mutableListOf<Row>()
        for (element in array) {
            if (!element.isJsonObject) return null
            val obj = element.asJsonObject
            val name = obj.stringOrNull("name")?.takeIf { it.isNotBlank() && it.length <= MAX_TEXT_CHARS } ?: return null
            val type = when (obj.stringOrNull("type")) {
                "text" -> RowType.TEXT
                "password" -> RowType.PASSWORD
                "label" -> RowType.LABEL
                "select" -> RowType.SELECT
                "toggle" -> RowType.TOGGLE
                "button" -> RowType.BUTTON
                else -> return null
            }
            val key = obj.stringOrNull("key")
            val action = obj.stringOrNull("action")
            val hint = obj.stringOrNull("hint")
            val value = obj.stringOrNull("value")
            val countdown = obj.intOrNull("countdown")
            if (countdown != null && countdown !in 0..86_400) return null
            if (listOfNotNull(key, action, hint, value).any { it.length > MAX_TEXT_CHARS }) return null
            val optionsElement = obj.get("options")
            val options = if (optionsElement == null || optionsElement.isJsonNull) {
                emptyList()
            } else {
                if (!optionsElement.isJsonArray || optionsElement.asJsonArray.size() > MAX_OPTIONS) return null
                val parsed = mutableListOf<String>()
                for (option in optionsElement.asJsonArray) {
                    if (!option.isJsonPrimitive || !option.asJsonPrimitive.isString) return null
                    val text = option.asString
                    if (text.isBlank() || text.length > MAX_TEXT_CHARS) return null
                    parsed += text
                }
                parsed
            }
            when (type) {
                RowType.TEXT, RowType.PASSWORD -> if (key.isNullOrBlank()) return null
                RowType.LABEL -> Unit
                RowType.SELECT -> if (key.isNullOrBlank() || options.isEmpty()) return null
                RowType.TOGGLE -> if (key.isNullOrBlank() || (value != null && value !in setOf("true", "false"))) return null
                RowType.BUTTON -> if (action.isNullOrBlank()) return null
            }
            if (type == RowType.TOGGLE && action != null && action.isBlank()) return null
            rows += Row(name, type, key, action, hint, value, options, countdown)
        }
        val keys = rows.filter { it.type in setOf(RowType.TEXT, RowType.PASSWORD, RowType.SELECT, RowType.TOGGLE) }.mapNotNull { it.key }
        val actions = rows.filter { it.type == RowType.BUTTON || it.type == RowType.TOGGLE }.mapNotNull { it.action }
        if (keys.size != keys.distinct().size || actions.size != actions.distinct().size) return null
        return rows
    }

    fun parseActionResult(json: String?): ActionResult? {
        if (json.isNullOrBlank()) return ActionResult()
        val root = parseObject(json) ?: return null
        val state = root.get("state")?.takeUnless { it.isJsonNull }
        val error = root.get("error")?.takeUnless { it.isJsonNull }
        val login = root.get("login")?.takeUnless { it.isJsonNull }
        val close = root.get("close")?.takeUnless { it.isJsonNull }
        if ((state != null && !state.isJsonObject) ||
            (error != null && !error.isJsonObject) ||
            (login != null && !login.isJsonObject) ||
            (close != null && (!close.isJsonPrimitive || !close.asJsonPrimitive.isBoolean))
        ) return null
        val errors = linkedMapOf<String, String>()
        if (error != null) {
            for ((key, value) in error.asJsonObject.entrySet()) {
                if (key.isBlank() || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) return null
                errors[key] = value.asString.take(MAX_TEXT_CHARS)
            }
        }
        return ActionResult(
            stateJson = state?.toString(),
            errors = errors,
            loginJson = login?.toString(),
            close = close?.asBoolean ?: false,
            unknownKeys = root.keySet() - setOf("state", "error", "login", "close"),
        )
    }

    private fun parseObject(json: String?): JsonObject? {
        if (json.isNullOrBlank() || json.length > MAX_TEXT_CHARS * MAX_ROWS) return null
        return runCatching {
            JsonParser.parseString(json).takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull()
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = get(key) ?: return null
        if (value.isJsonNull) return null
        return value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val value = get(key) ?: return null
        if (value.isJsonNull) return null
        return runCatching {
            if (!value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) null else value.asInt
        }.getOrNull()
    }
}
