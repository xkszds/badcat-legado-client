package io.legado.app.help.crypto

import cn.hutool.core.util.HexUtil
import cn.hutool.crypto.symmetric.SymmetricCrypto
import io.legado.app.utils.EncoderUtils
import java.io.InputStream
import java.nio.charset.Charset

/** Host resource boundary around the pinned upstream Hutool-backed crypto API. */
class RhinoBoundedSymmetricCryptoAndroid(
    algorithm: String,
    key: ByteArray?,
) : SymmetricCryptoAndroid(algorithm, key) {

    override fun encrypt(data: ByteArray): ByteArray {
        requireRhinoCryptoSize(data.size, "symmetric input")
        return super.encrypt(data)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        requireRhinoCryptoSize(data.size, "symmetric input")
        return checkedRhinoCryptoOutput(super.decrypt(data), "symmetric output")
    }

    override fun encrypt(data: InputStream): ByteArray =
        encrypt(readRhinoCryptoInput(data))

    override fun decrypt(data: InputStream): ByteArray =
        decrypt(readRhinoCryptoInput(data))

    override fun decrypt(data: String): ByteArray {
        require(data.length <= MAX_RHINO_CRYPTO_BYTES * 2) {
            "Encoded symmetric input exceeds the 4 MiB character limit"
        }
        return checkedRhinoCryptoOutput(super.decrypt(data), "symmetric output")
    }

    private fun encodeBase64(bytes: ByteArray): String =
        EncoderUtils.base64Encode(bytes)

    private fun encodeHex(bytes: ByteArray): String =
        HexUtil.encodeHexStr(bytes)

    override fun encryptBase64(data: ByteArray): String = encodeBase64(encrypt(data))

    override fun encryptBase64(data: String, charset: String?): String =
        encodeBase64(encrypt(data, charset))

    override fun encryptBase64(data: String, charset: Charset?): String =
        encodeBase64(encrypt(data, charset))

    override fun encryptBase64(data: String): String = encodeBase64(encrypt(data))

    override fun encryptBase64(data: InputStream): String = encodeBase64(encrypt(data))

    override fun encryptHex(data: ByteArray): String = encodeHex(encrypt(data))

    override fun encryptHex(data: String, charset: String?): String = encodeHex(encrypt(data, charset))

    override fun encryptHex(data: String, charset: Charset?): String = encodeHex(encrypt(data, charset))

    override fun encryptHex(data: String): String = encodeHex(encrypt(data))

    override fun encryptHex(data: InputStream): String = encodeHex(encrypt(data))

    override fun setIv(iv: ByteArray): SymmetricCrypto {
        requireRhinoCryptoSize(iv.size, "crypto IV")
        return super.setIv(iv)
    }
}
