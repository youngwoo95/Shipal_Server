package com.shipal.shipal

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity) : SecurityFilterChain{
        http
            .csrf { it.disable() } // REST API면 보통 끔
            .cors { }              // CORS 필요 시 설정
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // 회원가입은 무조건 허용
                it.requestMatchers(HttpMethod.POST, "/api/v1/user/addUser").permitAll()

                // 개발 단계: 나머지도 일단 허용
                it.anyRequest().permitAll()
                // 운영에서 인증 걸 때만 ↓로 바꾸세요.
                // it.anyRequest().authenticated()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

        return http.build()

    }
}