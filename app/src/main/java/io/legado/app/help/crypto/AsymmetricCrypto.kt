package io.legado.app.help.crypto

import androidx.annotation.Keep
import cn.hutool.crypto.KeyUtil
import cn.hutool.crypto.SecureUtil
import cn.hutool.crypto.asymmetric.KeyType
import cn.hutool.crypto.asymmetric.AsymmetricCrypto as HutoolAsymmetricCrypto
import io.legado.app.utils.EncoderUtils
import cn.hutool.core.util.HexUtil
import java.io.InputStream

@Keep
@Suppress("unused")
class AsymmetricCrypto(algorithm: String) : HutoolAsymmetricCrypto(algorithm) {
    @Suppress("MemberVisibilityCanBePrivate")
    fun setPrivateKey(key: ByteArray): AsymmetricCrypto {
        requireRhinoCryptoSize(key.size, "private key")
        setPrivateKey(KeyUtil.generatePrivateKey(this.algorithm, key))
        return this
    }
    fun setPrivateKey(key: String): AsymmetricCrypto = setPrivateKey(key.encodeToByteArray())

    @Suppress("MemberVisibilityCanBePrivate")
    fun setPublicKey(key: ByteArray): AsymmetricCrypto {
        requireRhinoCryptoSize(key.size, "public key")
        setPublicKey(KeyUtil.generatePublicKey(this.algorithm, key))
        return this
    }
    fun setPublicKey(key: String): AsymmetricCrypto = setPublicKey(key.encodeToByteArray())

    private fun getKeyType(usePublicKey: Boolean? = true): KeyType =
        if (usePublicKey == true) KeyType.PublicKey else KeyType.PrivateKey

    private fun boundedEncryptData(data: Any): Any = when (data) {
        is ByteArray -> {
            requireRhinoCryptoSize(data.size, "asymmetric input")
            data
        }
        is String -> {
            requireRhinoCryptoSize(data.toByteArray(Charsets.UTF_8).size, "asymmetric input")
            data
        }
        is InputStream -> readRhinoCryptoInput(data)
        else -> throw IllegalArgumentException("Unexpected input type")
    }

    private fun boundedDecryptData(data: Any): Any = when (data) {
        is ByteArray -> {
            requireRhinoCryptoSize(data.size, "asymmetric input")
            data
        }
        is String -> {
            require(data.length <= MAX_RHINO_CRYPTO_BYTES * 2) { "Asymmetric encoded input exceeds the 4 MiB character limit" }
            SecureUtil.decode(data).also { requireRhinoCryptoSize(it.size, "asymmetric decoded input") }
        }
        is InputStream -> readRhinoCryptoInput(data)
        else -> throw IllegalArgumentException("Unexpected input type")
    }

    @JvmOverloads
    fun decrypt(data: Any, usePublicKey: Boolean? = true): ByteArray {
        val input = boundedDecryptData(data)
        val output = when (input) {
            is ByteArray -> super.decrypt(input, getKeyType(usePublicKey))
            is String -> super.decrypt(input, getKeyType(usePublicKey))
            else -> throw IllegalArgumentException("Unexpected input type")
        }
        return checkedRhinoCryptoOutput(output, "asymmetric output")
    }

    @JvmOverloads
    fun decryptStr(data: Any, usePublicKey: Boolean? = true): String =
        String(decrypt(data, usePublicKey))

    @JvmOverloads
    fun encrypt(data: Any, usePublicKey: Boolean? = true): ByteArray {
        val input = boundedEncryptData(data)
        val output = when (input) {
            is ByteArray -> super.encrypt(input, getKeyType(usePublicKey))
            is String -> super.encrypt(input, getKeyType(usePublicKey))
            else -> throw IllegalArgumentException("Unexpected input type")
        }
        return checkedRhinoCryptoOutput(output, "asymmetric output")
    }

    @JvmOverloads
    fun encryptHex(data: Any, usePublicKey: Boolean? = true): String {
        val bytes = encrypt(data, usePublicKey)
        requireRhinoCryptoEncodedSize(bytes.size.toLong() * 2L, "asymmetric Hex output")
        return checkedRhinoCryptoEncodedOutput(HexUtil.encodeHexStr(bytes), "asymmetric Hex output")
    }

    @JvmOverloads
    fun encryptBase64(data: Any, usePublicKey: Boolean? = true): String {
        val bytes = encrypt(data, usePublicKey)
        requireRhinoCryptoEncodedSize(((bytes.size.toLong() + 2L) / 3L) * 4L, "asymmetric Base64 output")
        return checkedRhinoCryptoEncodedOutput(EncoderUtils.base64Encode(bytes), "asymmetric Base64 output")
    }

}
