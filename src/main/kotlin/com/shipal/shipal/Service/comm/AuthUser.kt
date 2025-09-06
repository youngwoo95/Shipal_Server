package com.shipal.shipal.Service.comm

import java.io.Serializable

data class AuthUser (
    val uId: Int,
    val role: String,
    val name: String
) : Serializable