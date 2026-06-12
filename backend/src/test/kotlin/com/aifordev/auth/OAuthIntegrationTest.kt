package com.aifordev.auth

import com.aifordev.dto.AuthResponse
import com.aifordev.dto.UserResponse
import com.aifordev.service.OAuth2Service
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.net.HttpURLConnection
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
class OAuthIntegrationTest {
    @Value("\${local.server.port}")
    private var port: Int = 0

    @MockitoBean
    private lateinit var oauth2Service: OAuth2Service

    private lateinit var restTemplate: RestTemplate

    @BeforeEach
    fun setUp() {
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
        restTemplate = RestTemplate(factory)
    }

    private fun get(path: String): ResponseEntity<Void> =
        restTemplate.exchange(
            "http://localhost:$port$path",
            HttpMethod.GET,
            null,
            Void::class.java,
        )

    @Test
    fun `google auth endpoint redirects to Google`() {
        given(oauth2Service.buildAuthorizationUrl()).willReturn(
            "https://accounts.google.com/o/oauth2/v2/auth?client_id=test-client-id&response_type=code&scope=openid+email+profile",
        )

        val response = get("/api/auth/oauth2/google")

        assertTrue(response.statusCode.is3xxRedirection)
        val location = response.headers.location!!
        assertTrue(location.toString().contains("accounts.google.com"))
        assertTrue(location.toString().contains("client_id=test-client-id"))
        assertTrue(location.toString().contains("response_type=code"))
    }

    @Test
    fun `callback creates new user and redirects to frontend with tokens`() {
        given(oauth2Service.processCallback("test-code")).willReturn(
            AuthResponse(
                accessToken = "mock-access-token",
                refreshToken = "mock-refresh-token",
                user = UserResponse(id = "user-123", email = "googleuser@example.com"),
            ),
        )

        val response = get("/api/auth/oauth2/google/callback?code=test-code")

        assertTrue(response.statusCode.is3xxRedirection)
        val location = response.headers.location!!
        assertTrue(location.toString().contains("accessToken="))
        assertTrue(location.toString().contains("refreshToken="))
        assertTrue(location.toString().contains("email="))
    }

    @Test
    fun `callback for existing email user does not create duplicate`() {
        given(oauth2Service.processCallback("test-code")).willReturn(
            AuthResponse(
                accessToken = "at1",
                refreshToken = "rt1",
                user = UserResponse(id = "user-123", email = "googleuser@example.com"),
            ),
        )

        get("/api/auth/oauth2/google/callback?code=test-code")

        given(oauth2Service.processCallback("test-code-2")).willReturn(
            AuthResponse(
                accessToken = "at2",
                refreshToken = "rt2",
                user = UserResponse(id = "user-123", email = "googleuser@example.com"),
            ),
        )

        val secondResponse = get("/api/auth/oauth2/google/callback?code=test-code-2")

        assertTrue(secondResponse.statusCode.is3xxRedirection)
        val location = secondResponse.headers.location!!
        assertTrue(location.toString().contains("accessToken=at2"))
    }
}
