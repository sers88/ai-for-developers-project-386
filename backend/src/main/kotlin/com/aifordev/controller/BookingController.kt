package com.aifordev.controller

import com.aifordev.dto.BookingResponse
import com.aifordev.dto.CreateBookingRequest
import com.aifordev.dto.ErrorResponse
import com.aifordev.security.UserPrincipal
import com.aifordev.service.BookingAccessDeniedException
import com.aifordev.service.BookingAlreadyCancelledException
import com.aifordev.service.BookingNotFoundException
import com.aifordev.service.BookingService
import com.aifordev.service.EventTypeNotFoundException
import com.aifordev.service.SlotAlreadyBookedException
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class BookingController(
    private val bookingService: BookingService,
) {
    @GetMapping("/bookings")
    fun listBookings(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(name = "status", required = false) status: String?,
    ): ResponseEntity<List<BookingResponse>> = ResponseEntity.ok(bookingService.listBookings(UUID.fromString(principal.userId), status))

    @PostMapping("/bookings")
    fun createBooking(
        @Valid @RequestBody request: CreateBookingRequest,
    ): ResponseEntity<BookingResponse> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(bookingService.createBooking(request))

    @DeleteMapping("/bookings/{id}")
    fun cancelBooking(
        @AuthenticationPrincipal principal: UserPrincipal?,
        @PathVariable id: UUID,
        @RequestParam(name = "token", required = false) token: String?,
    ): ResponseEntity<BookingResponse> {
        val requesterUserId = principal?.userId?.let { UUID.fromString(it) }
        return ResponseEntity.ok(bookingService.cancelBooking(id, requesterUserId, token))
    }

    @ExceptionHandler(BookingNotFoundException::class)
    fun handleBookingNotFound(ex: BookingNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(message = ex.message ?: "Booking not found"))

    @ExceptionHandler(BookingAccessDeniedException::class)
    fun handleBookingAccessDenied(ex: BookingAccessDeniedException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = ex.message ?: "Unauthorized"))

    @ExceptionHandler(BookingAlreadyCancelledException::class)
    fun handleAlreadyCancelled(ex: BookingAlreadyCancelledException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(message = ex.message ?: "Booking already cancelled"))

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
