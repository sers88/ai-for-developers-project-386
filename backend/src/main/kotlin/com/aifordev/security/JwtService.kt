package com.aifordev.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.access-token-expiration-ms}") private val accessTokenExpirationMs: Long,
    @Value("\${app.jwt.refresh-token-expiration-ms}") private val refreshTokenExpirationMs: Long,
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(
        userId: String,
        email: String,
    ): String = buildToken(userId, email, accessTokenExpirationMs)

    fun generateRefreshToken(
        userId: String,
        email: String,
    ): String = buildToken(userId, email, refreshTokenExpirationMs)

    fun extractUserId(token: String): String = parseClaims(token).subject

    fun extractEmail(token: String): String = parseClaims(token)["email", String::class.java]

    fun isTokenValid(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    private fun buildToken(
        userId: String,
        email: String,
        expirationMs: Long,
    ): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts
            .builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    private fun parseClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
