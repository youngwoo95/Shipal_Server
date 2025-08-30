package com.shipal.shipal.auth

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/v1/login")
    fun login(@Valid @RequestBody body: LoginRequest): ResponseEntity<TokenResponse> {
        val token = authService.login(body.username, body.password)
        return ResponseEntity.ok(token)
    }
}

