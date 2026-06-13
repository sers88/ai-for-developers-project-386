package com.aifordev.service

import com.aifordev.dto.AvailabilityResponse
import com.aifordev.dto.AvailabilitySlot
import com.aifordev.dto.CreateScheduleRequest
import com.aifordev.dto.ScheduleResponse
import com.aifordev.dto.UpdateScheduleRequest
import com.aifordev.entity.Availability
import com.aifordev.entity.Schedule
import com.aifordev.repository.ScheduleRepository
import com.aifordev.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.util.UUID

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createSchedule(
        userId: UUID,
        request: CreateScheduleRequest,
    ): ScheduleResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        validateSlots(request.availabilities)

        val schedule = Schedule(user = user, name = request.name, timezone = request.timezone)
        request.availabilities.forEach { slot ->
            schedule.availabilities.add(
                Availability(
                    schedule = schedule,
                    dayOfWeek = slot.dayOfWeek,
                    startTime = LocalTime.parse(slot.startTime),
                    endTime = LocalTime.parse(slot.endTime),
                ),
            )
        }

        val saved = scheduleRepository.save(schedule)
        return toResponse(saved)
    }

    @Transactional
    fun listSchedules(userId: UUID): List<ScheduleResponse> {
        val schedules = scheduleRepository.findByUserId(userId)
        if (schedules.isEmpty()) {
            val default = createDefaultSchedule(userId)
            return listOf(default)
        }
        return schedules.map { toResponse(it) }
    }

    @Transactional
    fun updateSchedule(
        userId: UUID,
        scheduleId: UUID,
        request: UpdateScheduleRequest,
    ): ScheduleResponse {
        val schedule =
            scheduleRepository
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow { ScheduleNotFoundException("Schedule not found") }

        request.name?.let { schedule.name = it }
        request.timezone?.let { schedule.timezone = it }

        if (request.availabilities != null) {
            validateSlots(request.availabilities)
            schedule.availabilities.clear()
            request.availabilities.forEach { slot ->
                schedule.availabilities.add(
                    Availability(
                        schedule = schedule,
                        dayOfWeek = slot.dayOfWeek,
                        startTime = LocalTime.parse(slot.startTime),
                        endTime = LocalTime.parse(slot.endTime),
                    ),
                )
            }
        }

        val saved = scheduleRepository.save(schedule)
        return toResponse(saved)
    }

    @Transactional
    fun deleteSchedule(
        userId: UUID,
        scheduleId: UUID,
    ) {
        val schedule =
            scheduleRepository
                .findByIdAndUserId(scheduleId, userId)
                .orElseThrow { ScheduleNotFoundException("Schedule not found") }
        scheduleRepository.delete(schedule)
    }

    private fun createDefaultSchedule(userId: UUID): ScheduleResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val schedule = Schedule(user = user, name = "Default", timezone = "UTC")

        val days = listOf("MON", "TUE", "WED", "THU", "FRI")
        days.forEach { day ->
            schedule.availabilities.add(
                Availability(
                    schedule = schedule,
                    dayOfWeek = day,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(18, 0),
                ),
            )
        }

        val saved = scheduleRepository.save(schedule)
        return toResponse(saved)
    }

    private fun validateSlots(slots: List<AvailabilitySlot>) {
        slots.forEach { slot ->
            val start = LocalTime.parse(slot.startTime)
            val end = LocalTime.parse(slot.endTime)
            if (!start.isBefore(end)) {
                throw IllegalArgumentException("startTime must be before endTime")
            }
        }

        val byDay = slots.groupBy { it.dayOfWeek }
        byDay.forEach { (_, windows) ->
            val sorted = windows.sortedBy { LocalTime.parse(it.startTime) }
            for (i in 0 until sorted.size - 1) {
                if (LocalTime.parse(sorted[i].endTime) > LocalTime.parse(sorted[i + 1].startTime)) {
                    throw IllegalArgumentException("Availability windows must not overlap")
                }
            }
        }
    }

    private fun toResponse(schedule: Schedule): ScheduleResponse =
        ScheduleResponse(
            id = schedule.id.toString(),
            userId = schedule.user.id.toString(),
            name = schedule.name,
            timezone = schedule.timezone,
            availabilities =
                schedule.availabilities.map { a ->
                    AvailabilityResponse(
                        id = a.id.toString(),
                        dayOfWeek = a.dayOfWeek,
                        startTime = a.startTime.toString(),
                        endTime = a.endTime.toString(),
                    )
                },
        )
}
