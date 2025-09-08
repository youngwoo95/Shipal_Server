package com.shipal.shipal.Dto.User

data class RefreshDto (
    val userSeq: Int,
    val uuid: String,
    val refreshToken: String
)