package app.badcat.engine

/**
 * Parsed suffix from Legado SourceRule's `rule##regex##replacement###` syntax.
 */
data class ParsedLegadoRule(
    val expression: String,
    val replaceRegex: String? = null,
    val replacement: String = "",
    val replaceFirst: Boolean = false,
    private val separatorCount: Int = 0,
) {
    fun withExpression(value: String): ParsedLegadoRule = copy(expression = value)

    fun toRuleString(): String = when (separatorCount) {
        0 -> expression
        1 -> "$expression##${replaceRegex.orEmpty()}"
        2 -> "$expression##${replaceRegex.orEmpty()}##$replacement"
        else -> "$expression##${replaceRegex.orEmpty()}##$replacement##"
    }

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
    fun parse(expression: String): ParsedLegadoRule {
        val parts = expression.split("##", limit = 4)
        if (parts.size == 1) return ParsedLegadoRule(expression = expression)
        return ParsedLegadoRule(
            expression = parts[0],
            replaceRegex = parts.getOrNull(1).orEmpty(),
            replacement = parts.getOrNull(2).orEmpty(),
            replaceFirst = parts.size > 3,
            separatorCount = parts.size - 1,
        )
    }
}
