package com.aifordev.controller

import com.aifordev.dto.PublicEventTypeResponse
import com.aifordev.service.EventTypeNotFoundException
import com.aifordev.service.PublicService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/public")
class PublicController(
    private val publicService: PublicService,
) {
    @GetMapping("/{userId}/{slug}")
    fun getPublicEventType(
        @PathVariable userId: UUID,
        @PathVariable slug: String,
    ): ResponseEntity<PublicEventTypeResponse> = ResponseEntity.ok(publicService.getPublicEventType(userId, slug))

    @ExceptionHandler(EventTypeNotFoundException::class)
    fun handleNotFound(ex: EventTypeNotFoundException): ResponseEntity<Map<String, String>> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("message" to (ex.message ?: "Event type not found")))
}
