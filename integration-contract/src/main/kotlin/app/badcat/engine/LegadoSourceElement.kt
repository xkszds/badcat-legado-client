package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * Safe JavaScript-facing wrapper for Legado's element helpers. Only parsed
 * text, HTML, attributes and descendant selection are exposed.
 */
class LegadoSourceElement(
    private val fragments: List<String>,
    private val baseUri: String = "",
) {
    constructor(fragment: String, baseUri: String = "") : this(listOf(fragment), baseUri)

    private fun elements(): Elements {
        val result = Elements()
        fragments.forEach { fragment ->
            val body = Jsoup.parseBodyFragment(fragment, baseUri).body()
            if (body.children().isEmpty()) {
                if (body.html().isNotBlank()) result.add(body)
            } else result.addAll(body.children())
        }
        return result
    }

    fun text(): String = elements().text()
    fun html(): String = elements().html()
    fun attr(name: String): String = elements().firstOrNull()?.attr(name).orEmpty()
    fun select(selector: String): LegadoSourceElement =
        LegadoSourceElement(elements().select(selector).map { it.outerHtml() }, baseUri)
    fun selectFirst(selector: String): LegadoSourceElement? = select(selector).first()
    fun first(): LegadoSourceElement? = elements().firstOrNull()?.let { LegadoSourceElement(it.outerHtml(), baseUri) }
    fun last(): LegadoSourceElement? = elements().lastOrNull()?.let { LegadoSourceElement(it.outerHtml(), baseUri) }
    fun size(): Int = elements().size
    fun getLength(): Int = size()
    fun get(index: Int): LegadoSourceElement = LegadoSourceElement(elements().get(index).outerHtml(), baseUri)
    fun toArray(): Array<LegadoSourceElement> =
        elements().map { LegadoSourceElement(it.outerHtml(), baseUri) }.toTypedArray()
    fun ownText(): String = elements().firstOrNull()?.ownText().orEmpty()
    fun tagName(): String = elements().firstOrNull()?.tagName().orEmpty()
    fun className(): String = elements().firstOrNull()?.className().orEmpty()
    fun id(): String = elements().firstOrNull()?.id().orEmpty()
    fun hasClass(name: String): Boolean = elements().firstOrNull()?.hasClass(name) ?: false
    fun absUrl(attribute: String): String = elements().firstOrNull()?.absUrl(attribute).orEmpty()
    fun children(): LegadoSourceElementList = LegadoSourceElementList(
        elements().flatMap { element -> element.children().map { child -> LegadoSourceElement(child.outerHtml(), baseUri) } },
    )
    override fun toString(): String = elements().joinToString("") { it.outerHtml() }
}

/** Java-list-compatible collection with .size(), .get(), .toArray() and indexed access. */
class LegadoSourceElementList(private val items: List<LegadoSourceElement>) :
    java.util.AbstractList<LegadoSourceElement>() {
    override val size: Int
        get() = items.size

    override fun get(index: Int): LegadoSourceElement = items[index]
    fun first(): LegadoSourceElement? = items.firstOrNull()
    fun last(): LegadoSourceElement? = items.lastOrNull()
    fun getLength(): Int = size
}
