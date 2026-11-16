package app.badcat.engine

/**
 * Executes the bounded pure-JavaScript subset of AnalyzeUrl's @js and <js> forms.
 * This intentionally exposes only string key/page/result bindings through the
 * injected sandbox. It does not emulate Legado's java/http/cookie/cache/WebView
 * host objects, shared source scope, login state or asynchronous network APIs.
 */
object LegadoUrlScriptRule {
    private val pattern = Regex("<js>([\\w\\W]*?)</js>|@js:([\\w\\W]*)", RegexOption.IGNORE_CASE)

    fun containsRule(value: String): Boolean =
        value.contains("@js:", ignoreCase = true) || value.contains("<js>", ignoreCase = true)

    suspend fun resolve(
        ruleUrl: String,
        key: String,
        page: Int,
        evaluator: LegadoEmbeddedScriptEvaluator,
    ): EngineResult<String> {
        if (!containsRule(ruleUrl)) return EngineResult.Success(ruleUrl)
        var result = ruleUrl
        var start = 0
        for (match in pattern.findAll(ruleUrl)) {
            if (match.range.first > start) {
                val prefix = ruleUrl.substring(start, match.range.first).trim()
                if (prefix.isNotEmpty()) result = prefix.replace("@result", result)
            }
            val script = match.groups[2]?.value ?: match.groups[1]?.value.orEmpty()
            val evaluated = when (val value = evaluator.evaluateWithResult(script, key, page, result)) {
                is EngineResult.Failure -> return value
                is EngineResult.Success -> value.value
            }
            result = evaluated?.toString() ?: "null"
            start = match.range.last + 1
        }
        if (start < ruleUrl.length) {
            val suffix = ruleUrl.substring(start).trim()
            if (suffix.isNotEmpty()) result = suffix.replace("@result", result)
        }
        return EngineResult.Success(result)
    }
}
