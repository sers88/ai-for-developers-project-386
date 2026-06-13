package com.aifordev.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateEventTypeRequest(
    @field:NotBlank(message = "Title is required")
    val title: String,
    val description: String? = null,
    @field:Min(value = 5, message = "Duration must be at least 5 minutes")
    val duration: Int,
    val scheduleId: String? = null,
    @field:Min(value = 0, message = "Buffer before must be non-negative")
    val bufferBefore: Int = 0,
    @field:Min(value = 0, message = "Buffer after must be non-negative")
    val bufferAfter: Int = 0,
)

data class UpdateEventTypeRequest(
    @field:NotBlank(message = "Title is required")
    val title: String? = null,
    val description: String? = null,
    @field:Min(value = 5, message = "Duration must be at least 5 minutes")
    val duration: Int? = null,
    val scheduleId: String? = null,
    @field:Min(value = 0, message = "Buffer before must be non-negative")
    val bufferBefore: Int? = null,
    @field:Min(value = 0, message = "Buffer after must be non-negative")
    val bufferAfter: Int? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventTypeResponse(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val slug: String,
    val duration: Int,
    val scheduleId: String?,
    val scheduleName: String?,
    val bufferBefore: Int,
    val bufferAfter: Int,
    val bookingUrl: String,
    val createdAt: String,
)
