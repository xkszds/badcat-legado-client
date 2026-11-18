package app.badcat.client.runtime

import androidx.annotation.Keep
import java.io.File
import java.nio.charset.Charset

/**
 * Narrow Rhino-visible file facade. The backing java.io.File is never returned
 * to JavaScript because the Rhino class shutter deliberately blocks File.
 */
@Keep
class RhinoSourceFileHandle internal constructor(
    private val file: File,
    private val safeRoot: File,
) {
    private fun checked(): File {
        val resolved = file.canonicalFile
        val prefix = safeRoot.path + File.separator
        if (resolved.path != safeRoot.path && !resolved.path.startsWith(prefix)) {
            throw SecurityException("Illegal source file path")
        }
        return file
    }

    val name: String get() = checked().name
    val path: String get() = checked().path
    val absolutePath: String get() = checked().absolutePath
    val parent: String? get() = checked().parent
    val parentFile: RhinoSourceFileHandle?
        get() = checked().parentFile?.let { runCatching { RhinoSourceFileHandle(it, safeRoot) }.getOrNull() }
    val isFile: Boolean get() = checked().isFile
    val isDirectory: Boolean get() = checked().isDirectory
    val isAbsolute: Boolean get() = checked().isAbsolute

    fun exists(): Boolean = checked().exists()
    fun length(): Long = checked().length()
    fun lastModified(): Long = checked().lastModified()
    fun canRead(): Boolean = checked().canRead()
    fun canWrite(): Boolean = checked().canWrite()
    fun isHidden(): Boolean = checked().isHidden
    fun mkdir(): Boolean = checked().mkdir()
    fun mkdirs(): Boolean = checked().mkdirs()
    fun createNewFile(): Boolean = checked().createNewFile()
    fun delete(): Boolean = checked().delete()
    fun deleteRecursively(): Boolean = checked().deleteRecursively()
    fun setLastModified(time: Long): Boolean = checked().setLastModified(time)

    fun listFiles(): Array<RhinoSourceFileHandle>? =
        checked().listFiles()?.mapNotNull { child ->
            runCatching { RhinoSourceFileHandle(child, safeRoot) }.getOrNull()
        }?.toTypedArray()

    fun readBytes(): ByteArray = checked().readBytes()
    fun readText(): String = checked().readText(Charsets.UTF_8)
    fun readText(charsetName: String): String = checked().readText(Charset.forName(charsetName))
    fun writeBytes(bytes: ByteArray) = checked().writeBytes(bytes)
    fun writeText(text: String) = checked().writeText(text, Charsets.UTF_8)
    fun writeText(text: String, charsetName: String) = checked().writeText(text, Charset.forName(charsetName))
    fun appendText(text: String) = checked().appendText(text, Charsets.UTF_8)
    fun appendText(text: String, charsetName: String) = checked().appendText(text, Charset.forName(charsetName))

    override fun toString(): String = path
}
