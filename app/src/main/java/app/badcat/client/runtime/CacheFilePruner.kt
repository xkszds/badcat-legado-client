package app.badcat.client.runtime

import java.io.File

/**
 * Evicts least-recently-used cache files without deleting the file that was
 * just committed. Filesystem timestamp resolution can make a new file tie
 * with older entries, so the protected target must never rely on sort order.
 */
internal fun pruneCacheFiles(
    files: Iterable<File>,
    maxBytes: Long,
    protectedFile: File? = null,
) {
    require(maxBytes >= 0) { "Cache budget must not be negative" }
    val cacheFiles = files.filter { it.isFile }
    var total = cacheFiles.sumOf { it.length() }
    if (total <= maxBytes) return

    for (file in cacheFiles.asSequence()
        .filterNot { it == protectedFile }
        .sortedBy { it.lastModified() }
    ) {
        if (total <= maxBytes) break
        val size = file.length()
        if (file.delete()) total -= size
    }
}
