package com.aifordev.auth

import com.aifordev.contract.ContractValidator
import com.aifordev.dto.AuthResponse
import com.aifordev.dto.LoginRequest
import com.aifordev.dto.RefreshRequest
import com.aifordev.dto.RegisterRequest
import com.aifordev.dto.TokenResponse
import com.aifordev.dto.UserResponse
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
        "spring.mail.host=localhost",
    ],
)
class AuthIntegrationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private val mapper = ObjectMapper()

    @Test
    fun `register with valid data returns tokens and user`() {
        val request = RegisterRequest(email = "test@example.com", password = "password123")
        val entity = HttpEntity(request)

        val response = restTemplate.postForEntity("/api/auth/register", entity, AuthResponse::class.java)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body.accessToken.isNotBlank())
        assertTrue(body.refreshToken.isNotBlank())
        assertEquals("test@example.com", body.user.email)
        assertTrue(body.user.id.isNotBlank())

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 201, bodyJson)
    }

    @Test
    fun `register with duplicate email returns bad request`() {
        val request = RegisterRequest(email = "duplicate@example.com", password = "password123")
        val entity = HttpEntity(request)

        restTemplate.postForEntity("/api/auth/register", entity, AuthResponse::class.java)
        val response = restTemplate.postForEntity("/api/auth/register", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `register with invalid email returns bad request`() {
        val request = RegisterRequest(email = "not-an-email", password = "password123")
        val entity = HttpEntity(request)

        val response = restTemplate.postForEntity("/api/auth/register", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `register with short password returns bad request`() {
        val request = RegisterRequest(email = "test@example.com", password = "123")
        val entity = HttpEntity(request)

        val response = restTemplate.postForEntity("/api/auth/register", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `login with valid credentials returns tokens`() {
        val registerRequest = RegisterRequest(email = "login@example.com", password = "password123")
        restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), AuthResponse::class.java)

        val loginRequest = LoginRequest(email = "login@example.com", password = "password123")
        val entity = HttpEntity(loginRequest)

        val response = restTemplate.postForEntity("/api/auth/login", entity, AuthResponse::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body.accessToken.isNotBlank())
        assertTrue(body.refreshToken.isNotBlank())
        assertEquals("login@example.com", body.user.email)

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/login", Request.Method.POST, 200, bodyJson)
    }

    @Test
    fun `login with wrong password returns bad request`() {
        val registerRequest = RegisterRequest(email = "wrongpwd@example.com", password = "password123")
        restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), AuthResponse::class.java)

        val loginRequest = LoginRequest(email = "wrongpwd@example.com", password = "wrongpassword")
        val entity = HttpEntity(loginRequest)

        val response = restTemplate.postForEntity("/api/auth/login", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/login", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `login with non-existent email returns bad request`() {
        val loginRequest = LoginRequest(email = "nobody@example.com", password = "password123")
        val entity = HttpEntity(loginRequest)

        val response = restTemplate.postForEntity("/api/auth/login", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/login", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `refresh with valid token returns new access token`() {
        val registerRequest = RegisterRequest(email = "refresh@example.com", password = "password123")
        val registerResponse = restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), AuthResponse::class.java)
        val refreshToken = registerResponse.body!!.refreshToken

        val refreshRequest = RefreshRequest(refreshToken = refreshToken)
        val entity = HttpEntity(refreshRequest)

        val response = restTemplate.postForEntity("/api/auth/refresh", entity, TokenResponse::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertTrue(body.accessToken.isNotBlank())

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/refresh", Request.Method.POST, 200, bodyJson)
    }

    @Test
    fun `refresh with invalid token returns bad request`() {
        val refreshRequest = RefreshRequest(refreshToken = "invalid-token")
        val entity = HttpEntity(refreshRequest)

        val response = restTemplate.postForEntity("/api/auth/refresh", entity, Map::class.java)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body
        assertNotNull(body)
        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/auth/refresh", Request.Method.POST, 400, bodyJson)
    }

    @Test
    fun `get me with valid token returns user profile`() {
        val registerRequest = RegisterRequest(email = "me@example.com", password = "password123")
        val registerResponse = restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), AuthResponse::class.java)
        val accessToken = registerResponse.body!!.accessToken

        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        val entity = HttpEntity<Any>(headers)

        val response = restTemplate.exchange("/api/me", HttpMethod.GET, entity, UserResponse::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val body = response.body
        assertNotNull(body)
        assertEquals("me@example.com", body.email)
        assertTrue(body.id.isNotBlank())

        val bodyJson = mapper.writeValueAsString(body)
        ContractValidator.validateResponse("/api/me", Request.Method.GET, 200, bodyJson)
    }

    @Test
    fun `get me without token returns unauthorized`() {
        val response = restTemplate.getForEntity("/api/me", Map::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        ContractValidator.validateResponse("/api/me", Request.Method.GET, 401)
    }

    @Test
    fun `get me with invalid token returns unauthorized`() {
        val headers = HttpHeaders()
        headers.setBearerAuth("invalid-token")
        val entity = HttpEntity<Any>(headers)

        val response = restTemplate.exchange("/api/me", HttpMethod.GET, entity, Map::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        ContractValidator.validateResponse("/api/me", Request.Method.GET, 401)
    }
}
