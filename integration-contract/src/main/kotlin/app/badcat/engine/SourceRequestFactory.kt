package app.badcat.engine

import com.google.gson.JsonParser

/**
 * Converts source-level request settings into the transport contract.
 *
 * This handles only the static header/cookie portion of Legado's request setup.
 * URL interpolation, JS request options, redirects and cookie persistence remain
 * responsibilities of the concrete upstream-compatible transport adapter.
 */
object SourceRequestFactory {

    fun create(
        source: BookSource,
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): EngineResult<SourceHttpRequest> {
        if (source.id.isBlank() || url.isBlank()) {
            return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source identity and request URL must not be blank",
            )
        }

        val sourceHeaders = parseHeaders(source.header)
            ?: return EngineResult.Failure(
                EngineErrorCode.INVALID_SOURCE,
                "Source header must be a JSON object with string values",
            )

        val merged = LinkedHashMap<String, String>()
        sourceHeaders.forEach { (name, value) -> merged[name] = value }
        headers.forEach { (name, value) ->
            val existing = merged.keys.firstOrNull { it.equals(name, ignoreCase = true) }
            if (existing != null) merged.remove(existing)
            merged[name] = value
        }

        return EngineResult.Success(
            SourceHttpRequest(
                sourceId = source.id,
                url = url,
                method = method.uppercase(),
                headers = merged,
                body = body,
                useCookieJar = source.enabledCookieJar != false,
            ),
        )
    }

    private fun parseHeaders(raw: String?): Map<String, String>? {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JsonParser.parseString(raw)
            require(root.isJsonObject)
            buildMap {
                root.asJsonObject.entrySet().forEach { (name, value) ->
                    require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                    put(name, value.asString)
                }
            }
        }.getOrNull()
    }
}
