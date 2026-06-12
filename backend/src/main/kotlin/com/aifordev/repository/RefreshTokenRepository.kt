package com.aifordev.repository

import com.aifordev.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): Optional<RefreshToken>

    fun deleteByUserEmail(email: String)
}
