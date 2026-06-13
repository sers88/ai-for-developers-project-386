package com.aifordev.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "calendar_connections")
class CalendarConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,
    @Column(name = "google_email", nullable = false)
    val googleEmail: String,
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    val accessToken: String,
    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    val refreshToken: String,
    @Column(name = "token_expires_at", nullable = false)
    val tokenExpiresAt: Instant,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)
