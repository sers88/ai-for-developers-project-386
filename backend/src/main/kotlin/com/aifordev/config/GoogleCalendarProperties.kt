package com.aifordev.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.oauth2.google.calendar")
data class GoogleCalendarProperties(
    val redirectUri: String = "http://localhost:8080/api/calendar/google/callback",
    val encryptionKey: String = "default-test-encryption-key-min-32-bytes!!",
    val frontendUrl: String = "http://localhost:3000",
)
