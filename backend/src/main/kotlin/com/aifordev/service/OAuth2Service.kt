package com.aifordev.service

import com.aifordev.config.GoogleOAuth2Properties
import com.aifordev.dto.AuthResponse
import com.aifordev.dto.UserResponse
import com.aifordev.entity.RefreshToken
import com.aifordev.entity.User
import com.aifordev.repository.RefreshTokenRepository
import com.aifordev.repository.UserRepository
import com.aifordev.security.JwtService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

@Service
class OAuth2Service(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val googleProperties: GoogleOAuth2Properties,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient by lazy { restClientBuilder.build() }

    fun buildAuthorizationUrl(): String =
        UriComponentsBuilder
            .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", googleProperties.clientId)
            .queryParam("redirect_uri", googleProperties.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email profile")
            .queryParam("access_type", "offline")
            .build(true)
            .toUriString()

    @Transactional
    fun processCallback(code: String): AuthResponse {
        val tokenResponse = exchangeCodeForTokens(code)
        val accessToken =
            tokenResponse["access_token"] as? String
                ?: throw IllegalArgumentException("Failed to obtain access token from Google")

        val userInfo = fetchUserInfo(accessToken)
        val email =
            userInfo["email"] as? String
                ?: throw IllegalArgumentException("Email not provided by Google")
        val googleId =
            userInfo["sub"] as? String
                ?: throw IllegalArgumentException("Google ID not provided")
        val name = userInfo["name"] as? String
        val avatarUrl = userInfo["picture"] as? String

        val user = findOrCreateUser(email, googleId, name, avatarUrl)
        return generateAuthResponse(user)
    }

    @Suppress("UNCHECKED_CAST")
    private fun exchangeCodeForTokens(code: String): Map<String, Any> {
        val body: MultiValueMap<String, String> = LinkedMultiValueMap()
        body.add("code", code)
        body.add("client_id", googleProperties.clientId)
        body.add("client_secret", googleProperties.clientSecret)
        body.add("redirect_uri", googleProperties.redirectUri)
        body.add("grant_type", "authorization_code")

        val response =
            restClient
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map::class.java)
                as? Map<String, Any>

        return response ?: throw IllegalArgumentException("Empty response from Google token endpoint")
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchUserInfo(accessToken: String): Map<String, Any> {
        val response =
            restClient
                .get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(Map::class.java)
                as? Map<String, Any>

        return response ?: throw IllegalArgumentException("Empty response from Google userinfo endpoint")
    }

    private fun findOrCreateUser(
        email: String,
        googleId: String,
        name: String?,
        avatarUrl: String?,
    ): User {
        val existingByGoogle = userRepository.findByGoogleId(googleId)
        if (existingByGoogle.isPresent) {
            return existingByGoogle.get()
        }

        val existingByEmail = userRepository.findByEmail(email)
        if (existingByEmail.isPresent) {
            val user = existingByEmail.get()
            val updated =
                User(
                    id = user.id,
                    email = user.email,
                    passwordHash = user.passwordHash,
                    googleId = googleId,
                    name = name ?: user.name,
                    avatarUrl = avatarUrl ?: user.avatarUrl,
                    createdAt = user.createdAt,
                )
            return userRepository.save(updated)
        }

        val newUser =
            User(
                email = email,
                googleId = googleId,
                name = name,
                avatarUrl = avatarUrl,
            )
        return userRepository.save(newUser)
    }

    private fun generateAuthResponse(user: User): AuthResponse {
        val userId = user.id.toString()
        val accessToken = jwtService.generateAccessToken(userId, user.email)
        val refreshTokenString = jwtService.generateRefreshToken(userId, user.email)

        val refreshTokenEntity =
            RefreshToken(
                user = user,
                token = refreshTokenString,
                expiresAt = Instant.now().plusMillis(604800000),
            )
        refreshTokenRepository.save(refreshTokenEntity)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenString,
            user = UserResponse(id = userId, email = user.email),
        )
    }
}
