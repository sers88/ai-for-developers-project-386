package com.aifordev.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "availabilities")
class Availability(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    val schedule: Schedule,
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: String,
    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,
    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,
)
