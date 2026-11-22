package app.badcat.engine

/**
 * Bounded implementation of Legado ElementsSingle list indexing for default-mode
 * CSS/JSoup collection and chained scalar rules. Explicit @CSS: remains direct CSS.
 */
internal object LegadoElementIndex {

    internal data class ParsedRule(
        val selector: String,
        val exclude: Boolean,
        val terms: List<IndexTerm>,
    )

    internal sealed interface IndexTerm {
        data class Single(val value: Int) : IndexTerm
        data class Range(val start: Int?, val end: Int?, val step: Int) : IndexTerm
    }

    /** Returns null when the expression is ordinary CSS, including attribute selectors. */
    fun parse(expression: String): ParsedRule? {
        val rule = expression.trim()
        if (rule.isEmpty()) return null

        if (rule.endsWith(']')) {
            val open = rule.lastIndexOf('[')
            if (open <= 0) return null
            val raw = rule.substring(open + 1, rule.length - 1).trim()
            val exclude = raw.startsWith('!')
            val body = if (exclude) raw.drop(1).trim() else raw
            val terms = parseTerms(body) ?: return null
            return ParsedRule(rule.substring(0, open).trim(), exclude, terms)
                .takeIf { it.selector.isNotEmpty() }
        }

        // Legacy suffixes: selector.0, selector.-1, selector.1:4:2,
        // selector!0:3. Only numeric suffixes are recognized, so CSS classes
        // and pseudo-classes remain intact.
        val delimiter = rule.indexOfLast { it == '.' || it == '!' }
        if (delimiter <= 0) return null
        val suffix = rule.substring(delimiter + 1).trim()
        val exclude = rule[delimiter] == '!'
        // Upstream ElementsSingle's legacy form uses ':' as a list separator,
        // not as a range operator: tag.div!0:1:2 excludes indexes 0, 1 and 2.
        // Ranges are expressed by the newer bracket form, e.g. tag.div[0:8:2].
        val terms = suffix.split(':').map { token ->
            val index = token.trim().toIntOrNull() ?: return null
            IndexTerm.Single(index)
        }
        if (terms.isEmpty()) return null
        return ParsedRule(
            selector = rule.substring(0, delimiter).trim(),
            exclude = exclude,
            terms = terms,
        ).takeIf { it.selector.isNotEmpty() }
    }

    private fun parseTerms(value: String): List<IndexTerm>? {
        if (value.isBlank()) return null
        val parts = value.split(',')
        if (parts.any(String::isBlank)) return null
        return parts.map { token ->
            val fields = token.trim().split(':')
            when (fields.size) {
                1 -> IndexTerm.Single(fields[0].trim().toIntOrNull() ?: return null)
                2, 3 -> {
                    val start = fields[0].trim().takeIf(String::isNotEmpty)?.toIntOrNull()
                        ?: if (fields[0].isBlank()) null else return null
                    val end = fields[1].trim().takeIf(String::isNotEmpty)?.toIntOrNull()
                        ?: if (fields[1].isBlank()) null else return null
                    val step = if (fields.size == 3) {
                        fields[2].trim().toIntOrNull() ?: return null
                    } else 1
                    if (step == 0) return null
                    IndexTerm.Range(start, end, step)
                }
                else -> return null
            }
        }
    }

    fun <T> apply(items: List<T>, rule: ParsedRule): List<T> {
        if (items.isEmpty()) return emptyList()
        val indexes = linkedSetOf<Int>()
        val size = items.size

        for (term in rule.terms) {
            when (term) {
                is IndexTerm.Single -> normalize(term.value, size)?.let(indexes::add)
                is IndexTerm.Range -> {
                    var start = term.start?.let { normalizeBoundary(it, size) } ?: 0
                    var end = term.end?.let { normalizeBoundary(it, size) } ?: (size - 1)
                    if ((start < 0 && end < 0) || (start >= size && end >= size)) continue
                    start = start.coerceIn(0, size - 1)
                    end = end.coerceIn(0, size - 1)
                    if (start == end || term.step >= size) {
                        indexes += start
                        continue
                    }
                    // Mirrors ElementsSingle's negative-step normalization.
                    val step = when {
                        term.step > 0 -> term.step
                        -term.step < size -> term.step + size
                        else -> 1
                    }.coerceAtLeast(1)
                    if (start <= end) {
                        var index = start
                        while (index <= end) {
                            indexes += index
                            if (end - index < step) break
                            index += step
                        }
                    } else {
                        var index = start
                        while (index >= end) {
                            indexes += index
                            if (index - end < step) break
                            index -= step
                        }
                    }
                }
            }
        }

        return if (rule.exclude) {
            items.filterIndexed { index, _ -> index !in indexes }
        } else {
            indexes.mapNotNull(items::getOrNull)
        }
    }

    private fun normalize(value: Int, size: Int): Int? {
        val index = if (value < 0) size + value else value
        return index.takeIf { it in 0 until size }
    }

    private fun normalizeBoundary(value: Int, size: Int): Int =
        if (value < 0) size + value else value
}
