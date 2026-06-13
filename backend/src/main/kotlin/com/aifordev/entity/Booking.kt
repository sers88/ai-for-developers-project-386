package com.aifordev.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "bookings")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id", nullable = false)
    val eventType: EventType,
    @Column(name = "guest_name", nullable = false)
    val guestName: String,
    @Column(name = "guest_email", nullable = false)
    val guestEmail: String,
    @Column(columnDefinition = "TEXT")
    val notes: String? = null,
    @Column(name = "start_time", nullable = false)
    val startTime: Instant,
    @Column(name = "end_time", nullable = false)
    val endTime: Instant,
    @Column(name = "google_event_id")
    var googleEventId: String? = null,
    @Column(nullable = false)
    var status: String = "CONFIRMED",
    @Column(name = "cancel_token", nullable = false, unique = true)
    val cancelToken: UUID = UUID.randomUUID(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
