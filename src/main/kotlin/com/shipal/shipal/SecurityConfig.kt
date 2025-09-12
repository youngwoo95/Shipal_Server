package com.shipal.shipal

import com.shipal.shipal.Service.comm.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity) : SecurityFilterChain{
        http
            .csrf { it.disable() } // REST API면 보통 끔
            .cors { }              // CORS 필요 시 설정
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/images/**"
                ).permitAll()
                // 회원가입은 무조건 허용
                it.requestMatchers(HttpMethod.POST, "/api/v1/user/addUser").permitAll()
                // 토큰 발급은 허용 (테스트/개발용)
                it.requestMatchers(HttpMethod.POST, "/api/v1/user/login").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/user/refresh").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/user/profile").permitAll()

                //it.requestMatchers(HttpMethod.POST, "/api/v1/user/logout").permitAll()

                // 그 외 요청은 인증 필요
                it.anyRequest().authenticated()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()

    }
}


