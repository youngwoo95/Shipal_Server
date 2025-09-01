package com.shipal.shipal.common.dto

data class UserDtoRequest
(
    val loginId : String,
    val loginPw : String,
    val phone : String,
    val name : String,
    val address : String,
    val nickName : String?
)