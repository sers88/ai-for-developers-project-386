package com.aifordev.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.oauth2.google")
data class GoogleOAuth2Properties(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val frontendUrl: String,
)
