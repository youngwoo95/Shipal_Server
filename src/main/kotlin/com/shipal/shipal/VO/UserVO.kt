package com.shipal.shipal.VO

data class UserVO (
    /*
    * 유저 시퀀스
    * */
    val userSeq: Int,

    /*
    * 로그인 아이디
    * */
    val loginId: String,

    /*
    * 전화번호
    * */
    val phone: String,

    /*
    * 이름
    * */
    val name: String,

    /*
    * 주소
    * */
    val address: String,

    /*
    * 닉네임
    * */
    val nickname: String?,

    /*
    * 이미지
    * */
    var images: String?
)