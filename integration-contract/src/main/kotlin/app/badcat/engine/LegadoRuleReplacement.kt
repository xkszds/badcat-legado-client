package app.badcat.engine

/**
 * Parsed suffix from Legado SourceRule's `rule##regex##replacement###` syntax.
 */
data class ParsedLegadoRule(
    val expression: String,
    val replaceRegex: String? = null,
    val replacement: String = "",
    val replaceFirst: Boolean = false,
    private val suffix: String = "",
) {
    fun withExpression(value: String): ParsedLegadoRule = copy(expression = value)

    fun toRuleString(): String = expression + suffix

    fun apply(values: List<String>): EngineResult<List<String>> {
        val pattern = replaceRegex?.takeIf(String::isNotEmpty) ?: return EngineResult.Success(values)
        val output = ArrayList<String>(values.size)
        for (value in values) {
            when (val result = LegadoRegexElementEvaluator.replace(
                input = value,
                expression = pattern,
                replacement = replacement,
                replaceFirst = replaceFirst,
            )) {
                is EngineResult.Failure -> return result
                is EngineResult.Success -> output += result.value
            }
        }
        return EngineResult.Success(output)
    }
}

object LegadoRuleReplacement {
    fun transformExpression(expression: String, transform: (String) -> String): String {
        val parsed = parse(expression)
        return parsed.withExpression(transform(parsed.expression)).toRuleString()
    }

    fun parse(expression: String): ParsedLegadoRule {
        val parts = expression.split("##", limit = 4)
        if (parts.size == 1) return ParsedLegadoRule(expression = expression)
        return ParsedLegadoRule(
            expression = parts[0],
            replaceRegex = parts.getOrNull(1).orEmpty(),
            replacement = parts.getOrNull(2).orEmpty(),
            replaceFirst = parts.size > 3,
            suffix = expression.substring(parts[0].length),
        )
    }
}
