package com.shipal.shipal.auth

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)

