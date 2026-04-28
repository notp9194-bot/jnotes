package com.notp9194bot.jnotes.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Simple AES-GCM envelope: salt(16) || iv(12) || ciphertext (Base64 wrapped).
 * Key is derived from a passphrase via PBKDF2-SHA256 (120k iterations).
 */
object Crypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun encrypt(passphrase: String, plaintext: ByteArray): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(plaintext)
        val out = ByteArray(salt.size + iv.size + ct.size)
        System.arraycopy(salt, 0, out, 0, salt.size)
        System.arraycopy(iv, 0, out, salt.size, iv.size)
        System.arraycopy(ct, 0, out, salt.size + iv.size, ct.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(passphrase: String, blob: String): ByteArray? = try {
        val all = Base64.decode(blob, Base64.NO_WRAP)
        val salt = all.copyOfRange(0, 16)
        val iv = all.copyOfRange(16, 28)
        val ct = all.copyOfRange(28, all.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.doFinal(ct)
    } catch (_: Throwable) { null }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }
}
