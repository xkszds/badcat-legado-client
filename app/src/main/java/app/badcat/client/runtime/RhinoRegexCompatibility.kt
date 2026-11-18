package app.badcat.client.runtime

/**
 * Adapts legacy JavaScript RegExp literals to the ICU-backed regular-expression
 * implementation used by the pinned HtmlUnit Rhino fork on Android.
 *
 * ECMAScript permits an unmatched closing brace in a regular-expression
 * literal as a legacy literal character. ICU rejects it, so escape braces
 * unless they form a valid quantifier. Strings, comments and character classes
 * are left untouched.
 */
internal object RhinoRegexCompatibility {
    private val expressionPrefixKeywords = setOf(
        "await", "case", "delete", "do", "else", "in", "instanceof",
        "new", "of", "return", "throw", "typeof", "void", "yield",
    )

    fun normalize(source: String): String {
        val output = StringBuilder(source.length + 16)
        var index = 0
        var canStartRegex = true
        while (index < source.length) {
            val char = source[index]
            when {
                char == '\'' || char == '"' || char == '`' -> {
                    index = copyQuoted(source, index, char, output)
                    canStartRegex = false
                }
                char == '/' && source.getOrNull(index + 1) == '/' -> {
                    index = copyLineComment(source, index, output)
                }
                char == '/' && source.getOrNull(index + 1) == '*' -> {
                    index = copyBlockComment(source, index, output)
                }
                char == '/' && canStartRegex -> {
                    val end = findRegexEnd(source, index + 1)
                    if (end < 0) {
                        output.append(char)
                        index++
                        canStartRegex = true
                    } else {
                        output.append('/')
                        output.append(normalizePattern(source.substring(index + 1, end)))
                        output.append('/')
                        index = end + 1
                        while (index < source.length && source[index].isLetter()) {
                            output.append(source[index++])
                        }
                        canStartRegex = false
                    }
                }
                char.isLetter() || char == '_' || char == '$' -> {
                    val start = index++
                    while (index < source.length &&
                        (source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '$')
                    ) index++
                    val word = source.substring(start, index)
                    output.append(word)
                    canStartRegex = word in expressionPrefixKeywords
                }
                char.isDigit() -> {
                    output.append(char)
                    index++
                    while (index < source.length &&
                        (source[index].isLetterOrDigit() || source[index] == '.' || source[index] == '_')
                    ) output.append(source[index++])
                    canStartRegex = false
                }
                char.isWhitespace() -> {
                    output.append(char)
                    index++
                }
                else -> {
                    output.append(char)
                    when (char) {
                        ')', ']', '}', '.' -> canStartRegex = false
                        '+', '-' -> {
                            val doubled = source.getOrNull(index + 1) == char
                            if (doubled) {
                                output.append(char)
                                index++
                                canStartRegex = false
                            } else canStartRegex = true
                        }
                        else -> canStartRegex = char in "([{,:;?=!*%&|^~<>"
                    }
                    index++
                }
            }
        }
        return output.toString()
    }

    private fun copyQuoted(source: String, start: Int, quote: Char, output: StringBuilder): Int {
        var index = start
        output.append(source[index++])
        var escaped = false
        while (index < source.length) {
            val char = source[index++]
            output.append(char)
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == quote -> break
            }
        }
        return index
    }

    private fun copyLineComment(source: String, start: Int, output: StringBuilder): Int {
        var index = start
        while (index < source.length) {
            val char = source[index++]
            output.append(char)
            if (char == '\n' || char == '\r') break
        }
        return index
    }

    private fun copyBlockComment(source: String, start: Int, output: StringBuilder): Int {
        var index = start
        while (index < source.length) {
            val char = source[index++]
            output.append(char)
            if (char == '*' && source.getOrNull(index) == '/') {
                output.append('/')
                return index + 1
            }
        }
        return index
    }

    private fun findRegexEnd(source: String, start: Int): Int {
        var index = start
        var escaped = false
        var inCharacterClass = false
        while (index < source.length) {
            val char = source[index]
            when {
                escaped -> escaped = false
                char == '\\' -> escaped = true
                char == '[' -> inCharacterClass = true
                char == ']' -> inCharacterClass = false
                char == '/' && !inCharacterClass -> return index
                char == '\n' || char == '\r' -> return -1
            }
            index++
        }
        return -1
    }

    private fun normalizePattern(pattern: String): String {
        val output = StringBuilder(pattern.length + 8)
        var index = 0
        var inCharacterClass = false
        while (index < pattern.length) {
            val char = pattern[index]
            when {
                char == '\\' -> {
                    val escaped = pattern.getOrNull(index + 1)
                    when {
                        escaped == null -> {
                            output.append(char)
                            index++
                        }
                        isLegacyIdentityEscape(escaped) -> {
                            // ECMAScript non-Unicode regexes treat unknown escapes such as
                            // \\い and \\く as the escaped character itself. ICU rejects
                            // those escapes, so drop only the redundant backslash.
                            output.append(escaped)
                            index += 2
                        }
                        else -> {
                            output.append(char).append(escaped)
                            index += 2
                        }
                    }
                }
                char == '[' -> { inCharacterClass = true; output.append(char); index++ }
                char == ']' -> { inCharacterClass = false; output.append(char); index++ }
                !inCharacterClass && char == '{' -> {
                    val end = findQuantifierEnd(pattern, index)
                    if (end >= 0) { output.append(pattern, index, end + 1); index = end + 1 }
                    else { output.append("\\{"); index++ }
                }
                !inCharacterClass && char == '}' -> { output.append("\\}"); index++ }
                else -> { output.append(char); index++ }
            }
        }
        return output.toString()
    }

    /**
     * Returns true for ECMAScript Annex B identity escapes that ICU does not
     * accept as escapes. Syntax characters and standard RegExp escapes remain
     * escaped; legacy unknown escapes become literal characters.
     */
    private fun isLegacyIdentityEscape(char: Char): Boolean {
        val syntaxCharacters = "^$\\\\.*+?()[]{}|/"
        val standardEscapes = "dDsSwWbBfnrtv0cxuUpP01234567"
        return char !in syntaxCharacters && char !in standardEscapes
    }

    private fun findQuantifierEnd(pattern: String, start: Int): Int {
        var index = start + 1
        val digitsStart = index
        while (index < pattern.length && pattern[index].isDigit()) index++
        if (index == digitsStart) return -1
        if (pattern.getOrNull(index) == ',') {
            index++
            while (index < pattern.length && pattern[index].isDigit()) index++
        }
        return if (pattern.getOrNull(index) == '}') index else -1 ?: -1
    }
}