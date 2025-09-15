package com.shipal.shipal.Model

import java.time.LocalDateTime

data class UserInfo (
    /* USER PK */
    var userSeq: Int? = null,

    /* 로그인 아이디 */
    val loginId: String,

    /* 전화번호 */
    var phone: String,

    /* 로그인 비밀번호 */
    var loginPw: String,

    /* 이름 */
    var name: String,

    /* 닉네임 */
    var nickname: String? = null,

    /* 생성시간 */
    var createDt: LocalDateTime,

    /* 이미지 첨부파일 */
    var attach: String? = null
)