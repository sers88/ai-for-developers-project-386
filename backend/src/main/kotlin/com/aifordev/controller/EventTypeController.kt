package com.aifordev.controller

import com.aifordev.dto.CreateEventTypeRequest
import com.aifordev.dto.ErrorResponse
import com.aifordev.dto.EventTypeResponse
import com.aifordev.dto.UpdateEventTypeRequest
import com.aifordev.security.UserPrincipal
import com.aifordev.service.EventTypeNotFoundException
import com.aifordev.service.EventTypeService
import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
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
class EventTypeController(
    private val eventTypeService: EventTypeService,
) {
    @PostMapping("/event-types")
    fun createEventType(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateEventTypeRequest,
    ): ResponseEntity<EventTypeResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(eventTypeService.createEventType(UUID.fromString(principal.userId), request))

    @GetMapping("/event-types")
    fun listEventTypes(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<List<EventTypeResponse>> = ResponseEntity.ok(eventTypeService.listEventTypes(UUID.fromString(principal.userId)))

    @GetMapping("/event-types/{id}")
    fun getEventType(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ResponseEntity<EventTypeResponse> = ResponseEntity.ok(eventTypeService.getEventType(UUID.fromString(principal.userId), id))

    @PutMapping("/event-types/{id}")
    fun updateEventType(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateEventTypeRequest,
    ): ResponseEntity<EventTypeResponse> =
        ResponseEntity.ok(eventTypeService.updateEventType(UUID.fromString(principal.userId), id, request))

    @DeleteMapping("/event-types/{id}")
    fun deleteEventType(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        eventTypeService.deleteEventType(UUID.fromString(principal.userId), id)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "Bad request"))

    @ExceptionHandler(EventTypeNotFoundException::class)
    fun handleNotFound(ex: EventTypeNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Event type not found"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        val message = ex.mostSpecificCause.message ?: "Data integrity violation"
        return if (message.contains("unique") || message.contains("duplicate")) {
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse(message = "Event type with this slug already exists"))
        } else {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(message = message))
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message))
    }
}
