package com.aifordev

import com.aifordev.config.GoogleCalendarProperties
import com.aifordev.config.GoogleOAuth2Properties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(GoogleOAuth2Properties::class, GoogleCalendarProperties::class)
class AiForDevApplication

fun main(args: Array<String>) {
    runApplication<AiForDevApplication>(*args)
}
