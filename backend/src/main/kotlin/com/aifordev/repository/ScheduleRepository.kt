package com.aifordev.repository

import com.aifordev.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ScheduleRepository : JpaRepository<Schedule, UUID> {
    fun findByUserId(userId: UUID): List<Schedule>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Optional<Schedule>
}
