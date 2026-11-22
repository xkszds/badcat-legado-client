package io.legado.app.help.crypto

import androidx.annotation.Keep
import cn.hutool.crypto.KeyUtil
import java.io.InputStream
import cn.hutool.crypto.asymmetric.Sign as HutoolSign

@Keep
@Suppress("unused")
class Sign(algorithm: String) : HutoolSign(algorithm) {
    fun setPrivateKey(key: ByteArray): Sign {
        requireRhinoCryptoSize(key.size, "private key")
        setPrivateKey(KeyUtil.generatePrivateKey(algorithm, key))
        return this
    }
    fun setPrivateKey(key: String): Sign = setPrivateKey(key.encodeToByteArray())

    fun setPublicKey(key: ByteArray): Sign {
        requireRhinoCryptoSize(key.size, "public key")
        setPublicKey(KeyUtil.generatePublicKey(algorithm, key))
        return this
    }
    fun setPublicKey(key: String): Sign = setPublicKey(key.encodeToByteArray())
    override fun sign(data: ByteArray): ByteArray {
        requireRhinoCryptoSize(data.size, "signature input")
        return checkedRhinoCryptoOutput(super.sign(data), "signature output")
    }

    override fun sign(data: String): ByteArray {
        requireRhinoCryptoSize(data.toByteArray(Charsets.UTF_8).size, "signature input")
        return checkedRhinoCryptoOutput(super.sign(data), "signature output")
    }

    override fun sign(data: InputStream): ByteArray = sign(readRhinoCryptoInput(data))

    override fun verify(data: ByteArray, sign: ByteArray): Boolean {
        requireRhinoCryptoSize(data.size, "verification input")
        requireRhinoCryptoSize(sign.size, "signature input")
        return super.verify(data, sign)
    }
}

