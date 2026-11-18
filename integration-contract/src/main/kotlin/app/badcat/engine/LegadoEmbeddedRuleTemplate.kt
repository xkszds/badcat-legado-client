package app.badcat.engine

/**
 * Shared, bounded implementation of Legado's embedded rule expansion.
 * Scripts receive only primitive source/request values and a current result;
 * Android, filesystem, network and reflection objects are never exposed.
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
        resolveJsonPathUrlTemplates: Boolean = true,
    ): EngineResult<String> {
        val evaluator = object : LegadoEmbeddedScriptEvaluator {
            override suspend fun evaluate(script: String, key: String, page: Int): EngineResult<Any?> =
                evaluateWithResult(script, key, page, result)

            override suspend fun evaluateRule(
                rule: String,
                key: String,
                page: Int,
            ): EngineResult<Any?> {
                val document = SourceDocument(url = baseUrl, body = result, page = page, key = key)
                return when (val evaluated = LegadoRuleExpressionDispatcher.evaluate(document, rule)) {
                    is EngineResult.Failure -> evaluated
                    is EngineResult.Success -> EngineResult.Success(
                        LegadoRuleExpressionDispatcher.firstScalarValue(rule, evaluated.value).orEmpty(),
                    )
                }
            }

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

            override suspend fun getString(key: String): EngineResult<String?> =
                scriptRuntime.getString(source, key)

            override suspend fun putString(key: String, value: String): EngineResult<Unit> =
                scriptRuntime.putString(source, key, value)
        }
        return resolve(template, key, page, result, evaluator, baseUrl, resolveJsonPathUrlTemplates)
    }

    suspend fun resolve(
        template: String,
        key: String,
        page: Int,
        result: String,
        evaluator: LegadoEmbeddedScriptEvaluator,
        baseUrl: String = "",
        resolveJsonPathUrlTemplates: Boolean = true,
    ): EngineResult<String> {
        if (template.length > MAX_TEMPLATE_CHARS) {
            return EngineResult.Failure(
                EngineErrorCode.UNSUPPORTED_RULE,
                "Embedded Legado rule exceeds the configured size limit",
            )
        }
        var expandedTemplate = template
        val putMatches = findPutTokens(expandedTemplate)
        if (putMatches.size > MAX_VARIABLE_OPERATIONS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Too many @put operations in one rule")
        }
        val pendingValues = mutableListOf<Pair<String, String>>()
        for (match in putMatches) {
            val values = runCatching {
                val json = com.google.gson.JsonParser.parseString(match.json)
                require(json.isJsonObject)
                buildList {
                    json.asJsonObject.entrySet().forEach { (name, value) ->
                        require(name.isNotBlank() && name.length <= MAX_VARIABLE_KEY_CHARS)
                        require(value.isJsonPrimitive && value.asJsonPrimitive.isString)
                        require(value.asString.length <= MAX_VARIABLE_VALUE_CHARS)
                        add(name to value.asString)
                    }
                }
            }.getOrElse {
                return EngineResult.Failure(
                    EngineErrorCode.UNSUPPORTED_RULE,
                    "Legado @put must contain a bounded JSON object with string values",
                    it,
                )
            }
            pendingValues += values
        }
        // Validate every directive before the first persistent write. A malformed
        // later @put must not leave earlier keys partially updated.
        for ((name, value) in pendingValues) {
            when (val stored = evaluator.putString(name, value)) {
                is EngineResult.Failure -> return stored
                is EngineResult.Success -> Unit
            }
        }
        for (match in putMatches.asReversed()) {
            expandedTemplate = expandedTemplate.removeRange(match.range)
        }
        val getMatches = GET_PATTERN.findAll(expandedTemplate).toList()
        if (getMatches.size > MAX_VARIABLE_OPERATIONS) {
            return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Too many @get operations in one rule")
        }
        for (match in getMatches.asReversed()) {
            val name = match.groupValues[1]
            if (name.isBlank() || name.length > MAX_VARIABLE_KEY_CHARS) {
                return EngineResult.Failure(EngineErrorCode.UNSUPPORTED_RULE, "Invalid Legado @get variable name")
            }
            val value = when (val loaded = evaluator.getString(name)) {
                is EngineResult.Failure -> return loaded
                is EngineResult.Success -> loaded.value.orEmpty()
            }
            expandedTemplate = expandedTemplate.replaceRange(match.range, value)
        }
        val contextualEvaluator = object : LegadoEmbeddedScriptEvaluator {
            override suspend fun evaluate(script: String, key: String, page: Int): EngineResult<Any?> =
                evaluator.evaluateWithResult(script, key, page, result)

            override suspend fun evaluateWithResult(
                script: String,
                key: String,
                page: Int,
                result: String,
            ): EngineResult<Any?> = evaluator.evaluateWithResult(script, key, page, result)

            override suspend fun getString(key: String): EngineResult<String?> = evaluator.getString(key)

            override suspend fun putString(key: String, value: String): EngineResult<Unit> =
                evaluator.putString(key, value)
        }
        val urlTemplate = if (resolveJsonPathUrlTemplates) {
            when (val resolved = resolveJsonPathPlaceholders(
                template = expandedTemplate,
                result = result,
                baseUrl = baseUrl,
            )) {
                is EngineResult.Failure -> return resolved
                is EngineResult.Success -> resolved.value
            }
        } else {
            expandedTemplate
        }
        return LegadoEmbeddedUrlTemplate.resolve(
            template = urlTemplate,
            key = key,
            evaluator = contextualEvaluator,
            page = page,
        )
    }

    /**
     * Non-search AnalyzeRule URL fields may use {{$.field}} and {$.field}
     * JSONPath references. Search and explore URLs follow AnalyzeUrl semantics
     * instead: their double-brace expressions are JavaScript only, so callers
     * disable this expansion for those two URL contexts.
     */
    private fun resolveJsonPathPlaceholders(
        template: String,
        result: String,
        baseUrl: String,
    ): EngineResult<String> {
        val document = SourceDocument(baseUrl, result)
        var expanded = template
        for (pattern in listOf(DOUBLE_BRACE_JSON_PATH, SINGLE_BRACE_JSON_PATH)) {
            val matches = pattern.findAll(expanded).toList()
            for (match in matches.asReversed()) {
                val path = match.groupValues[1].trim()
                val value = when (val selected = JsonPathRuleEvaluator.evaluate(document, path)) {
                    is EngineResult.Failure -> return selected
                    is EngineResult.Success -> selected.value.firstOrNull().orEmpty()
                }
                expanded = expanded.replaceRange(match.range, value)
            }
        }
        return EngineResult.Success(expanded)
    }

    fun containsVariableRule(value: String): Boolean =
        value.contains("@get:{", true) || value.contains("@put:{", true)

    private data class PutToken(val range: IntRange, val json: String)

    /**
     * Read @put JSON objects with balanced braces. A delimiter regex can
     * truncate valid JSON strings that themselves contain a closing brace.
     * Incomplete directives remain literal, matching the prior behavior.
     */
    private fun findPutTokens(template: String): List<PutToken> {
        val tokens = mutableListOf<PutToken>()
        var searchFrom = 0
        while (searchFrom < template.length) {
            val marker = template.indexOf("@put:", searchFrom, ignoreCase = true)
            if (marker < 0) break
            val objectStart = marker + "@put:".length
            if (objectStart >= template.length || template[objectStart] != '{') {
                searchFrom = objectStart
                continue
            }
            var depth = 0
            var quote = false
            var escaped = false
            var end = -1
            var index = objectStart
            while (index < template.length) {
                val char = template[index]
                if (quote) {
                    when {
                        escaped -> escaped = false
                        char == '\\' -> escaped = true
                        char == '"' -> quote = false
                    }
                } else {
                    when (char) {
                        '"' -> quote = true
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) {
                                end = index
                                break
                            }
                        }
                    }
                }
                index++
            }
            if (end >= objectStart) {
                tokens += PutToken(marker..end, template.substring(objectStart, end + 1))
                searchFrom = end + 1
            } else {
                searchFrom = objectStart + 1
            }
        }
        return tokens
    }

    private const val MAX_TEMPLATE_CHARS = 16 * 1024
    private const val MAX_VARIABLE_OPERATIONS = 32
    private const val MAX_VARIABLE_KEY_CHARS = 256
    private const val MAX_VARIABLE_VALUE_CHARS = 16 * 1024
    private val GET_PATTERN = Regex("@get:\\{([^}]+?)\\}", RegexOption.IGNORE_CASE)
    private val DOUBLE_BRACE_JSON_PATH = Regex("""\{\{\s*(\$(?:\.|\[)[^{}]*?)\s*\}\}""")
    private val SINGLE_BRACE_JSON_PATH = Regex("""(?<!\{)\{\s*(\$(?:\.|\[)[^{}]*?)\s*\}(?!\})""")
}
