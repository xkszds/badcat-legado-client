package io.legado.app.help.crypto

import cn.hutool.crypto.symmetric.SymmetricCrypto
import java.io.InputStream

/** Host resource boundary around the pinned upstream Hutool-backed crypto API. */
class RhinoBoundedSymmetricCryptoAndroid(
    algorithm: String,
    key: ByteArray?,
) : SymmetricCryptoAndroid(algorithm, key) {

    override fun encrypt(data: ByteArray): ByteArray {
        requireRhinoCryptoSize(data.size, "symmetric input")
        return checkedRhinoCryptoOutput(super.encrypt(data), "symmetric output")
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
            "Encoded symmetric input exceeds the 2 MiB decoded bound"
        }
        return checkedRhinoCryptoOutput(super.decrypt(data), "symmetric output")
    }

    override fun setIv(iv: ByteArray): SymmetricCrypto {
        requireRhinoCryptoSize(iv.size, "crypto IV")
        return super.setIv(iv)
    }
}
