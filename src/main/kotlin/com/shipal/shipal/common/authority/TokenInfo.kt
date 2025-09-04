package com.shipal.shipal.common.authority

data class TokenInfo (
    val grantType: String,
    val accessToken: String,

    /* 리프레쉬 토큰 만들어야함. */
)
