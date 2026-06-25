package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasscodeCredential(
    val passwordHash: String,
    val salt: String
)

object PasscodeHasher {
    private const val PREFIX = "pbkdf2_sha256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private val secureRandom = SecureRandom()

    fun hash(passcode: String): PasscodeCredential {
        val saltBytes = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(saltBytes)
        val hashBytes = pbkdf2(passcode, saltBytes, ITERATIONS)
        val salt = encode(saltBytes)
        val hash = listOf(PREFIX, ITERATIONS.toString(), salt, encode(hashBytes)).joinToString("$")
        return PasscodeCredential(passwordHash = hash, salt = salt)
    }

    fun verify(passcode: String, storedHash: String, salt: String): Boolean {
        return if (storedHash.startsWith("${PREFIX}$")) {
            verifyPbkdf2(passcode, storedHash)
        } else {
            constantTimeEquals(storedHash, legacySha256(passcode, salt))
        }
    }

    fun needsRehash(storedHash: String): Boolean = !storedHash.startsWith("${PREFIX}$")

    internal fun legacySha256(passcode: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest((passcode + salt).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPbkdf2(passcode: String, storedHash: String): Boolean {
        val parts = storedHash.split("$")
        if (parts.size != 4 || parts[0] != PREFIX) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val saltBytes = decode(parts[2]) ?: return false
        val expectedHash = decode(parts[3]) ?: return false
        val actualHash = pbkdf2(passcode, saltBytes, iterations)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    private fun pbkdf2(passcode: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passcode.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        return MessageDigest.isEqual(
            left.toByteArray(Charsets.UTF_8),
            right.toByteArray(Charsets.UTF_8)
        )
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)

    private fun decode(value: String): ByteArray? = try {
        Base64.getDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
