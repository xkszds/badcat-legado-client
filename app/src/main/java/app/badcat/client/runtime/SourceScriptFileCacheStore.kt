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
    /** Implementations must preserve arbitrary bytes; UTF-8 conversion is not lossless. */
    fun getBytes(sourceId: String, key: String): ByteArray?
    fun putBytes(sourceId: String, key: String, value: ByteArray)
}

/** JVM fallback for tests; production uses the app-private disk implementation. */
object InMemorySourceScriptFileCacheStore : SourceScriptFileCacheStore {
    private val values = ConcurrentHashMap<String, ByteArray>()
    override fun get(sourceId: String, key: String): String? =
        getBytes(sourceId, key)?.let { String(it, Charsets.UTF_8) }
    override fun getBytes(sourceId: String, key: String): ByteArray? =
        values[storageKey(sourceId, key)]?.copyOf()
    override fun put(sourceId: String, key: String, value: String) {
        putBytes(sourceId, key, value.toByteArray(Charsets.UTF_8))
    }
    override fun putBytes(sourceId: String, key: String, value: ByteArray) {
        values[storageKey(sourceId, key)] = value.copyOf()
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
    override fun get(sourceId: String, key: String): String? =
        getBytes(sourceId, key)?.let { String(it, Charsets.UTF_8) }

    @Synchronized
    override fun getBytes(sourceId: String, key: String): ByteArray? {
        val target = AtomicFile(cacheFile(sourceId, key))
        return runCatching {
            // openRead performs AtomicFile backup recovery; checking baseFile first
            // would incorrectly hide a valid .bak after an interrupted write.
            target.openRead().use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_CACHE_BYTES) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }.getOrNull()
    }

    @Synchronized
    override fun put(sourceId: String, key: String, value: String) =
        putBytes(sourceId, key, value.toByteArray(Charsets.UTF_8))

    @Synchronized
    override fun putBytes(sourceId: String, key: String, value: ByteArray) {
        val encoded = value
        require(encoded.size <= MAX_CACHE_BYTES) { "cache file value exceeds the 50 MiB limit" }
        val target = AtomicFile(cacheFile(sourceId, key))
        val output = target.startWrite()
        try {
            output.write(encoded)
            output.flush()
            target.finishWrite(output)
            prune(target.baseFile)
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

    private fun prune(protectedFile: File) {
        val files = directory.listFiles()?.filter { it.extension == "cache" } ?: return
        pruneCacheFiles(files, MAX_CACHE_BYTES, protectedFile)
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val CACHE_DIRECTORY = "legado_source_file_cache"
        const val MAX_CACHE_BYTES = 50L * 1024L * 1024L
    }
}
