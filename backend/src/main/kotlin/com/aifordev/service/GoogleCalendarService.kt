package com.aifordev.service

import com.aifordev.config.GoogleCalendarProperties
import com.aifordev.config.GoogleOAuth2Properties
import com.aifordev.entity.CalendarConnection
import com.aifordev.repository.CalendarConnectionRepository
import com.aifordev.repository.UserRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant
import java.util.UUID

@Service
class GoogleCalendarService(
    private val userRepository: UserRepository,
    private val connectionRepository: CalendarConnectionRepository,
    private val cryptoService: CryptoService,
    private val googleProperties: GoogleOAuth2Properties,
    private val calendarProperties: GoogleCalendarProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val restClient: RestClient by lazy { restClientBuilder.build() }

    fun buildAuthorizationUrl(userId: String): String =
        UriComponentsBuilder
            .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", googleProperties.clientId)
            .queryParam("redirect_uri", calendarProperties.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "https://www.googleapis.com/auth/calendar")
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .queryParam("state", userId)
            .build(true)
            .toUriString()

    @Transactional
    fun processCallback(
        userId: UUID,
        code: String,
    ) {
        val user =
            userRepository
                .findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }

        val tokenResponse = exchangeCodeForTokens(code)
        val accessToken =
            tokenResponse["access_token"] as? String
                ?: throw IllegalArgumentException("Failed to obtain access token from Google")
        val refreshToken =
            tokenResponse["refresh_token"] as? String
                ?: throw IllegalArgumentException("Failed to obtain refresh token from Google")
        val expiresIn =
            (tokenResponse["expires_in"] as? Number)?.toLong()
                ?: throw IllegalArgumentException("Token expiry not provided")

        val userInfo = fetchCalendarUserInfo(accessToken)
        val email =
            userInfo["email"] as? String
                ?: throw IllegalArgumentException("Email not provided by Google Calendar")

        val encryptedAccessToken = cryptoService.encrypt(accessToken)
        val encryptedRefreshToken = cryptoService.encrypt(refreshToken)
        val expiresAt = Instant.now().plusSeconds(expiresIn)

        val existing = connectionRepository.findByUserId(user.id!!)
        if (existing.isPresent) {
            val conn = existing.get()
            val updated =
                CalendarConnection(
                    id = conn.id,
                    user = conn.user,
                    googleEmail = email,
                    accessToken = encryptedAccessToken,
                    refreshToken = encryptedRefreshToken,
                    tokenExpiresAt = expiresAt,
                    createdAt = conn.createdAt,
                    updatedAt = Instant.now(),
                )
            connectionRepository.save(updated)
        } else {
            val connection =
                CalendarConnection(
                    user = user,
                    googleEmail = email,
                    accessToken = encryptedAccessToken,
                    refreshToken = encryptedRefreshToken,
                    tokenExpiresAt = expiresAt,
                )
            connectionRepository.save(connection)
        }
    }

    fun getConnectionStatus(userId: UUID): Map<String, Any?> {
        val connection = connectionRepository.findByUserId(userId)
        return if (connection.isPresent) {
            mapOf(
                "connected" to true,
                "email" to connection.get().googleEmail,
            )
        } else {
            mapOf("connected" to false)
        }
    }

    fun getBusySlots(
        userId: UUID,
        timeMin: String,
        timeMax: String,
    ): List<Map<String, String>> {
        val accessToken = getValidAccessToken(userId)

        val requestBody =
            mapOf(
                "timeMin" to timeMin,
                "timeMax" to timeMax,
                "items" to listOf(mapOf("id" to "primary")),
            )

        @Suppress("UNCHECKED_CAST")
        val response =
            restClient
                .post()
                .uri("https://www.googleapis.com/calendar/v3/freeBusy")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map::class.java) as? Map<String, Any>
                ?: throw IllegalArgumentException("Empty response from Google Calendar")

        @Suppress("UNCHECKED_CAST")
        val calendars =
            response["calendars"] as? Map<String, Any>
                ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val primary =
            calendars["primary"] as? Map<String, Any>
                ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val busy =
            primary["busy"] as? List<Map<String, String>>
                ?: return emptyList()

        return busy
    }

    fun createEvent(
        userId: UUID,
        summary: String,
        start: String,
        end: String,
        description: String?,
    ): Map<String, Any?> {
        val accessToken = getValidAccessToken(userId)

        val eventBody =
            mutableMapOf<String, Any>(
                "summary" to summary,
                "start" to mapOf("dateTime" to start),
                "end" to mapOf("dateTime" to end),
            )
        if (description != null) {
            eventBody["description"] = description
        }

        @Suppress("UNCHECKED_CAST")
        val response =
            restClient
                .post()
                .uri("https://www.googleapis.com/calendar/v3/calendars/primary/events")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(eventBody)
                .retrieve()
                .body(Map::class.java) as? Map<String, Any>
                ?: throw IllegalArgumentException("Failed to create calendar event")

        return mapOf(
            "googleEventId" to (response["id"] as? String),
            "summary" to (response["summary"] as? String),
            "description" to (response["description"] as? String),
            "start" to extractDateTime(response, "start"),
            "end" to extractDateTime(response, "end"),
        )
    }

    fun deleteEvent(
        userId: UUID,
        googleEventId: String,
    ) {
        val accessToken = getValidAccessToken(userId)

        try {
            restClient
                .delete()
                .uri(URI("https://www.googleapis.com/calendar/v3/calendars/primary/events/$googleEventId"))
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .toBodilessEntity()
        } catch (e: Exception) {
            if (e is org.springframework.web.client.HttpClientErrorException.NotFound) {
                throw IllegalArgumentException("Event not found")
            }
            throw e
        }
    }

    @Transactional
    fun getValidAccessToken(userId: UUID): String {
        val connection =
            connectionRepository
                .findByUserId(userId)
                .orElseThrow { IllegalArgumentException("Google Calendar is not connected") }

        if (connection.tokenExpiresAt.isAfter(Instant.now())) {
            return cryptoService.decrypt(connection.accessToken)
        }

        val refreshToken = cryptoService.decrypt(connection.refreshToken)
        return refreshAccessToken(connection, refreshToken)
    }

    private fun refreshAccessToken(
        connection: CalendarConnection,
        refreshToken: String,
    ): String {
        val body = LinkedMultiValueMap<String, String>()
        body.add("client_id", googleProperties.clientId)
        body.add("client_secret", googleProperties.clientSecret)
        body.add("refresh_token", refreshToken)
        body.add("grant_type", "refresh_token")

        @Suppress("UNCHECKED_CAST")
        val response =
            restClient
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map::class.java) as? Map<String, Any>
                ?: throw IllegalArgumentException("Failed to refresh access token")

        val newAccessToken =
            response["access_token"] as? String
                ?: throw IllegalArgumentException("Access token not in refresh response")
        val expiresIn = (response["expires_in"] as? Number)?.toLong() ?: 3600L

        val encryptedAccessToken = cryptoService.encrypt(newAccessToken)
        val updated =
            CalendarConnection(
                id = connection.id,
                user = connection.user,
                googleEmail = connection.googleEmail,
                accessToken = encryptedAccessToken,
                refreshToken = connection.refreshToken,
                tokenExpiresAt = Instant.now().plusSeconds(expiresIn),
                createdAt = connection.createdAt,
                updatedAt = Instant.now(),
            )
        connectionRepository.save(updated)

        return newAccessToken
    }

    @Suppress("UNCHECKED_CAST")
    private fun exchangeCodeForTokens(code: String): Map<String, Any> {
        val body = LinkedMultiValueMap<String, String>()
        body.add("code", code)
        body.add("client_id", googleProperties.clientId)
        body.add("client_secret", googleProperties.clientSecret)
        body.add("redirect_uri", calendarProperties.redirectUri)
        body.add("grant_type", "authorization_code")

        val response =
            restClient
                .post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map::class.java)
                as? Map<String, Any>

        return response ?: throw IllegalArgumentException("Empty response from Google token endpoint")
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchCalendarUserInfo(accessToken: String): Map<String, Any> {
        val response =
            restClient
                .get()
                .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                .header("Authorization", "Bearer $accessToken")
                .retrieve()
                .body(Map::class.java)
                as? Map<String, Any>

        return response ?: throw IllegalArgumentException("Empty response from Google userinfo endpoint")
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractDateTime(
        response: Map<String, Any>,
        key: String,
    ): String? {
        val nested = response[key] as? Map<String, Any>
        return nested?.get("dateTime") as? String
    }
}
