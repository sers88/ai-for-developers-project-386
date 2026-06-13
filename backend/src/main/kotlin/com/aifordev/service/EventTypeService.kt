package com.aifordev.service

import com.aifordev.dto.CreateEventTypeRequest
import com.aifordev.dto.EventTypeResponse
import com.aifordev.dto.UpdateEventTypeRequest
import com.aifordev.entity.EventType
import com.aifordev.repository.EventTypeRepository
import com.aifordev.repository.ScheduleRepository
import com.aifordev.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
class EventTypeService(
    private val eventTypeRepository: EventTypeRepository,
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository,
) {
    @Transactional
    fun createEventType(
        userId: UUID,
        request: CreateEventTypeRequest,
    ): EventTypeResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val slug = generateUniqueSlug(userId, request.title)

        val schedule =
            request.scheduleId?.let { scheduleId ->
                scheduleRepository
                    .findByIdAndUserId(UUID.fromString(scheduleId), userId)
                    .orElseThrow { IllegalArgumentException("Schedule not found") }
            }

        val eventType =
            EventType(
                user = user,
                title = request.title,
                description = request.description,
                slug = slug,
                duration = request.duration,
                schedule = schedule,
                bufferBefore = request.bufferBefore,
                bufferAfter = request.bufferAfter,
            )

        val saved = eventTypeRepository.save(eventType)
        return toResponse(saved)
    }

    @Transactional
    fun listEventTypes(userId: UUID): List<EventTypeResponse> =
        eventTypeRepository
            .findByUserId(userId)
            .map { toResponse(it) }

    @Transactional
    fun getEventType(
        userId: UUID,
        eventTypeId: UUID,
    ): EventTypeResponse {
        val eventType =
            eventTypeRepository
                .findByIdAndUserId(eventTypeId, userId)
                .orElseThrow { EventTypeNotFoundException("Event type not found") }
        return toResponse(eventType)
    }

    @Transactional
    fun updateEventType(
        userId: UUID,
        eventTypeId: UUID,
        request: UpdateEventTypeRequest,
    ): EventTypeResponse {
        val eventType =
            eventTypeRepository
                .findByIdAndUserId(eventTypeId, userId)
                .orElseThrow { EventTypeNotFoundException("Event type not found") }

        request.title?.let { newTitle ->
            if (newTitle != eventType.title) {
                val newSlug = generateUniqueSlug(userId, newTitle, excludeId = eventTypeId)
                eventType.title = newTitle
                eventType.slug = newSlug
            }
        }

        request.description?.let { eventType.description = it }
        request.duration?.let { eventType.duration = it }
        request.bufferBefore?.let { eventType.bufferBefore = it }
        request.bufferAfter?.let { eventType.bufferAfter = it }

        if (request.scheduleId != null) {
            if (request.scheduleId == "") {
                eventType.schedule = null
            } else {
                val schedule =
                    scheduleRepository
                        .findByIdAndUserId(UUID.fromString(request.scheduleId), userId)
                        .orElseThrow { IllegalArgumentException("Schedule not found") }
                eventType.schedule = schedule
            }
        }

        val saved = eventTypeRepository.save(eventType)
        return toResponse(saved)
    }

    @Transactional
    fun deleteEventType(
        userId: UUID,
        eventTypeId: UUID,
    ) {
        val eventType =
            eventTypeRepository
                .findByIdAndUserId(eventTypeId, userId)
                .orElseThrow { EventTypeNotFoundException("Event type not found") }
        eventTypeRepository.delete(eventType)
    }

    private fun generateUniqueSlug(
        userId: UUID,
        title: String,
        excludeId: UUID? = null,
    ): String {
        var baseSlug = titleToSlug(title)
        if (baseSlug.isBlank()) {
            baseSlug = "event-type"
        }

        val duplicateExists =
            if (excludeId != null) {
                eventTypeRepository.existsByUserIdAndSlugAndIdNot(userId, baseSlug, excludeId)
            } else {
                eventTypeRepository.existsByUserIdAndSlug(userId, baseSlug)
            }

        if (!duplicateExists) {
            return baseSlug
        }

        var suffix = 1
        var candidate: String
        do {
            candidate = "$baseSlug-$suffix"
            val exists =
                if (excludeId != null) {
                    eventTypeRepository.existsByUserIdAndSlugAndIdNot(userId, candidate, excludeId)
                } else {
                    eventTypeRepository.existsByUserIdAndSlug(userId, candidate)
                }
            if (!exists) return candidate
            suffix++
        } while (true)
    }

    private fun titleToSlug(title: String): String {
        val normalized =
            Normalizer
                .normalize(title, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return normalized
            .lowercase(Locale.ENGLISH)
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
            .trim('-')
    }

    private fun toResponse(eventType: EventType): EventTypeResponse =
        EventTypeResponse(
            id = eventType.id.toString(),
            userId = eventType.user.id.toString(),
            title = eventType.title,
            description = eventType.description,
            slug = eventType.slug,
            duration = eventType.duration,
            scheduleId = eventType.schedule?.id?.toString(),
            scheduleName = eventType.schedule?.name,
            bufferBefore = eventType.bufferBefore,
            bufferAfter = eventType.bufferAfter,
            bookingUrl = "/book/${eventType.user.id}/${eventType.slug}",
            createdAt = eventType.createdAt.atOffset(ZoneOffset.UTC).toString(),
        )
}
