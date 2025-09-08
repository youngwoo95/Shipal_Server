package com.shipal.shipal.Dto.User

data class ResponseTokenDto (
    val accessToken: String,
    val refreshToken: String,
    val uuid: String
)