package app.badcat.engine

/**
 * HTTP fragments are client-side only and are never part of the resource
 * requested from the server. Ignore them when detecting pagination aliases.
 */
internal fun paginationUrlIdentity(url: String): String = url.substringBefore('#')
