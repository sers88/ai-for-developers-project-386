package com.aifordev.controller

import com.aifordev.dto.BookingResponse
import com.aifordev.dto.CreateBookingRequest
import com.aifordev.dto.ErrorResponse
import com.aifordev.service.BookingService
import com.aifordev.service.EventTypeNotFoundException
import com.aifordev.service.SlotAlreadyBookedException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class BookingController(
    private val bookingService: BookingService,
) {
    @PostMapping("/bookings")
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
    ): ResponseEntity<BookingResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(bookingService.createBooking(request))

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

    @ExceptionHandler(SlotAlreadyBookedException::class)
    fun handleSlotConflict(ex: SlotAlreadyBookedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = ex.message ?: "Slot already booked"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = message))
    }
}
