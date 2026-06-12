package com.aifordev.service

import com.aifordev.dto.AuthResponse
import com.aifordev.dto.LoginRequest
import com.aifordev.dto.RefreshRequest
import com.aifordev.dto.RegisterRequest
import com.aifordev.dto.TokenResponse
import com.aifordev.dto.UserResponse
import com.aifordev.entity.RefreshToken
import com.aifordev.entity.User
import com.aifordev.repository.RefreshTokenRepository
import com.aifordev.repository.UserRepository
import com.aifordev.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user =
            User(
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
            )

        val savedUser = userRepository.save(user)
        return generateAuthResponse(savedUser)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user =
            userRepository
                .findByEmail(request.email)
                .orElseThrow { IllegalArgumentException("Invalid email or password") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        return generateAuthResponse(user)
    }

    @Transactional
    fun refresh(request: RefreshRequest): TokenResponse {
        if (!jwtService.isTokenValid(request.refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        val storedToken =
            refreshTokenRepository
                .findByToken(request.refreshToken)
                .orElseThrow { IllegalArgumentException("Refresh token not found") }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken)
            throw IllegalArgumentException("Refresh token expired")
        }

        val user = storedToken.user
        val accessToken = jwtService.generateAccessToken(user.id.toString(), user.email)

        return TokenResponse(accessToken = accessToken)
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
