package com.notp9194bot.jnotes.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object Hashing {
    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.toHex()
    }

    fun newSalt(bytes: Int = 16): String {
        val b = ByteArray(bytes)
        SecureRandom().nextBytes(b)
        return b.toHex()
    }

    fun pbkdf2(input: String, saltHex: String, iterations: Int = 120_000, keyLenBits: Int = 256): String {
        val salt = saltHex.hexToBytes()
        val spec = PBEKeySpec(input.toCharArray(), salt, iterations, keyLenBits)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return key.toHex()
    }

    /**
     * Verifies a PIN against stored hash. If salt is null/empty, falls back to legacy SHA-256
     * format used in app v1 so existing PINs still work.
     */
    fun verifyPin(pin: String, storedHash: String, storedSalt: String?): Boolean {
        return if (storedSalt.isNullOrEmpty()) {
            sha256(pin).equals(storedHash, ignoreCase = true)
        } else {
            pbkdf2(pin, storedSalt).equals(storedHash, ignoreCase = true)
        }
    }
}

private fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        if (v < 0x10) sb.append('0')
        sb.append(Integer.toHexString(v))
    }
    return sb.toString()
}

private fun String.hexToBytes(): ByteArray {
    val s = if (length % 2 == 0) this else "0$this"
    val out = ByteArray(s.length / 2)
    for (i in out.indices) {
        out[i] = ((Character.digit(s[i * 2], 16) shl 4) + Character.digit(s[i * 2 + 1], 16)).toByte()
    }
    return out
}
