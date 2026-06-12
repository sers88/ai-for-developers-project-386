package com.aifordev.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
)

data class TokenResponse(
    val accessToken: String,
)

data class UserResponse(
    val id: String,
    val email: String,
)
