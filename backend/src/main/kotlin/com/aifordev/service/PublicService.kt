package com.aifordev.service

import com.aifordev.dto.PublicEventTypeResponse
import com.aifordev.repository.EventTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PublicService(
    private val eventTypeRepository: EventTypeRepository,
) {
    @Transactional
    fun getPublicEventType(
        userId: UUID,
        slug: String,
    ): PublicEventTypeResponse {
        val eventType =
            eventTypeRepository.findByUserIdAndSlug(userId, slug)
                ?: throw EventTypeNotFoundException("Event type not found")

        return PublicEventTypeResponse(
            id = eventType.id.toString(),
            title = eventType.title,
            description = eventType.description,
            duration = eventType.duration,
            slug = eventType.slug,
            bufferBefore = eventType.bufferBefore,
            bufferAfter = eventType.bufferAfter,
            timezone = eventType.schedule?.timezone ?: "UTC",
            ownerName = eventType.user.name,
        )
    }
}
