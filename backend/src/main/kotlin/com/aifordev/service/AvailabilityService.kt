package com.aifordev.service

import com.aifordev.dto.AvailabilitySlotResponse
import com.aifordev.dto.AvailabilitySlotsResponse
import com.aifordev.entity.Availability
import com.aifordev.entity.EventType
import com.aifordev.repository.BookingRepository
import com.aifordev.repository.EventTypeRepository
import com.aifordev.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class AvailabilityService(
    private val eventTypeRepository: EventTypeRepository,
    private val bookingRepository: BookingRepository,
    private val googleCalendarService: GoogleCalendarService,
    private val userRepository: UserRepository,
) {
    fun getAvailability(
        eventTypeId: UUID,
        date: LocalDate,
    ): AvailabilitySlotsResponse {
        val eventType =
            eventTypeRepository
                .findById(eventTypeId)
                .orElseThrow { EventTypeNotFoundException("Event type not found") }

        val schedule = eventType.schedule
        if (schedule == null || schedule.availabilities.isEmpty()) {
            return AvailabilitySlotsResponse(date = date.toString(), slots = emptyList())
        }

        val zoneId = ZoneId.of(schedule.timezone)
        val dayOfWeek = date.atStartOfDay(zoneId).dayOfWeek

        val windows =
            schedule.availabilities
                .filter { it.dayOfWeek == dayOfWeek.name }
                .sortedBy { it.startTime }

        if (windows.isEmpty()) {
            return AvailabilitySlotsResponse(date = date.toString(), slots = emptyList())
        }

        val duration = eventType.duration
        val bufferBefore = eventType.bufferBefore
        val bufferAfter = eventType.bufferAfter

        val allBlockedRanges = mutableListOf<Pair<Instant, Instant>>()

        val dayStart = date.atStartOfDay(zoneId).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant()

        val existingBookings =
            bookingRepository.findConflictingBookings(
                eventTypeId = eventTypeId,
                rangeStart = dayStart.minusSeconds(bufferBefore.toLong() * 60),
                rangeEnd = dayEnd.plusSeconds(bufferAfter.toLong() * 60),
            )
        existingBookings.forEach { booking ->
            allBlockedRanges.add(
                booking.startTime.minusSeconds(bufferBefore.toLong() * 60) to
                    booking.endTime.plusSeconds(bufferAfter.toLong() * 60),
            )
        }

        val busySlots = getGoogleBusySlots(eventType, dayStart, dayEnd)
        busySlots.forEach { busy ->
            val start = Instant.parse(busy["start"])
            val end = Instant.parse(busy["end"])
            allBlockedRanges.add(
                start.minusSeconds(bufferBefore.toLong() * 60) to
                    end.plusSeconds(bufferAfter.toLong() * 60),
            )
        }

        val availableSlots = mutableListOf<AvailabilitySlotResponse>()

        for (window in windows) {
            val candidates = generateSlots(window, date, zoneId, duration)
            for (candidate in candidates) {
                val (slotStart, slotEnd) = candidate
                val bufferedStart = slotStart.minusSeconds(bufferBefore.toLong() * 60)
                val bufferedEnd = slotEnd.plusSeconds(bufferAfter.toLong() * 60)

                val conflicts =
                    allBlockedRanges.any { (blockStart, blockEnd) ->
                        bufferedStart < blockEnd && bufferedEnd > blockStart
                    }

                if (!conflicts && slotEnd.isAfter(Instant.now())) {
                    availableSlots.add(
                        AvailabilitySlotResponse(
                            start = slotStart.toString(),
                            end = slotEnd.toString(),
                        ),
                    )
                }
            }
        }

        return AvailabilitySlotsResponse(
            date = date.toString(),
            slots = availableSlots.sortedBy { it.start },
        )
    }

    private fun generateSlots(
        window: Availability,
        date: LocalDate,
        zoneId: ZoneId,
        durationMinutes: Int,
    ): List<Pair<Instant, Instant>> {
        val slots = mutableListOf<Pair<Instant, Instant>>()
        var current = ZonedDateTime.of(date, window.startTime, zoneId)
        val windowEnd = ZonedDateTime.of(date, window.endTime, zoneId)

        while (current.plusMinutes(durationMinutes.toLong()) <= windowEnd) {
            val slotEnd = current.plusMinutes(durationMinutes.toLong())
            slots.add(current.toInstant() to slotEnd.toInstant())
            current = slotEnd
        }

        return slots
    }

    private fun getGoogleBusySlots(
        eventType: EventType,
        dayStart: Instant,
        dayEnd: Instant,
    ): List<Map<String, String>> {
        val userId = eventType.user.id ?: return emptyList()
        val connectionExists =
            userRepository
                .findById(userId)
                .map { _ ->
                    try {
                        googleCalendarService.getBusySlots(
                            userId,
                            dayStart.toString(),
                            dayEnd.toString(),
                        )
                    } catch (e: Exception) {
                        emptyList()
                    }
                }.orElse(emptyList())
        return connectionExists
    }
}
