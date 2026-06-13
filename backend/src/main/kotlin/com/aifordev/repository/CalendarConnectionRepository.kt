package com.aifordev.repository

import com.aifordev.entity.CalendarConnection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CalendarConnectionRepository : JpaRepository<CalendarConnection, UUID> {
    fun findByUserId(userId: UUID): Optional<CalendarConnection>

    fun deleteByUserId(userId: UUID)
}
