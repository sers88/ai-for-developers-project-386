package com.aifordev.controller

import com.aifordev.dto.UserResponse
import com.aifordev.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController {
    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(UserResponse(id = principal.userId, email = principal.email))
}
