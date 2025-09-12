package com.shipal.shipal.VO

data class UserVO (
    val userSeq: Int, /* 유저 시퀀스 */
    val loginId: String, /* 로그인 아이디 */
    val phone: String, /* 전화번호 */
    val name: String, /* 이름 */
    val address: String, /* 주소 */
    val nickname: String?, /* 닉네임 */
    var images: String? /* 이미지 */
)