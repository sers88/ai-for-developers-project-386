package com.aifordev.service

import java.time.Instant

data class BookingEmailData(
    val guestName: String,
    val guestEmail: String,
    val eventTitle: String,
    val duration: Int,
    val startTime: Instant,
    val endTime: Instant,
    val ownerEmail: String,
    val ownerName: String,
    val cancelUrl: String,
    val notes: String?,
)
