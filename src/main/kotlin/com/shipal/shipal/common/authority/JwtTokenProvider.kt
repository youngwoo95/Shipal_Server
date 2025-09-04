package com.shipal.shipal.common.authority

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.Authentication

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Date

const val EXPIRATION_MILLISECONDS: Long = 1000 * 60 * 30 // 30분

@Component
class JwtTokenProvider {
    @Value("\${jwt.secret}")
    lateinit var secretKey: String

    private val key by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))
    }

    /*
    * Token 생성
    * */
    fun createToken(authentication: Authentication) : TokenInfo{
        val authorities: String = authentication
            .authorities
            .joinToString(",", transform= GrantedAuthority::getAuthority)

        val now = Date()
        val accessExpiration = Date(now.time + EXPIRATION_MILLISECONDS)

        val accessToken = Jwts.builder()
            .subject(authentication.name)
            .claim("auth", authorities)
            .issuedAt(now)
            .expiration(accessExpiration)
            .signWith(key)                     // 0.12+ 권장: 키에서 알고리즘 추론(HS256 계열)
            .compact()

        // Access Token
        /*
        val accessToken = Jwts
            .builder()
            .setSubject(authentication.name)
            .claim("auth",authorities)
            .setIssuedAt(now)
            .setExpiration(accessExpiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
        */

        return TokenInfo("Bearer", accessToken)
    }

    /*
    * Token 정보 추출
    * */
    fun getAuthentication(token: String) : Authentication{
        val claims: Claims = getClaims(token)

        val auth = claims["auth"] ?: throw RuntimeException("잘못된 토큰입니다.")

        // 권한 정보 추출
        val authorities: Collection<GrantedAuthority> = (auth as String)
            .split(",")
            .map { SimpleGrantedAuthority(it) }

        val principal: UserDetails = User(claims.subject, "", authorities)

        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    /*
    * Token 검증
    * */
    fun validateToken(token: String) : Boolean{
        try{
            getClaims(token)
            return true
        }catch (e: Exception){
            when(e) {
                is SecurityException -> {} // Invalid JWT Token
                is MalformedJwtException -> {} // Invalid JWT Token
                is ExpiredJwtException -> {} // Expired JWT Token
                is UnsupportedJwtException -> {} // Unsupported JWT Token
                is IllegalArgumentException -> {} // JWT claims string is empty
                else -> {} // else
             }
            println(e.message)
        }
        return false
    }

    private fun getClaims(token: String) : Claims =
        Jwts.parser()                // 0.12+에서는 parser() 사용
            .verifyWith(key)         // key: java.security.Key (HMAC의 경우 Keys.hmacShaKeyFor(...))
            .build()
            .parseSignedClaims(token) // 예전의 parseClaimsJws(token)
            .payload

}