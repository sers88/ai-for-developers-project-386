package com.aifordev.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class AvailabilitySlot(
    @field:NotBlank(message = "dayOfWeek is required")
    val dayOfWeek: String,
    @field:NotBlank(message = "startTime is required")
    @field:Pattern(
        regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
        message = "startTime must be in HH:MM format",
    )
    val startTime: String,
    @field:NotBlank(message = "endTime is required")
    @field:Pattern(
        regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
        message = "endTime must be in HH:MM format",
    )
    val endTime: String,
)

data class CreateScheduleRequest(
    @field:NotBlank(message = "Schedule name is required")
    val name: String,
    @field:NotBlank(message = "Timezone is required")
    val timezone: String,
    @field:NotEmpty(message = "At least one availability window is required")
    val availabilities: List<@Valid AvailabilitySlot>,
)

data class UpdateScheduleRequest(
    val name: String? = null,
    val timezone: String? = null,
    val availabilities: List<@Valid AvailabilitySlot>? = null,
)

data class ScheduleResponse(
    val id: String,
    val userId: String,
    val name: String,
    val timezone: String,
    val availabilities: List<AvailabilityResponse>,
)

data class AvailabilityResponse(
    val id: String,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
)
