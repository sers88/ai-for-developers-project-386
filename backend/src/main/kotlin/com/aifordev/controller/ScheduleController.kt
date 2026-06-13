package com.aifordev.controller

import com.aifordev.dto.CreateScheduleRequest
import com.aifordev.dto.ErrorResponse
import com.aifordev.dto.ScheduleResponse
import com.aifordev.dto.UpdateScheduleRequest
import com.aifordev.security.UserPrincipal
import com.aifordev.service.ScheduleNotFoundException
import com.aifordev.service.ScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class ScheduleController(
    private val scheduleService: ScheduleService,
) {
    @PostMapping("/schedules")
    fun createSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateScheduleRequest,
    ): ResponseEntity<ScheduleResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(scheduleService.createSchedule(UUID.fromString(principal.userId), request))

    @GetMapping("/schedules")
    fun listSchedules(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<List<ScheduleResponse>> = ResponseEntity.ok(scheduleService.listSchedules(UUID.fromString(principal.userId)))

    @PutMapping("/schedules/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): ResponseEntity<ScheduleResponse> = ResponseEntity.ok(scheduleService.updateSchedule(UUID.fromString(principal.userId), id, request))

    @DeleteMapping("/schedules/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        scheduleService.deleteSchedule(UUID.fromString(principal.userId), id)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Bad request"))

    @ExceptionHandler(ScheduleNotFoundException::class)
    fun handleNotFound(ex: ScheduleNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Schedule not found"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message))
    }
}
