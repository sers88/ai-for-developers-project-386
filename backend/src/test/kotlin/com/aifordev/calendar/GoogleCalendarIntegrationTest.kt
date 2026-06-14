package com.aifordev.calendar

import com.aifordev.contract.ContractValidator
import com.aifordev.dto.RegisterRequest
import com.aifordev.service.GoogleCalendarService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.openapi4j.operation.validator.model.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.net.HttpURLConnection
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctaW4tdGVzdHM",
        "app.jwt.access-token-expiration-ms=900000",
        "app.jwt.refresh-token-expiration-ms=604800000",
        "app.oauth2.google.client-id=test-client-id",
        "app.oauth2.google.client-secret=test-client-secret",
        "app.oauth2.google.redirect-uri=http://localhost:8080/api/auth/oauth2/google/callback",
        "app.oauth2.google.frontend-url=http://localhost:3000",
        "app.oauth2.google.calendar.redirect-uri=http://localhost:8080/api/calendar/google/callback",
        "app.oauth2.google.calendar.frontend-url=http://localhost:3000",
        "app.oauth2.google.calendar.encryption-key=test-encryption-key-min-32-bytes!!",
        "spring.mail.host=localhost",
    ],
)
class GoogleCalendarIntegrationTest {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate

    @MockitoBean
    private lateinit var googleCalendarService: GoogleCalendarService

    private val mapper = ObjectMapper()

    companion object {
        private val emailCounter = AtomicInteger(0)
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerAndGetToken(): Pair<String, UUID> {
        val email = "cal${emailCounter.incrementAndGet()}@example.com"
        val request = RegisterRequest(email = email, password = "password123")
        val responseStr =
            testRestTemplate.postForEntity("/api/auth/register", HttpEntity(request), String::class.java)
        val bodyMap = mapper.readValue(responseStr.body!!, Map::class.java)
        val token = bodyMap["accessToken"] as String
        val userId = UUID.fromString((bodyMap["user"] as Map<String, String>)["id"] as String)
        return Pair(token, userId)
    }

    private fun authHeaders(token: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        return headers
    }

    private fun noRedirectRestTemplate(): RestTemplate {
        val factory =
            object : SimpleClientHttpRequestFactory() {
                override fun prepareConnection(
                    connection: HttpURLConnection,
                    httpMethod: String,
                ) {
                    super.prepareConnection(connection, httpMethod)
                    connection.instanceFollowRedirects = false
                }
            }
        return RestTemplate(factory)
    }

    private fun getRedirect(
        path: String,
        headers: HttpHeaders? = null,
    ): ResponseEntity<Void> =
        noRedirectRestTemplate().exchange(
            "http://localhost:$port$path",
            HttpMethod.GET,
            if (headers != null) HttpEntity<Any>(headers) else null,
            Void::class.java,
        )

    @Test
    fun `connect endpoint requires authentication`() {
        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/connect",
                HttpMethod.GET,
                null,
                Void::class.java,
            )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `connect endpoint redirects to Google for authenticated user`() {
        val (token, userId) = registerAndGetToken()
        given(googleCalendarService.buildAuthorizationUrl(userId.toString())).willReturn(
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=test&redirect_uri=test&scope=calendar",
        )

        val response = getRedirect("/api/calendar/google/connect", authHeaders(token))

        assertTrue(response.statusCode.is3xxRedirection)
        val location = response.headers.location!!
        assertTrue(location.toString().contains("accounts.google.com"))
    }

    @Test
    fun `callback redirects to frontend with connected status`() {
        val (token, userId) = registerAndGetToken()

        val redirectResponse = getRedirect("/api/calendar/google/callback?code=test-code&state=$userId")

        assertTrue(redirectResponse.statusCode.is3xxRedirection)
        val location = redirectResponse.headers.location!!
        assertTrue(location.toString().contains("/settings"))
        assertTrue(location.toString().contains("calendar=connected"))
    }

    @Test
    fun `status returns not connected for user without connection`() {
        val (token, userId) = registerAndGetToken()
        given(googleCalendarService.getConnectionStatus(userId)).willReturn(
            mapOf("connected" to false),
        )

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/status",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(false, body["connected"])
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/status", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `status returns connected for user with connection`() {
        val (token, userId) = registerAndGetToken()
        given(googleCalendarService.getConnectionStatus(userId)).willReturn(
            mapOf("connected" to true, "email" to "user@gmail.com"),
        )

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/status",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(true, body["connected"])
        assertEquals("user@gmail.com", body["email"])
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/status", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `get busy slots returns list of slots`() {
        val (token, userId) = registerAndGetToken()
        given(
            googleCalendarService.getBusySlots(
                userId,
                "2026-01-01T00:00:00Z",
                "2026-01-02T00:00:00Z",
            ),
        ).willReturn(
            listOf(
                mapOf("start" to "2026-01-01T09:00:00Z", "end" to "2026-01-01T10:00:00Z"),
            ),
        )

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/events?timeMin=2026-01-01T00:00:00Z&timeMax=2026-01-02T00:00:00Z",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        @Suppress("UNCHECKED_CAST")
        val busy = body["busy"] as List<Map<String, String>>
        assertEquals(1, busy.size)
        assertEquals("2026-01-01T09:00:00Z", busy[0]["start"])
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/events", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `create event returns googleEventId`() {
        val (token, userId) = registerAndGetToken()
        given(
            googleCalendarService.createEvent(
                userId,
                "Test Event",
                "2026-01-01T09:00:00Z",
                "2026-01-01T10:00:00Z",
                null,
            ),
        ).willReturn(
            mapOf(
                "googleEventId" to "evt-123",
                "summary" to "Test Event",
                "start" to "2026-01-01T09:00:00Z",
                "end" to "2026-01-01T10:00:00Z",
            ),
        )

        val requestBody =
            mapOf(
                "summary" to "Test Event",
                "start" to "2026-01-01T09:00:00Z",
                "end" to "2026-01-01T10:00:00Z",
            )

        val response =
            testRestTemplate.postForEntity(
                "/api/calendar/google/events",
                HttpEntity(requestBody, authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("evt-123", body["googleEventId"])
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/events", Request.Method.POST, 201, bodyJson)
    }

    @Test
    fun `delete event returns no content`() {
        val (token, _) = registerAndGetToken()

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/events/evt-123",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders(token)),
                Void::class.java,
            )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        ContractValidator.validateResponse("/api/calendar/google/events/{googleEventId}", Request.Method.DELETE, 204)
    }

    @Test
    fun `calendar not connected returns error for events`() {
        val (token, userId) = registerAndGetToken()
        given(
            googleCalendarService.getBusySlots(
                userId,
                "2026-01-01T00:00:00Z",
                "2026-01-02T00:00:00Z",
            ),
        ).willThrow(IllegalArgumentException("Google Calendar is not connected"))

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/events?timeMin=2026-01-01T00:00:00Z&timeMax=2026-01-02T00:00:00Z",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("Google Calendar is not connected", body["message"])
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/events", Request.Method.GET, 400, bodyJson)
    }

    @Test
    fun `delete non-existent event returns not found`() {
        val (token, userId) = registerAndGetToken()
        given(
            googleCalendarService.deleteEvent(
                userId,
                "evt-nonexistent",
            ),
        ).willThrow(IllegalArgumentException("Event not found"))

        val response =
            testRestTemplate.exchange(
                "/api/calendar/google/events/evt-nonexistent",
                HttpMethod.DELETE,
                HttpEntity<Any>(authHeaders(token)),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/calendar/google/events/{googleEventId}", Request.Method.DELETE, 404, bodyJson)
    }
}
