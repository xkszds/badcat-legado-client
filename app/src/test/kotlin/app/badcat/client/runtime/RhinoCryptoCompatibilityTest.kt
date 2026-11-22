package app.badcat.client.runtime

import app.badcat.engine.BookSource
import app.badcat.engine.EngineResult
import io.legado.app.help.crypto.RhinoBoundedSymmetricCryptoAndroid
import io.legado.app.help.crypto.requireRhinoCryptoEncodedSize
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.security.KeyPairGenerator
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class RhinoCryptoCompatibilityTest {
    @Test
    fun supportsLegadoSymmetricCryptoBase64AndHexEncryptionHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://symmetric-encrypt.example/",
            name = "Symmetric encryption compatibility",
            baseUrl = "https://symmetric-encrypt.example/",
        )
        val result = runtime.evaluate(
            source,
            """
                var crypto = java.createSymmetricCrypto(
                    'AES/CBC/PKCS5Padding',
                    '0123456789abcdef',
                    'abcdef0123456789'
                );
                var base64Cipher = crypto.encryptBase64('Legado crypto roundtrip');
                var hexCipher = crypto.encryptHex('Legado crypto roundtrip');
                crypto.decryptStr(base64Cipher) + '|' +
                    crypto.decryptStr(hexCipher) + '|' +
                    (crypto.decrypt(base64Cipher).length > 0) + '|' +
                    (crypto.decrypt(hexCipher).length > 0);
            """.trimIndent(),
        )

        assertTrue("Legado symmetric encrypt helpers failed: $result", result is EngineResult.Success)
        assertEquals("Unexpected crypto roundtrip result: $result", "Legado crypto roundtrip|Legado crypto roundtrip|true|true", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsPinnedLegadoAesLegacyHelpersAndHistoricalQuirks() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(id = "https://pinned-aes.example/", name = "Pinned AES", baseUrl = "https://pinned-aes.example/")
        val key = "0123456789abcdef"
        val iv = "abcdef0123456789"
        val plaintext = "Pinned AES compatibility fixture"
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"), IvParameterSpec(iv.toByteArray(Charsets.UTF_8)))
        val encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)))
        val keyBase64 = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.getEncoder().encodeToString(iv.toByteArray(Charsets.UTF_8))
        val result = runtime.evaluate(source, """
            var decoded = java.aesDecodeToString('$encrypted', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var decodedBase64 = java.aesBase64DecodeToString('$encrypted', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var decodedBytes = java.aesDecodeToByteArray('$encrypted', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var decodedBase64Bytes = java.aesBase64DecodeToByteArray('$encrypted', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var crypto = java.createSymmetricCrypto('AES/CBC/PKCS5Padding', '$key', '$iv');
            var encodedBytes = java.aesEncodeToByteArray('$plaintext', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var encodedBase64Bytes = java.aesEncodeToBase64ByteArray('$plaintext', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var encodedBase64 = java.aesEncodeToBase64String('$plaintext', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            var encodedArgs = java.aesEncodeArgsBase64Str('$plaintext', '$key', 'CBC', 'PKCS5Padding', '$iv');
            var argsDecoded = java.aesDecodeArgsBase64Str('$encrypted', '$keyBase64', 'CBC', 'PKCS5Padding', '$ivBase64');
            // Pinned upstream quirk: aesEncodeToString calls decryptStr(data).
            var encodeStringQuirk = java.aesEncodeToString('$encrypted', '$key', 'AES/CBC/PKCS5Padding', '$iv');
            [decoded, decodedBase64, decodedBytes.length > 0, decodedBase64Bytes.length > 0,
             crypto.decryptStr(encodedBytes), encodedBase64Bytes.length > 0,
             crypto.decryptStr(encodedBase64), crypto.decryptStr(encodedArgs), argsDecoded, encodeStringQuirk].join('|');
        """.trimIndent())
        assertTrue("Pinned AES legacy helpers failed: $result", result is EngineResult.Success)
        assertEquals("$plaintext|$plaintext|true|true|$plaintext|true|$plaintext|$plaintext|$plaintext|$plaintext", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsPinnedLegadoDesAndTripleDesLegacyHelpers() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(id = "https://pinned-des.example/", name = "Pinned DES", baseUrl = "https://pinned-des.example/")
        val desKey = "12345678"
        val desIv = "abcdefgh"
        val desPlain = "Pinned DES compatibility"
        val desCipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
        desCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(desKey.toByteArray(Charsets.UTF_8), "DES"), IvParameterSpec(desIv.toByteArray(Charsets.UTF_8)))
        val desEncrypted = Base64.getEncoder().encodeToString(desCipher.doFinal(desPlain.toByteArray(Charsets.UTF_8)))
        val tripleKey = "123456789012345678901234"
        val tripleIv = "abcdefgh"
        val triplePlain = "Pinned triple DES compatibility"
        val tripleCipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
        tripleCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(tripleKey.toByteArray(Charsets.UTF_8), "DESede"), IvParameterSpec(tripleIv.toByteArray(Charsets.UTF_8)))
        val tripleEncrypted = Base64.getEncoder().encodeToString(tripleCipher.doFinal(triplePlain.toByteArray(Charsets.UTF_8)))
        val tripleKeyBase64 = Base64.getEncoder().encodeToString(tripleKey.toByteArray(Charsets.UTF_8))
        val result = runtime.evaluate(source, """
            var desDecoded = java.desDecodeToString('$desEncrypted', '$desKey', 'DES/CBC/PKCS5Padding', '$desIv');
            var desBase64Decoded = java.desBase64DecodeToString('$desEncrypted', '$desKey', 'DES/CBC/PKCS5Padding', '$desIv');
            var desEncoded = java.desEncodeToBase64String('$desPlain', '$desKey', 'DES/CBC/PKCS5Padding', '$desIv');
            var desRoundTrip = java.createSymmetricCrypto('DES/CBC/PKCS5Padding', '$desKey', '$desIv').decryptStr(desEncoded);
            var desBinary = java.desEncodeToString('$desPlain', '$desKey', 'DES/CBC/PKCS5Padding', '$desIv');
            var tripleDecoded = java.tripleDESDecodeStr('$tripleEncrypted', '$tripleKey', 'CBC', 'PKCS5Padding', '$tripleIv');
            var tripleArgsDecoded = java.tripleDESDecodeArgsBase64Str('$tripleEncrypted', '$tripleKeyBase64', 'CBC', 'PKCS5Padding', '$tripleIv');
            var tripleEncoded = java.tripleDESEncodeBase64Str('$triplePlain', '$tripleKey', 'CBC', 'PKCS5Padding', '$tripleIv');
            var tripleArgsEncoded = java.tripleDESEncodeArgsBase64Str('$triplePlain', '$tripleKeyBase64', 'CBC', 'PKCS5Padding', '$tripleIv');
            var tripleRoundTrip = java.createSymmetricCrypto('DESede/CBC/PKCS5Padding', '$tripleKey', '$tripleIv').decryptStr(tripleEncoded);
            var tripleArgsRoundTrip = java.createSymmetricCrypto('DESede/CBC/PKCS5Padding', java.base64DecodeToByteArray('$tripleKeyBase64'), java.utf8Encode('$tripleIv')).decryptStr(tripleArgsEncoded);
            [desDecoded, desBase64Decoded, desRoundTrip, String(desBinary).length > 0,
             tripleDecoded, tripleArgsDecoded, tripleRoundTrip, tripleArgsRoundTrip].join('|');
        """.trimIndent())
        assertTrue("Pinned DES/3DES legacy helpers failed: $result", result is EngineResult.Success)
        assertEquals("$desPlain|$desPlain|$desPlain|true|$triplePlain|$triplePlain|$triplePlain|$triplePlain", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsPinnedLegadoAsymmetricCryptoAndSignFactories() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(id = "https://pinned-rsa.example/", name = "Pinned RSA", baseUrl = "https://pinned-rsa.example/")
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey = Base64.getEncoder().encodeToString(pair.public.encoded)
        val privateKey = Base64.getEncoder().encodeToString(pair.private.encoded)
        val result = runtime.evaluate(source, """
            var asymmetric = java.createAsymmetricCrypto('RSA');
            asymmetric.setPublicKey(java.base64DecodeToByteArray('$publicKey'));
            asymmetric.setPrivateKey(java.base64DecodeToByteArray('$privateKey'));
            var encrypted = asymmetric.encryptBase64('Pinned RSA fixture');
            var encryptedHex = asymmetric.encryptHex('Pinned RSA fixture');
            var decrypted = asymmetric.decryptStr(encrypted, false);
            var decryptedHex = asymmetric.decryptStr(encryptedHex, false);
            var signer = java.createSign('SHA256withRSA');
            signer.setPrivateKey(java.base64DecodeToByteArray('$privateKey'));
            signer.setPublicKey(java.base64DecodeToByteArray('$publicKey'));
            var signature = signer.sign(java.utf8Encode('Pinned signature fixture'));
            var verified = signer.verify(java.utf8Encode('Pinned signature fixture'), signature);
            decrypted + '|' + decryptedHex + '|' + verified;
        """.trimIndent())
        assertTrue("Pinned RSA/sign factories failed: $result", result is EngineResult.Success)
        assertEquals("Pinned RSA fixture|Pinned RSA fixture|true", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)
    }

    @Test
    fun supportsPinnedSymmetricCryptoOverloadsAndRandomKeySemantics() = runBlocking {
        val runtime = RhinoSourceScriptRuntime()
        val source = BookSource(
            id = "https://pinned-crypto-overloads.example/",
            name = "Pinned overloads",
            baseUrl = "https://pinned-crypto-overloads.example/",
        )
        val result = runtime.evaluate(
            source,
            """
                var stringCrypto = java.createSymmetricCrypto('AES/ECB/PKCS5Padding', '0123456789abcdef');
                var stringCipher = stringCrypto.encryptBase64('string key fixture');
                var stringPlain = stringCrypto.decryptStr(stringCipher);
                var byteCrypto = java.createSymmetricCrypto('AES/ECB/PKCS5Padding', java.utf8Encode('0123456789abcdef'));
                var byteCipher = byteCrypto.encryptBase64('byte key fixture');
                var bytePlain = byteCrypto.decryptStr(byteCipher);
                stringPlain + '|' + bytePlain;
            """.trimIndent(),
        )
        assertTrue("Pinned symmetric overloads failed: $result", result is EngineResult.Success)
        assertEquals("string key fixture|byte key fixture", (result as EngineResult.Success).value)
        runtime.clearSourceState(source.id)

        val randomCrypto = RhinoBoundedSymmetricCryptoAndroid("AES/ECB/PKCS5Padding", null)
        val randomCipher = randomCrypto.encryptBase64("random key fixture")
        assertEquals("random key fixture", randomCrypto.decryptStr(randomCipher))
    }

    @Test
    fun allowsSymmetricEncodedOutputExpansionAndPreservesInputBounds() {
        val key = "0123456789abcdef".toByteArray(Charsets.UTF_8)
        val crypto = RhinoBoundedSymmetricCryptoAndroid("AES/ECB/NoPadding", key)
        val maxBytes = 2 * 1024 * 1024
        val exactBytes = ByteArray(maxBytes) { (it % 251).toByte() }
        val exactText = "a".repeat(maxBytes)

        val base64Bytes = crypto.encryptBase64(exactBytes)
        assertTrue("Base64 expansion must not be capped at 2 MiB", base64Bytes.length > maxBytes)
        assertEquals(maxBytes, crypto.decrypt(base64Bytes).size)
        assertTrue(crypto.encryptBase64(exactText).length > maxBytes)
        assertTrue(crypto.encryptBase64(exactText, "UTF-8").length > maxBytes)
        assertTrue(crypto.encryptBase64(exactText, Charsets.UTF_8).length > maxBytes)
        assertTrue(crypto.encryptBase64(ByteArrayInputStream(exactBytes)).length > maxBytes)

        val hexBytes = crypto.encryptHex(exactBytes)
        assertEquals(4 * 1024 * 1024, hexBytes.length)
        assertEquals(maxBytes, crypto.decrypt(hexBytes).size)
        assertEquals(4 * 1024 * 1024, crypto.encryptHex(exactText).length)
        assertEquals(4 * 1024 * 1024, crypto.encryptHex(exactText, "UTF-8").length)
        assertEquals(4 * 1024 * 1024, crypto.encryptHex(exactText, Charsets.UTF_8).length)
        assertEquals(4 * 1024 * 1024, crypto.encryptHex(ByteArrayInputStream(exactBytes)).length)

        val tooLongBytes = exactBytes + byteArrayOf(0)
        val tooLongText = exactText + "a"
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encrypt(tooLongBytes) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptBase64(tooLongBytes) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptHex(tooLongBytes) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptBase64(tooLongText) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptBase64(tooLongText, "UTF-8") }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptBase64(tooLongText, Charsets.UTF_8) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptBase64(ByteArrayInputStream(tooLongBytes)) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptHex(tooLongText) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptHex(tooLongText, "UTF-8") }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptHex(tooLongText, Charsets.UTF_8) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.encryptHex(ByteArrayInputStream(tooLongBytes)) }

        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { crypto.decrypt(tooLongBytes) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            crypto.decrypt(ByteArrayInputStream(tooLongBytes))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            crypto.decrypt("0".repeat(4 * 1024 * 1024 + 1))
        }
        val oversizedDecodedCiphertext = Base64.getEncoder().encodeToString(ByteArray(maxBytes + 1) { 0xFF.toByte() })
        assertTrue(oversizedDecodedCiphertext.any { it == '/' || it == '+' })
        assertEquals(maxBytes + 1, Base64.getDecoder().decode(oversizedDecodedCiphertext).size)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            crypto.decrypt(oversizedDecodedCiphertext)
        }

        val padded = RhinoBoundedSymmetricCryptoAndroid("AES/CBC/PKCS5Padding", key).apply {
            setIv("abcdef0123456789".toByteArray(Charsets.UTF_8))
        }
        val paddedRaw = padded.encrypt(exactBytes)
        assertEquals(maxBytes + 16, paddedRaw.size)
        assertTrue(padded.encryptBase64(exactBytes).length > maxBytes)
        assertTrue(padded.encryptHex(exactBytes).length > 4 * 1024 * 1024)
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { padded.decrypt(paddedRaw) }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            padded.decrypt(padded.encryptBase64(exactBytes))
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            padded.decrypt(padded.encryptHex(exactBytes))
        }

        requireRhinoCryptoEncodedSize(4L * 1024L * 1024L, "asymmetric encoded fixture")
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            requireRhinoCryptoEncodedSize(4L * 1024L * 1024L + 1L, "asymmetric encoded fixture")
        }
    }
}
