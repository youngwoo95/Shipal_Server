package com.shipal.shipal.Service.comm

import org.springframework.security.core.context.SecurityContextHolder

object GetTokenValues{
    fun get() : AuthUser? {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.principal as? AuthUser
    }

}
