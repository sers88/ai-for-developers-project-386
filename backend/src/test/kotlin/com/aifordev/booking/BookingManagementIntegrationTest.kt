package com.aifordev.booking

import com.aifordev.contract.ContractValidator
import com.aifordev.dto.AuthResponse
import com.aifordev.dto.BookingResponse
import com.aifordev.dto.CreateBookingRequest
import com.aifordev.dto.CreateEventTypeRequest
import com.aifordev.dto.EventTypeResponse
import com.aifordev.dto.RegisterRequest
import com.aifordev.service.EmailService
import com.aifordev.service.GoogleCalendarService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.openapi4j.operation.validator.model.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
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
        "app.frontend-url=http://localhost:3000",
        "app.mail.from=noreply@test.com",
    ],
)
class BookingManagementIntegrationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @MockitoBean
    private lateinit var googleCalendarService: GoogleCalendarService

    @MockitoBean
    private lateinit var emailService: EmailService

    private val mapper =
        ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    @BeforeEach
    fun setUp() {
        Mockito.clearInvocations(emailService, googleCalendarService)
        given(googleCalendarService.createEvent(any(), any(), any(), any(), any()))
            .willReturn(mapOf("googleEventId" to "google-evt-123"))
        given(emailService.buildCancelUrl(any(), any(), any(), any()))
            .willReturn("http://localhost:3000/cancel")
    }

    private fun registerAndGetToken(email: String): String {
        val request = RegisterRequest(email = email, password = "password123")
        val response = restTemplate.postForEntity("/api/auth/register", HttpEntity(request), AuthResponse::class.java)
        return response.body!!.accessToken
    }

    private fun authHeaders(token: String): HttpEntity<Any> {
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        return HttpEntity(headers)
    }

    private fun <T> authEntity(
        token: String,
        body: T,
    ): HttpEntity<T> {
        val headers = HttpHeaders()
        headers.setBearerAuth(token)
        return HttpEntity(body, headers)
    }

    private fun createEventType(
        token: String,
        title: String = "Consultation",
        duration: Int = 30,
    ): EventTypeResponse {
        val request = CreateEventTypeRequest(title = title, duration = duration)
        return restTemplate.postForEntity("/api/event-types", authEntity(token, request), EventTypeResponse::class.java).body!!
    }

    private fun createBooking(
        eventTypeId: String,
        guestName: String = "Jane Guest",
        guestEmail: String = "jane@example.com",
        durationMinutes: Int = 30,
        hoursAhead: Long = 2,
    ): BookingResponse {
        val start = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(hoursAhead, ChronoUnit.HOURS)
        val end = start.plus(durationMinutes.toLong(), ChronoUnit.MINUTES)
        val request =
            CreateBookingRequest(
                eventTypeId = eventTypeId,
                guestName = guestName,
                guestEmail = guestEmail,
                startTime = start.toString(),
                endTime = end.toString(),
            )
        return restTemplate.postForEntity("/api/bookings", HttpEntity(request), BookingResponse::class.java).body!!
    }

    private fun createPastBooking(
        eventTypeId: String,
        durationMinutes: Int = 30,
    ): BookingResponse {
        val start = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(2, ChronoUnit.HOURS)
        val end = start.plus(durationMinutes.toLong(), ChronoUnit.MINUTES)
        val request =
            CreateBookingRequest(
                eventTypeId = eventTypeId,
                guestName = "Past Guest",
                guestEmail = "past@example.com",
                startTime = start.toString(),
                endTime = end.toString(),
            )
        return restTemplate.postForEntity("/api/bookings", HttpEntity(request), BookingResponse::class.java).body!!
    }

    @Test
    fun `create booking returns 201 with cancelToken and sends emails`() {
        val token = registerAndGetToken("create-booking@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id, guestEmail = "guest-create@example.com")

        assertEquals("CONFIRMED", booking.status)
        assertNotNull(booking.cancelToken)
        assertTrue(booking.cancelToken.isNotBlank())

        val bodyJson = mapper.writeValueAsString(booking)
        ContractValidator.validateResponse("/api/bookings", Request.Method.POST, 201, bodyJson)

        verify(emailService).sendBookingConfirmationToGuest(any())
        verify(emailService).sendBookingNotificationToOrganizer(any())
    }

    @Test
    fun `list upcoming bookings returns owner bookings`() {
        val token = registerAndGetToken("list-upcoming@example.com")
        val eventType = createEventType(token)
        createBooking(eventType.id, guestEmail = "g1@example.com")

        val response =
            restTemplate.exchange(
                "/api/bookings",
                HttpMethod.GET,
                authHeaders(token),
                Array<BookingResponse>::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        assertTrue(body.any { it.guestEmail == "g1@example.com" })

        val bodyJson = mapper.writeValueAsString(body.toList())
        ContractValidator.validateResponse("/api/bookings", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `list bookings with past filter returns past bookings`() {
        val token = registerAndGetToken("list-past@example.com")
        val eventType = createEventType(token)
        createPastBooking(eventType.id)

        val response =
            restTemplate.exchange(
                "/api/bookings?status=past",
                HttpMethod.GET,
                authHeaders(token),
                Array<BookingResponse>::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body!!
        assertTrue(body.any { it.guestEmail == "past@example.com" })
    }

    @Test
    fun `list bookings without token returns 401`() {
        val response = restTemplate.getForEntity("/api/bookings", Map::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        ContractValidator.validateResponse("/api/bookings", Request.Method.GET, 401)
    }

    @Test
    fun `owner cancels booking returns 200 and deletes google event`() {
        val token = registerAndGetToken("owner-cancel@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id)

        val response =
            restTemplate.exchange(
                "/api/bookings/${booking.id}",
                HttpMethod.DELETE,
                authHeaders(token),
                BookingResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("CANCELLED", response.body!!.status)

        val bodyJson = mapper.writeValueAsString(response.body)
        ContractValidator.validateResponse("/api/bookings/{id}", Request.Method.DELETE, 200, bodyJson)

        verify(googleCalendarService).deleteEvent(any(), any())
        verify(emailService).sendCancellationToGuest(any())
        verify(emailService).sendCancellationToOrganizer(any())
    }

    @Test
    fun `guest cancels booking with cancel token returns 200`() {
        val token = registerAndGetToken("guest-cancel@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id, guestEmail = "guestcancel@example.com")

        val response =
            restTemplate.exchange(
                "/api/bookings/${booking.id}?token=${booking.cancelToken}",
                HttpMethod.DELETE,
                HttpEntity<Any>(HttpHeaders()),
                BookingResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("CANCELLED", response.body!!.status)
    }

    @Test
    fun `cancel without auth or token returns 401`() {
        val token = registerAndGetToken("no-auth-cancel@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id)

        val response =
            restTemplate.exchange(
                "/api/bookings/${booking.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(HttpHeaders()),
                Map::class.java,
            )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        val bodyJson = mapper.writeValueAsString(response.body)
        ContractValidator.validateResponse("/api/bookings/{id}", Request.Method.DELETE, 401, bodyJson)
    }

    @Test
    fun `cancel already cancelled booking returns 409`() {
        val token = registerAndGetToken("double-cancel@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id)

        restTemplate.exchange("/api/bookings/${booking.id}", HttpMethod.DELETE, authHeaders(token), BookingResponse::class.java)

        val response =
            restTemplate.exchange(
                "/api/bookings/${booking.id}",
                HttpMethod.DELETE,
                authHeaders(token),
                Map::class.java,
            )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val bodyJson = mapper.writeValueAsString(response.body)
        ContractValidator.validateResponse("/api/bookings/{id}", Request.Method.DELETE, 409, bodyJson)
    }

    @Test
    fun `cross-user cancel returns 401`() {
        val token1 = registerAndGetToken("owner-isolation@example.com")
        val eventType = createEventType(token1)
        val booking = createBooking(eventType.id)

        val token2 = registerAndGetToken("intruder@example.com")
        val response =
            restTemplate.exchange(
                "/api/bookings/${booking.id}",
                HttpMethod.DELETE,
                authHeaders(token2),
                Map::class.java,
            )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `cancel non-existent booking returns 404`() {
        val token = registerAndGetToken("notfound-cancel@example.com")
        val response =
            restTemplate.exchange(
                "/api/bookings/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                authHeaders(token),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val bodyJson = mapper.writeValueAsString(response.body)
        ContractValidator.validateResponse("/api/bookings/{id}", Request.Method.DELETE, 404, bodyJson)
    }

    @Test
    fun `failed cancel does not send cancellation emails`() {
        val token = registerAndGetToken("no-email-cancel@example.com")
        val eventType = createEventType(token)
        val booking = createBooking(eventType.id)

        Mockito.clearInvocations(emailService)
        restTemplate.exchange(
            "/api/bookings/${booking.id}",
            HttpMethod.DELETE,
            HttpEntity<Any>(HttpHeaders()),
            Map::class.java,
        )

        verifyNoInteractions(emailService)
    }

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()
}
