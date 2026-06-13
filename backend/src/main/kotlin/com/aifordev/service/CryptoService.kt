package com.aifordev.service

import com.aifordev.config.GoogleCalendarProperties
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Service
class CryptoService(
    calendarProperties: GoogleCalendarProperties,
) {
    private val key: SecretKeySpec
    private val secureRandom = SecureRandom()
    private val salt = ByteArray(16) { 0x42.toByte() }
    private val gcmTagLength = 128
    private val gcmIvLength = 12

    init {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(calendarProperties.encryptionKey.toCharArray(), salt, 65536, 256)
        val secretKey = factory.generateSecret(spec)
        key = SecretKeySpec(secretKey.encoded, "AES")
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(gcmIvLength)
        secureRandom.nextBytes(iv)
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + cipherText
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedText: String): String {
        val combined = Base64.getDecoder().decode(encryptedText)
        val iv = combined.copyOfRange(0, gcmIvLength)
        val cipherText = combined.copyOfRange(gcmIvLength, combined.size)
        val gcmSpec = GCMParameterSpec(gcmTagLength, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }
}
