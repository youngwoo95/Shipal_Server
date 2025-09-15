package com.shipal.shipal.Model

import java.time.LocalDateTime

data class UserInfo (
    var userSeq: Int? = null,
    val loginId: String,
    var phone: String,
    var loginPw: String,
    var name: String,
    var nickname: String? = null,
    var createDt: LocalDateTime,
    var attach: String? = null
)