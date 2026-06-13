package com.aifordev.service

import com.aifordev.dto.BookingResponse
import com.aifordev.dto.CreateBookingRequest
import com.aifordev.entity.Booking
import com.aifordev.repository.BookingRepository
import com.aifordev.repository.EventTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Service
class BookingService(
    private val eventTypeRepository: EventTypeRepository,
    private val bookingRepository: BookingRepository,
    private val googleCalendarService: GoogleCalendarService,
    private val availabilityService: AvailabilityService,
) {
    @Transactional
    fun createBooking(request: CreateBookingRequest): BookingResponse {
        val eventTypeId = UUID.fromString(request.eventTypeId)
        val eventType =
            eventTypeRepository
                .findById(eventTypeId)
                .orElseThrow { EventTypeNotFoundException("Event type not found") }

        val startTime = Instant.parse(request.startTime)
        val endTime = Instant.parse(request.endTime)

        if (!endTime.isAfter(startTime)) {
            throw IllegalArgumentException("endTime must be after startTime")
        }

        if (endTime.toEpochMilli() - startTime.toEpochMilli() != eventType.duration.toLong() * 60 * 1000) {
            throw IllegalArgumentException("Slot duration must match event type duration (${eventType.duration} minutes)")
        }

        val conflicts =
            bookingRepository.findConflictingBookings(
                eventTypeId = eventTypeId,
                rangeStart = startTime.minusSeconds(eventType.bufferBefore.toLong() * 60),
                rangeEnd = endTime.plusSeconds(eventType.bufferAfter.toLong() * 60),
            )

        if (conflicts.isNotEmpty()) {
            throw SlotAlreadyBookedException("This time slot is already booked")
        }

        val booking =
            Booking(
                eventType = eventType,
                guestName = request.guestName,
                guestEmail = request.guestEmail,
                notes = request.notes,
                startTime = startTime,
                endTime = endTime,
            )

        val saved = bookingRepository.save(booking)

        val userId = eventType.user.id
        if (userId != null) {
            try {
                val calendarEvent =
                    googleCalendarService.createEvent(
                        userId = userId,
                        summary = "${eventType.title} — ${request.guestName}",
                        start = startTime.toString(),
                        end = endTime.toString(),
                        description = request.notes,
                    )
                val googleEventId = calendarEvent["googleEventId"] as? String
                if (googleEventId != null) {
                    saved.googleEventId = googleEventId
                    bookingRepository.save(saved)
                }
            } catch (e: Exception) {
                // Booking succeeds even if calendar event creation fails
            }
        }

        return toResponse(saved)
    }

    private fun toResponse(booking: Booking): BookingResponse =
        BookingResponse(
            id = booking.id.toString(),
            eventTypeId = booking.eventType.id.toString(),
            eventTitle = booking.eventType.title,
            guestName = booking.guestName,
            guestEmail = booking.guestEmail,
            notes = booking.notes,
            startTime = booking.startTime.toString(),
            endTime = booking.endTime.toString(),
            status = booking.status,
            createdAt = booking.createdAt.atOffset(ZoneOffset.UTC).toString(),
        )
}
