package com.shipal.shipal.auth

import com.shipal.shipal.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    fun login(username: String, password: String): TokenResponse {
        val authRequest = UsernamePasswordAuthenticationToken(username, password)
        val authResult: Authentication = authenticationManager.authenticate(authRequest)

        SecurityContextHolder.getContext().authentication = authResult

        val principal = authResult.principal
        val roles: List<String> = when (principal) {
            is UserDetails -> principal.authorities.map(GrantedAuthority::getAuthority)
            else -> authResult.authorities.map(GrantedAuthority::getAuthority)
        }

        val token = jwtTokenProvider.generateToken(username, roles)
        return TokenResponse(accessToken = token, tokenType = "Bearer")
    }
}

