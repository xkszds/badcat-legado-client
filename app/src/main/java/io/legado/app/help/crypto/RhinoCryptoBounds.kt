package io.legado.app.help.crypto

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal const val MAX_RHINO_CRYPTO_BYTES = 2 * 1024 * 1024

internal fun requireRhinoCryptoSize(size: Int, label: String) {
    require(size in 0..MAX_RHINO_CRYPTO_BYTES) { "$label exceeds the 2 MiB limit" }
}

internal fun checkedRhinoCryptoOutput(bytes: ByteArray, label: String): ByteArray {
    requireRhinoCryptoSize(bytes.size, label)
    return bytes
}

internal fun requireRhinoCryptoEncodedSize(size: Long, label: String) {
    require(size in 0L..MAX_RHINO_CRYPTO_BYTES.toLong()) {
        "$label exceeds the 2 MiB encoded-output limit"
    }
}

internal fun checkedRhinoCryptoEncodedOutput(value: String, label: String): String {
    requireRhinoCryptoEncodedSize(value.length.toLong(), label)
    return value
}

/** Reads at most 2 MiB, handles zero-progress bulk reads, and never closes the caller's stream. */
internal fun readRhinoCryptoInput(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream(8 * 1024)
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) {
            val single = input.read()
            if (single < 0) break
            requireRhinoCryptoSize(total + 1, "crypto stream input")
            output.write(single)
            total++
        } else {
            requireRhinoCryptoSize(total + count, "crypto stream input")
            output.write(buffer, 0, count)
            total += count
        }
    }
    return output.toByteArray()
}
