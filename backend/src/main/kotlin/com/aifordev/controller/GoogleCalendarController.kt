package com.aifordev.controller

import com.aifordev.config.GoogleCalendarProperties
import com.aifordev.security.UserPrincipal
import com.aifordev.service.GoogleCalendarService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/calendar/google")
class GoogleCalendarController(
    private val calendarService: GoogleCalendarService,
    private val calendarProperties: GoogleCalendarProperties,
) {
    @GetMapping("/connect")
    fun connect(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<Void> {
        val authUrl = calendarService.buildAuthorizationUrl(principal.userId)
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI(authUrl))
            .build()
    }

    @GetMapping("/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(state)
        calendarService.processCallback(userId, code)

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI("${calendarProperties.frontendUrl}/settings?calendar=connected"))
            .build()
    }

    @GetMapping("/status")
    fun getStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<Map<String, Any?>> {
        val userId = UUID.fromString(principal.userId)
        val status = calendarService.getConnectionStatus(userId)
        return ResponseEntity.ok(status)
    }

    @GetMapping("/events")
    fun getBusySlots(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam timeMin: String,
        @RequestParam timeMax: String,
    ): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(principal.userId)
        val busy = calendarService.getBusySlots(userId, timeMin, timeMax)
        return ResponseEntity.ok(mapOf("busy" to busy))
    }

    @PostMapping("/events")
    fun createEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): ResponseEntity<Map<String, Any?>> {
        val userId = UUID.fromString(principal.userId)
        val summary =
            body["summary"] as? String
                ?: throw IllegalArgumentException("summary is required")
        val start =
            body["start"] as? String
                ?: throw IllegalArgumentException("start is required")
        val end =
            body["end"] as? String
                ?: throw IllegalArgumentException("end is required")
        val description = body["description"] as? String

        val event = calendarService.createEvent(userId, summary, start, end, description)
        return ResponseEntity.status(HttpStatus.CREATED).body(event)
    }

    @DeleteMapping("/events/{googleEventId}")
    fun deleteEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable googleEventId: String,
    ): ResponseEntity<Void> {
        val userId = UUID.fromString(principal.userId)
        calendarService.deleteEvent(userId, googleEventId)
        return ResponseEntity.noContent().build()
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        val status =
            when (ex.message) {
                "Event not found" -> HttpStatus.NOT_FOUND
                "Google Calendar is not connected" -> HttpStatus.BAD_REQUEST
                else -> HttpStatus.BAD_REQUEST
            }
        return ResponseEntity.status(status).body(mapOf("message" to (ex.message ?: "Bad request")))
    }
}
