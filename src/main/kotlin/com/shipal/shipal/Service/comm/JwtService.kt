package com.shipal.shipal.Service.comm

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import java.util.Date
import java.util.concurrent.TimeUnit

@Service
class JwtService (
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.issuer}") private val issuer: String,
    @Value("\${jwt.access-exp-minutes:30}") private val accessExpMinutes: Long
)
{
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        // 키 준비: BASE64 인 경우 decoding, 아니면 UTF-8 바이트 사용
        val rawBytes = try {
            // BASE64 형태가 아니면 IllegalArgumentException 발생
            Decoders.BASE64.decode(secret)
        } catch (_: IllegalArgumentException) {
            secret.toByteArray(StandardCharsets.UTF_8)
        }

        // HS256은 256비트(=32바이트) 이상 필요. 미만이면 명확한 에러 메시지 제공
        require(rawBytes.size >= 32) {
            "jwt.secret must be at least 32 bytes (256 bits). Current: ${rawBytes.size} bytes"
        }

        key = Keys.hmacShaKeyFor(rawBytes)
    }

    fun generateAccessToken(
        subject: String,
        roles: List<String> = emptyList(),
        extra: Map<String, Any> = emptyMap()
    ): String {
        val now = Date()
        val exp = Date(now.time + TimeUnit.MINUTES.toMillis(accessExpMinutes))

        val builder = Jwts.builder()
            .subject(subject)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(exp)
            .claim("roles", roles)

        extra.forEach { (k, v) -> builder.claim(k, v) }

        // 알고리즘은 키 타입(HMAC)에서 자동 추론
        return builder.signWith(key).compact()
    }

    fun parse(token: String): Jws<Claims> =
        Jwts.parser()                      // 0.12+ 스타일
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
}
