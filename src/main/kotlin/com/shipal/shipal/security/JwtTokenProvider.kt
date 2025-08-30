package com.shipal.shipal.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret:dev-secret-please-override-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ}")
    private val secret: String,
    @Value("\${jwt.exp-minutes:120}")
    private val expMinutes: Long,
)
{
    private val key by lazy {
        // HMAC-SHA key from secret. Use at least 256-bit (32+ chars) secret in production.
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun validateAndGetClaims(token: String): Claims {
        val jws = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
        return jws.payload
    }

    fun generateToken(subject: String, roles: Collection<String> = emptyList()): String {
        val now = Date()
        val expiry = Date(now.time + expMinutes * 60_000)

        return Jwts.builder()
            .subject(subject)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }
}
