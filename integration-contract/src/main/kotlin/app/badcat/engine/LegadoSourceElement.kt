package app.badcat.engine

import org.jsoup.Jsoup

/**
 * Narrow JavaScript-facing wrapper for the Legado java.getElement helper.
 * It exposes parsed element text/HTML/attributes, never the underlying document
 * or network-capable DOM implementation.
 */
class LegadoSourceElement(private val fragment: String) {
    fun text(): String = Jsoup.parseBodyFragment(fragment).body().text()
    fun html(): String = Jsoup.parseBodyFragment(fragment).body().html()
    fun attr(name: String): String = Jsoup.parseBodyFragment(fragment).body().selectFirst("*")?.attr(name).orEmpty()
    override fun toString(): String = fragment
}
