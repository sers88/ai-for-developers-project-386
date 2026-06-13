package com.aifordev.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateBookingRequest(
    @field:NotBlank(message = "eventTypeId is required")
    val eventTypeId: String,
    @field:NotBlank(message = "Guest name is required")
    val guestName: String,
    @field:NotBlank(message = "Guest email is required")
    @field:Email(message = "Guest email must be valid")
    val guestEmail: String,
    val notes: String? = null,
    @field:NotBlank(message = "startTime is required")
    val startTime: String,
    @field:NotBlank(message = "endTime is required")
    val endTime: String,
    val timezone: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BookingResponse(
    val id: String,
    val eventTypeId: String,
    val eventTitle: String,
    val guestName: String,
    val guestEmail: String,
    val notes: String?,
    val startTime: String,
    val endTime: String,
    val status: String,
    val createdAt: String,
)

data class AvailabilitySlotResponse(
    val start: String,
    val end: String,
)

data class AvailabilitySlotsResponse(
    val date: String,
    val slots: List<AvailabilitySlotResponse>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PublicEventTypeResponse(
    val id: String,
    val title: String,
    val description: String?,
    val duration: Int,
    val slug: String,
    val bufferBefore: Int,
    val bufferAfter: Int,
    val timezone: String,
    val ownerName: String?,
)
