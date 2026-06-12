package com.aifordev.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column(name = "password_hash")
    val passwordHash: String? = null,
    @Column(name = "google_id", unique = true)
    val googleId: String? = null,
    @Column
    val name: String? = null,
    @Column(name = "avatar_url")
    val avatarUrl: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
