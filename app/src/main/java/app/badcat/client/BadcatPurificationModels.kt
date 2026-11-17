package app.badcat.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import app.badcat.engine.EngineResult
import app.badcat.engine.LegadoRegexElementEvaluator
import java.net.URI

/**
 * Faithful local representation of the original Badcat AdMark payload.
 *
 * markRules are retained as opaque source values until the original matching
 * semantics are fully verified; they are never treated as Legado text rules.
 */
data class BadcatAdMarkItem(
    val host: String,
    val urlPattern: String,
    val markRules: List<String>,
) {
    fun isValid(): Boolean =
        host.isNotBlank() &&
            host.length <= MAX_HOST_CHARS &&
            host.none { it.isWhitespace() || it == '/' || it == '@' } &&
            urlPattern.length <= MAX_PATTERN_CHARS &&
            urlPatternSegments() != null &&
            markRules.isNotEmpty() &&
            markRules.size <= MAX_RULE_COUNT &&
            markRules.all { it.isNotBlank() && it.length <= MAX_RULE_CHARS }

    fun appliesToHost(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        if ((scheme != "http" && scheme != "https") || uri.userInfo != null) return false
        val requestHost = uri.host?.lowercase() ?: return false
        return requestHost == host.lowercase()
    }

    /**
     * The original DEX parses urlPattern as a JSON integer array, joins the
     * values with "/" and applies Pattern.matcher(url).find().
     */
    fun matchesUrl(url: String): Boolean {
        if (!isValid() || !appliesToHost(url)) return false
        val segments = urlPatternSegments() ?: return false
        val expression = segments.joinToString("/")
        return when (val result = LegadoRegexElementEvaluator.matches(url, expression)) {
            is EngineResult.Success -> result.value
            is EngineResult.Failure -> false
        }
    }

    /**
     * The original WebView bridge wraps one selected markRule in a one-item
     * JSON array before calling "onDeleteAd". This builds that verified payload
     * shape only; it does not execute the page's DOM-removal callback.
     */
    fun webViewDeleteCallbackPayloads(): List<String> =
        markRules.map { rule ->
            com.google.gson.JsonArray().apply { add(rule) }.toString()
        }

    private fun urlPatternSegments(): List<Int>? {
        val array = runCatching { JsonParser.parseString(urlPattern).takeIf { it.isJsonArray }?.asJsonArray }
            .getOrNull() ?: return null
        if (array.size() !in 1..MAX_URL_SEGMENTS) return null
        val values = ArrayList<Int>(array.size())
        for (element in array) {
            if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) return null
            val exact = runCatching { element.asBigDecimal }.getOrNull() ?: return null
            val normalized = exact.stripTrailingZeros()
            if (normalized.scale() > 0 ||
                exact < java.math.BigDecimal.ZERO ||
                exact > java.math.BigDecimal.valueOf(Int.MAX_VALUE.toLong())
            ) return null
            values += exact.toInt()
        }
        return values
    }

    companion object {
        const val MAX_HOST_CHARS = 253
        const val MAX_PATTERN_CHARS = 2_048
        const val MAX_RULE_COUNT = 64
        const val MAX_RULE_CHARS = 2_048
        const val MAX_URL_SEGMENTS = 32
    }
}

/**
 * Codec for both the app's former flat AdMark JSON export and the original
 * Badcat issued-response shape: { markItems: [{ host, ruleItems: [...] }] }.
 *
 * Only the verified AdMark host/urlPattern/markRules subset is imported.
 * Server-provided adBlockJsInject is deliberately not auto-installed.
 */
object BadcatAdMarkJsonCodec {
    const val MAX_ITEMS = 5_000
    const val MAX_JSON_BYTES = 2 * 1024 * 1024

    fun decode(json: String): List<BadcatAdMarkItem> {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) {
            "Badcat AdMark import exceeds the size limit"
        }
        val root = JsonParser.parseString(json)
        val decoded = when {
            root.isJsonArray -> decodeFlat(root.asJsonArray)
            root.isJsonObject -> {
                val obj = root.asJsonObject
                val groups = obj.getAsJsonArray("markItems")
                    ?: if (obj.has("host")) JsonArray().apply { add(obj) }
                    else error("Badcat AdMark JSON has no markItems array")
                decodeGroups(groups)
            }
            else -> error("Badcat AdMark JSON must be an array or object")
        }
        require(decoded.size <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        require(decoded.all(BadcatAdMarkItem::isValid)) { "Badcat AdMark JSON contains invalid records" }
        require(decoded.distinctBy(::key).size == decoded.size) {
            "Badcat AdMark JSON contains duplicate records"
        }
        return decoded
    }

    /** Export the original nested markItems/ruleItems structure. */
    fun encode(items: List<BadcatAdMarkItem>): String {
        require(items.size <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        require(items.all(BadcatAdMarkItem::isValid)) { "Badcat AdMark catalog contains invalid records" }
        require(items.distinctBy(::key).size == items.size) {
            "Badcat AdMark catalog contains duplicate records"
        }
        val root = JsonObject()
        val groups = JsonArray()
        items.groupBy { it.host.lowercase() }.values.forEach { hostItems ->
            val group = JsonObject().apply { addProperty("host", hostItems.first().host) }
            val ruleItems = JsonArray()
            hostItems.forEach { item ->
                val rule = JsonObject().apply {
                    add("urlPattern", JsonParser.parseString(item.urlPattern))
                    add("markRules", JsonArray().apply { item.markRules.forEach { add(it) } })
                }
                ruleItems.add(rule)
            }
            group.add("ruleItems", ruleItems)
            groups.add(group)
        }
        root.add("markItems", groups)
        return root.toString()
    }

    private fun decodeFlat(array: JsonArray): List<BadcatAdMarkItem> = buildList {
        require(array.size() <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        array.forEachIndexed { index, value ->
            val item = value.takeIf { it.isJsonObject }?.asJsonObject
                ?: error("Badcat AdMark item $index is not an object")
            add(
                BadcatAdMarkItem(
                    host = normalizeHost(item.string("host")),
                    urlPattern = item.pattern("urlPattern"),
                    markRules = item.stringArray("markRules"),
                ),
            )
        }
    }

    private fun decodeGroups(groups: JsonArray): List<BadcatAdMarkItem> = buildList {
        require(groups.size() <= MAX_ITEMS) { "Badcat AdMark catalog exceeds the item limit" }
        groups.forEachIndexed { groupIndex, value ->
            val group = value.takeIf { it.isJsonObject }?.asJsonObject
                ?: error("Badcat AdMark group $groupIndex is not an object")
            val host = normalizeHost(group.string("host"))
            val ruleItems = group.getAsJsonArray("ruleItems")
                ?: error("Badcat AdMark group $groupIndex has no ruleItems")
            ruleItems.forEachIndexed { ruleIndex, ruleValue ->
                val rule = ruleValue.takeIf { it.isJsonObject }?.asJsonObject
                    ?: error("Badcat AdMark rule $groupIndex/$ruleIndex is not an object")
                add(
                    BadcatAdMarkItem(
                        host = host,
                        urlPattern = rule.pattern("urlPattern"),
                        markRules = rule.stringArray("markRules"),
                    ),
                )
            }
        }
    }

    private fun JsonObject.string(name: String): String {
        val value = get(name) ?: error("Badcat AdMark item has no $name")
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            "Badcat AdMark $name must be a string"
        }
        return value.asString
    }

    private fun JsonObject.stringArray(name: String): List<String> {
        val array = getAsJsonArray(name) ?: error("Badcat AdMark item has no $name array")
        return array.mapIndexed { index, value ->
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                "Badcat AdMark $name[$index] must be a string"
            }
            value.asString
        }
    }

    private fun JsonObject.pattern(name: String): String {
        val value = get(name) ?: error("Badcat AdMark item has no $name")
        return when {
            value.isJsonArray -> value.asJsonArray.toString()
            value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString
            else -> error("Badcat AdMark $name must be an integer array or JSON-array string")
        }
    }

    private fun normalizeHost(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return runCatching { URI(trimmed).host }.getOrNull()?.takeIf(String::isNotBlank) ?: trimmed
        }
        return trimmed
    }

    private fun key(item: BadcatAdMarkItem): String =
        item.host.lowercase() + "|" + item.urlPattern
}

/**
 * One shared view of Badcat purification settings and rule catalogs.
 * Legado replacement rules remain separately accessible and separately enabled.
 */
data class BadcatPurificationSnapshot(
    val webViewAdFilterEnabled: Boolean,
    val webViewInjectionScript: String?,
    val adMarks: List<BadcatAdMarkItem>,
)

object BadcatPurificationUrlPolicy {
    /** Exact-host plus verified URL-pattern matching; markRules stay opaque. */
    fun marksForUrl(url: String, marks: List<BadcatAdMarkItem>): List<BadcatAdMarkItem> =
        marks.filter { it.matchesUrl(url) }

    /**
     * Injection is limited to web pages. Never run the stored page script in
     * file:, content:, data: or about: documents (including local app content).
     */
    fun injectionForPage(snapshot: BadcatPurificationSnapshot, pageUrl: String): String? {
        val uri = runCatching { URI(pageUrl) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if ((scheme != "http" && scheme != "https") ||
            uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null
        ) return null
        return injectionForPage(snapshot)
    }

    fun injectionForPage(snapshot: BadcatPurificationSnapshot): String? =
        snapshot.webViewInjectionScript
            ?.takeIf { snapshot.webViewAdFilterEnabled && it.isNotBlank() && it.length <= MAX_SCRIPT_CHARS }

    const val MAX_SCRIPT_CHARS = 256 * 1024
}
