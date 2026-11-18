package app.badcat.client.runtime

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Source-isolated file cache used by Legado cache.putFile/getFile. */
interface SourceScriptFileCacheStore {
    fun get(sourceId: String, key: String): String?
    fun put(sourceId: String, key: String, value: String)
    fun remove(sourceId: String, key: String)
    fun clearSource(sourceId: String)
}

/** JVM fallback for tests; production uses the app-private disk implementation. */
object InMemorySourceScriptFileCacheStore : SourceScriptFileCacheStore {
    private val values = ConcurrentHashMap<String, String>()
    override fun get(sourceId: String, key: String): String? = values[storageKey(sourceId, key)]
    override fun put(sourceId: String, key: String, value: String) {
        values[storageKey(sourceId, key)] = value
    }
    override fun remove(sourceId: String, key: String) {
        values.remove(storageKey(sourceId, key))
    }
    override fun clearSource(sourceId: String) {
        val prefix = "$sourceId\n"
        values.keys.removeAll { it.startsWith(prefix) }
    }
    private fun storageKey(sourceId: String, key: String) = "$sourceId\n$key"
}

/**
 * App-private, atomic file cache with a global 50 MiB budget. File names are
 * SHA-256 digests, so untrusted source IDs and cache keys never become paths.
 */
class FileSourceScriptCacheStore(context: Context) : SourceScriptFileCacheStore {
    private val directory = File(context.applicationContext.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }

    @Synchronized
    override fun get(sourceId: String, key: String): String? {
        val target = AtomicFile(cacheFile(sourceId, key))
        if (!target.baseFile.isFile || target.baseFile.length() > MAX_CACHE_BYTES) return null
        return runCatching {
            target.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    @Synchronized
    override fun put(sourceId: String, key: String, value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        require(encoded.size <= MAX_CACHE_BYTES) { "cache file value exceeds the 50 MiB limit" }
        val target = AtomicFile(cacheFile(sourceId, key))
        val output = target.startWrite()
        try {
            output.write(encoded)
            output.flush()
            target.finishWrite(output)
            prune()
        } catch (error: Exception) {
            target.failWrite(output)
            throw error
        }
    }

    @Synchronized
    override fun remove(sourceId: String, key: String) {
        AtomicFile(cacheFile(sourceId, key)).delete()
    }

    @Synchronized
    override fun clearSource(sourceId: String) {
        val prefix = digest(sourceId) + "_"
        directory.listFiles()?.filter { it.isFile && it.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    private fun cacheFile(sourceId: String, key: String): File =
        File(directory, digest(sourceId) + "_" + digest(key) + ".cache")

    private fun prune() {
        val files = directory.listFiles()?.filter { it.isFile && it.extension == "cache" } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val CACHE_DIRECTORY = "legado_source_file_cache"
        const val MAX_CACHE_BYTES = 50L * 1024L * 1024L
    }
}
