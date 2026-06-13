package com.aifordev.eventtype

import com.aifordev.contract.ContractValidator
import com.aifordev.dto.AuthResponse
import com.aifordev.dto.CreateEventTypeRequest
import com.aifordev.dto.EventTypeResponse
import com.aifordev.dto.RegisterRequest
import com.aifordev.dto.UpdateEventTypeRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.openapi4j.operation.validator.model.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
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
        "spring.mail.host=localhost",
    ],
)
class EventTypeIntegrationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapper =
        ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

    private fun registerAndGetToken(email: String = "eventtype@example.com"): String {
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

    @Test
    fun `create event type with valid data returns 201`() {
        val token = registerAndGetToken("create-et@example.com")
        val request =
            CreateEventTypeRequest(
                title = "Consultation",
                description = "30-minute consultation call",
                duration = 30,
                bufferBefore = 5,
                bufferAfter = 10,
            )
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/event-types", entity, EventTypeResponse::class.java)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("Consultation", body.title)
        assertEquals("30-minute consultation call", body.description)
        assertEquals("consultation", body.slug)
        assertEquals(30, body.duration)
        assertEquals(5, body.bufferBefore)
        assertEquals(10, body.bufferAfter)

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/event-types", Request.Method.POST, 201, bodyJson)
    }

    @Test
    fun `create event type generates slug from title`() {
        val token = registerAndGetToken("slug@example.com")
        val request =
            CreateEventTypeRequest(
                title = "My Special Event!",
                duration = 15,
            )
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/event-types", entity, EventTypeResponse::class.java)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("my-special-event", response.body!!.slug)
    }

    @Test
    fun `create event type with duplicate title appends suffix to slug`() {
        val token = registerAndGetToken("dup-slug@example.com")
        val request1 = CreateEventTypeRequest(title = "Meeting", duration = 30)
        restTemplate.postForEntity("/api/event-types", authEntity(token, request1), EventTypeResponse::class.java)

        val request2 = CreateEventTypeRequest(title = "Meeting", duration = 60)
        val response = restTemplate.postForEntity("/api/event-types", authEntity(token, request2), EventTypeResponse::class.java)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("meeting-1", response.body!!.slug)
    }

    @Test
    fun `list event types returns created types`() {
        val token = registerAndGetToken("list-et@example.com")
        val request1 = CreateEventTypeRequest(title = "Type A", duration = 15)
        restTemplate.postForEntity("/api/event-types", authEntity(token, request1), EventTypeResponse::class.java)
        val request2 = CreateEventTypeRequest(title = "Type B", duration = 30)
        restTemplate.postForEntity("/api/event-types", authEntity(token, request2), EventTypeResponse::class.java)

        val response = restTemplate.exchange("/api/event-types", HttpMethod.GET, authHeaders(token), Array<EventTypeResponse>::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body.size >= 2)
        val bodyJson = mapper.writeValueAsString(body.toList())
        ContractValidator.validateResponse("/api/event-types", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `get event type by id returns 200`() {
        val token = registerAndGetToken("get-et@example.com")
        val createRequest = CreateEventTypeRequest(title = "Quick Call", duration = 10)
        val created = restTemplate.postForEntity("/api/event-types", authEntity(token, createRequest), EventTypeResponse::class.java)
        val eventTypeId = created.body!!.id

        val response =
            restTemplate.exchange(
                "/api/event-types/$eventTypeId",
                HttpMethod.GET,
                authHeaders(token),
                EventTypeResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Quick Call", response.body!!.title)
        val bodyJson = mapper.writeValueAsString(response.body)
        ContractValidator.validateResponse("/api/event-types/{id}", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `update event type returns updated data`() {
        val token = registerAndGetToken("update-et@example.com")
        val createRequest = CreateEventTypeRequest(title = "Old Title", duration = 15)
        val created = restTemplate.postForEntity("/api/event-types", authEntity(token, createRequest), EventTypeResponse::class.java)
        val eventTypeId = created.body!!.id

        val updateRequest =
            UpdateEventTypeRequest(
                title = "New Title",
                duration = 45,
                bufferBefore = 5,
                bufferAfter = 5,
            )
        val response =
            restTemplate.exchange(
                "/api/event-types/$eventTypeId",
                HttpMethod.PUT,
                authEntity(token, updateRequest),
                EventTypeResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("New Title", body.title)
        assertEquals("new-title", body.slug)
        assertEquals(45, body.duration)
        assertEquals(5, body.bufferBefore)
        assertEquals(5, body.bufferAfter)

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/event-types/{id}", Request.Method.PUT, 200, bodyJson)
    }

    @Test
    fun `update event type slug when title changes`() {
        val token = registerAndGetToken("update-slug@example.com")
        val createRequest = CreateEventTypeRequest(title = "Old Title", duration = 15)
        val created = restTemplate.postForEntity("/api/event-types", authEntity(token, createRequest), EventTypeResponse::class.java)
        val eventTypeId = created.body!!.id
        assertEquals("old-title", created.body!!.slug)

        val updateRequest = UpdateEventTypeRequest(title = "New Title")
        val response =
            restTemplate.exchange(
                "/api/event-types/$eventTypeId",
                HttpMethod.PUT,
                authEntity(token, updateRequest),
                EventTypeResponse::class.java,
            )

        assertEquals("new-title", response.body!!.slug)
    }

    @Test
    fun `delete event type returns 204`() {
        val token = registerAndGetToken("delete-et@example.com")
        val createRequest = CreateEventTypeRequest(title = "ToDelete", duration = 10)
        val created = restTemplate.postForEntity("/api/event-types", authEntity(token, createRequest), EventTypeResponse::class.java)
        val eventTypeId = created.body!!.id

        val response =
            restTemplate.exchange(
                "/api/event-types/$eventTypeId",
                HttpMethod.DELETE,
                authHeaders(token),
                Void::class.java,
            )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        ContractValidator.validateResponse("/api/event-types/{id}", Request.Method.DELETE, 204)
    }

    @Test
    fun `get event types without token returns 401`() {
        val response = restTemplate.getForEntity("/api/event-types", Map::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        ContractValidator.validateResponse("/api/event-types", Request.Method.GET, 401)
    }

    @Test
    fun `get non-existent event type returns 404`() {
        val token = registerAndGetToken("notfound-et@example.com")
        val response =
            restTemplate.exchange(
                "/api/event-types/00000000-0000-0000-0000-000000000000",
                HttpMethod.GET,
                authHeaders(token),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/event-types/{id}", Request.Method.GET, 404, bodyJson)
    }

    @Test
    fun `update another user event type returns 404`() {
        val token1 = registerAndGetToken("owner-et@example.com")
        val createRequest = CreateEventTypeRequest(title = "Mine", duration = 30)
        val created = restTemplate.postForEntity("/api/event-types", authEntity(token1, createRequest), EventTypeResponse::class.java)
        val eventTypeId = created.body!!.id

        val token2 = registerAndGetToken("other-et@example.com")
        val updateRequest = UpdateEventTypeRequest(title = "Stolen")
        val response =
            restTemplate.exchange(
                "/api/event-types/$eventTypeId",
                HttpMethod.PUT,
                authEntity(token2, updateRequest),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/event-types/{id}", Request.Method.PUT, 404, bodyJson)
    }

    @Test
    fun `create event type with missing title returns 400`() {
        val token = registerAndGetToken("bad-req@example.com")
        val request = CreateEventTypeRequest(title = "", duration = 30)
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/event-types", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/event-types", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `list event types cross-user isolation`() {
        val token1 = registerAndGetToken("isolated1@example.com")
        val request1 = CreateEventTypeRequest(title = "User 1 Event", duration = 15)
        restTemplate.postForEntity("/api/event-types", authEntity(token1, request1), EventTypeResponse::class.java)

        val token2 = registerAndGetToken("isolated2@example.com")
        val response = restTemplate.exchange("/api/event-types", HttpMethod.GET, authHeaders(token2), Array<EventTypeResponse>::class.java)

        val body = response.body
        assertNotNull(body)
        assertTrue(body.none { it.title == "User 1 Event" })
    }
}
