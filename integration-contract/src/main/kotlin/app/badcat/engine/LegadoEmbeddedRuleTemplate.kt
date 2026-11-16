package app.badcat.engine

/**
 * Shared, bounded implementation of Legado's embedded `{{...}}` rule expansion.
 * The caller supplies the current document/item as `result`; scripts receive
 * only the source metadata and request values, never Android or transport objects.
 */
object LegadoEmbeddedRuleTemplate {
    suspend fun resolve(
        source: BookSource,
        template: String,
        result: String,
        baseUrl: String,
        ruleName: String,
        scriptRuntime: SourceScriptRuntime,
        key: String = "",
        page: Int = 1,
    ): EngineResult<String> {
        if (template.length > MAX_TEMPLATE_CHARS) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Embedded Legado rule exceeds the configured size limit",
            )
        }
        var expandedTemplate = template
        val putMatches = PUT_PATTERN.findAll(expandedTemplate).toList()
        if (putMatches.size > MAX_VARIABLE_OPERATIONS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Too many @put operations in one rule")
        }
        for (match in putMatches) {
            val values = runCatching {
                val json = com.google.gson.JsonParser.parseString(match.groupValues[1])
                require(json.isJsonObject)
                buildMap {
                    json.asJsonObject.entrySet().forEach { (name, value) ->
                        require(name.isNotBlank() && name.length <= MAX_VARIABLE_KEY_CHARS)
                        require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                        put(name, value.asString)
                    }
                }
            }.getOrElse {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Legado @put must contain a JSON object with string values",
                    it,
                )
            }
            for ((name, value) in values) {
                if (value.length > MAX_VARIABLE_VALUE_CHARS) {
                    return EngineResult.Failure(
                        EngineErrorCode.UNSUPPORTED_RULE,
                        "Legado @put value exceeds the configured size limit",
                    )
                }
                when (val stored = scriptRuntime.putString(source, name, value)) {
                    is EngineResult.Failure -> return stored
                    is EngineResult.Success -> Unit
                }
            }
        }
        expandedTemplate = PUT_PATTERN.replace(expandedTemplate, "")
        val getMatches = GET_PATTERN.findAll(expandedTemplate).toList()
        if (getMatches.size > MAX_VARIABLE_OPERATIONS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Too many @get operations in one rule")
        }
        for (match in getMatches.asReversed()) {
            val name = match.groupValues[1]
            if (name.isBlank() || name.length > MAX_VARIABLE_KEY_CHARS) {
                return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Invalid Legado @get variable name")
            }
            val value = when (val loaded = scriptRuntime.getString(source, name)) {
                is EngineResult.Failure -> return loaded
                is EngineResult.Success -> loaded.value.orEmpty()
            }
            expandedTemplate = expandedTemplate.replaceRange(match.range, value)
        }
        if (!expandedTemplate.contains("{{")) return EngineResult.Success(expandedTemplate)
        val evaluator = object : LegadoEmbeddedScriptEvaluator {
            override suspend fun evaluate(
                script: String,
                key: String,
                page: Int,
            ): EngineResult<Any?> = evaluateWithResult(script, key, page, result)

            override suspend fun evaluateWithResult(
                script: String,
                key: String,
                page: Int,
                result: String,
            ): EngineResult<Any?> = when (val evaluated = scriptRuntime.evaluate(
                source = source,
                script = script,
                bindings = mapOf(
                    "result" to result,
                    "baseUrl" to baseUrl,
                    "sourceUrl" to source.id,
                    "sourceId" to source.id,
                    "sourceName" to source.name,
                    "ruleName" to ruleName,
                    "key" to key,
                    "page" to page.toString(),
                ),
            )) {
                is EngineResult.Failure -> evaluated
                is EngineResult.Success -> EngineResult.Success(evaluated.value)
            }
        }
        return LegadoEmbeddedUrlTemplate.resolve(
            template = expandedTemplate,
            key = key,
            evaluator = evaluator,
            page = page,
        )
    }

    private const val MAX_TEMPLATE_CHARS = 16 * 1024
    private const val MAX_VARIABLE_OPERATIONS = 32
    private const val MAX_VARIABLE_KEY_CHARS = 256
    private const val MAX_VARIABLE_VALUE_CHARS = 16 * 1024
    private val PUT_PATTERN = Regex("@put:(\\{[^}]+?\\})", RegexOption.IGNORE_CASE)
    private val GET_PATTERN = Regex("@get:\\{([^}]+?)\\}", RegexOption.IGNORE_CASE)
}
