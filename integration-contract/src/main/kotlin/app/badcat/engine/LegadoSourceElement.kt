package app.badcat.engine

import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * Safe JavaScript-facing wrapper for Legado's element helpers. Only parsed
 * text, HTML, attributes and descendant selection are exposed.
 */
class LegadoSourceElement(private val fragments: List<String>) {
    constructor(fragment: String) : this(listOf(fragment))

    private fun elements(): Elements {
        val result = Elements()
        fragments.forEach { fragment ->
            val body = Jsoup.parseBodyFragment(fragment).body()
            if (body.children().isEmpty()) result.add(body) else result.addAll(body.children())
        }
        return result
    }

    fun text(): String = elements().text()
    fun html(): String = elements().html()
    fun attr(name: String): String = elements().firstOrNull()?.attr(name).orEmpty()
    fun select(selector: String): LegadoSourceElement =
        LegadoSourceElement(elements().select(selector).map { it.outerHtml() })
    fun size(): Int = elements().size
    fun getLength(): Int = size()
    fun get(index: Int): LegadoSourceElement = LegadoSourceElement(elements().get(index).outerHtml())
    fun toArray(): Array<LegadoSourceElement> = elements().map { LegadoSourceElement(it.outerHtml()) }.toTypedArray()
    override fun toString(): String = elements().joinToString("") { it.outerHtml() }
}

/** Java-list-compatible collection with both .size() and .length/index access. */
class LegadoSourceElementList(private val items: List<LegadoSourceElement>) : Iterable<LegadoSourceElement> {
    fun size(): Int = items.size
    fun getLength(): Int = items.size
    fun get(index: Int): LegadoSourceElement = items[index]
    fun toArray(): Array<LegadoSourceElement> = items.toTypedArray()
    override fun iterator(): Iterator<LegadoSourceElement> = items.iterator()
}
