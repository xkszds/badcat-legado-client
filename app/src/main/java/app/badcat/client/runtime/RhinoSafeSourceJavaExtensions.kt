package app.badcat.client.runtime

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.github.liuyueyi.quick.transfer.ChineseUtils as QuickChineseUtils
import com.github.liuyueyi.quick.transfer.constants.TransType

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
    private val ruleStringReader: (String) -> String = { "" },
    private val contentWriter: (String) -> Unit = {},
) {

    /** Evaluate a Legado selector against the current AnalyzeRule content. */
    fun getString(rule: String): String = ruleStringReader(rule)

    /** Explicit stored-string accessor for host integrations. */
    fun getStoredString(key: String): String {
        requireValidKey(key)
        return store.get(sourceId, key).orEmpty()
    }

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

    /** Legado's general key/value aliases use the same source-isolated persistent store. */
    fun get(key: String): String = getStoredString(key)

    fun put(key: String, value: String): String = putString(key, value)

    /** Legado timestamp formatter used by imported source rules. */
    fun timeFormat(timestamp: Number): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp.toLong()))

    fun timeFormat(timestamp: String): String =
        timestamp.toLongOrNull()?.let { timeFormat(it) }.orEmpty()

    /** Synchronous compatibility bridge; the host enforces transport bounds. */
    fun ajax(urlAndOptions: String): String = ajaxRequest(urlAndOptions)

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
