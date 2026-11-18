package app.badcat.client.runtime

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Base64
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
    private val ruleStringReader: (String) -> String = { "" },
    private val ruleStringReaderWithContent: (String, String) -> String = { rule, _ -> ruleStringReader(rule) },
    private val ruleStringListReader: (String) -> List<String> = { emptyList() },
    private val ruleStringListReaderWithContent: (String, String) -> List<String> = { rule, _ -> ruleStringListReader(rule) },
    private val ruleElementReader: (String) -> LegadoSourceElement? = { null },
    private val ruleElementReaderWithContent: (String, String) -> LegadoSourceElement? = { rule, _ -> ruleElementReader(rule) },
    private val ruleElementsReader: (String) -> LegadoSourceElementList = { LegadoSourceElementList(emptyList()) },
    private val ruleElementsReaderWithContent: (String, String) -> LegadoSourceElementList = { rule, _ -> ruleElementsReader(rule) },
    private val contentWriter: (String) -> Unit = {},
) {

    /** Evaluate a Legado selector against the current AnalyzeRule content. */
    fun getString(rule: String): String = ruleStringReader(rule)

    /** Legado overload evaluates a selector against an explicitly supplied result fragment. */
    fun getString(rule: String, content: String): String = ruleStringReaderWithContent(rule, content)

    /** Legado's list selector helper, returned as a Java-compatible mutable list. */
    fun getStringList(rule: String): Array<String> = ruleStringListReader(rule).toTypedArray()

    fun getStringList(rule: String, content: String): Array<String> =
        ruleStringListReaderWithContent(rule, content).toTypedArray()

    /** Legado's element selector helper, exposing only safe text/HTML/attribute methods. */
    fun getElement(rule: String): LegadoSourceElement =
        ruleElementReader(rule) ?: LegadoSourceElement("")

    fun getElement(rule: String, content: String): LegadoSourceElement =
        ruleElementReaderWithContent(rule, content) ?: LegadoSourceElement("")

    /** Legado's multi-element selector helper, supporting size/get/toArray and index access. */
    fun getElements(rule: String): LegadoSourceElementList = ruleElementsReader(rule)

    fun getElements(rule: String, content: String): LegadoSourceElementList =
        ruleElementsReaderWithContent(rule, content)

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

    fun putString(key: String, value: String): String {
        requireValidKey(key)
        require(value.length <= MAX_VALUE_CHARS) { "Legado java.putString value exceeds the size limit" }
        store.put(sourceId, key, value)
        return value
    }

    /** Legado HTTP GET overloads. Headers may be a JS object or JSON string. */
    fun get(url: String): RhinoSourceHttpResponse = RhinoSourceHttpResponse(httpRequest("GET", url, null, emptyMap()))

    fun get(url: String, headers: Any?): RhinoSourceHttpResponse =
        RhinoSourceHttpResponse(httpRequest("GET", url, null, parseRequestHeaders(headers)))

    fun head(url: String, headers: Any?): RhinoSourceHttpResponse =
        RhinoSourceHttpResponse(httpRequest("HEAD", url, null, parseRequestHeaders(headers)))

    fun post(url: String, body: String, headers: Any?): RhinoSourceHttpResponse =
        RhinoSourceHttpResponse(httpRequest("POST", url, body, parseRequestHeaders(headers)))

    fun connect(url: String): RhinoSourceHttpResponse = get(url)

    fun connect(url: String, headers: Any?): RhinoSourceHttpResponse = get(url, headers)

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

    fun put(key: String, value: String): String = putString(key, value)

    /** Legado timestamp formatter used by imported source rules. */
    fun timeFormat(timestamp: Number): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp.toLong()))

    fun timeFormat(timestamp: String): String =
        timestamp.toLongOrNull()?.let { timeFormat(it) }.orEmpty()

    /** Synchronous compatibility bridge; the host enforces transport bounds. */
    fun ajax(urlAndOptions: String): String = ajaxRequest(urlAndOptions)

    /** Fetch a source-owned helper script; callers decide whether/how to evaluate its text. */
    fun importScript(url: String): String = ajaxRequest(url)

    /** Stable mobile WebView-compatible UA for rules that request the host UA. */
    fun getWebViewUA(): String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"

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
    fun md5Encode16(value: String): String = md5Encode(value).substring(8, 24)

    fun base64Encode(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    fun base64Decode(value: String): String =
        String(Base64.getDecoder().decode(value), Charsets.UTF_8)

    fun base64DecodeToString(value: String): String = base64Decode(value)

    fun base64DecodeToByteArray(value: String): ByteArray = base64DecodeBytes(value)

    fun bytesToStr(value: ByteArray): String = utf8Decode(value)

    fun hexDecodeToString(value: String): String {
        require(value.length <= MAX_AES_INPUT_BYTES * 2 && value.length % 2 == 0) {
            "Hex input must contain an even number of bounded characters"
        }
        val bytes = ByteArray(value.length / 2) { index ->
            value.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        return utf8Decode(bytes)
    }

    fun strToBytes(value: String): ByteArray = utf8Encode(value)

    fun aesBase64DecodeToString(value: String, key: String, transformation: String, iv: String): String =
        createSymmetricCrypto(transformation, utf8Encode(key), utf8Encode(iv))
            .decryptStr(base64DecodeBytes(value))

    fun createSymmetricCrypto(transformation: String, key: ByteArray, iv: ByteArray): RhinoLegadoSymmetricCrypto =
        RhinoLegadoSymmetricCrypto(transformation, key, iv)

    /** Common source rules pass UTF-8 key/IV strings instead of byte arrays. */
    fun createSymmetricCrypto(transformation: String, key: String, iv: String): RhinoLegadoSymmetricCrypto =
        RhinoLegadoSymmetricCrypto(transformation, utf8Encode(key), utf8Encode(iv))

    fun toURL(value: String): RhinoLegadoUrl = RhinoLegadoUrl(value)

    /** Narrow byte helpers used only by the reviewed JavaImporter AES compatibility shim. */
    fun utf8Encode(value: String): ByteArray {
        require(value.length <= MAX_AES_INPUT_BYTES) { "AES input text exceeds the size limit" }
        return value.toByteArray(Charsets.UTF_8)
    }

    fun utf8Decode(value: ByteArray): String {
        require(value.size <= MAX_AES_INPUT_BYTES) { "AES output exceeds the size limit" }
        return String(value, Charsets.UTF_8)
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

    /** JavaScript encodeURI: retain URI syntax delimiters, encode all other UTF-8 bytes. */
    fun encodeURI(value: String): String =
        encodeJsUri(value, "-_.!~*'();,/?:@&=+$#")

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

    private fun digest(algorithm: String, value: String): String =
        MessageDigest.getInstance(algorithm).digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun requireValidKey(key: String) {
        require(key.isNotBlank() && key.length <= MAX_KEY_CHARS) {
            "Legado java storage key is blank or too long"
        }
    }

    companion object {
        private const val MAX_KEY_CHARS = 256
        private const val MAX_VALUE_CHARS = 16 * 1024
        private const val MAX_AES_INPUT_BYTES = 2 * 1024 * 1024
    }
}

/** Read-only response facade returned by the Legado java.get(url, headers) helper. */
class RhinoSourceHttpResponse(private val response: SourceHttpResponse) {
    fun body(): String = response.body
    fun code(): Int = response.statusCode
    fun statusCode(): Int = response.statusCode
    fun url(): String = response.finalUrl
    fun header(name: String): String =
        response.headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value?.firstOrNull().orEmpty()
    fun headers(name: String): String = header(name)
    fun parse(): LegadoSourceElement = LegadoSourceElement(response.body, response.finalUrl)
    fun select(selector: String): LegadoSourceElement = parse().select(selector)
    fun charset(): String = response.contentType.orEmpty().substringAfter("charset=", "UTF-8").trim().trim('"')
}

/** Narrow URL metadata view for source rules; no connection or stream methods. */
class RhinoLegadoUrl(value: String) {
    private val parsed = URL(value)
    fun getHost(): String = parsed.host
    fun getPath(): String = parsed.path
    fun getProtocol(): String = parsed.protocol
    fun getQuery(): String? = parsed.query
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

    fun decryptStr(data: ByteArray): String {
        require(data.size <= 2 * 1024 * 1024) { "Symmetric crypto input exceeds the size limit" }
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
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, algorithm), IvParameterSpec(ivBytes))
        return String(cipher.doFinal(data), Charsets.UTF_8)
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
