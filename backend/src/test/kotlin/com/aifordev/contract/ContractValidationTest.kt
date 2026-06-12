package com.aifordev.contract

import com.aifordev.dto.LoginRequest
import com.aifordev.dto.RefreshRequest
import com.aifordev.dto.RegisterRequest
import org.junit.jupiter.api.Test
import org.openapi4j.operation.validator.model.Request
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.TestPropertySource

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
class ContractValidationTest {
    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `register 201 response matches OpenAPI spec`() {
        val request = RegisterRequest(email = "contract@example.com", password = "password123")
        val response = restTemplate.postForEntity("/api/auth/register", HttpEntity(request), String::class.java)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 201, response.body)
    }

    @Test
    fun `register 400 response matches OpenAPI spec`() {
        val request = RegisterRequest(email = "duplicate@example.com", password = "password123")
        restTemplate.postForEntity("/api/auth/register", HttpEntity(request), String::class.java)
        val response = restTemplate.postForEntity("/api/auth/register", HttpEntity(request), String::class.java)
        ContractValidator.validateResponse("/api/auth/register", Request.Method.POST, 400, response.body)
    }

    @Test
    fun `login 200 response matches OpenAPI spec`() {
        val registerRequest = RegisterRequest(email = "login-contract@example.com", password = "password123")
        restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), String::class.java)

        val loginRequest = LoginRequest(email = "login-contract@example.com", password = "password123")
        val response = restTemplate.postForEntity("/api/auth/login", HttpEntity(loginRequest), String::class.java)
        ContractValidator.validateResponse("/api/auth/login", Request.Method.POST, 200, response.body)
    }

    @Test
    fun `login 400 response matches OpenAPI spec`() {
        val loginRequest = LoginRequest(email = "nobody@example.com", password = "password123")
        val response = restTemplate.postForEntity("/api/auth/login", HttpEntity(loginRequest), String::class.java)
        ContractValidator.validateResponse("/api/auth/login", Request.Method.POST, 400, response.body)
    }

    @Test
    fun `refresh 200 response matches OpenAPI spec`() {
        val registerRequest = RegisterRequest(email = "refresh-contract@example.com", password = "password123")
        val registerResponse = restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), String::class.java)
        val refreshToken = extractJsonField(registerResponse.body!!, "refreshToken")

        val refreshRequest = RefreshRequest(refreshToken = refreshToken)
        val response = restTemplate.postForEntity("/api/auth/refresh", HttpEntity(refreshRequest), String::class.java)
        ContractValidator.validateResponse("/api/auth/refresh", Request.Method.POST, 200, response.body)
    }

    @Test
    fun `refresh 400 response matches OpenAPI spec`() {
        val refreshRequest = RefreshRequest(refreshToken = "invalid-token")
        val response = restTemplate.postForEntity("/api/auth/refresh", HttpEntity(refreshRequest), String::class.java)
        ContractValidator.validateResponse("/api/auth/refresh", Request.Method.POST, 400, response.body)
    }

    @Test
    fun `me 200 response matches OpenAPI spec`() {
        val registerRequest = RegisterRequest(email = "me-contract@example.com", password = "password123")
        val registerResponse = restTemplate.postForEntity("/api/auth/register", HttpEntity(registerRequest), String::class.java)
        val accessToken = extractJsonField(registerResponse.body!!, "accessToken")

        val headers = HttpHeaders()
        headers.setBearerAuth(accessToken)
        val response = restTemplate.exchange("/api/me", HttpMethod.GET, HttpEntity<Any>(headers), String::class.java)
        ContractValidator.validateResponse("/api/me", Request.Method.GET, 200, response.body)
    }

    @Test
    fun `me 401 response matches OpenAPI spec`() {
        val response = restTemplate.exchange("/api/me", HttpMethod.GET, null, String::class.java)
        ContractValidator.validateResponse("/api/me", Request.Method.GET, 401)
    }

    private fun extractJsonField(
        json: String,
        field: String,
    ): String {
        val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: throw IllegalStateException("Field '$field' not found in $json")
    }
}
