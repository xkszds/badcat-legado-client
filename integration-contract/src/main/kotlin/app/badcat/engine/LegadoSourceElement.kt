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
    private val selectionContext: List<String>? = null,
    private val selectionIndex: Int = 0,
    private val selectionParents: List<String?>? = null,
    private val selectionAncestors: List<List<String>>? = null,
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
    fun hasAttr(name: String): Boolean = elements().firstOrNull()?.hasAttr(name) ?: false
    fun `val`(): String = elements().firstOrNull()?.`val`().orEmpty()
    fun data(): String = elements().firstOrNull()?.data().orEmpty()
    fun select(selector: String): LegadoSourceElement {
        val selected = elements().select(selector)
        val html = selected.map { it.outerHtml() }
        return LegadoSourceElement(
            fragments = html,
            baseUri = baseUri,
            selectionContext = html,
            selectionParents = selected.map { it.parents().firstOrNull()?.outerHtml() },
            selectionAncestors = selected.map { element -> element.parents().map { it.outerHtml() } },
        )
    }
    fun selectFirst(selector: String): LegadoSourceElement? = select(selector).first()
    fun first(): LegadoSourceElement? = if (contextHtml().isEmpty()) null else selectedElement(0)
    fun last(): LegadoSourceElement? = if (contextHtml().isEmpty()) null else selectedElement(contextHtml().lastIndex)
    fun parent(): LegadoSourceElement? = selectionParents?.getOrNull(selectionIndex)?.let { LegadoSourceElement(it, baseUri) }
        ?: elements().firstOrNull()?.parents()?.firstOrNull()?.let { LegadoSourceElement(it.outerHtml(), baseUri) }
    fun parents(): LegadoSourceElementList = LegadoSourceElementList(
        selectionAncestors?.getOrNull(selectionIndex)?.map { LegadoSourceElement(it, baseUri) }
            ?: elements().firstOrNull()?.parents()?.map { LegadoSourceElement(it.outerHtml(), baseUri) }.orEmpty(),
    )
    fun next(): LegadoSourceElement? = selectedElement(selectionIndex + 1)
    fun prev(): LegadoSourceElement? = selectedElement(selectionIndex - 1)
    fun siblings(): LegadoSourceElementList = LegadoSourceElementList(
        contextHtml().indices
            .filter { it != selectionIndex }
            .mapNotNull { index -> selectedElement(index) },
    )
    fun size(): Int = elements().size
    fun getLength(): Int = size()
    fun get(index: Int): LegadoSourceElement = selectedElement(index)
    fun toArray(): Array<LegadoSourceElement> =
        contextHtml().indices.mapNotNull(::selectedElement).toTypedArray()

    private fun contextHtml(): List<String> =
        selectionContext ?: elements().map { it.outerHtml() }

    private fun contextParents(): List<String?> =
        selectionParents ?: elements().map { it.parents().firstOrNull()?.outerHtml() }

    private fun contextAncestors(): List<List<String>> =
        selectionAncestors ?: elements().map { element -> element.parents().map { it.outerHtml() } }

    private fun selectedElement(index: Int): LegadoSourceElement? {
        val context = contextHtml()
        val html = context.getOrNull(index) ?: return null
        return LegadoSourceElement(
            fragments = listOf(html),
            baseUri = baseUri,
            selectionContext = context,
            selectionIndex = index,
            selectionParents = contextParents(),
            selectionAncestors = contextAncestors(),
        )
    }
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
