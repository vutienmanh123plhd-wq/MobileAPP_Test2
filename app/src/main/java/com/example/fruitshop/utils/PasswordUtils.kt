package com.example.fruitshop.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    fun hash(password: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return "pbkdf2_sha256:$ITERATIONS:${Base64.encodeToString(salt, Base64.NO_WRAP)}:${derive(password, salt, ITERATIONS)}"
    }
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
        return try {
            val expected = Base64.decode(parts[3], Base64.DEFAULT)
            val actual = Base64.decode(derive(password, Base64.decode(parts[2], Base64.DEFAULT), parts[1].toInt()), Base64.DEFAULT)
            java.security.MessageDigest.isEqual(expected, actual)
        } catch (_: Exception) { false }
    }
    private fun derive(password: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try { Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP) }
        finally { spec.clearPassword() }
    }
}

