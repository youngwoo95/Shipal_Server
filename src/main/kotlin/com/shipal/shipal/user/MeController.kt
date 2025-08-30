package com.shipal.shipal.user

import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class MeController {

    @GetMapping("/me")
    fun me(): ResponseEntity<MeResponse> {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth is AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).build()
        }

        val username = auth.name
        val roles = auth.authorities.map(GrantedAuthority::getAuthority)
        return ResponseEntity.ok(MeResponse(username = username, roles = roles))
    }
}

