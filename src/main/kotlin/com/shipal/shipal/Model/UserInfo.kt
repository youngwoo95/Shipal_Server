package com.shipal.shipal.Model

import java.time.LocalDateTime

data class UserInfo (
    var userSeq: Int? = null,
    val loginId: String,
    val phone: String,
    val loginPw: String,
    val name: String,
    val address: String,
    val nickname: String? = null,
    val createDt: LocalDateTime,
    val createUser: String,
    val updateDt : LocalDateTime,
    val updateUser: String,
    val attach: String?
)