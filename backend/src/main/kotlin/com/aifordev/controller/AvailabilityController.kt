package com.aifordev.controller

import com.aifordev.dto.AvailabilitySlotsResponse
import com.aifordev.dto.ErrorResponse
import com.aifordev.service.AvailabilityService
import com.aifordev.service.EventTypeNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/event-types")
class AvailabilityController(
    private val availabilityService: AvailabilityService,
) {
    @GetMapping("/{id}/availability")
    fun getAvailability(
        @PathVariable id: UUID,
        @RequestParam date: String,
    ): ResponseEntity<AvailabilitySlotsResponse> = ResponseEntity.ok(availabilityService.getAvailability(id, LocalDate.parse(date)))

    @ExceptionHandler(EventTypeNotFoundException::class)
    fun handleNotFound(ex: EventTypeNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Event type not found"))
}
