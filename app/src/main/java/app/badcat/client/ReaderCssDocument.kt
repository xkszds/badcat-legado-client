package app.badcat.client

import android.content.Context
import org.json.JSONArray

/** Builds a local, script-free reader document and applies bounded user CSS profiles. */
internal object ReaderCssDocument {
    private val allowedProperties = setOf(
        "color", "background-color", "font-size", "font-weight", "font-style",
        "font-family", "line-height", "letter-spacing", "text-align", "text-indent",
        "margin", "margin-top", "margin-bottom", "padding", "padding-top",
        "padding-bottom", "border", "border-bottom", "border-radius",
        "white-space", "word-break", "overflow-wrap", "opacity",
    )

    fun build(context: Context, text: String, textSizeSp: Float, lineSpacing: Float,
              background: Int, foreground: Int): String {
        val css = activeCss(context)
        val paragraphs = text.replace("\r\n", "\n").replace('\r', '\n')
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString("") { "<p class=\"reader-paragraph\">${escape(it)}</p>" }
        val bg = String.format("#%06X", 0xFFFFFF and background)
        val fg = String.format("#%06X", 0xFFFFFF and foreground)
        return """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
            <style>
            html,body{margin:0;padding:0;background:$bg;color:$fg}
            body{font-size:${textSizeSp * context.resources.displayMetrics.scaledDensity}px;line-height:$lineSpacing;font-family:sans-serif}
            .reader-root{padding:16px 20px 28px;overflow-wrap:anywhere}
            .reader-paragraph{margin:0 0 1.15em 0;text-indent:2em;white-space:normal}
            $css
            </style></head><body><main class="reader-root"><article class="reader-content">$paragraphs</article></main></body></html>
        """.trimIndent()
    }

    fun preview(context: Context, selectorRaw: String, declarationsRaw: String): String =
        previewRules(context, listOf(selectorRaw to declarationsRaw))

    fun previewRules(context: Context, rules: List<Pair<String, String>>): String {
        val css = buildString {
            rules.take(120).forEach { (selectorRaw, declarationsRaw) ->
                val selector = sanitizeSelector(selectorRaw) ?: return@forEach
                val declarations = sanitizeDeclarations(declarationsRaw)
                if (declarations.isEmpty()) return@forEach
                append(scopeSelector(selector)).append('{').append(declarations).append("}\n")
            }
        }
        val bg = String.format("#%06X", 0xFFFFFF and android.graphics.Color.WHITE)
        val fg = String.format("#%06X", 0xFFFFFF and android.graphics.Color.rgb(35, 35, 35))
        val previewFontSize = 18f * context.resources.displayMetrics.scaledDensity
        return """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
            html,body{margin:0;padding:0;background:$bg;color:$fg}
            body{font-size:${previewFontSize}px;line-height:1.7;font-family:sans-serif}
            .reader-root{padding:16px 20px}
            .reader-paragraph{margin:0 0 1em;text-indent:2em}
            $css
            </style></head><body><main class="reader-root"><article class="reader-content">
            <p class="reader-paragraph">这是阅读页面样式预览。你可以观察字体、颜色、行距和段落效果。</p>
            <p class="reader-paragraph">第二段示例文字，用于测试 nth-child 选择器和局部样式。</p>
            <p class="reader-paragraph">第三段示例文字，方便比较不同段落的显示差异。</p>
            </article></main></body></html>
        """.trimIndent()
    }

    private fun activeCss(context: Context): String = runCatching {
        val prefs = context.getSharedPreferences("reader_css_profiles", Context.MODE_PRIVATE)
        val profiles = JSONArray(prefs.getString("profiles_v1", "[]"))
        buildString {
            var count = 0
            for (i in 0 until profiles.length()) {
                val profile = profiles.optJSONObject(i) ?: continue
                if (!profile.optBoolean("enabled", false)) continue
                val rules = profile.optJSONArray("rules") ?: continue
                for (j in 0 until rules.length()) {
                    if (count >= 120) return@buildString
                    val rule = rules.optJSONObject(j) ?: continue
                    val selector = sanitizeSelector(rule.optString("selector")) ?: continue
                    val declarations = sanitizeDeclarations(rule.optString("css"))
                    if (declarations.isNotEmpty()) {
                        append(scopeSelector(selector)).append('{').append(declarations).append("}\n")
                        count++
                    }
                }
            }
        }
    }.getOrDefault("")

    private fun scopeSelector(selector: String): String {
        // Scope each top-level selector independently. A comma inside :not(...)
        // or an attribute value is not a selector-group separator.
        return splitSelectorGroups(selector).joinToString(", ") { group ->
            val value = group.trim()
            val rootPrefix = ".reader-root"
            val isRootSelector = value.startsWith(rootPrefix) &&
                (value.length == rootPrefix.length ||
                    value[rootPrefix.length] in charArrayOf(' ', '.', '#', ':', '[', '>', '+', '~'))
            if (isRootSelector) value else "$rootPrefix $value"
        }
    }

    private fun splitSelectorGroups(selector: String): List<String> {
        val groups = mutableListOf<String>()
        var start = 0
        var parentheses = 0
        var brackets = 0
        var quote: Char? = null
        selector.forEachIndexed { index, char ->
            if (quote != null) {
                if (char == quote && (index == 0 || selector[index - 1] != '\\')) quote = null
            } else {
                when (char) {
                    '\\'', '"' -> quote = char
                    '(' -> parentheses++
                    ')' -> parentheses--
                    '[' -> brackets++
                    ']' -> brackets--
                    ',' -> if (parentheses == 0 && brackets == 0) {
                        groups += selector.substring(start, index)
                        start = index + 1
                    }
                }
            }
        }
        groups += selector.substring(start)
        return groups
    }
    private fun sanitizeSelector(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || value.length > 240) return null
        if (value.contains('<') || value.contains("</")) return null
        if (!value.matches(Regex("""[A-Za-z0-9_#.*,\s>+~:()\[\]='\-]+"""))) return null
        if (value.contains("url", true) || value.contains("expression", true) ||
            value.contains("@") || value.contains("{") || value.contains("}")) return null
        return value
    }

    private fun sanitizeDeclarations(raw: String): String {
        if (raw.length > 4000) return ""
        return raw.split(';').mapNotNull { declaration ->
            val colon = declaration.indexOf(':')
            if (colon <= 0) return@mapNotNull null
            val property = declaration.substring(0, colon).trim().lowercase()
            val value = declaration.substring(colon + 1).trim()
            if (property !in allowedProperties || value.isEmpty() || value.length > 160) return@mapNotNull null
            if (!value.matches(Regex("""[A-Za-z0-9_#.*,\s>+~:()\[\]='\-]+"""))) return@mapNotNull null
            if (value.contains("url", true) || value.contains("expression", true) ||
                value.contains("javascript", true) || value.contains("important", true)) return@mapNotNull null
            "$property:$value"
        }.joinToString(";")
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
        .replace("'", "&#39;")
}
