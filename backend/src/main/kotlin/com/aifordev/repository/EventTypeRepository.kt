package com.aifordev.repository

import com.aifordev.entity.EventType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface EventTypeRepository : JpaRepository<EventType, UUID> {
    fun findByUserId(userId: UUID): List<EventType>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Optional<EventType>

    fun existsByUserIdAndSlug(
        userId: UUID,
        slug: String,
    ): Boolean

    fun existsByUserIdAndSlugAndIdNot(
        userId: UUID,
        slug: String,
        id: UUID,
    ): Boolean
}
