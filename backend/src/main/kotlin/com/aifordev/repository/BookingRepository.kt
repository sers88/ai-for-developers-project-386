package com.aifordev.repository

import com.aifordev.entity.Booking
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface BookingRepository : JpaRepository<Booking, UUID> {
    @Query(
        """
        SELECT b FROM Booking b
        WHERE b.eventType.id = :eventTypeId
          AND b.status = 'CONFIRMED'
          AND b.startTime < :rangeEnd
          AND b.endTime > :rangeStart
        """,
    )
    fun findConflictingBookings(
        @Param("eventTypeId") eventTypeId: UUID,
        @Param("rangeStart") rangeStart: Instant,
        @Param("rangeEnd") rangeEnd: Instant,
    ): List<Booking>
}
