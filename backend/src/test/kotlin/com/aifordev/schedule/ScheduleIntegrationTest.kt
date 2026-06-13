package com.aifordev.schedule

import com.aifordev.contract.ContractValidator
import com.aifordev.dto.AuthResponse
import com.aifordev.dto.AvailabilitySlot
import com.aifordev.dto.CreateScheduleRequest
import com.aifordev.dto.RegisterRequest
import com.aifordev.dto.ScheduleResponse
import com.aifordev.dto.UpdateScheduleRequest
import com.fasterxml.jackson.databind.ObjectMapper
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
    ],
)
class ScheduleIntegrationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapper = ObjectMapper()

    private fun registerAndGetToken(email: String = "schedule@example.com"): String {
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
    fun `create schedule with valid data returns 201`() {
        val token = registerAndGetToken("create@example.com")
        val request =
            CreateScheduleRequest(
                name = "Work",
                timezone = "Europe/Moscow",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "09:00", endTime = "18:00"),
                        AvailabilitySlot(dayOfWeek = "TUE", startTime = "09:00", endTime = "18:00"),
                    ),
            )
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/schedules", entity, ScheduleResponse::class.java)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("Work", body.name)
        assertEquals("Europe/Moscow", body.timezone)
        assertEquals(2, body.availabilities.size)

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules", Request.Method.POST, 201, bodyJson)
    }

    @Test
    fun `get schedules returns default schedule for new user`() {
        val token = registerAndGetToken("default@example.com")
        val entity = authHeaders(token)

        val response = restTemplate.exchange("/api/schedules", HttpMethod.GET, entity, Array<ScheduleResponse>::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals(1, body.size)
        val schedule = body[0]
        assertEquals("Default", schedule.name)
        assertEquals("UTC", schedule.timezone)
        assertEquals(5, schedule.availabilities.size)

        val bodyJson = mapper.writeValueAsString(body.asList())
        ContractValidator.validateResponse("/api/schedules", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `get schedules returns created schedules`() {
        val token = registerAndGetToken("list@example.com")
        val request =
            CreateScheduleRequest(
                name = "Morning",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "06:00", endTime = "12:00"),
                    ),
            )
        restTemplate.postForEntity("/api/schedules", authEntity(token, request), ScheduleResponse::class.java)

        val response = restTemplate.exchange("/api/schedules", HttpMethod.GET, authHeaders(token), Array<ScheduleResponse>::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body.size >= 1)
        val bodyJson = mapper.writeValueAsString(body.asList())
        ContractValidator.validateResponse("/api/schedules", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `update schedule returns updated data`() {
        val token = registerAndGetToken("update@example.com")
        val createRequest =
            CreateScheduleRequest(
                name = "Old",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "09:00", endTime = "17:00"),
                    ),
            )
        val created = restTemplate.postForEntity("/api/schedules", authEntity(token, createRequest), ScheduleResponse::class.java)
        val scheduleId = created.body!!.id

        val updateRequest =
            UpdateScheduleRequest(
                name = "Updated",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "TUE", startTime = "10:00", endTime = "16:00"),
                    ),
            )
        val response =
            restTemplate.exchange(
                "/api/schedules/$scheduleId",
                HttpMethod.PUT,
                authEntity(token, updateRequest),
                ScheduleResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("Updated", body.name)
        assertEquals("UTC", body.timezone)
        assertEquals(1, body.availabilities.size)
        assertEquals("TUE", body.availabilities[0].dayOfWeek)

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules/{id}", Request.Method.PUT, 200, bodyJson)
    }

    @Test
    fun `delete schedule returns 204`() {
        val token = registerAndGetToken("delete@example.com")
        val createRequest =
            CreateScheduleRequest(
                name = "ToDelete",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "FRI", startTime = "13:00", endTime = "17:00"),
                    ),
            )
        val created = restTemplate.postForEntity("/api/schedules", authEntity(token, createRequest), ScheduleResponse::class.java)
        val scheduleId = created.body!!.id

        val response =
            restTemplate.exchange(
                "/api/schedules/$scheduleId",
                HttpMethod.DELETE,
                authHeaders(token),
                Void::class.java,
            )

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        ContractValidator.validateResponse("/api/schedules/{id}", Request.Method.DELETE, 204)
    }

    @Test
    fun `create schedule with overlapping windows returns 400`() {
        val token = registerAndGetToken("overlap@example.com")
        val request =
            CreateScheduleRequest(
                name = "Overlap",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "09:00", endTime = "14:00"),
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "13:00", endTime = "18:00"),
                    ),
            )
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/schedules", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `create schedule with start time after end time returns 400`() {
        val token = registerAndGetToken("invalid@example.com")
        val request =
            CreateScheduleRequest(
                name = "Invalid",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "WED", startTime = "18:00", endTime = "09:00"),
                    ),
            )
        val entity = authEntity(token, request)

        val response = restTemplate.postForEntity("/api/schedules", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `get schedules without token returns 401`() {
        val response = restTemplate.getForEntity("/api/schedules", Map::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        ContractValidator.validateResponse("/api/schedules", Request.Method.GET, 401)
    }

    @Test
    fun `delete non-existent schedule returns 404`() {
        val token = registerAndGetToken("notfound@example.com")
        val response =
            restTemplate.exchange(
                "/api/schedules/00000000-0000-0000-0000-000000000000",
                HttpMethod.DELETE,
                authHeaders(token),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules/{id}", Request.Method.DELETE, 404, bodyJson)
    }

    @Test
    fun `delete another user schedule returns 404`() {
        val token1 = registerAndGetToken("owner@example.com")
        val createRequest =
            CreateScheduleRequest(
                name = "Mine",
                timezone = "UTC",
                availabilities =
                    listOf(
                        AvailabilitySlot(dayOfWeek = "MON", startTime = "09:00", endTime = "18:00"),
                    ),
            )
        val created = restTemplate.postForEntity("/api/schedules", authEntity(token1, createRequest), ScheduleResponse::class.java)
        val scheduleId = created.body!!.id

        val token2 = registerAndGetToken("other@example.com")
        val response =
            restTemplate.exchange(
                "/api/schedules/$scheduleId",
                HttpMethod.DELETE,
                authHeaders(token2),
                Map::class.java,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/schedules/{id}", Request.Method.DELETE, 404, bodyJson)
    }
}
