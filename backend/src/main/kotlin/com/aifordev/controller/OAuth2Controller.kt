package com.aifordev.controller

import com.aifordev.config.GoogleOAuth2Properties
import com.aifordev.service.OAuth2Service
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/auth/oauth2")
class OAuth2Controller(
    private val oauth2Service: OAuth2Service,
    private val googleProperties: GoogleOAuth2Properties,
) {
    @GetMapping("/google")
    fun googleAuth(response: HttpServletResponse) {
        val authUrl = oauth2Service.buildAuthorizationUrl()
        response.sendRedirect(authUrl)
    }

    @GetMapping("/google/callback")
    fun googleCallback(
        @RequestParam code: String,
        response: HttpServletResponse,
    ) {
        val authResponse = oauth2Service.processCallback(code)
        val redirectUrl =
            "${googleProperties.frontendUrl}/auth/callback" +
                "?accessToken=${urlEncode(authResponse.accessToken)}" +
                "&refreshToken=${urlEncode(authResponse.refreshToken)}" +
                "&email=${urlEncode(authResponse.user.email)}"
        response.sendRedirect(redirectUrl)
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
