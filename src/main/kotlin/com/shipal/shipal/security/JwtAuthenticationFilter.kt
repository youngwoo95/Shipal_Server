package com.shipal.shipal.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        val token = header?.takeIf { it.startsWith("Bearer ") }?.substring(7)

        if (!token.isNullOrBlank()) {
            try {
                val claims = jwtTokenProvider.validateAndGetClaims(token)

                // 사용자 식별자 (subject)와 권한을 Claims에서 읽습니다. 필요에 맞게 조정하세요.
                val username = claims.subject ?: "anonymous"
                val roles = (claims["roles"] as? Collection<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val authorities = roles.map { SimpleGrantedAuthority(it) }

                val authentication = UsernamePasswordAuthenticationToken(username, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (_: Exception) {
                // 토큰이 유효하지 않으면 인증을 세팅하지 않고 통과 → 이후 인증이 필요한 경우 401 처리됨
            }
        }

        filterChain.doFilter(request, response)
    }
}

