package com.shipal.shipal.Service.comm

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {


    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI.removePrefix(request.contextPath ?: "")
        return uri.startsWith("/images/") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/swagger-ui")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            try {
                val jws = jwtService.parse(token)

                val claims = jws.payload

                val loginId = claims.subject ?: ""

                // 유저 시퀀스
                val uid = when (val raw = claims["uId"]) {
                    is Int -> raw
                    is Long -> raw.toInt()
                    is Number -> raw.toInt()
                    is String -> raw.toIntOrNull() ?: 0
                    else -> 0
                }

                // 유저 이름
                val name = claims["name"] as? String ?: ""

                val role = when(val roles = claims["roles"]){
                    is List<*> -> roles.filterIsInstance<String>().firstOrNull()
                    is Array<*> -> roles.filterIsInstance<String>().firstOrNull()
                    is String -> roles.split(',').map { it.trim() }.firstOrNull()
                    else -> null
                } ?: "USER"

                // 권한 리스트로 생성
                val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

                // principal = AuthUser
                if (loginId.isNotBlank() && SecurityContextHolder.getContext().authentication == null) {
                    val principal = AuthUser(
                        uId = uid,
                        role = role,
                        name = name
                    )
                    val auth = UsernamePasswordAuthenticationToken(principal, null, authorities)
                    SecurityContextHolder.getContext().authentication = auth
                }

            } catch (_: Exception) {
                // 토큰 파싱 실패 시 인증 미설정(요청은 계속 진행)
            }
        }

        filterChain.doFilter(request, response)
    }
}

