package app.badcat.client.runtime

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import javax.crypto.Mac
import java.text.SimpleDateFormat
import java.util.Date
import java.util.SimpleTimeZone
import java.util.Locale
import java.util.UUID
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.github.liuyueyi.quick.transfer.ChineseUtils as QuickChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType
import app.badcat.engine.LegadoSourceElement
import app.badcat.engine.LegadoSourceElementList
import app.badcat.engine.SourceHttpResponse
import org.htmlunit.corejs.javascript.Scriptable
import org.htmlunit.corejs.javascript.ScriptableObject
import org.htmlunit.corejs.javascript.Context
import org.htmlunit.corejs.javascript.Function

/**
 * Explicitly allow-listed subset of Legado's `java` helper for source scripts.
 *
 * This object is deliberately not a general Java bridge: no class lookup,
 * reflection, filesystem, process, Android, network, Cookie or WebView access.
 * The small key/value store is isolated per book source, size-bounded, and backed by the host's storage adapter.
 */
class RhinoSafeSourceJavaExtensions(
    private val sourceId: String,
    private val store: SourceScriptKeyValueStore,
    private val cookieReader: (String) -> String = { "" },
    private val ajaxRequest: (String) -> String = { throw UnsupportedOperationException("java.ajax is not connected to the source HTTP transport") },
    private val httpGet: (String, Map<String, String>) -> SourceHttpResponse = { _, _ -> throw UnsupportedOperationException("java.get HTTP transport is unavailable") },
    private val httpRequest: (String, String, String?, Map<String, String>) -> SourceHttpResponse = { method, url, body, headers ->
        if (method.equals("GET", true) && body == null) httpGet(url, headers)
        else throw UnsupportedOperationException("java.$method HTTP transport is unavailable")
    },
    private val httpRequestWithTimeout: (String, String, String?, Map<String, String>, Long?) -> SourceHttpResponse = { method, url, body, headers, timeout ->
        require(timeout == null) { "Per-call timeout is not connected to the source HTTP transport" }
        httpRequest(method, url, body, headers)
    },
    private val ruleStringReader: (String) -> String = { "" },
    private val ruleStringReaderWithContent: (String, String) -> String = { rule, _ -> ruleStringReader(rule) },
    private val ruleStringListReader: (String) -> List<String> = { emptyList() },
    private val ruleStringListReaderWithContent: (String, String) -> List<String> = { rule, _ -> ruleStringListReader(rule) },
    private val ruleElementReader: (String) -> LegadoSourceElement? = { null },
    private val ruleElementReaderWithContent: (String, String) -> LegadoSourceElement? = { rule, _ -> ruleElementReader(rule) },
    private val ruleElementsReader: (String) -> LegadoSourceElementList = { LegadoSourceElementList(emptyList()) },
    private val ruleElementsReaderWithContent: (String, String) -> LegadoSourceElementList = { rule, _ -> ruleElementsReader(rule) },
    private val contentWriter: (String) -> Unit = {},
    private val baseUrlWriter: (String?) -> Unit = {},
    private val fileStore: SourceScriptFileCacheStore = InMemorySourceScriptFileCacheStore,
    private val webViewUserAgent: String = DEFAULT_WEBVIEW_USER_AGENT,
    private val functionInvoker: (Function) -> Unit = { throw UnsupportedOperationException("source callback invoker is not connected") },
    private val rateLimit: (Boolean) -> Unit = {},
    private val sourceTag: String = sourceId,
    private val sourceDescriptor: Map<String, Any?> = emptyMap(),
    private val archiveEntryReader: (java.io.InputStream, String, Int) -> ByteArray? =
        { input, path, maxBytes -> SourceLibArchiveReader.readEntry(input, path, maxBytes) },
    private val archiveExtractor: (ByteArray, Int, Int, Int) -> Map<String, ByteArray> =
        { bytes, maxEntry, maxTotal, maxEntries ->
            SourceLibArchiveReader.extractEntries(bytes.inputStream(), maxEntry, maxTotal, maxEntries)
        },
) {

    /** Legado source metadata helpers; expose a read-only map rather than the Android entity. */
    fun getTag(): String = sourceTag

    fun getSource(): Map<String, Any?> = sourceDescriptor.toMap()

    /** Evaluate a Legado selector against the current AnalyzeRule content. */
    fun getString(rule: String): String = ruleStringReader(rule)

    /** Legado overload evaluates a selector against an explicitly supplied result fragment. */
    fun getString(rule: String, content: String): String = ruleStringReaderWithContent(rule, content)

    /** Legado AnalyzeRule overload: optionally fetch mContent as a URL before applying the selector. */
    fun getString(rule: String, content: Any?, isUrl: Boolean): String {
        if (content == null) return ruleStringReader(rule)
        val value = content.toString()
        val actualContent = if (isUrl) { rateLimit(false); httpRequest("GET", value, null, emptyMap()).body } else value
        return ruleStringReaderWithContent(rule, actualContent)
    }

    /** Legado's list selector helper, returned as a Java-compatible mutable list. */
    fun getStringList(rule: String): Array<String> = ruleStringListReader(rule).toTypedArray()

    fun getStringList(rule: String, content: String): Array<String> =
        ruleStringListReaderWithContent(rule, content).toTypedArray()

    /** List equivalent of the three-argument AnalyzeRule overload. */
    fun getStringList(rule: String, content: Any?, isUrl: Boolean): Array<String> {
        if (content == null) return ruleStringListReader(rule).toTypedArray()
        val value = content.toString()
        val actualContent = if (isUrl) { rateLimit(false); httpRequest("GET", value, null, emptyMap()).body } else value
        return ruleStringListReaderWithContent(rule, actualContent).toTypedArray()
    }

    /** Legado's element selector helper, exposing only safe text/HTML/attribute methods. */
    fun getElement(rule: String): LegadoSourceElement =
        ruleElementReader(rule) ?: LegadoSourceElement("")

    fun getElement(rule: String, content: String): LegadoSourceElement =
        ruleElementReaderWithContent(rule, content) ?: LegadoSourceElement("")

    /** Legado's multi-element selector helper, supporting size/get/toArray and index access. */
    fun getElements(rule: String): LegadoSourceElementList = ruleElementsReader(rule)

    fun getElements(rule: String, content: String): LegadoSourceElementList =
        ruleElementsReaderWithContent(rule, content)

    /** Parse HTML for legacy source scripts using the pinned JSoup implementation. */
    fun parseHtml(content: String): org.jsoup.nodes.Document = org.jsoup.Jsoup.parse(content)

    /** Parse HTML with an explicit base URI for JSoup absUrl()/relative-link semantics. */
    fun parseHtml(content: String, baseUri: String): org.jsoup.nodes.Document =
        org.jsoup.Jsoup.parse(content, baseUri)

    /** Explicit stored-string accessor for host integrations. */
    fun getStoredString(key: String): String {
        requireValidKey(key)
        return store.get(sourceId, key).orEmpty()
    }

    /** Explicit variable API for values written by java.put/putString. */
    fun getVariable(key: String): String = getStoredString(key)

    fun setContent(content: String): String {
        require(content.length <= MAX_VALUE_CHARS * 512) { "Legado content exceeds the size limit" }
        contentWriter(content)
        return content
    }

    /** Legado overload also changes the base URL used for subsequent relative-link resolution. */
    fun setContent(content: Any?, baseUrl: String?): String {
        val value = content?.toString().orEmpty()
        require(value.length <= MAX_VALUE_CHARS * 512) { "Legado content exceeds the size limit" }
        contentWriter(value)
        if (!baseUrl.isNullOrBlank()) {
            app.badcat.engine.WebUrlResolver.resolve(baseUrl, baseUrl)
            baseUrlWriter(baseUrl)
        }
        return value
    }

    /**
     * Download the original response octets into the source-private cache.
     * The transport supplies raw bytes so images, archives and legacy text
     * encodings are not corrupted by an earlier UTF-8 String conversion.
     */
    fun downloadFile(url: String): String {
        require(url.isNotBlank()) { "downloadFile URL must not be blank" }
        rateLimit(false)
        val response = httpRequest("GET", url, null, emptyMap())
        require(response.statusCode in 200..299) {
            "downloadFile returned HTTP ${response.statusCode} ${response.statusMessage}".trim()
        }
        val bytes = response.bodyBytes ?: response.body.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_DOWNLOAD_BYTES) {
            "downloadFile response exceeds the 8 MiB limit"
        }
        val token = UUID.randomUUID().toString()
        fileStore.putBytes(sourceId, DOWNLOAD_FILE_PREFIX + token, bytes)
        return "downloads/$token.txt"
    }

    /** Legacy hexadecimal-data overload retained for older imported sources. */
    @Deprecated("Use downloadFile(url)", ReplaceWith("downloadFile(url)"))
    fun downloadFile(content: String, url: String): String {
        require(url.isNotBlank()) { "downloadFile URL must not be blank" }
        val extension = runCatching {
            URL(url).path.substringAfterLast('/', "").substringAfterLast('.', "")
        }.getOrDefault("")
        if (extension.isBlank() || !extension.matches(Regex("^[A-Za-z0-9]{1,10}$"))) return ""
        val bytes = decodeHex(content)
        require(bytes.size <= MAX_DOWNLOAD_BYTES) {
            "downloadFile response exceeds the 8 MiB limit"
        }
        val token = UUID.randomUUID().toString()
        fileStore.putBytes(sourceId, DOWNLOAD_FILE_PREFIX + token, bytes)
        return "downloads/$token.$extension"
    }

    /** Read only opaque tokens returned by this source's downloadFile call. */
    fun readTxtFile(path: String): String {
        val bytes = readFile(path) ?: return ""
        return String(bytes, detectTextCharset(bytes))
    }

    /** Decode the original downloaded bytes using the requested source charset. */
    fun readTxtFile(path: String, charsetName: String): String {
        val bytes = readFile(path) ?: return ""
        return String(bytes, java.nio.charset.Charset.forName(charsetName))
    }

    /** Return the original downloaded bytes; null mirrors Legado's missing-file result. */
    fun readFile(path: String): ByteArray? {
        val token = runCatching { downloadToken(path) }.getOrNull()
        if (token != null) return fileStore.getBytes(sourceId, DOWNLOAD_FILE_PREFIX + token)
        return fileStore.getBytes(sourceId, virtualFileKey(normalizeVirtualPath(path)))
    }

    /** Extract an archive into source-private virtual files and return its relative folder. */
    fun unzipFile(zipPath: String): String = extractArchive(zipPath, false)
    fun un7zFile(zipPath: String): String = extractArchive(zipPath, true)
    fun unrarFile(zipPath: String): String = extractArchive(zipPath, true)
    fun unArchiveFile(zipPath: String): String =
        runCatching { extractArchive(zipPath, false) }.getOrElse { extractArchive(zipPath, true) }

    private fun extractArchive(archivePath: String, useLibArchive: Boolean): String {
        if (archivePath.isEmpty()) return ""
        val bytes = readFile(archivePath)
            ?: throw IllegalArgumentException("Archive file does not exist: $archivePath")
        require(bytes.size <= MAX_DOWNLOAD_BYTES) { "Archive exceeds the 8 MiB compressed limit" }
        val archiveName = archivePath.substringAfterLast('/').ifBlank { "archive" }
        val folder = "ArchiveTemp/" + digestFileName(archiveName)
        val entries = if (useLibArchive) {
            archiveExtractor(bytes, MAX_ARCHIVE_ENTRY_BYTES, MAX_ARCHIVE_TOTAL_BYTES.toInt(), MAX_ARCHIVE_FILES)
        } else {
            extractZipEntries(bytes)
        }
        require(entries.size <= MAX_ARCHIVE_FILES) { "Archive contains too many files" }
        var total = 0L
        val safeEntries = linkedMapOf<String, ByteArray>()
        entries.forEach { (rawName, content) ->
            val name = normalizeArchiveEntryPath(rawName) ?: return@forEach
            require(content.size <= MAX_ARCHIVE_ENTRY_BYTES) { "Archive entry exceeds the 16 MiB expanded limit" }
            total += content.size
            require(total <= MAX_ARCHIVE_TOTAL_BYTES) { "Archive exceeds the 64 MiB total expanded limit" }
            safeEntries[name] = content.copyOf()
        }
        require(safeEntries.isNotEmpty()) { "Archive contains no safe regular files" }
        safeEntries.forEach { (name, content) ->
            fileStore.putBytes(sourceId, virtualFileKey("$folder/$name"), content)
        }
        writeDirectoryManifest(folder, safeEntries.keys)
        return folder
    }

    private fun extractZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    require(result.size < MAX_ARCHIVE_FILES) { "Archive contains too many files" }
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var countTotal = 0
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        countTotal += count
                        require(countTotal <= MAX_ARCHIVE_ENTRY_BYTES) {
                            "Archive entry exceeds the 16 MiB expanded limit"
                        }
                        output.write(buffer, 0, count)
                    }
                    result[entry.name] = output.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return result
    }

    private fun normalizeArchiveEntryPath(path: String): String? {
        if (path.isBlank() || path.length > MAX_ARCHIVE_PATH_CHARS) return null
        if (path.startsWith("/") || path.startsWith("\\") ||
            Regex("^[A-Za-z]:").containsMatchIn(path)
        ) return null
        val parts = path.replace('\\', '/').split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null
        if (parts.any { part -> part.any { it.code < 0x20 || it == '\u0000' } }) return null
        return parts.joinToString("/")
    }

    private fun normalizeVirtualPath(path: String): String {
        require(path.isNotBlank() && path.length <= MAX_ARCHIVE_PATH_CHARS)
        require(!path.startsWith("/") && !path.startsWith("\\")) { "Absolute source file paths are not allowed" }
        val normalized = path.replace('\\', '/')
        require(normalized.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "Invalid source file path"
        }
        return normalized
    }

    private fun virtualFileKey(path: String) = VIRTUAL_FILE_PREFIX + path

    private fun writeDirectoryManifest(root: String, paths: Set<String>) {
        paths.forEach { path ->
            val parts = path.split('/')
            val first = parts.first()
            appendDirectoryChild(root, first)
            for (index in 1 until parts.size) {
                appendDirectoryChild(root + "/" + parts.take(index).joinToString("/"), parts[index])
            }
        }
    }

    private fun appendDirectoryChild(directory: String, child: String) {
        val key = VIRTUAL_DIR_PREFIX + directory
        val existing = fileStore.get(sourceId, key)?.split("\n")?.filter(String::isNotEmpty).orEmpty()
        if (child !in existing) {
            fileStore.put(sourceId, key, (existing + child).joinToString("\n"))
        }
    }

    fun getTxtInFolder(path: String): String {
        if (path.isEmpty()) return ""
        val directory = normalizeVirtualPath(path)
        val children = fileStore.get(sourceId, VIRTUAL_DIR_PREFIX + directory)
            ?.split("\n")?.filter(String::isNotEmpty).orEmpty()
        return children.mapNotNull { child ->
            val bytes = fileStore.getBytes(sourceId, virtualFileKey("$directory/$child"))
            bytes?.let { String(it, detectTextCharset(it)) }
        }.joinToString("\\n")
    }

    private fun digestFileName(name: String): String =
        MessageDigest.getInstance("MD5").digest(name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    /**
     * Read one entry from a ZIP supplied by URL or hexadecimal archive data.
     * Both compressed input and expanded entry data are bounded to avoid
     * untrusted source archives exhausting memory.
     */
    fun getZipByteArrayContent(url: String, path: String): ByteArray? {
        require(path.isNotBlank()) { "ZIP entry path must not be blank" }
        val archive = if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
            rateLimit(false)
            val response = httpRequest("GET", url, null, emptyMap())
            require(response.statusCode in 200..299) { "ZIP download returned HTTP ${response.statusCode}" }
            response.bodyBytes ?: response.body.toByteArray(Charsets.UTF_8)
        } else {
            decodeHex(url)
        }
        require(archive.size <= MAX_DOWNLOAD_BYTES) { "ZIP archive exceeds the 8 MiB limit" }
        java.util.zip.ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name == path) {
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_ARCHIVE_ENTRY_BYTES) {
                            "ZIP entry exceeds the 16 MiB expanded limit"
                        }
                        output.write(buffer, 0, count)
                    }
                    return output.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return null
    }

    /** Read a regular-file entry from a RAR archive (URL or hexadecimal bytes). */
    fun getRarByteArrayContent(url: String, path: String): ByteArray? =
        readLibArchiveEntry(url, path)

    /** Read a regular-file entry from a 7z archive (URL or hexadecimal bytes). */
    fun get7zByteArrayContent(url: String, path: String): ByteArray? =
        readLibArchiveEntry(url, path)

    fun getRarStringContent(url: String, path: String): String {
        val bytes = getRarByteArrayContent(url, path) ?: return ""
        return String(bytes, detectTextCharset(bytes))
    }

    fun getRarStringContent(url: String, path: String, charsetName: String): String {
        val bytes = getRarByteArrayContent(url, path) ?: return ""
        return String(bytes, java.nio.charset.Charset.forName(charsetName))
    }

    fun get7zStringContent(url: String, path: String): String {
        val bytes = get7zByteArrayContent(url, path) ?: return ""
        return String(bytes, detectTextCharset(bytes))
    }

    fun get7zStringContent(url: String, path: String, charsetName: String): String {
        val bytes = get7zByteArrayContent(url, path) ?: return ""
        return String(bytes, java.nio.charset.Charset.forName(charsetName))
    }

    private fun readLibArchiveEntry(url: String, path: String): ByteArray? {
        require(path.isNotBlank()) { "Archive entry path must not be blank" }
        val archiveBytes = if (
            url.startsWith("http://", true) || url.startsWith("https://", true)
        ) {
            rateLimit(false)
            val response = httpRequest("GET", url, null, emptyMap())
            require(response.statusCode in 200..299) {
                "Archive download returned HTTP ${response.statusCode} ${response.statusMessage}".trim()
            }
            response.bodyBytes ?: response.body.toByteArray(Charsets.UTF_8)
        } else {
            decodeHex(url)
        }
        require(archiveBytes.size <= MAX_DOWNLOAD_BYTES) {
            "Archive exceeds the 8 MiB compressed limit"
        }
        return archiveBytes.inputStream().use {
            archiveEntryReader(it, path, MAX_ARCHIVE_ENTRY_BYTES)
        }
    }

    fun getZipStringContent(url: String, path: String): String {
        val bytes = getZipByteArrayContent(url, path) ?: return ""
        return String(bytes, detectTextCharset(bytes))
    }

    fun getZipStringContent(url: String, path: String, charsetName: String): String {
        val bytes = getZipByteArrayContent(url, path) ?: return ""
        return String(bytes, java.nio.charset.Charset.forName(charsetName))
    }

    private fun decodeHex(value: String): ByteArray {
        require(value.length % 2 == 0 && value.length <= MAX_DOWNLOAD_BYTES * 2) {
            "Invalid or oversized hexadecimal archive"
        }
        require(value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            "Archive input must be a URL or hexadecimal data"
        }
        return ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun detectTextCharset(bytes: ByteArray): java.nio.charset.Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) return Charsets.UTF_8
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        val utf8 = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
            .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
        return try {
            utf8.decode(java.nio.ByteBuffer.wrap(bytes))
            Charsets.UTF_8
        } catch (_: java.nio.charset.CharacterCodingException) {
            java.nio.charset.Charset.forName("GBK")
        }
    }

    /** Delete only a source-owned downloaded text file token. */
    fun deleteFile(path: String): Boolean {
        val token = runCatching { downloadToken(path) }.getOrNull()
        if (token != null) {
            val key = DOWNLOAD_FILE_PREFIX + token
            val existed = fileStore.getBytes(sourceId, key) != null
            if (existed) fileStore.remove(sourceId, key)
            return existed
        }
        val safePath = normalizeVirtualPath(path)
        return deleteVirtualTree(safePath)
    }

    private fun deleteVirtualTree(path: String): Boolean {
        val fileKey = virtualFileKey(path)
        val fileExists = fileStore.getBytes(sourceId, fileKey) != null
        if (fileExists) fileStore.remove(sourceId, fileKey)
        val manifestKey = VIRTUAL_DIR_PREFIX + path
        val children = fileStore.get(sourceId, manifestKey)?.split("\\n")
            ?.filter(String::isNotEmpty).orEmpty()
        var removed = fileExists || children.isNotEmpty()
        children.forEach { child ->
            removed = deleteVirtualTree("$path/$child") || removed
        }
        if (children.isNotEmpty()) fileStore.remove(sourceId, manifestKey)
        return removed
    }

    private fun downloadToken(path: String): String {
        require(path.matches(Regex("^downloads/[0-9a-fA-F-]{36}(?:\\.[A-Za-z0-9]{1,10})?$"))) {
            "Invalid or unsafe Legado download path"
        }
        return path.removePrefix("downloads/").substringBeforeLast(".")
    }

    fun putString(key: String, value: String): String {
        requireValidKey(key)
        require(value.length <= MAX_VALUE_CHARS) { "Legado java.putString value exceeds the size limit" }
        store.put(sourceId, key, value)
        return value
    }

    /**
     * Legado overload: URL-like values perform HTTP GET; simple names read the
     * source-scoped variable written by java.put(). This mirrors AnalyzeRule's
     * historical java.get(key) / java.get(url) dual use.
     */
    fun get(urlOrKey: String): Any =
        if (isHttpTarget(urlOrKey)) {
            rateLimit(false)
            RhinoSourceHttpResponse(httpRequest("GET", urlOrKey, null, emptyMap()))
        } else {
            getStoredString(urlOrKey)
        }

    private fun isHttpTarget(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.isEmpty()) return false
        return candidate.startsWith("http://", true) ||
            candidate.startsWith("https://", true) ||
            candidate.startsWith("//") ||
            candidate.startsWith("/") ||
            candidate.startsWith("./") ||
            candidate.startsWith("../") ||
            candidate.startsWith("?") ||
            candidate.startsWith("#") ||
            candidate.startsWith("www.", true) ||
            candidate.contains('/')
    }

    fun get(url: String, headers: Any?): RhinoSourceHttpResponse =
        rateLimit(false).let { RhinoSourceHttpResponse(httpRequest("GET", url, null, parseRequestHeaders(headers))) }

    fun head(url: String, headers: Any?): RhinoSourceHttpResponse =
        rateLimit(false).let { RhinoSourceHttpResponse(httpRequest("HEAD", url, null, parseRequestHeaders(headers))) }

    fun post(url: String, body: String, headers: Any?): RhinoSourceHttpResponse =
        rateLimit(false).let { RhinoSourceHttpResponse(httpRequest("POST", url, body, parseRequestHeaders(headers))) }

    /**
     * Legado connect() always returns a StrResponse-shaped value. Transport
     * failures become a synthetic response carrying the exception stack trace;
     * they must not abort the surrounding Rhino rule.
     */
    fun connect(url: String): RhinoSourceHttpResponse = connect(url, null, null)

    fun connect(url: String, headers: Any?): RhinoSourceHttpResponse = connect(url, headers, null)

    fun connect(url: String, headers: Any?, callTimeout: Long?): RhinoSourceHttpResponse = try {
        require(callTimeout == null || callTimeout > 0) { "connect callTimeout must be positive" }
        rateLimit(false)
        RhinoSourceHttpResponse(httpRequestWithTimeout("GET", url, null, parseRequestHeaders(headers), callTimeout))
    } catch (error: Exception) {
        if (error is java.util.concurrent.CancellationException) throw error
        RhinoSourceHttpResponse(
            SourceHttpResponse(
                statusCode = 200,
                finalUrl = runCatching { URL(url).toString() }.getOrDefault("http://localhost/"),
                body = error.stackTraceToString().ifEmpty { error.localizedMessage ?: "noErrorMsg" },
                statusMessage = "OK",
            ),
        )
    }

    /** Bounded Legado batch GET helper with capped parallelism and input-order results. */
    @JvmOverloads
    fun ajaxAll(urls: Any?, skipRateLimit: Boolean = false): Array<RhinoSourceHttpResponse> {
        val values = when (urls) {
            is Scriptable -> {
                val length = (ScriptableObject.getProperty(urls, "length") as? Number)?.toInt()
                    ?: throw IllegalArgumentException("java.ajaxAll expects an array of URLs")
                require(length in 0..MAX_AJAX_ALL_URLS) { "java.ajaxAll accepts at most $MAX_AJAX_ALL_URLS URLs" }
                (0 until length).map { index -> ScriptableObject.getProperty(urls, index) }
            }
            is Iterable<*> -> urls.toList()
            is Array<*> -> urls.toList()
            is String -> {
                val json = com.google.gson.JsonParser.parseString(urls)
                require(json.isJsonArray) { "java.ajaxAll string input must be a JSON array" }
                json.asJsonArray.map { it.asString }
            }
            else -> throw IllegalArgumentException("java.ajaxAll expects an array of URLs")
        }
        require(values.size <= MAX_AJAX_ALL_URLS) { "java.ajaxAll accepts at most $MAX_AJAX_ALL_URLS URLs" }
        val normalizedUrls = values.map { value ->
            value?.toString()?.trim().orEmpty().also { url ->
                require(url.isNotEmpty()) { "java.ajaxAll URL must not be blank" }
            }
        }
        if (normalizedUrls.isEmpty()) return emptyArray()
        val executor = Executors.newFixedThreadPool(
            minOf(MAX_AJAX_ALL_CONCURRENCY, normalizedUrls.size),
        )
        return try {
            val tasks = normalizedUrls.map { url ->
                Callable { rateLimit(skipRateLimit); RhinoSourceHttpResponse(httpRequest("GET", url, null, emptyMap())) }
            }
            executor.invokeAll(tasks).map { future -> future.get() }.toTypedArray()
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * Legado ajaxTestAll: concurrent requests with a per-call timeout. In test mode,
     * transport failures are represented as StrResponse-compatible values whose
     * callTime carries the upstream negative error code.
     */
    @JvmOverloads
    fun ajaxTestAll(urls: Any?, timeout: Int, skipRateLimit: Boolean = false): Array<RhinoSourceHttpResponse> {
        require(timeout > 0) { "ajaxTestAll timeout must be positive" }
        val values = when (urls) {
            is Scriptable -> {
                val length = (ScriptableObject.getProperty(urls, "length") as? Number)?.toInt()
                    ?: throw IllegalArgumentException("java.ajaxTestAll expects an array of URLs")
                require(length in 0..MAX_AJAX_ALL_URLS) { "java.ajaxTestAll accepts at most $MAX_AJAX_ALL_URLS URLs" }
                (0 until length).map { index -> ScriptableObject.getProperty(urls, index) }
            }
            is Iterable<*> -> urls.toList()
            is Array<*> -> urls.toList()
            is String -> {
                val json = com.google.gson.JsonParser.parseString(urls)
                require(json.isJsonArray) { "java.ajaxTestAll string input must be a JSON array" }
                json.asJsonArray.map { it.asString }
            }
            else -> throw IllegalArgumentException("java.ajaxTestAll expects an array of URLs")
        }
        require(values.size <= MAX_AJAX_ALL_URLS) { "java.ajaxTestAll accepts at most $MAX_AJAX_ALL_URLS URLs" }
        val normalizedUrls = values.map { value ->
            value?.toString()?.trim().orEmpty().also { url ->
                require(url.isNotEmpty()) { "java.ajaxTestAll URL must not be blank" }
            }
        }
        if (normalizedUrls.isEmpty()) return emptyArray()
        val executor = Executors.newFixedThreadPool(minOf(MAX_AJAX_ALL_CONCURRENCY, normalizedUrls.size))
        return try {
            normalizedUrls.map { url ->
                Callable {
                    val response = try {
                        rateLimit(skipRateLimit)
                        httpRequestWithTimeout("GET", url, null, emptyMap(), timeout.toLong())
                    } catch (error: Exception) {
                        if (error is kotlin.coroutines.cancellation.CancellationException) throw error
                        // Gateway adapters may wrap the original transport exception.
                        val cause = generateSequence(error as Throwable?) { it.cause }.last()
                        val code = when (cause) {
                            is java.net.SocketTimeoutException -> -2
                            is java.net.UnknownHostException -> -3
                            is java.net.ConnectException -> -4
                            is java.net.SocketException -> -5
                            is javax.net.ssl.SSLException -> -6
                            is java.io.InterruptedIOException ->
                                if (cause.message?.contains("timeout") == true) -1 else -7
                            else -> -7
                        }
                        SourceHttpResponse(
                            statusCode = 200,
                            finalUrl = runCatching { URL(url).toString() }.getOrDefault("http://localhost/"),
                            body = error.message.orEmpty(),
                            statusMessage = "OK",
                            callTime = code,
                        )
                    }
                    RhinoSourceHttpResponse(response)
                }
            }.let { tasks -> executor.invokeAll(tasks).map { it.get() }.toTypedArray() }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun parseRequestHeaders(headers: Any?): Map<String, String> = when (headers) {
        null -> emptyMap()
        is Scriptable -> buildMap {
            headers.ids.forEach { id ->
                val name = id.toString()
                val value = headers.get(name, headers)
                if (value != null) put(name, value.toString())
            }
        }
        is Map<*, *> -> headers.entries.associate { (key, value) ->
            require(key != null && value != null) { "HTTP header names and values cannot be null" }
            key.toString() to value.toString()
        }
        is String -> {
            val json = com.google.gson.JsonParser.parseString(headers)
            require(json.isJsonObject) { "HTTP headers must be a JSON object" }
            json.asJsonObject.entrySet().associate { (key, value) ->
                require(value.isJsonPrimitive) { "HTTP header values must be scalar" }
                key to value.asString
            }
        }
        else -> throw IllegalArgumentException("HTTP headers must be an object or JSON string")
    }

    /** Legado source-scoped single-flight execution; successful work is coalesced by key. */
    fun singleFlight(name: String, action: Function) = singleFlight(name, action, 15_000L)

    fun singleFlight(name: String, action: Function, timeoutMs: Long) {
        val key = sourceConcurrencyKey(name)
        LegadoSourceLock.singleFlight(key, timeoutMs) { invokeSourceFunction(action) }
    }

    /** Legado source-scoped mutual-exclusion execution. */
    fun lock(name: String, action: Function) = lock(name, action, 15_000L)

    fun lock(name: String, action: Function, timeoutMs: Long) {
        val key = sourceConcurrencyKey(name)
        LegadoSourceLock.lock(key, timeoutMs) { invokeSourceFunction(action) }
    }

    /** Returns the previous counter value, matching the upstream tick contract. */
    fun tick(name: String): Int = LegadoSourceLock.tick(sourceConcurrencyKey(name))

    private fun sourceConcurrencyKey(name: String): String {
        require(name.isNotBlank() && name.length <= 256) {
            "name 不能为空且长度不能超过 256"
        }
        require(sourceId.isNotBlank()) {
            "并发工具只能在书源、订阅源或朗读源中使用"
        }
        return "$sourceId@$name"
    }

    private fun invokeSourceFunction(action: Function) {
        functionInvoker(action)
    }

    fun put(key: String, value: String): String = putString(key, value)

    /** Legado timestamp formatter used by imported source rules. */
    fun timeFormat(timestamp: Number): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp.toLong()))

    fun timeFormat(timestamp: String): String =
        timestamp.toLongOrNull()?.let { timeFormat(it) }.orEmpty()

    /** Matches the original JsExtensions contract: sh is a raw millisecond offset. */
    fun timeFormatUTC(timestamp: Number, format: String, sh: Int): String =
        SimpleDateFormat(format, Locale.getDefault()).run {
            timeZone = SimpleTimeZone(sh, "UTC")
            format(Date(timestamp.toLong()))
        }

    fun timeFormatUTC(timestamp: String, format: String, sh: Int): String =
        timestamp.toLongOrNull()?.let { timeFormatUTC(it, format, sh) }.orEmpty()

    /** Mirrors Hutool HexUtil.decodeHex: malformed and whitespace-padded input throws. */
    fun hexDecodeToByteArray(hex: String): ByteArray? {
        require(hex.length <= MAX_AES_INPUT_BYTES * 2) { "Hex input exceeds the size limit" }
        require(hex.length % 2 == 0 && hex.all { it.digitToIntOrNull(16) != null }) {
            "Invalid hexadecimal input"
        }
        return ByteArray(hex.length / 2) { index ->
            ((hex[index * 2].digitToInt(16) shl 4) or hex[index * 2 + 1].digitToInt(16)).toByte()
        }
    }

    fun hexDecodeToString(hex: String): String? =
        String(hexDecodeToByteArray(hex)!!, Charsets.UTF_8)

    fun hexEncodeToString(value: String): String =
        value.toByteArray(Charsets.UTF_8).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun logType(value: Any?): String = value?.javaClass?.name ?: "null"

    private val nbspRegex = "(&nbsp;)+".toRegex()
    private val espRegex = "(&ensp;|&emsp;)".toRegex()
    private val noPrintRegex = "(&thinsp;|&zwnj;|&zwj;|\\u2009|\\u200C|\\u200D)".toRegex()
    private val wrapHtmlRegex = "</?(?:div|p|br|hr|h\\d|article|dd|dl)[^>]*>".toRegex()
    private val commentRegex = "<!--[^>]*-->".toRegex()
    private val notImgHtmlRegex = "</?(?!img)[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val otherHtmlRegex = "</?[a-zA-Z]+(?=[ >])[^<>]*>".toRegex()
    private val indent1Regex = "\\s*\\n+\\s*".toRegex()
    private val indent2Regex = "^[\\n\\s]+".toRegex()
    private val lastRegex = "[\\n\\s]+$".toRegex()
    private val formatImagePattern = java.util.regex.Pattern.compile(
        """<img[^>]*\ssrc\s*=\s*['"]([^'"{>]*\{(?:[^{}]|\{[^}>]+\})+\})['"][^>]*>|<img[^>]*\sdata-(?:src|original|srcset)\s*=\s*['"]([^'">]+)['"][^>]*>|<img[^>]*\ssrc\s*=\s*"([^">]+)"[^>]*>|<img[^>]*\s(?:data-[^=>]*|src)=\s*['"]([^'">]*)['"][^>]*>""",
        java.util.regex.Pattern.CASE_INSENSITIVE,
    )

    fun htmlFormat(value: String?): String = formatKeepImages(value, null)

    fun htmlFormat(value: String?, redirectUrl: String?): String {
        val base = redirectUrl?.let { runCatching { URL(it) }.getOrNull() }
        return formatKeepImages(value, base)
    }

    private fun normalizeEscapedLineBreaks(text: String): String =
        text.replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "\n")

    private fun formatHtmlText(html: String?, otherRegex: Regex = otherHtmlRegex): String {
        html ?: return ""
        return normalizeEscapedLineBreaks(html)
            .replace(nbspRegex, " ")
            .replace(espRegex, " ")
            .replace(noPrintRegex, "")
            .replace(wrapHtmlRegex, "\n")
            .replace(commentRegex, "")
            .replace(otherRegex, "")
            .replace(indent1Regex, "\n　　")
            .replace(indent2Regex, "　　")
            .replace(lastRegex, "")
    }

    private fun formatKeepImages(html: String?, redirectUrl: URL?): String {
        html ?: return ""
        val keepImgHtml = formatHtmlText(html, notImgHtmlRegex)
        val matcher = formatImagePattern.matcher(keepImgHtml)
        var appendPos = 0
        val result = StringBuilder()
        while (matcher.find()) {
            var param = ""
            val imagePath = matcher.group(1)?.let { candidate ->
                val urlMatcher = legadoUrlParamPattern.matcher(candidate)
                if (urlMatcher.find()) {
                    param = "," + candidate.substring(urlMatcher.end())
                    candidate.substring(0, urlMatcher.start())
                } else candidate
            } ?: matcher.group(2) ?: matcher.group(3) ?: matcher.group(4)!!
            result.append(
                keepImgHtml.substring(appendPos, matcher.start()),
                "<img src=\"",
                resolveLegadoAbsoluteUrl(redirectUrl, imagePath) + param,
                "\">",
            )
            appendPos = matcher.end()
        }
        if (appendPos < keepImgHtml.length) {
            result.append(keepImgHtml.substring(appendPos))
        }
        return result.toString()
    }

    private fun resolveLegadoAbsoluteUrl(baseUrl: URL?, rawPath: String): String {
        val path = rawPath.trim()
        if (baseUrl == null) return path
        if (path.startsWith("data:", ignoreCase = true)) return path
        if (path.startsWith("javascript")) return ""
        if (runCatching { URL(path) }.isSuccess) return path
        return runCatching { URL(baseUrl, rawPath).toString() }.getOrDefault(path)
    }

    private val legadoUrlParamPattern =
        java.util.regex.Pattern.compile("\\s*,\\s*(?=\\{)")

    /** Synchronous compatibility bridge; the host enforces transport bounds. */
    /** URL/options overload; Legado returns a stack trace string rather than throwing request failures into Rhino. */
    fun ajax(urlAndOptions: String): String = ajax(urlAndOptions as Any?)

    /** Upstream accepts either a URL string or a list whose first item is the URL. */
    fun ajax(url: Any?): String = runCatching {
        rateLimit(false)
        val value = when (url) {
            is List<*> -> url.firstOrNull()?.toString().orEmpty()
            is Scriptable -> {
                val length = (ScriptableObject.getProperty(url, "length") as? Number)?.toInt() ?: 0
                if (length > 0) ScriptableObject.getProperty(url, 0)?.toString().orEmpty() else ""
            }
            else -> url?.toString().orEmpty()
        }
        require(value.isNotBlank()) { "java.ajax URL must not be blank" }
        ajaxRequest(value)
    }.getOrElse { error ->
        error.stackTraceToString().ifEmpty { error.localizedMessage ?: "noErrorMsg" }
    }

    fun randomUUID(): String = UUID.randomUUID().toString()

    /** Fetch a source-owned helper script; callers decide whether/how to evaluate its text. */
    fun importScript(url: String): String = ajaxRequest(url)

    /** Stable mobile WebView-compatible UA for rules that request the host UA. */
    fun getWebViewUA(): String = webViewUserAgent

    /** Legado-compatible, source-scoped cookie lookup. */
    fun getCookie(tag: String): String = cookieReader(tag)

    fun getCookie(tag: String, key: String?): String {
        if (key == null) return cookieReader(tag)
        return cookieReader(tag).split(';').asSequence()
            .map(String::trim)
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator <= 0) null else pair.substring(0, separator).trim() to pair.substring(separator + 1).trim()
            }
            .firstOrNull { it.first == key }
            ?.second.orEmpty()
    }

    fun log(value: Any?): String = value?.toString().orEmpty()

    fun md5Encode(value: String): String = digest("MD5", value)

    /** Legado digest helpers; algorithm names are explicitly allow-listed. */
    fun digestHex(data: String, algorithm: String): String =
        digest(requireDigestAlgorithm(algorithm), data)

    fun digestBase64Str(data: String, algorithm: String): String {
        val bytes = MessageDigest.getInstance(requireDigestAlgorithm(algorithm))
            .digest(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun HMacHex(data: String, algorithm: String, key: String): String =
        hmac(data, algorithm, key).joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

    fun HMacBase64(data: String, algorithm: String, key: String): String =
        Base64.getEncoder().encodeToString(hmac(data, algorithm, key))

    fun md5Encode16(value: String): String = md5Encode(value).substring(8, 24)

    /** Legado EncoderUtils defaults to Android Base64.NO_WRAP for string encoding. */
    fun base64Encode(value: String): String = base64Encode(value, BASE64_NO_WRAP)

    fun base64Encode(value: String, flags: Int): String =
        encodeBase64(value.toByteArray(Charsets.UTF_8), flags)

    fun base64Decode(value: String): String = base64Decode(value, Charsets.UTF_8.name())

    fun base64Decode(value: String, charset: String): String =
        String(decodeBase64(value, BASE64_DEFAULT), java.nio.charset.Charset.forName(charset))

    fun base64Decode(value: String, flags: Int): String =
        String(decodeBase64(value, flags), Charsets.UTF_8)

    fun base64DecodeToString(value: String): String = base64Decode(value)

    fun base64DecodeToByteArray(value: String?): ByteArray? =
        value?.takeIf(String::isNotBlank)?.let { decodeBase64(it, BASE64_DEFAULT) }

    fun base64DecodeToByteArray(value: String?, flags: Int): ByteArray? =
        value?.takeIf(String::isNotBlank)?.let { decodeBase64(it, flags) }

    fun bytesToStr(value: ByteArray): String = bytesToStr(value, Charsets.UTF_8.name())

    fun bytesToStr(value: ByteArray, charset: String): String =
        String(value, java.nio.charset.Charset.forName(charset))

    fun strToBytes(value: String): ByteArray = strToBytes(value, Charsets.UTF_8.name())

    fun strToBytes(value: String, charset: String): ByteArray =
        value.toByteArray(java.nio.charset.Charset.forName(charset))

    fun aesBase64DecodeToString(value: String, key: String, transformation: String, iv: String): String =
        createSymmetricCrypto(transformation, utf8Encode(key), utf8Encode(iv))
            .decryptStr(base64DecodeBytes(value))

    fun createSymmetricCrypto(transformation: String, key: ByteArray, iv: ByteArray): RhinoLegadoSymmetricCrypto =
        RhinoLegadoSymmetricCrypto(transformation, key, iv)

    /** Common source rules pass UTF-8 key/IV strings instead of byte arrays. */
    fun createSymmetricCrypto(transformation: String, key: String, iv: String): RhinoLegadoSymmetricCrypto =
        RhinoLegadoSymmetricCrypto(transformation, utf8Encode(key), utf8Encode(iv))

    /** Legado's chapter-title number normalization, matching StringUtils.stringToInt. */
    fun toNumChapter(source: String?): String? {
        if (source.isNullOrEmpty()) return source
        val match = LEGADO_TITLE_NUMBER_PATTERN.find(source) ?: return source
        val number = match.groupValues[2]
        val normalized = fullWidthToHalf(number).replace("\\s+".toRegex(), "")
        val parsed = normalized.toIntOrNull() ?: legadoChineseNumberToInt(normalized)
        // Upstream JsExtensions.toNumChapter returns only the matched chapter label,
        // not the original title text surrounding it.
        return "${match.groupValues[1]}${parsed}${match.groupValues[3]}"
    }

    fun toURL(value: String): RhinoLegadoUrl = RhinoLegadoUrl(value)

    /** Resolve a relative URL against an explicit HTTP(S) base, matching Legado's two-argument helper. */
    fun toURL(value: String, baseUrl: String?): RhinoLegadoUrl =
        RhinoLegadoUrl(value, baseUrl)

    /** Narrow byte helpers used only by the reviewed JavaImporter AES compatibility shim. */
    fun utf8Encode(value: String): ByteArray {
        require(value.length <= MAX_AES_INPUT_BYTES) { "AES input text exceeds the size limit" }
        return value.toByteArray(Charsets.UTF_8)
    }

    fun utf8Decode(value: ByteArray): String {
        require(value.size <= MAX_AES_INPUT_BYTES) { "AES output exceeds the size limit" }
        return String(value, Charsets.UTF_8)
    }

    private fun encodeBase64(value: ByteArray, flags: Int): String {
        require(value.size <= MAX_AES_INPUT_BYTES) { "Base64 input exceeds the size limit" }
        val urlSafe = flags and BASE64_URL_SAFE != 0
        var encoder = if (urlSafe) Base64.getUrlEncoder() else Base64.getEncoder()
        if (flags and BASE64_NO_PADDING != 0) encoder = encoder.withoutPadding()
        if (flags and BASE64_NO_WRAP != 0) return encoder.encodeToString(value)
        val encoded = encoder.encodeToString(value)
        if (encoded.isEmpty()) return encoded
        val lineSeparator = if (flags and BASE64_CRLF != 0) "\r\n" else "\n"
        return encoded.chunked(76).joinToString(lineSeparator) + lineSeparator
    }

    private fun decodeBase64(value: String, flags: Int): ByteArray {
        require(value.length <= MAX_AES_INPUT_BYTES * 2) { "Base64 input exceeds the size limit" }
        val normalized = value.filterNot { it == '\r' || it == '\n' || it == ' ' || it == '\t' }
        val decoder = if (flags and BASE64_URL_SAFE != 0) Base64.getUrlDecoder() else Base64.getDecoder()
        return decoder.decode(normalized).also {
            require(it.size <= MAX_AES_INPUT_BYTES) { "Decoded Base64 input exceeds the size limit" }
        }
    }

    fun base64DecodeBytes(value: String): ByteArray {
        require(value.length <= MAX_AES_INPUT_BYTES * 2) { "Base64 input exceeds the size limit" }
        return Base64.getDecoder().decode(value).also {
            require(it.size <= MAX_AES_INPUT_BYTES) { "Decoded Base64 input exceeds the size limit" }
        }
    }

    fun copyByteRange(value: ByteArray, from: Int, to: Int): ByteArray {
        require(from >= 0 && to >= from && to <= value.size) { "Invalid byte range" }
        require(to - from <= MAX_AES_INPUT_BYTES) { "Byte range exceeds the size limit" }
        return value.copyOfRange(from, to)
    }

    /** AES-CBC only; arbitrary algorithms, providers and Java class access remain unavailable. */
    fun aesCrypt(data: ByteArray, key: ByteArray, iv: ByteArray, mode: Int): ByteArray {
        require(data.size <= MAX_AES_INPUT_BYTES) { "AES input exceeds the size limit" }
        require(key.size == 16 || key.size == 24 || key.size == 32) { "AES key must be 128, 192 or 256 bits" }
        require(iv.size == 16) { "AES-CBC IV must be 16 bytes" }
        require(mode == Cipher.ENCRYPT_MODE || mode == Cipher.DECRYPT_MODE) { "Unsupported AES operation" }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data).also {
            require(it.size <= MAX_AES_INPUT_BYTES) { "AES output exceeds the size limit" }
        }
    }

    fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    /** Original Legado JsExtensions overload; this is form-style URL encoding. */
    fun encodeURI(value: String, charset: String): String = runCatching {
        URLEncoder.encode(value, charset)
    }.getOrDefault("")

    /** Matches Legado's form-style URLEncoder overload using UTF-8. */
    fun encodeURI(value: String): String = runCatching {
        URLEncoder.encode(value, Charsets.UTF_8.name())
    }.getOrDefault("")

    /** JavaScript encodeURIComponent: retain only the component-safe punctuation. */
    fun encodeURIComponent(value: String): String =
        encodeJsUri(value, "-_.!~*'()")

    /** JavaScript decodeURIComponent; unlike form decoding, '+' remains a plus sign. */
    fun decodeURIComponent(value: String): String =
        URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())

    /**
     * JavaScript decodeURI leaves percent-encoded reserved delimiters escaped,
     * so decoding a path/query cannot accidentally change its URI structure.
     */
    fun decodeURI(value: String): String {
        val reserved = ";/?:@&=+$,#"
        val pattern = Regex("(?:%[0-9A-Fa-f]{2})+")
        return pattern.replace(value) { match ->
            val bytes = match.value.chunked(3).map { it.substring(1).toInt(16).toByte() }
            buildString {
                val pending = ArrayList<Byte>()
                fun flush() {
                    if (pending.isNotEmpty()) {
                        append(String(pending.toByteArray(), Charsets.UTF_8))
                        pending.clear()
                    }
                }
                bytes.forEachIndexed { index, byte ->
                    val ascii = byte.toInt() and 0xff
                    if (ascii < 128 && ascii.toChar() in reserved) {
                        flush()
                        append(match.value.substring(index * 3, index * 3 + 3))
                    } else {
                        pending += byte
                    }
                }
                flush()
            }
        }
    }

    private fun encodeJsUri(value: String, additionallySafe: String): String {
        val safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + additionallySafe
        return buildString {
            value.toByteArray(Charsets.UTF_8).forEach { byte ->
                val unsigned = byte.toInt() and 0xff
                val char = unsigned.toChar()
                if (unsigned < 128 && char in safe) append(char)
                else {
                    append('%')
                    append("0123456789ABCDEF"[unsigned ushr 4])
                    append("0123456789ABCDEF"[unsigned and 0x0f])
                }
            }
        }
    }

    fun urlDecode(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

    /** Legado-compatible traditional-to-simplified conversion with upstream exclusions. */
    fun t2s(value: String): String = LegadoChineseConversion.traditionalToSimple(value)

    /** Legado-compatible simplified-to-traditional conversion. */
    fun s2t(value: String): String = QuickChineseUtils.s2t(value)

    private fun requireDigestAlgorithm(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT).replace("-", "")
        val supported = mapOf(
            "MD5" to "MD5",
            "SHA1" to "SHA-1",
            "SHA256" to "SHA-256",
            "SHA384" to "SHA-384",
            "SHA512" to "SHA-512",
        )
        return supported[normalized] ?: throw IllegalArgumentException("Unsupported digest algorithm")
    }

    private fun hmac(data: String, algorithm: String, key: String): ByteArray {
        require(key.isNotEmpty() && key.length <= MAX_KEY_CHARS) { "HMAC key is blank or too long" }
        val normalized = algorithm.trim().uppercase(Locale.ROOT).replace("-", "")
        val supported = setOf("HMACSHA1", "HMACSHA256", "HMACSHA384", "HMACSHA512")
        require(normalized in supported) { "Unsupported HMAC algorithm" }
        require(data.length <= MAX_VALUE_CHARS * 128) { "HMAC input exceeds the size limit" }
        val mac = Mac.getInstance(normalized)
        mac.init(javax.crypto.spec.SecretKeySpec(key.toByteArray(Charsets.UTF_8), normalized))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun digest(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun requireValidKey(key: String) {
        require(key.isNotBlank() && key.length <= MAX_KEY_CHARS) {
            "Legado java storage key is blank or too long"
        }
    }

    companion object {
        private const val BASE64_DEFAULT = 0
        private const val BASE64_NO_PADDING = 1
        private const val BASE64_NO_WRAP = 2
        private const val BASE64_CRLF = 4
        private const val BASE64_URL_SAFE = 8

        const val DEFAULT_WEBVIEW_USER_AGENT = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val MAX_AJAX_ALL_URLS = 20
        private const val MAX_AJAX_ALL_CONCURRENCY = 4
        private const val MAX_KEY_CHARS = 256
        private const val MAX_VALUE_CHARS = 16 * 1024
        private const val MAX_AES_INPUT_BYTES = 2 * 1024 * 1024
        private const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024
        const val MAX_ARCHIVE_ENTRY_BYTES = 16 * 1024 * 1024
        private const val DOWNLOAD_FILE_PREFIX = "__legado_download__:"
        private const val VIRTUAL_FILE_PREFIX = "__legado_virtual_file__:"
        private const val VIRTUAL_DIR_PREFIX = "__legado_virtual_dir__:"
        private const val MAX_ARCHIVE_TOTAL_BYTES = 64L * 1024L * 1024L
        private const val MAX_ARCHIVE_FILES = 1000
        private const val MAX_ARCHIVE_PATH_CHARS = 512
    }
}

/** Read-only response facade returned by the Legado java.get(url, headers) helper. */
class RhinoSourceHttpResponse(private val response: SourceHttpResponse) {
    fun body(): String = response.body
    fun getBody(): String = response.body
    fun code(): Int = response.statusCode
    fun statusCode(): Int = response.statusCode
    fun isSuccessful(): Boolean = response.statusCode in 200..299
    fun message(): String = response.statusMessage
    /** Elapsed HTTP request time in milliseconds, as on Legado StrResponse. */
    fun callTime(): Int = response.callTime
    fun getCallTime(): Int = response.callTime
    fun url(): String = response.finalUrl
    fun getUrl(): String = response.finalUrl
    fun header(name: String): String =
        response.headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value?.firstOrNull().orEmpty()
    fun headers(name: String): String = header(name)
    fun headers(): Map<String, List<String>> = response.headers
    fun raw(): RhinoSourceRawResponse = RhinoSourceRawResponse(response)
    fun getRaw(): RhinoSourceRawResponse = raw()
    fun parse(): LegadoSourceElement = LegadoSourceElement(response.body, response.finalUrl)
    fun select(selector: String): LegadoSourceElement = parse().select(selector)
    fun charset(): String = response.contentType.orEmpty().substringAfter("charset=", "UTF-8").trim().trim('\"')
    override fun toString(): String = "HTTP " + response.statusCode + " " + response.statusMessage + " " + response.finalUrl
}

/** Safe read-only raw-response metadata; no sockets or streams are exposed. */
class RhinoSourceRawResponse(private val response: SourceHttpResponse) {
    fun request(): RhinoSourceRawRequest = RhinoSourceRawRequest(response.finalUrl)
    fun code(): Int = response.statusCode
    fun message(): String = response.statusMessage
    fun headers(): Map<String, List<String>> = response.headers
    fun isSuccessful(): Boolean = response.statusCode in 200..299
}

class RhinoSourceRawRequest(private val requestUrl: String) {
    fun url(): String = requestUrl
}

/** Legado JsURL metadata surface; deliberately excludes connection and stream methods. */
class RhinoLegadoUrl(value: String, baseUrl: String? = null) {
    private val parsed = if (baseUrl.isNullOrEmpty()) {
        URL(value)
    } else {
        URL(URL(baseUrl), value)
    }

    private val queryParameters: Map<String, String>? = parsed.query?.let { query ->
        linkedMapOf<String, String>().apply {
            query.split("&").forEach { part ->
                val pair = part.split("=", limit = 2)
                if (pair.size == 2) put(pair[0], URLDecoder.decode(pair[1], Charsets.UTF_8.name()))
            }
        }
    }

    fun getHost(): String = parsed.host
    fun getOrigin(): String = if (parsed.port > 0) {
        "${parsed.protocol}://${parsed.host}:${parsed.port}"
    } else {
        "${parsed.protocol}://${parsed.host}"
    }
    fun getPathname(): String = parsed.path
    fun getSearchParams(): Map<String, String>? = queryParameters

    // Retain the earlier narrow metadata aliases for already imported scripts.
    fun getPath(): String = parsed.path
    fun getProtocol(): String = parsed.protocol
    fun getQuery(): String? = parsed.query
}

private val LEGADO_TITLE_NUMBER_PATTERN = Regex("(第)(.+?)(章)")

private fun fullWidthToHalf(value: String): String = buildString(value.length) {
    value.forEach { char ->
        append(when (char.code) {
            12288 -> ' '
            in 65281..65374 -> (char.code - 65248).toChar()
            else -> char
        })
    }
}

private fun legadoChineseNumberToInt(value: String): Int {
    val numbers = mapOf(
        '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5,
        '六' to 6, '七' to 7, '八' to 8, '九' to 9, '十' to 10,
        '〇' to 0, '壹' to 1, '贰' to 2, '叁' to 3, '肆' to 4, '伍' to 5,
        '陆' to 6, '柒' to 7, '捌' to 8, '玖' to 9, '拾' to 10,
        '两' to 2, '百' to 100, '佰' to 100, '千' to 1000, '仟' to 1000,
        '万' to 10000, '亿' to 100000000,
    )
    var result = 0
    var temporary = 0
    var billion = 0
    return runCatching {
        value.forEachIndexed { index, char ->
            val number = numbers[char] ?: error("Unsupported Chinese numeral")
            when (number) {
                100000000 -> {
                    result += temporary
                    result *= number
                    billion = billion * 100000000 + result
                    result = 0
                    temporary = 0
                }
                10000 -> {
                    result += temporary
                    result *= number
                    temporary = 0
                }
                in 10..9999 -> {
                    if (temporary == 0) temporary = 1
                    result += number * temporary
                    temporary = 0
                }
                else -> {
                    temporary = if (index >= 2 && index == value.lastIndex &&
                        (numbers[value[index - 1]] ?: 0) > 10
                    ) number * (numbers[value[index - 1]] ?: 0) / 10
                    else temporary * 10 + number
                }
            }
        }
        result + temporary + billion
    }.getOrDefault(-1)
}

/** Allow-listed AES-CBC helper matching the common Legado crypto extension. */
class RhinoLegadoSymmetricCrypto(
    transformation: String,
    key: ByteArray,
    iv: ByteArray,
) {
    private val algorithm = transformation.substringBefore('/').uppercase()
    private val transformation = when (transformation.uppercase()) {
        "AES/CBC/PKCS5PADDING", "AES/CBC/PKCS7PADDING" -> "AES/CBC/PKCS5Padding"
        "DES/CBC/PKCS5PADDING" -> "DES/CBC/PKCS5Padding"
        else -> throw UnsupportedOperationException("Only AES/DES CBC with PKCS5/PKCS7 padding is supported")
    }
    private val keyBytes = key.copyOf()
    private val ivBytes = iv.copyOf()

    fun encrypt(data: String): ByteArray {
        val input = data.toByteArray(Charsets.UTF_8)
        require(input.size <= 2 * 1024 * 1024) { "Symmetric crypto input exceeds the size limit" }
        validateKeyAndIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, algorithm), IvParameterSpec(ivBytes))
        return cipher.doFinal(input)
    }

    /** Hutool-compatible Base64 ciphertext helper used by Legado source rules. */
    fun encryptBase64(data: String): String =
        Base64.getEncoder().encodeToString(encrypt(data))

    /** Hutool-compatible lowercase hexadecimal ciphertext helper. */
    fun encryptHex(data: String): String =
        encrypt(data).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    fun decrypt(data: ByteArray): ByteArray {
        require(data.size <= 2 * 1024 * 1024) { "Symmetric crypto input exceeds the size limit" }
        validateKeyAndIv()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, algorithm), IvParameterSpec(ivBytes))
        return cipher.doFinal(data)
    }

    fun decrypt(data: String): ByteArray = decrypt(decodeHutoolCipherText(data))

    fun decryptStr(data: ByteArray): String = String(decrypt(data), Charsets.UTF_8)

    /** Hutool accepts either a hexadecimal or Base64 encoded ciphertext string. */
    fun decryptStr(data: String): String = String(decrypt(data), Charsets.UTF_8)

    private fun decodeHutoolCipherText(value: String): ByteArray {
        require(value.length <= 4 * 1024 * 1024) { "Encoded symmetric crypto input exceeds the size limit" }
        // Legado's String.isHex() checks every character without trimming and
        // does not pre-classify odd-length hexadecimal strings as Base64.
        val isHex = value.all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' }
        if (isHex) {
            require(value.length % 2 == 0) { "Hexadecimal symmetric crypto input must have an even length" }
            return ByteArray(value.length / 2) { index ->
                value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
        return Base64.getDecoder().decode(value).also {
            require(it.size <= 2 * 1024 * 1024) { "Symmetric crypto input exceeds the size limit" }
        }
    }

    private fun validateKeyAndIv() {
        when (algorithm) {
            "AES" -> {
                require(keyBytes.size == 16 || keyBytes.size == 24 || keyBytes.size == 32) { "Invalid AES key length" }
                require(ivBytes.size == 16) { "Invalid AES-CBC IV length" }
            }
            "DES" -> {
                require(keyBytes.size == 8) { "Invalid DES key length" }
                require(ivBytes.size == 8) { "Invalid DES-CBC IV length" }
            }
        }
    }
}

/** Keeps the pinned Legado conversion exclusions across per-source helper instances. */
private object LegadoChineseConversion {
    @Volatile private var initialized = false

    @Synchronized
    fun traditionalToSimple(value: String): String {
        if (!initialized) {
            QuickChineseUtils.loadExcludeDict(
                TransType.TRADITIONAL_TO_SIMPLE,
                listOf(
                    "槃", "划槳", "列根", "雪梨", "雪糕", "多士", "起司", "芝士", "沙芬", "母音",
                    "华乐", "民乐", "晶元", "晶片", "映像", "明覆", "明瞭", "新力", "新喻",
                    "零錢", "零钱", "離線", "碟片", "模組", "桌球", "案頭", "機車", "電漿",
                    "鳳梨", "魔戒", "載入", "菲林", "整合", "變數", "解碼", "散钱", "插水",
                    "房屋", "房价", "快取", "德士", "建立", "常式", "席丹", "布殊", "布希",
                    "巴哈", "巨集", "夜学", "向量", "半形", "加彭", "列印", "函式", "全形",
                    "光碟", "介面", "乳酪", "沈船", "永珍", "演化", "牛油", "相容", "磁碟",
                    "菲林", "規則", "酵素", "雷根", "饭盒", "路易斯", "非同步", "出租车", "周杰倫",
                    "马铃薯", "馬鈴薯", "機械人", "電單車", "電扶梯", "音效卡", "飆車族", "點陣圖",
                    "個入球", "顆進球", "沃尓沃", "晶片集", "斯瓦巴", "斜角巷", "战列舰", "快速面",
                    "希特拉", "太空梭", "吐瓦魯", "吉布堤", "吉布地", "史太林", "南冰洋", "区域网",
                    "波札那", "解析度", "酷洛米", "金夏沙", "魔獸紀元", "高空彈跳", "铁达尼号",
                    "太空战士", "埃及妖后", "吉里巴斯", "附加元件", "魔鬼終結者", "純文字檔案",
                    "奇幻魔法Melody", "列支敦斯登",
                ),
            )
            initialized = true
        }
        return QuickChineseUtils.t2s(value)
    }
}
